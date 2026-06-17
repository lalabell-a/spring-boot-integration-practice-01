package ec.edu.epn.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.epn.dto.AirportRequest;
import ec.edu.epn.dto.FlightRequest;
import ec.edu.epn.repository.AirportRepository;
import ec.edu.epn.repository.FlightRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class FlightControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AirportRepository airportRepository;

    @Autowired
    private FlightRepository flightRepository;

    private AirportRequest airportRequest;
    private AirportRequest airportRequestTwo;

    private AirportRequest airportRequestBuilder(String name, String code, String city, String country) {
        AirportRequest request = new AirportRequest();
        request.setName(name);
        request.setCode(code);
        request.setCity(city);
        request.setCountry(country);
        return request;
    }

    private FlightRequest flightRequest(String flightNumber, Long originId, Long destinationId) {
        FlightRequest request = new FlightRequest();
        request.setFlightNumber(flightNumber);
        request.setOriginId(originId); 
        request.setDestinationId(destinationId);
        return request;
    }

    @BeforeEach
    void setUp() {
        flightRepository.deleteAll();
        airportRepository.deleteAll();
        airportRequest = airportRequestBuilder("Aeropuerto Mariscal Sucre", "UIO", "Quito", "Ecuador");
        airportRequestTwo = airportRequestBuilder("Aeropuerto Internacional John F. Kennedy", "NYC", "New York", "United States");
    }

private ResultActions createAirport(AirportRequest request) throws Exception {
        return mockMvc.perform(post("/api/airports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
    }

    private Long createAirportAndGetId(AirportRequest request) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(createAirport(request)
                .andReturn() // Lo agregamos aquí
                .getResponse().getContentAsString());
        return jsonNode.get("id").asLong();
    }

    private ResultActions createFlight(FlightRequest request) throws Exception {
        return mockMvc.perform(post("/api/flights")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
    }

    private Long createFlightAndGetId(FlightRequest request) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(createFlight(request)
                .andReturn() // Lo agregamos aquí
                .getResponse().getContentAsString());
        return jsonNode.get("id").asLong();
    }

    //shouldCreateFlight — Crear un vuelo (necesitas crear aeropuertos primero en @BeforeEach)
    @Test
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
    @Test
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
    @Test
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
    @Test
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
    @Test
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
    @Test
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
    @Test
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
    @Test
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
    @Test
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
    @Test
    void shouldReturn404WhenFlightNotFound() throws Exception {
        //Act + Assert
        mockMvc.perform(get("/api/flights/{id}", 99999L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message", containsString("Vuelo no encontrado con id")));
    }

    //shouldDeleteFlight — Eliminar y verificar que ya no existe
    @Test
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