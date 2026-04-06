package com.smarttrafficflow.backend.domain.simulations.service;

import com.smarttrafficflow.backend.api.dto.CreateTrafficRecordRequest;
import com.smarttrafficflow.backend.api.dto.SimulationRequest;
import com.smarttrafficflow.backend.api.dto.TrafficRecordResponse;
import com.smarttrafficflow.backend.domain.trafficrecords.service.TrafficRecordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SimulationService — testes unitários")
class SimulationServiceTest {

    @Mock
    private TrafficRecordService trafficRecordService;

    @InjectMocks
    private SimulationService service;

    private static final Set<String> VALID_ROAD_TYPES = Set.of("LOCAL", "ARTERIAL", "HIGHWAY");
    private static final Set<String> VALID_WEATHER = Set.of("SUNNY", "RAIN", "CLOUDY");

    private TrafficRecordResponse mockResp(String roadType) {
        return new TrafficRecordResponse(
                UUID.randomUUID(), OffsetDateTime.now(), roadType, 100, null, null, "DEFAULT_REGION");
    }

    // ─── Quantidade ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("generate() deve criar exatamente o número solicitado de registros")
    void generate_shouldCreateExactCount() {
        when(trafficRecordService.create(any())).thenReturn(mockResp("ARTERIAL"));

        List<TrafficRecordResponse> result = service.generate(new SimulationRequest(10, "Teste"));

        assertThat(result).hasSize(10);
        verify(trafficRecordService, times(10)).create(any());
    }

    @Test
    @DisplayName("generate() com 1 registro deve funcionar")
    void generate_oneRecord_shouldWork() {
        when(trafficRecordService.create(any())).thenReturn(mockResp("LOCAL"));

        assertThat(service.generate(new SimulationRequest(1, "Mínimo"))).hasSize(1);
    }

    // ─── Campos gerados ───────────────────────────────────────────────────────

    @Test
    @DisplayName("generate() deve usar scenarioName como eventType")
    void generate_shouldUseScenarioNameAsEventType() {
        when(trafficRecordService.create(any())).thenReturn(mockResp("ARTERIAL"));

        service.generate(new SimulationRequest(3, "Varredura Noturna"));

        ArgumentCaptor<CreateTrafficRecordRequest> captor =
                ArgumentCaptor.forClass(CreateTrafficRecordRequest.class);
        verify(trafficRecordService, times(3)).create(captor.capture());
        captor.getAllValues().forEach(req ->
                assertThat(req.eventType()).isEqualTo("Varredura Noturna"));
    }

    @Test
    @DisplayName("generate() deve gerar apenas tipos de via válidos")
    void generate_shouldGenerateOnlyValidRoadTypes() {
        when(trafficRecordService.create(any())).thenReturn(mockResp("LOCAL"));
        service.generate(new SimulationRequest(50, "Teste"));

        ArgumentCaptor<CreateTrafficRecordRequest> captor =
                ArgumentCaptor.forClass(CreateTrafficRecordRequest.class);
        verify(trafficRecordService, times(50)).create(captor.capture());
        captor.getAllValues().forEach(req ->
                assertThat(VALID_ROAD_TYPES).contains(req.roadType()));
    }

    @Test
    @DisplayName("generate() deve gerar apenas condições climáticas válidas")
    void generate_shouldGenerateOnlyValidWeather() {
        when(trafficRecordService.create(any())).thenReturn(mockResp("LOCAL"));
        service.generate(new SimulationRequest(50, "Teste"));

        ArgumentCaptor<CreateTrafficRecordRequest> captor =
                ArgumentCaptor.forClass(CreateTrafficRecordRequest.class);
        verify(trafficRecordService, times(50)).create(captor.capture());
        captor.getAllValues().forEach(req ->
                assertThat(VALID_WEATHER).contains(req.weather()));
    }

    @Test
    @DisplayName("generate() deve gerar volume entre 20 e 300 inclusive")
    void generate_shouldGenerateVolumeInRange() {
        when(trafficRecordService.create(any())).thenReturn(mockResp("HIGHWAY"));
        service.generate(new SimulationRequest(100, "Teste"));

        ArgumentCaptor<CreateTrafficRecordRequest> captor =
                ArgumentCaptor.forClass(CreateTrafficRecordRequest.class);
        verify(trafficRecordService, times(100)).create(captor.capture());
        captor.getAllValues().forEach(req -> {
            assertThat(req.vehicleVolume()).isGreaterThanOrEqualTo(20);
            assertThat(req.vehicleVolume()).isLessThanOrEqualTo(300);
        });
    }

    @Test
    @DisplayName("generate() deve gerar timestamps no passado (até 7 dias)")
    void generate_shouldGeneratePastTimestamps() {
        when(trafficRecordService.create(any())).thenReturn(mockResp("LOCAL"));
        service.generate(new SimulationRequest(20, "Teste"));

        ArgumentCaptor<CreateTrafficRecordRequest> captor =
                ArgumentCaptor.forClass(CreateTrafficRecordRequest.class);
        verify(trafficRecordService, times(20)).create(captor.capture());

        OffsetDateTime now = OffsetDateTime.now().plusMinutes(1);
        OffsetDateTime limit = OffsetDateTime.now().minusHours(169);
        captor.getAllValues().forEach(req -> {
            assertThat(req.timestamp()).isBefore(now);
            assertThat(req.timestamp()).isAfter(limit);
        });
    }

    // ─── Diversidade ─────────────────────────────────────────────────────────

    @RepeatedTest(3)
    @DisplayName("generate() com 30 registros deve usar pelo menos 2 tipos de via")
    void generate_withManyRecords_shouldUseDiverseRoadTypes() {
        when(trafficRecordService.create(any())).thenAnswer(inv -> {
            CreateTrafficRecordRequest r = inv.getArgument(0);
            return mockResp(r.roadType());
        });
        service.generate(new SimulationRequest(30, "Diversidade"));

        ArgumentCaptor<CreateTrafficRecordRequest> captor =
                ArgumentCaptor.forClass(CreateTrafficRecordRequest.class);
        verify(trafficRecordService, times(30)).create(captor.capture());

        Set<String> usedTypes = captor.getAllValues().stream()
                .map(CreateTrafficRecordRequest::roadType)
                .collect(Collectors.toSet());
        assertThat(usedTypes.size()).isGreaterThanOrEqualTo(2);
    }
}
