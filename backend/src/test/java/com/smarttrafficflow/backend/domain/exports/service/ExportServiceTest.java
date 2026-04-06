package com.smarttrafficflow.backend.domain.exports.service;

import com.smarttrafficflow.backend.api.dto.TrafficRecordResponse;
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
@DisplayName("ExportService — testes unitários")
class ExportServiceTest {

    @Mock
    private TrafficRecordService trafficRecordService;

    @InjectMocks
    private ExportService service;

    private static final UUID FIXED_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final OffsetDateTime FIXED_TIME =
            OffsetDateTime.of(2024, 6, 17, 8, 0, 0, 0, ZoneOffset.UTC);

    private TrafficRecordResponse resp(String roadType, int volume,
                                       String event, String weather, String region) {
        return new TrafficRecordResponse(FIXED_ID, FIXED_TIME, roadType, volume, event, weather, region);
    }

    // ─── Estrutura ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("exportCsv() deve ter cabeçalho correto como primeira linha")
    void exportCsv_shouldHaveCorrectHeader() {
        when(trafficRecordService.findAll()).thenReturn(List.of());

        String csv = service.exportCsv();

        assertThat(csv.split("\n")[0])
                .isEqualTo("id,timestamp,roadType,vehicleVolume,eventType,weather,region");
    }

    @Test
    @DisplayName("exportCsv() sem registros deve retornar apenas cabeçalho")
    void exportCsv_noRecords_shouldReturnOnlyHeader() {
        when(trafficRecordService.findAll()).thenReturn(List.of());

        long lines = service.exportCsv().lines().filter(l -> !l.isEmpty()).count();

        assertThat(lines).isEqualTo(1);
    }

    @Test
    @DisplayName("exportCsv() com 3 registros deve ter cabeçalho + 3 linhas")
    void exportCsv_withRecords_shouldHaveCorrectLineCount() {
        when(trafficRecordService.findAll()).thenReturn(List.of(
                resp("ARTERIAL", 100, "RUSH", "SUNNY", "CENTRO"),
                resp("HIGHWAY", 200, "SHOW", "RAIN", "NORTE"),
                resp("LOCAL", 50, null, null, null)
        ));

        long dataLines = service.exportCsv().lines().filter(l -> !l.isEmpty()).count() - 1;

        assertThat(dataLines).isEqualTo(3);
    }

    // ─── Nulos ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("exportCsv() deve converter null em string vazia")
    void exportCsv_nullFields_shouldBeEmpty() {
        when(trafficRecordService.findAll()).thenReturn(List.of(
                resp("LOCAL", 50, null, null, null)
        ));

        String dataLine = service.exportCsv().split("\n")[1];

        assertThat(dataLine).endsWith(",,,");
    }

    // ─── FIX: Escape RFC 4180 ─────────────────────────────────────────────────

    @Test
    @DisplayName("exportCsv() deve envolver em aspas campo com vírgula interna")
    void exportCsv_commaInField_shouldQuote() {
        when(trafficRecordService.findAll()).thenReturn(List.of(
                resp("ARTERIAL", 100, "obra, show, acidente", "SUNNY", "CENTRO")
        ));

        assertThat(service.exportCsv()).contains("\"obra, show, acidente\"");
    }

    @Test
    @DisplayName("exportCsv() deve escapar aspas internas dobrando-as")
    void exportCsv_quoteInField_shouldDoubleQuote() {
        when(trafficRecordService.findAll()).thenReturn(List.of(
                resp("ARTERIAL", 100, "evento \"vip\"", "SUNNY", "CENTRO")
        ));

        assertThat(service.exportCsv()).contains("\"evento \"\"vip\"\"\"");
    }

    @Test
    @DisplayName("exportCsv() deve envolver em aspas campo com quebra de linha")
    void exportCsv_newlineInField_shouldQuote() {
        when(trafficRecordService.findAll()).thenReturn(List.of(
                resp("ARTERIAL", 100, "linha1\nlinha2", "SUNNY", "CENTRO")
        ));

        assertThat(service.exportCsv()).contains("\"linha1\nlinha2\"");
    }

    @Test
    @DisplayName("exportCsv() não deve adicionar aspas desnecessárias em campos simples")
    void exportCsv_simpleFields_shouldNotQuote() {
        when(trafficRecordService.findAll()).thenReturn(List.of(
                resp("ARTERIAL", 100, "RUSH_HOUR", "SUNNY", "CENTRO")
        ));

        String dataLine = service.exportCsv().split("\n")[1];

        assertThat(dataLine).doesNotContain("\"RUSH_HOUR\"");
        assertThat(dataLine).doesNotContain("\"SUNNY\"");
    }

    @Test
    @DisplayName("exportCsv() deve manter volume como número sem aspas")
    void exportCsv_vehicleVolume_shouldBeUnquotedNumber() {
        when(trafficRecordService.findAll()).thenReturn(List.of(
                resp("LOCAL", 9999, "RUSH", "SUNNY", "CENTRO")
        ));

        String dataLine = service.exportCsv().split("\n")[1];

        assertThat(dataLine).contains(",9999,");
        assertThat(dataLine).doesNotContain("\"9999\"");
    }
}
