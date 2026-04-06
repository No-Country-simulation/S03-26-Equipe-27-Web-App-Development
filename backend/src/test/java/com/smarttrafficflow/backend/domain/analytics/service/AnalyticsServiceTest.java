package com.smarttrafficflow.backend.domain.analytics.service;

import com.smarttrafficflow.backend.api.dto.TrafficStatsResponse;
import com.smarttrafficflow.backend.domain.trafficrecords.entity.TrafficRecord;
import com.smarttrafficflow.backend.domain.trafficrecords.service.TrafficRecordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsService — testes unitários")
class AnalyticsServiceTest {

    @Mock
    private TrafficRecordService trafficRecordService;

    @InjectMocks
    private AnalyticsService service;

    private TrafficRecord record(int hour, String roadType, int volume) {
        TrafficRecord r = new TrafficRecord();
        r.setId(UUID.randomUUID());
        // 2024-06-17 = segunda-feira
        r.setTimestamp(OffsetDateTime.of(2024, 6, 17, hour, 0, 0, 0, ZoneOffset.UTC));
        r.setRoadType(roadType);
        r.setVehicleVolume(volume);
        return r;
    }

    private TrafficRecord recordOnDay(DayOfWeek day, int volume) {
        TrafficRecord r = new TrafficRecord();
        r.setId(UUID.randomUUID());
        OffsetDateTime base = OffsetDateTime.of(2024, 6, 10, 8, 0, 0, 0, ZoneOffset.UTC); // segunda
        r.setTimestamp(base.plusDays(day.getValue() - DayOfWeek.MONDAY.getValue()));
        r.setRoadType("ARTERIAL");
        r.setVehicleVolume(volume);
        return r;
    }

    // ─── groupBy=hour ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getStats(hour) deve agregar volumes por hora e formatar com 2 dígitos")
    void getStats_groupByHour_shouldAggregateAndFormat() {
        when(trafficRecordService.findAllEntities()).thenReturn(List.of(
                record(8, "ARTERIAL", 100),
                record(8, "HIGHWAY", 50),
                record(17, "LOCAL", 200)
        ));

        TrafficStatsResponse response = service.getStats("hour");

        assertThat(response.labels()).contains("08", "17");
        int idx8 = response.labels().indexOf("08");
        assertThat(response.values().get(idx8)).isEqualTo(150);
        int idx17 = response.labels().indexOf("17");
        assertThat(response.values().get(idx17)).isEqualTo(200);
    }

    @Test
    @DisplayName("getStats(hour) deve formatar hora com zero à esquerda")
    void getStats_groupByHour_shouldPadHour() {
        when(trafficRecordService.findAllEntities()).thenReturn(List.of(record(3, "LOCAL", 30)));

        TrafficStatsResponse response = service.getStats("hour");

        assertThat(response.labels()).containsExactly("03");
    }

    // ─── groupBy=weekday ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getStats(weekday) deve agregar volumes por dia da semana")
    void getStats_groupByWeekday_shouldAggregateByDay() {
        when(trafficRecordService.findAllEntities()).thenReturn(List.of(
                recordOnDay(DayOfWeek.MONDAY, 100),
                recordOnDay(DayOfWeek.MONDAY, 80),
                recordOnDay(DayOfWeek.FRIDAY, 200)
        ));

        TrafficStatsResponse response = service.getStats("weekday");

        assertThat(response.labels()).contains("MONDAY", "FRIDAY");
        int idxMon = response.labels().indexOf("MONDAY");
        assertThat(response.values().get(idxMon)).isEqualTo(180);
    }

    // ─── groupBy=roadType ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getStats(roadType) deve agregar volumes por tipo de via")
    void getStats_groupByRoadType_shouldAggregateByRoadType() {
        when(trafficRecordService.findAllEntities()).thenReturn(List.of(
                record(8, "ARTERIAL", 100),
                record(9, "ARTERIAL", 80),
                record(10, "HIGHWAY", 200)
        ));

        TrafficStatsResponse response = service.getStats("roadType");

        int idxArterial = response.labels().indexOf("ARTERIAL");
        assertThat(response.values().get(idxArterial)).isEqualTo(180);
        int idxHighway = response.labels().indexOf("HIGHWAY");
        assertThat(response.values().get(idxHighway)).isEqualTo(200);
    }

    @Test
    @DisplayName("getStats(roadType) deve usar 'UNKNOWN' para roadType nulo")
    void getStats_groupByRoadType_nullRoadType_shouldUseUnknown() {
        TrafficRecord r = record(8, null, 50);
        when(trafficRecordService.findAllEntities()).thenReturn(List.of(r));

        TrafficStatsResponse response = service.getStats("roadType");

        assertThat(response.labels()).containsExactly("UNKNOWN");
        assertThat(response.values()).containsExactly(50);
    }

    @Test
    @DisplayName("getStats(roadType) deve usar 'UNKNOWN' para roadType em branco")
    void getStats_groupByRoadType_blankRoadType_shouldUseUnknown() {
        TrafficRecord r = record(8, "   ", 50);
        when(trafficRecordService.findAllEntities()).thenReturn(List.of(r));

        assertThat(service.getStats("roadType").labels()).containsExactly("UNKNOWN");
    }

    // ─── groupBy inválido ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getStats() deve lançar IllegalArgumentException para groupBy inválido")
    void getStats_invalidGroupBy_shouldThrow() {
        when(trafficRecordService.findAllEntities()).thenReturn(
                List.of(record(8, "LOCAL", 100)));

        assertThatThrownBy(() -> service.getStats("invalido"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalido");
    }

    // ─── lista vazia ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getStats() deve retornar listas vazias quando não há registros")
    void getStats_emptyRecords_shouldReturnEmpty() {
        when(trafficRecordService.findAllEntities()).thenReturn(List.of());

        TrafficStatsResponse response = service.getStats("hour");

        assertThat(response.labels()).isEmpty();
        assertThat(response.values()).isEmpty();
    }

    // ─── acumulação ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getStats() deve acumular corretamente volumes do mesmo grupo")
    void getStats_shouldAccumulateVolumes() {
        when(trafficRecordService.findAllEntities()).thenReturn(List.of(
                record(12, "HIGHWAY", 100),
                record(12, "HIGHWAY", 150),
                record(12, "HIGHWAY", 200)
        ));

        TrafficStatsResponse response = service.getStats("hour");

        assertThat(response.labels()).containsExactly("12");
        assertThat(response.values()).containsExactly(450);
    }
}
