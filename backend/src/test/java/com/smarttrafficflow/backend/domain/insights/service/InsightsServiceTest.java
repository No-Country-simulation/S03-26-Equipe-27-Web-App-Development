package com.smarttrafficflow.backend.domain.insights.service;

import com.smarttrafficflow.backend.api.dto.TrafficInsightResponse;
import com.smarttrafficflow.backend.domain.trafficrecords.entity.TrafficRecord;
import com.smarttrafficflow.backend.domain.trafficrecords.service.TrafficRecordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InsightService — testes unitários")
class InsightServiceTest {

    @Mock
    private TrafficRecordService trafficRecordService;

    @InjectMocks
    private InsightService service;

    private TrafficRecord record(int hour, String roadType, int volume) {
        TrafficRecord r = new TrafficRecord();
        r.setId(UUID.randomUUID());
        r.setTimestamp(OffsetDateTime.of(2024, 6, 17, hour, 0, 0, 0, ZoneOffset.UTC));
        r.setRoadType(roadType);
        r.setVehicleVolume(volume);
        r.setRegion("DEFAULT_REGION");
        return r;
    }

    @Test
    @DisplayName("generateInsights() sem dados deve retornar mensagem de dados insuficientes")
    void generateInsights_noData_shouldReturnFallback() {
        when(trafficRecordService.findAllEntities()).thenReturn(List.of());

        TrafficInsightResponse response = service.generateInsights();

        assertThat(response.insights()).hasSize(1);
        assertThat(response.insights().get(0)).containsIgnoringCase("dados");
    }

    @Test
    @DisplayName("generateInsights() com dados deve retornar exatamente 2 insights")
    void generateInsights_withData_shouldReturnTwoInsights() {
        when(trafficRecordService.findAllEntities()).thenReturn(List.of(
                record(8, "ARTERIAL", 100),
                record(17, "HIGHWAY", 50)
        ));

        assertThat(service.generateInsights().insights()).hasSize(2);
    }

    @Test
    @DisplayName("generateInsights() deve identificar corretamente a hora de pico")
    void generateInsights_shouldIdentifyPeakHour() {
        // Hora 8: 100+80=180, Hora 17: 50
        when(trafficRecordService.findAllEntities()).thenReturn(List.of(
                record(8, "ARTERIAL", 100),
                record(8, "LOCAL", 80),
                record(17, "HIGHWAY", 50)
        ));

        TrafficInsightResponse response = service.generateInsights();

        assertThat(response.insights().get(0)).contains("8");
    }

    @Test
    @DisplayName("generateInsights() deve identificar corretamente o tipo de via de pico")
    void generateInsights_shouldIdentifyPeakRoadType() {
        // ARTERIAL: 100+80=180, HIGHWAY: 50
        when(trafficRecordService.findAllEntities()).thenReturn(List.of(
                record(8, "ARTERIAL", 100),
                record(9, "ARTERIAL", 80),
                record(10, "HIGHWAY", 50)
        ));

        TrafficInsightResponse response = service.generateInsights();

        assertThat(response.insights().get(1)).contains("ARTERIAL");
    }

    @Test
    @DisplayName("generateInsights() não deve lançar NPE para roadType nulo")
    void generateInsights_nullRoadType_shouldNotThrow() {
        when(trafficRecordService.findAllEntities()).thenReturn(List.of(
                record(8, null, 200),
                record(9, "LOCAL", 50)
        ));

        TrafficInsightResponse response = service.generateInsights();

        assertThat(response.insights()).hasSize(2);
        assertThat(response.insights().get(1)).contains("UNKNOWN");
    }

    @Test
    @DisplayName("generateInsights() com registro único deve funcionar corretamente")
    void generateInsights_singleRecord_shouldWork() {
        when(trafficRecordService.findAllEntities()).thenReturn(List.of(
                record(10, "HIGHWAY", 300)
        ));

        TrafficInsightResponse response = service.generateInsights();

        assertThat(response.insights()).hasSize(2);
        assertThat(response.insights().get(0)).contains("10");
        assertThat(response.insights().get(1)).contains("HIGHWAY");
    }

    @Test
    @DisplayName("generateInsights() com volumes iguais não deve lançar exceção")
    void generateInsights_equalVolumes_shouldNotThrow() {
        when(trafficRecordService.findAllEntities()).thenReturn(List.of(
                record(8, "LOCAL", 100),
                record(9, "ARTERIAL", 100),
                record(10, "HIGHWAY", 100)
        ));

        assertThat(service.generateInsights().insights()).hasSize(2);
    }
}
