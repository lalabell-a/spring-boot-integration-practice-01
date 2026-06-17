package ec.edu.epn.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.epn.dto.AirportRequest;
import ec.edu.epn.repository.AirportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AirportControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AirportRepository airportRepository;

    @BeforeEach
    void cleanDatabase() {
        airportRepository.deleteAll();
    }

    private AirportRequest airportRequest(String name, String code, String city, String country) {
        AirportRequest request = new AirportRequest();
        request.setName(name);
        request.setCode(code);
        request.setCity(city);
        request.setCountry(country);
        return request;
    }

private ResultActions createAirport(AirportRequest request) throws Exception {
        return mockMvc.perform(post("/api/airports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
    }

    private Long createAirportAndGetId(AirportRequest request) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(createAirport(request)
                .andReturn() 
                .getResponse().getContentAsString());
        return jsonNode.get("id").asLong();
    }

    // ACTIVIDAD

    // shouldCreateAirport — Crear un aeropuerto y verificar HTTP 201 + datos en la respuesta
    @Test
    void shouldCreateAirport() throws Exception {
        //Arrange
        AirportRequest airportRequest = airportRequest("Aeropuerto Mariscal Sucre", "UIO", "Quito", "Ecuador");
        //Act + Assert
        createAirport(airportRequest)
            .andExpect(jsonPath("$.name").value("Aeropuerto Mariscal Sucre"))
            .andExpect(jsonPath("$.code").value("UIO"))
            .andExpect(jsonPath("$.city").value("Quito"))
            .andExpect(jsonPath("$.country").value("Ecuador"))
            .andExpect(jsonPath("$.id").isNumber());
    }

    // shouldRejectDuplicateAirportCode — Intentar crear dos aeropuertos con el mismo código IATA
    @Test
    void shouldRejectDuplicateAirportCode() throws Exception {
        //Arrange
        //Aeropuerto uno
        AirportRequest airportRequest = airportRequest("Aeropuerto Mariscal Sucre", "UIO", "Quito", "Ecuador");

        //Aeropuerto dos
        AirportRequest airportRequestTwo = airportRequest("Aeropuerto Mariscal Sucre duplicado", "UIO", "Guayaquil", "Ecuador");

        //Act + Assert
        //comprobar ambos aeropuertos
        createAirport(airportRequest);

        mockMvc.perform(post("/api/airports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(airportRequestTwo)))
        //Esperar prohibición 
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("El código de aeropuerto ya existe")));
    }

    //shouldFindAllAirports — Listar todos los aeropuertos (crea 2 antes)
    @Test
    void shouldFindAllAirports() throws Exception {
        //Arrange
        AirportRequest airportRequest = airportRequest("Aeropuerto Mariscal Sucre", "UIO", "Quito", "Ecuador");
        AirportRequest airportRequestTwo = airportRequest("Aeropuerto Internacional John F. Kennedy", "NYC", "New York", "United States");

        createAirport(airportRequest);
        createAirport(airportRequestTwo);

        //Act + Assert
        mockMvc.perform(get("/api/airports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].code", containsInAnyOrder("UIO", "NYC")));
    }

    //shouldFindAirportById — Buscar por ID y verificar los datos
    @Test
    void shouldFindAirportById() throws Exception {
        //Arrange
        AirportRequest airportRequest = airportRequest("Aeropuerto de Quito", "UIO", "Quito", "Ecuador");
        Long airportId = createAirportAndGetId(airportRequest);
        //Act + Assert
        mockMvc.perform(get("/api/airports/{id}", airportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(airportId))
                .andExpect(jsonPath("$.name").value("Aeropuerto de Quito"))
                .andExpect(jsonPath("$.code").value("UIO"))
                .andExpect(jsonPath("$.city").value("Quito"))
                .andExpect(jsonPath("$.country").value("Ecuador"));
    }

    //shouldFindAirportByCode — Buscar por código IATA
    @Test
    void shouldFindAirportByCode() throws Exception {
        createAirportAndGetId(airportRequest("Aeropuerto de Quito", "UIO", "Quito", "Ecuador"));

        mockMvc.perform(get("/api/airports/code/{code}", "UIO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Aeropuerto de Quito"))
                .andExpect(jsonPath("$.code").value("UIO"))
                .andExpect(jsonPath("$.city").value("Quito"))
                .andExpect(jsonPath("$.country").value("Ecuador"));
    }

    //shouldReturn404WhenAirportNotFound — Buscar un ID inexistente → HTTP 404
    @Test
    void shouldReturn404WhenAirportNotFound() throws Exception {
        //Act + Assert
        mockMvc.perform(get("/api/airports/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message", containsString("Aeropuerto no encontrado con id")));
    }

    //shouldUpdateAirport — Actualizar y verificar los cambios
    @Test
    void shouldUpdateAirport() throws Exception {
         //Arrange
        AirportRequest airportRequest = airportRequest("Santiago de Chile", "SCL", "Santiago", "Chile");
        //Act + Assert
        Long id = createAirportAndGetId(airportRequest);
        airportRequest.setName("Santiago de Chile - Aeropuerto Internacional");
        mockMvc.perform(put("/api/airports/{id}", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(airportRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.name").value("Santiago de Chile - Aeropuerto Internacional"))
            .andExpect(jsonPath("$.code").value("SCL"));
    }

    //shouldDeleteAirport — Eliminar y verificar que ya no existe (GET → 404)
    @Test
    void shouldDeleteAirport() throws Exception {
        //Arrange
        AirportRequest airportRequest = airportRequest("Santiago de Chile", "SCL", "Santiago", "Chile");
        //Act + Assert
        Long id = createAirportAndGetId(airportRequest);
        mockMvc.perform(delete("/api/airports/{id}", id))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/airports/{id}", id))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(status().isNotFound());
    }

    //shouldRejectInvalidAirportRequest — Enviar datos inválidos y verificar HTTP 400
    @Test
    void shouldRejectInvalidAirportRequest() throws Exception {
        AirportRequest invalidRequest = airportRequest("", "AB", "", "");

        mockMvc.perform(post("/api/airports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.errors", hasSize(4)));
    }
}