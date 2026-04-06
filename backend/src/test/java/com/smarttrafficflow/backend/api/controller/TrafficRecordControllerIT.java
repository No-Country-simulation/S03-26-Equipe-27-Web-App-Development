package com.smarttrafficflow.backend.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttrafficflow.backend.domain.trafficrecords.repository.TrafficRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("TrafficRecordController — testes de integração")
class TrafficRecordControllerIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("app.cors.allowed-origins", () -> "http://localhost:5173");
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired TrafficRecordRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    // ─── POST /api/traffic-records ────────────────────────────────────────────

    @Test
    @DisplayName("POST deve retornar 201 com registro criado")
    void create_shouldReturn201() throws Exception {
        Map<String, Object> body = Map.of(
                "timestamp", "2024-06-17T08:00:00+00:00",
                "roadType", "ARTERIAL",
                "vehicleVolume", 120,
                "eventType", "RUSH_HOUR",
                "weather", "SUNNY",
                "region", "CENTRO"
        );

        mockMvc.perform(post("/api/traffic-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.roadType", is("ARTERIAL")))
                .andExpect(jsonPath("$.vehicleVolume", is(120)));
    }

    @Test
    @DisplayName("POST deve retornar 400 quando roadType está ausente")
    void create_missingRoadType_shouldReturn400() throws Exception {
        Map<String, Object> body = Map.of(
                "timestamp", "2024-06-17T08:00:00+00:00",
                "vehicleVolume", 120
        );

        mockMvc.perform(post("/api/traffic-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("POST deve retornar 400 para vehicleVolume negativo")
    void create_negativeVolume_shouldReturn400() throws Exception {
        Map<String, Object> body = Map.of(
                "timestamp", "2024-06-17T08:00:00+00:00",
                "roadType", "LOCAL",
                "vehicleVolume", -1
        );

        mockMvc.perform(post("/api/traffic-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    // ─── GET /api/traffic-records ─────────────────────────────────────────────

    @Test
    @DisplayName("GET deve retornar lista vazia quando banco está vazio")
    void getAll_emptyDb_shouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/api/traffic-records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET deve retornar todos os registros criados")
    void getAll_withRecords_shouldReturnAll() throws Exception {
        for (String roadType : List.of("LOCAL", "HIGHWAY")) {
            mockMvc.perform(post("/api/traffic-records")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "timestamp", "2024-06-17T08:00:00+00:00",
                                    "roadType", roadType,
                                    "vehicleVolume", 100))))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/traffic-records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // ─── GET /api/traffic-stats ───────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/traffic-stats?groupBy=hour deve retornar dados agrupados")
    void getStats_groupByHour_shouldReturnData() throws Exception {
        mockMvc.perform(post("/api/traffic-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "timestamp", "2024-06-17T08:00:00+00:00",
                                "roadType", "ARTERIAL",
                                "vehicleVolume", 150))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/traffic-stats?groupBy=hour"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.labels", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.values", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("GET /api/traffic-stats com groupBy inválido deve retornar 400")
    void getStats_invalidGroupBy_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/traffic-stats?groupBy=INVALIDO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_ARGUMENT")));
    }

    // ─── POST /api/simulations/generate ──────────────────────────────────────

    @Test
    @DisplayName("POST /api/simulations/generate deve retornar 201 com registros gerados")
    void simulation_shouldReturn201() throws Exception {
        mockMvc.perform(post("/api/simulations/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "recordsToGenerate", 5,
                                "scenarioName", "Teste IT"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    @DisplayName("POST /api/simulations/generate com mais de 500 deve retornar 400")
    void simulation_tooMany_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/simulations/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "recordsToGenerate", 501,
                                "scenarioName", "Excesso"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    // ─── GET /api/exports ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/exports?format=csv deve retornar CSV com cabeçalho")
    void export_csv_shouldReturnWithHeader() throws Exception {
        mockMvc.perform(get("/api/exports?format=csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(containsString("id,timestamp,roadType")));
    }

    @Test
    @DisplayName("GET /api/exports com formato inválido deve retornar 400")
    void export_invalidFormat_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/exports?format=xml"))
                .andExpect(status().isBadRequest());
    }
}
