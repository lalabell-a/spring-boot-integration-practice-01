import ec.edu.epn.dto.FlightRequest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class FlightControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

     private AirportRequest airportRequest(String name, String code, String city, String country) {
        AirportRequest request = new AirportRequest();
        request.setName(name);
        request.setCode(code);
        request.setCity(city);
        request.setCountry(country);
        return request;
    }

    private FlightRequest flightRequest(String flightNumber, Long departureAirportId, Long arrivalAirportId) {
        FlightRequest request = new FlightRequest();
        request.setFlightNumber(flightNumber);
        request.setDepartureAirportId(departureAirportId);
        request.setArrivalAirportId(arrivalAirportId);
        return request;
    }

    @BeforeEach
    void setUp() {
        airportRequest = airportRequest("Aeropuerto Mariscal Sucre", "UIO", "Quito", "Ecuador");
        airportRequestTwo = airportRequest("Aeropuerto Internacional John F. Kennedy", "NYC", "New York", "United States");
    }

    //shouldCreateFlight — Crear un vuelo (necesitas crear aeropuertos primero en @BeforeEach)
    void shouldCreateFlight() throws Exception {
        //Arrange
        Long destinoAeropuerto = createAirportAndGetId(airportRequest);
        Long llegadaAeropuerto = createAirportAndGetId(airportRequestTwo);
        FlightRequest flightRequest = flightRequest("FL6767", destinoAeropuerto, llegadaAeropuerto);
        flightRequest.setDepartureTime(LocalDateTime.now().plusDays(1));
        flightRequest.setArrivalTime(LocalDateTime.now().plusDays(1).plusHours(2));
        flightRequest.setStatus("PROGRAMADO");

        //Act + Assert
        mockMvc.perform(post("/api/flights")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(flightRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.flightNumber").value("FL6767"))
            .andExpect(jsonPath("$.departureAirport.id").value(destinoAeropuerto))
            .andExpect(jsonPath("$.arrivalAirport.id").value(llegadaAeropuerto))
            .andExpect(jsonPath("$.status").value("PROGRAMADO"))
            .andExpect(jsonPath("$.id").isNumber());
    }

    //shouldRejectDuplicateFlightNumber — Intentar crear dos vuelos con el mismo número
    void shouldRejectDuplicateFlightNumber() throws Exception {
        //Arrange
        Long destinoAeropuerto = createAirportAndGetId(airportRequest);
        Long llegadaAeropuerto = createAirportAndGetId(airportRequestTwo);
        FlightRequest flightRequest = flightRequest("FL6767", destinoAeropuerto, llegadaAeropuerto);
        flightRequest.setDepartureTime(LocalDateTime.now().plusDays(1));
        flightRequest.setArrivalTime(LocalDateTime.now().plusDays(1).plusHours(2));
        flightRequest.setStatus("PROGRAMADO");

        //Act
        mockMvc.perform(post("/api/flights")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(flightRequest)))
            .andExpect(status().isCreated());

        //Assert
        mockMvc.perform(post("/api/flights")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(flightRequest)))
            .andExpect(status().isConflict());
    }

    //shouldRejectArrivalBeforeDeparture — Validar que la llegada no sea antes de la salida
    void shouldRejectArrivalBeforeDeparture() throws Exception {
        //Arrange
        Long destinoAeropuerto = createAirportAndGetId(airportRequest);
        Long llegadaAeropuerto = createAirportAndGetId(airportRequestTwo);
        FlightRequest flightRequest = flightRequest("FL6767", destinoAeropuerto, llegadaAeropuerto);
        flightRequest.setDepartureTime(LocalDateTime.now().plusDays(1));
        flightRequest.setArrivalTime(LocalDateTime.now().plusDays(1).minusHours(2)); // Llegada antes de la salida
        flightRequest.setStatus("PROGRAMADO");

        //Act + Assert
        mockMvc.perform(post("/api/flights")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(flightRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("La hora de llegada no puede ser antes de la hora de salida")));
    }

    //shouldFindAllFlights — Listar todos los vuelos
    void shouldFindAllFlights() throws Exception {
        //Arrange
        Long destinoAeropuerto = createAirportAndGetId(airportRequest);
        Long llegadaAeropuerto = createAirportAndGetId(airportRequestTwo);
        //vuelo uno
        FlightRequest flightRequest = flightRequest("FL6767", destinoAeropuerto, llegadaAeropuerto);
        flightRequest.setDepartureTime(LocalDateTime.now().plusDays(1));
        flightRequest.setArrivalTime(LocalDateTime.now().plusDays(1).plusHours(2));
        flightRequest.setStatus("PROGRAMADO");
        //vuelo dos
        FlightRequest flightRequestTwo = flightRequest("FL1234", destinoAeropuerto, llegadaAeropuerto);
        flightRequestTwo.setDepartureTime(LocalDateTime.now().plusDays(1));
        flightRequestTwo.setArrivalTime(LocalDateTime.now().plusDays(1).plusHours(2));
        flightRequestTwo.setStatus("PROGRAMADO");

        createFlight(flightRequest);
        createFlight(flightRequestTwo);

        //Act + Assert
        mockMvc.perform(get("/api/flights"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
            .andExpect(jsonPath("$[0].flightNumber").value("FL6767"))
            .andExpect(jsonPath("$[1].flightNumber").value("FL1234"));
    }

    //shouldFindFlightById — Buscar por ID
    void shouldFindFlightById() throws Exception {
        //Arrange
        Long destinoAeropuerto = createAirportAndGetId(airportRequest);
        Long llegadaAeropuerto = createAirportAndGetId(airportRequestTwo);
        FlightRequest flightRequest = flightRequest("FL6767", destinoAeropuerto, llegadaAeropuerto);
        flightRequest.setDepartureTime(LocalDateTime.now().plusDays(1));
        flightRequest.setArrivalTime(LocalDateTime.now().plusDays(1).plusHours(2));
        flightRequest.setStatus("PROGRAMADO");
        Long flightId = createFlightAndGetId(flightRequest);

        //Act + Assert
        mockMvc.perform(get("/api/flights/{id}", flightId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(flightId))
            .andExpect(jsonPath("$.flightNumber").value("FL6767"))
            .andExpect(jsonPath("$.departureAirport.id").value(destinoAeropuerto))
            .andExpect(jsonPath("$.arrivalAirport.id").value(llegadaAeropuerto))
            .andExpect(jsonPath("$.status").value("PROGRAMADO"));
    }

    //shouldFindFlightByFlightNumber — Buscar por número de vuelo
    void shouldFindFlightByFlightNumber() throws Exception {
        //Arrange
        Long destinoAeropuerto = createAirportAndGetId(airportRequest);
        Long llegadaAeropuerto = createAirportAndGetId(airportRequestTwo);
        FlightRequest flightRequest = flightRequest("FL6767", destinoAeropuerto, llegadaAeropuerto);
        flightRequest.setDepartureTime(LocalDateTime.now().plusDays(1));
        flightRequest.setArrivalTime(LocalDateTime.now().plusDays(1).plusHours(2));
        flightRequest.setStatus("PROGRAMADO");
        createFlight(flightRequest);

        //Act + Assert
        mockMvc.perform(get("/api/flights/number/{flightNumber}", "FL6767"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.flightNumber").value("FL6767"))
            .andExpect(jsonPath("$.departureAirport.id").value(destinoAeropuerto))
            .andExpect(jsonPath("$.arrivalAirport.id").value(llegadaAeropuerto))
            .andExpect(jsonPath("$.status").value("PROGRAMADO"));
    }

    //shouldFindFlightsByStatus — Filtrar por estado
    void shouldFindFlightsByStatus() throws Exception {
        //Arrange
        Long destinoAeropuerto = createAirportAndGetId(airportRequest);
        Long llegadaAeropuerto = createAirportAndGetId(airportRequestTwo);
        FlightRequest flightRequest = flightRequest("FL6767", destinoAeropuerto, llegadaAeropuerto);
        flightRequest.setDepartureTime(LocalDateTime.now().plusDays(1));
        flightRequest.setArrivalTime(LocalDateTime.now().plusDays(1).plusHours(2));
        flightRequest.setStatus("PROGRAMADO SEGUN LO ESPERADO");
        createFlight(flightRequest);

        //Act + Assert
        mockMvc.perform(get("/api/flights/status/{status}", "PROGRAMADO SEGUN LO ESPERADO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$[0].flightNumber").value("FL6767"))
            .andExpect(jsonPath("$[0].status").value("PROGRAMADO SEGUN LO ESPERADO"));
    }

    //shouldFindFlightsBetweenDates — Buscar vuelos en un rango de fechas
    void shouldFindFlightsBetweenDates() throws Exception {
        //Arrange
        Long destinoAeropuerto = createAirportAndGetId(airportRequest);
        Long llegadaAeropuerto = createAirportAndGetId(airportRequestTwo);
        FlightRequest flightRequest = flightRequest("FL6767", destinoAeropuerto, llegadaAeropuerto);
        flightRequest.setDepartureTime(LocalDateTime.now().plusDays(1));
        flightRequest.setArrivalTime(LocalDateTime.now().plusDays(1).plusHours(2));
        flightRequest.setStatus("PROGRAMADO");
        createFlight(flightRequest);

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().plusDays(2);

        //Act + Assert
        mockMvc.perform(get("/api/flights/dates")
                .param("start", start.toString())
                .param("end", end.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$[0].flightNumber").value("FL6767"));
    }

    //shouldUpdateFlight — Actualizar estado del vuelo
    void shouldUpdateFlight() throws Exception {
        //Arrange
        Long destinoAeropuerto = createAirportAndGetId(airportRequest);
        Long llegadaAeropuerto = createAirportAndGetId(airportRequestTwo);
        FlightRequest flightRequest = flightRequest("FL6767", destinoAeropuerto, llegadaAeropuerto);
        flightRequest.setDepartureTime(LocalDateTime.now().plusDays(1));
        flightRequest.setArrivalTime(LocalDateTime.now().plusDays(1).plusHours(2));
        flightRequest.setStatus("PROGRAMADO");
        Long flightId = createFlightAndGetId(flightRequest);

        //Actualizar estado a "CANCELADO"
        flightRequest.setStatus("CANCELADO");

        //Act + Assert
        mockMvc.perform(put("/api/flights/{id}", flightId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(flightRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(flightId))
            .andExpect(jsonPath("$.status").value("CANCELADO"));
    }

    //shouldReturn404WhenFlightNotFound — Flight inexistente → HTTP 404
    void shouldReturn404WhenFlightNotFound() throws Exception {
        //Act + Assert
        mockMvc.perform(get("/api/flights/{id}", 99999L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message", containsString("Vuelo no encontrado con id")));
    }

    //shouldDeleteFlight — Eliminar y verificar que ya no existe
    void shouldDeleteFlight() throws Exception {
        //Arrange
        Long destinoAeropuerto = createAirportAndGetId(airportRequest);
        Long llegadaAeropuerto = createAirportAndGetId(airportRequestTwo);
        FlightRequest flightRequest = flightRequest("FL6767", destinoAeropuerto, llegadaAeropuerto);
        flightRequest.setDepartureTime(LocalDateTime.now().plusDays(1));
        flightRequest.setArrivalTime(LocalDateTime.now().plusDays(1).plusHours(2));
        flightRequest.setStatus("PROGRAMADO");
        Long flightId = createFlightAndGetId(flightRequest);

        //Act + Assert
        mockMvc.perform(delete("/api/flights/{id}", flightId))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/flights/{id}", flightId))
            .andExpect(status().isNotFound());
    }
}