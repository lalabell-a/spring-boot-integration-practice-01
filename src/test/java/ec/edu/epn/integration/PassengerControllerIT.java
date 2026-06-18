package ec.edu.epn.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.epn.dto.AirportRequest;
import ec.edu.epn.dto.PassengerRequest;
import ec.edu.epn.repository.PassengerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class PassengerControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PassengerRepository passengerRepository;

    private PassengerRequest passengerRequest(String firstName, String lastName, String email, String passport) {
        PassengerRequest request = new PassengerRequest();
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setEmail(email);
        request.setPassportNumber(passport);
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

    @BeforeEach
    void setUp() {
        passengerRepository.deleteAll();
    }

private ResultActions createPassenger(PassengerRequest request) throws Exception {
        // El paréntesis ahora encierra correctamente a contentType y content
        return mockMvc.perform(post("/api/passengers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
    }

    private Long createPassengerAndGetId(PassengerRequest request) throws Exception {
        // Agregamos .andReturn() aquí para obtener el MvcResult antes de leer el JSON
        JsonNode jsonNode = objectMapper.readTree(createPassenger(request)
                .andReturn()
                .getResponse().getContentAsString());
        return jsonNode.get("id").asLong();
    }

    //shouldCreatePassenger — Crear un pasajero y verificar HTTP 201
    @Test
    void shouldCreatePassenger() throws Exception {
        //Arrange
        PassengerRequest request = passengerRequest("Sebastian", "Sarasti", "sebastian.sarasti@gmail.com", "ABC123");

        //Act + Assert
        mockMvc.perform(post("/api/passengers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
    }

    // shouldRejectDuplicateEmail — Intentar crear dos pasajeros con el mismo email
    @Test
    void shouldRejectDuplicateEmail() throws Exception {
        //Arrange
        PassengerRequest request1 = passengerRequest("Sebastian", "Sarasti", "sebastian.sarasti@gmail.com", "ABC123");
        PassengerRequest request2 = passengerRequest("Angel", "Pastaz", "sebastian.sarasti@gmail.com", "DEF456");
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
            .andExpect(jsonPath("$.message").value("El email ya está registrado: sebastian.sarasti@gmail.com"));
    }

    // shouldFindAllPassengers — Listar todos los pasajeros
    @Test
    void shouldFindAllPassengers() throws Exception {
        //Arrange
        PassengerRequest request1 = passengerRequest("Sebastian", "Sarasti", "sebastian.sarasti@gmail.com", "ABC123");
        PassengerRequest request2 = passengerRequest("Angel", "Pastaz", "angel.pastaz@gmail.com", "DEF456");

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
    @Test
    void shouldFindPassengerById() throws Exception {
        //Arrange
        PassengerRequest request = passengerRequest("Sebastian", "Sarasti", "sebastian.sarasti@gmail.com", "ABC123");
        Long passengerId = createPassengerAndGetId(request);

        //Act + Assert
        mockMvc.perform(get("/api/passengers/{id}", passengerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(passengerId))
                .andExpect(jsonPath("$.firstName").value("Sebastian"))
                .andExpect(jsonPath("$.lastName").value("Sarasti"))
                .andExpect(jsonPath("$.email").value("sebastian.sarasti@gmail.com"))
                .andExpect(jsonPath("$.passportNumber").value("ABC123"));
    }

    // shouldFindPassengerByEmail — Buscar por email
    @Test
    void shouldFindPassengerByEmail() throws Exception {
        //Arrange
        PassengerRequest request = passengerRequest("Sebastian", "Sarasti", "sebastian.sarasti@gmail.com", "ABC123");
        Long passengerId = createPassengerAndGetId(request);

        //Act + Assert
        mockMvc.perform(get("/api/passengers/email/{email}", "sebastian.sarasti@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(passengerId))
                .andExpect(jsonPath("$.firstName").value("Sebastian"))
                .andExpect(jsonPath("$.lastName").value("Sarasti"))
                .andExpect(jsonPath("$.email").value("sebastian.sarasti@gmail.com"))
                .andExpect(jsonPath("$.passportNumber").value("ABC123"));
    }

    // shouldFindPassengerByPassportNumber — Buscar por número de pasaporte
    @Test
    void shouldFindPassengerByPassportNumber() throws Exception {
        //Arrange
        PassengerRequest request = passengerRequest("Sebastian", "Sarasti", "sebastian.sarasti@gmail.com", "ABC123");
        Long passengerId = createPassengerAndGetId(request);

        //Act + Assert
        mockMvc.perform(get("/api/passengers/passport/{passportNumber}", "ABC123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(passengerId))
                .andExpect(jsonPath("$.firstName").value("Sebastian"))
                .andExpect(jsonPath("$.lastName").value("Sarasti"))
                .andExpect(jsonPath("$.email").value("sebastian.sarasti@gmail.com"))
                .andExpect(jsonPath("$.passportNumber").value("ABC123"));
    }

    // shouldReturn404WhenPassengerNotFound — Pasajero inexistente → HTTP 404
    @Test
    void shouldReturn404WhenPassengerNotFound() throws Exception {
        //Act + Assert
        mockMvc.perform(get("/api/passengers/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message", containsString("Pasajero no encontrado con id")));
    }

    // shouldUpdatePassenger — Actualizar datos del pasajero
    @Test
    void shouldUpdatePassenger() throws Exception {
        //Arrange
        PassengerRequest request = passengerRequest("Sebastian", "Sarasti", "sebastian.sarasti@gmail.com", "ABC123" );
        Long passengerId = createPassengerAndGetId(request);

        //Act + Assert
        mockMvc.perform(put("/api/passengers/{id}", passengerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(passengerId))
                .andExpect(jsonPath("$.firstName").value("Sebastian"))
                .andExpect(jsonPath("$.lastName").value("Sarasti"))
                .andExpect(jsonPath("$.email").value("sebastian.sarasti@gmail.com"))
                .andExpect(jsonPath("$.passportNumber").value("ABC123"));
    }

    // shouldDeletePassenger — Eliminar y verificar que ya no existe
    @Test
    void shouldDeletePassenger() throws Exception {
        //Arrange
        PassengerRequest request = passengerRequest("Sebastian", "Sarasti", "sebastian.sarasti@gmail.com", "ABC123");
        Long passengerId = createPassengerAndGetId(request);

        //Act + Assert
        mockMvc.perform(delete("/api/passengers/{id}", passengerId))
                .andExpect(status().isNoContent());
    }

    // shouldRejectInvalidEmail — Enviar email inválido y verificar HTTP 400
    @Test
    void shouldRejectInvalidEmail() throws Exception {
        //Arrange
        PassengerRequest request = passengerRequest("Sebastian", "Sarasti", "invalid-email", "ABC123");

        //Act + Assert
        mockMvc.perform(post("/api/passengers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Error"))
            .andExpect(jsonPath("$.message", containsString("El email debe ser válido")));
    }
}