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
import org.springframework.test.web.servlet.MvcResult;

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

    private String createAirport(AirportRequest request) throws Exception {
        return mockMvc.perform(post("/api/airports"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(airportRequest)))
            .andExpect(status().isCreated());
            .andReturn().getResponse().getContentAsString();
    }


    }

    @Test
    void shouldCreateAirport() throws Exception {
        //Arrange
        AirportRequest airportRequest = new AirportRequest();
        airportRequest.setName("Aeropuerto Mariscal Sucre");
        airportRequest.setCity("Quito");
        airportRequest.setCode("UIO");
        airportRequest.setCountry("Ecuador");
        //Act + Assert
        mockMvc.perform(post("/api/airports"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(airportRequest)))
            .andExpect(status().isCreated());
            .andExpect(jsonPath("$.name").value("Aeropuerto Mariscal Sucre"))
            .andExpect(jsonPath("$.code").value("UIO"))
            .andExpect(jsonPath("$.city").value("Quito"))
            .andExpect(jsonPath("$.country").value("Ecuador"));
            .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void shouldDeleteAirport() throws Exception {
        //Arrange
        AirportRequest airportRequest = new AirportRequest();
        airportRequest.setName("Santiago de Chile");
        airportRequest.setCity("Santiago");
        airportRequest.setCode("SCL");
        airportRequest.setCountry("Chile");
        //Act + Assert
        String response = createAirport(airportRequest);
        Long id = objectMapper.readTree(response).get("id").asLong();
        mockMvc.perform(delete("/api/airport/", id))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/airport/", id))
            .andExpect(status().isNoContent());
    }

    @Test
    void shouldUpdateAirport() throws Exception {
         //Arrange
        AirportRequest airportRequest = new AirportRequest();
        airportRequest.setName("Santiago de Chile");
        airportRequest.setCity("Santiago");
        airportRequest.setCode("SCL");
        airportRequest.setCountry("Chile");
        //Act + Assert
        String response = createAirport(airportRequest);
        Long id = objectMapper.readTree(response).get("id").asLong();
        airportRequest.setName("Santiago de Chile - Aeropuerto Internacional");
        mockMvc.perform(put("/api/airport/", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(airportRequest)))
    }


    /*@Test
    void shouldRejectDuplicateAirportCode() throws Exception {
        AirportRequest firstRequest = airportRequest("Aeropuerto de Quito", "UIO", "Quito", "Ecuador");
        AirportRequest duplicateRequest = airportRequest("Otro Aeropuerto", "UIO", "Guayaquil", "Ecuador");

        mockMvc.perform(post("/api/airports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/airports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("El código de aeropuerto ya existe")));
    }

    @Test
    void shouldFindAllAirports() throws Exception {
        createAirportAndGetId("Aeropuerto de Quito", "UIO", "Quito", "Ecuador");
        createAirportAndGetId("Aeropuerto de Guayaquil", "GYE", "Guayaquil", "Ecuador");

        mockMvc.perform(get("/api/airports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].code", containsInAnyOrder("UIO", "GYE")));
    }

    @Test
    void shouldFindAirportById() throws Exception {
        Long airportId = createAirportAndGetId("Aeropuerto de Quito", "UIO", "Quito", "Ecuador");

        mockMvc.perform(get("/api/airports/{id}", airportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(airportId))
                .andExpect(jsonPath("$.name").value("Aeropuerto de Quito"))
                .andExpect(jsonPath("$.code").value("UIO"))
                .andExpect(jsonPath("$.city").value("Quito"))
                .andExpect(jsonPath("$.country").value("Ecuador"));
    }

    @Test
    void shouldFindAirportByCode() throws Exception {
        createAirportAndGetId("Aeropuerto de Quito", "UIO", "Quito", "Ecuador");

        mockMvc.perform(get("/api/airports/code/{code}", "UIO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Aeropuerto de Quito"))
                .andExpect(jsonPath("$.code").value("UIO"))
                .andExpect(jsonPath("$.city").value("Quito"))
                .andExpect(jsonPath("$.country").value("Ecuador"));
    }

    @Test
    void shouldReturn404WhenAirportNotFound() throws Exception {
        mockMvc.perform(get("/api/airports/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message", containsString("Aeropuerto no encontrado con id")));
    }
    

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

    private Long createAirportAndGetId(String name, String code, String city, String country) throws Exception {
        AirportRequest request = airportRequest(name, code, city, country);

        MvcResult result = mockMvc.perform(post("/api/airports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("id").asLong();
    }

    private AirportRequest airportRequest(String name, String code, String city, String country) {
        AirportRequest request = new AirportRequest();
        request.setName(name);
        request.setCode(code);
        request.setCity(city);
        request.setCountry(country);
        return request;
    }*/
}