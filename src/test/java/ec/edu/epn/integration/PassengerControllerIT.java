@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class PassengerControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private PassengerRequest passengerRequest(String firstName, String lastName, String email) {
        PassengerRequest request = new PassengerRequest();
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setEmail(email);
        return request;
    }

    private AirportRequest airportRequest(String name, String code, String city, String country) {
        AirportRequest request = new AirportRequest();
        request.setName(name);
        request.setCode(code);
        request.setCity(city);
        request.setCountry(country);
        return request;
    }

    //shouldCreatePassenger — Crear un pasajero y verificar HTTP 201
    void shouldCreatePassenger() throws Exception {
        //Arrange
        PassengerRequest request = passengerRequest("Sebastian", "Sarasti", "sebastian.sarasti@gmail.com");

        //Act + Assert
        mockMvc.perform(post("/api/passengers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
    }

    // shouldRejectDuplicateEmail — Intentar crear dos pasajeros con el mismo email
    void shouldRejectDuplicateEmail() throws Exception {
        //Arrange
        PassengerRequest request1 = passengerRequest("Sebastian", "Sarasti", "sebastian.sarasti@gmail.com");
        PassengerRequest request2 = passengerRequest("Angel", "Pastaz", "sebastian.sarasti@gmail.com");
        //Act + Assert
        mockMvc.perform(post("/api/passengers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/passengers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
        //Esperar prohibición 
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("El email ya existe")));
    }

    // shouldFindAllPassengers — Listar todos los pasajeros
    void shouldFindAllPassengers() throws Exception {
        //Arrange
        PassengerRequest request1 = passengerRequest("Sebastian", "Sarasti", "sebastian.sarasti@gmail.com");
        PassengerRequest request2 = passengerRequest("Angel", "Pastaz", "angel.pastaz@gmail.com");

        //Act + Assert
        mockMvc.perform(post("/api/passengers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/passengers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/passengers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));
    }

    // shouldFindPassengerById — Buscar por ID
    void shouldFindPassengerById() throws Exception {
        //Arrange
        PassengerRequest request = passengerRequest("Sebastian", "Sarasti", "sebastian.sarasti@gmail.com");
        Long passengerId = createPassengerAndGetId(request);

        //Act + Assert
        mockMvc.perform(get("/api/passengers/{id}", passengerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(passengerId))
                .andExpect(jsonPath("$.name").value("Sebastian"))
                .andExpect(jsonPath("$.surname").value("Sarasti"))
                .andExpect(jsonPath("$.email").value("sebastian.sarasti@gmail.com"));
    }

    // shouldFindPassengerByEmail — Buscar por email
    void shouldFindPassengerByEmail() throws Exception {
        //Arrange
        PassengerRequest request = passengerRequest("Sebastian", "Sarasti", "sebastian.sarasti@gmail.com");
        Long passengerId = createPassengerAndGetId(request);

        //Act + Assert
        mockMvc.perform(get("/api/passengers/email/{email}", "sebastian.sarasti@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(passengerId))
                .andExpect(jsonPath("$.name").value("Sebastian"))
                .andExpect(jsonPath("$.surname").value("Sarasti"))
                .andExpect(jsonPath("$.email").value("sebastian.sarasti@gmail.com"));
    }

    // shouldFindPassengerByPassportNumber — Buscar por número de pasaporte
    void shouldFindPassengerByPassportNumber() throws Exception {
        //Arrange
        PassengerRequest request = passengerRequest("Sebastian", "Sarasti", "sebastian.sarasti@gmail.com");
        Long passengerId = createPassengerAndGetId(request);

        //Act + Assert
        mockMvc.perform(get("/api/passengers/passport/{passportNumber}", "ABC123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(passengerId))
                .andExpect(jsonPath("$.name").value("Sebastian"))
                .andExpect(jsonPath("$.surname").value("Sarasti"))
                .andExpect(jsonPath("$.email").value("sebastian.sarasti@gmail.com"));
    }

    // shouldReturn404WhenPassengerNotFound — Pasajero inexistente → HTTP 404
    void shouldReturn404WhenPassengerNotFound() throws Exception {
        //Act + Assert
        mockMvc.perform(get("/api/passengers/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message", containsString("Pasajero no encontrado con id")));
    }

    // shouldUpdatePassenger — Actualizar datos del pasajero
    void shouldUpdatePassenger() throws Exception {
        //Arrange
        PassengerRequest request = passengerRequest("Sebastian", "Sarasti", "sebastian.sarasti@gmail.com");
        Long passengerId = createPassengerAndGetId(request);

        //Act + Assert
        mockMvc.perform(put("/api/passengers/{id}", passengerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(passengerId))
                .andExpect(jsonPath("$.name").value("Sebastian"))
                .andExpect(jsonPath("$.surname").value("Sarasti"))
                .andExpect(jsonPath("$.email").value("sebastian.sarasti@gmail.com"));
    }

    // shouldDeletePassenger — Eliminar y verificar que ya no existe
    void shouldDeletePassenger() throws Exception {
        //Arrange
        PassengerRequest request = passengerRequest("Sebastian", "Sarasti", "sebastian.sarasti@gmail.com");
        Long passengerId = createPassengerAndGetId(request);

        //Act + Assert
        mockMvc.perform(delete("/api/passengers/{id}", passengerId))
                .andExpect(status().isNoContent());
    }

    // shouldRejectInvalidEmail — Enviar email inválido y verificar HTTP 400
    void shouldRejectInvalidEmail() throws Exception {
        //Arrange
        PassengerRequest request = passengerRequest("Sebastian", "Sarasti", "invalid-email");

        //Act + Assert
        mockMvc.perform(post("/api/passengers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("El email no es válido")));
    }
}