package com.smarttrafficflow.backend.domain.trafficrecords.service;

import com.smarttrafficflow.backend.api.dto.CreateTrafficRecordRequest;
import com.smarttrafficflow.backend.api.dto.TrafficRecordResponse;
import com.smarttrafficflow.backend.domain.trafficrecords.entity.TrafficRecord;
import com.smarttrafficflow.backend.domain.trafficrecords.repository.TrafficRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrafficRecordService — testes unitários")
class TrafficRecordServiceTest {

    @Mock
    private TrafficRecordRepository repository;

    @InjectMocks
    private TrafficRecordService service;

    private OffsetDateTime now;

    @BeforeEach
    void setUp() {
        now = OffsetDateTime.of(2024, 6, 17, 8, 0, 0, 0, ZoneOffset.UTC);
    }

    private TrafficRecord buildEntity(String roadType, int volume) {
        TrafficRecord r = new TrafficRecord();
        r.setId(UUID.randomUUID());
        r.setTimestamp(now);
        r.setRoadType(roadType);
        r.setVehicleVolume(volume);
        r.setRegion("DEFAULT_REGION");
        return r;
    }

    // ─── create() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create() deve persistir e retornar resposta mapeada")
    void create_shouldPersistAndReturnMappedResponse() {
        CreateTrafficRecordRequest request = new CreateTrafficRecordRequest(
                now, "ARTERIAL", 120, "RUSH_HOUR", "SUNNY", "CENTRO");
        TrafficRecord saved = buildEntity("ARTERIAL", 120);
        saved.setEventType("RUSH_HOUR");
        saved.setWeather("SUNNY");
        saved.setRegion("CENTRO");
        when(repository.save(any())).thenReturn(saved);

        TrafficRecordResponse response = service.create(request);

        assertThat(response.id()).isEqualTo(saved.getId());
        assertThat(response.roadType()).isEqualTo("ARTERIAL");
        assertThat(response.vehicleVolume()).isEqualTo(120);
        assertThat(response.eventType()).isEqualTo("RUSH_HOUR");
        assertThat(response.weather()).isEqualTo("SUNNY");
        assertThat(response.region()).isEqualTo("CENTRO");
    }

    @Test
    @DisplayName("create() deve mapear todos os campos da request para a entidade")
    void create_shouldMapAllFieldsToEntity() {
        CreateTrafficRecordRequest request = new CreateTrafficRecordRequest(
                now, "HIGHWAY", 300, "SHOW", "RAIN", "NORTE");
        when(repository.save(any())).thenAnswer(inv -> {
            TrafficRecord e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        service.create(request);

        ArgumentCaptor<TrafficRecord> captor = ArgumentCaptor.forClass(TrafficRecord.class);
        verify(repository).save(captor.capture());
        TrafficRecord captured = captor.getValue();

        assertThat(captured.getRoadType()).isEqualTo("HIGHWAY");
        assertThat(captured.getVehicleVolume()).isEqualTo(300);
        assertThat(captured.getEventType()).isEqualTo("SHOW");
        assertThat(captured.getWeather()).isEqualTo("RAIN");
        assertThat(captured.getRegion()).isEqualTo("NORTE");
        assertThat(captured.getTimestamp()).isEqualTo(now);
    }

    @Test
    @DisplayName("create() deve aceitar campos opcionais nulos sem lançar exceção")
    void create_shouldHandleNullOptionalFields() {
        CreateTrafficRecordRequest request = new CreateTrafficRecordRequest(
                now, "LOCAL", 50, null, null, null);
        TrafficRecord saved = buildEntity("LOCAL", 50);
        when(repository.save(any())).thenReturn(saved);

        TrafficRecordResponse response = service.create(request);

        assertThat(response).isNotNull();
        verify(repository).save(any(TrafficRecord.class));
    }

    // ─── findAll() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll() deve retornar lista mapeada de respostas")
    void findAll_shouldReturnMappedList() {
        when(repository.findAll()).thenReturn(List.of(
                buildEntity("LOCAL", 50),
                buildEntity("HIGHWAY", 200)
        ));

        List<TrafficRecordResponse> result = service.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TrafficRecordResponse::roadType)
                .containsExactlyInAnyOrder("LOCAL", "HIGHWAY");
    }

    @Test
    @DisplayName("findAll() deve retornar lista vazia quando banco está vazio")
    void findAll_shouldReturnEmptyList() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.findAll()).isEmpty();
    }

    @Test
    @DisplayName("findAll() deve preservar todos os campos na resposta")
    void findAll_shouldPreserveAllFields() {
        TrafficRecord record = buildEntity("ARTERIAL", 80);
        record.setEventType("EVENTO");
        record.setWeather("CLOUDY");
        record.setRegion("SUL");
        when(repository.findAll()).thenReturn(List.of(record));

        TrafficRecordResponse response = service.findAll().get(0);

        assertThat(response.eventType()).isEqualTo("EVENTO");
        assertThat(response.weather()).isEqualTo("CLOUDY");
        assertThat(response.region()).isEqualTo("SUL");
    }

    // ─── findAllEntities() ────────────────────────────────────────────────────

    @Test
    @DisplayName("findAllEntities() deve retornar entidades brutas do banco")
    void findAllEntities_shouldReturnEntityList() {
        TrafficRecord r = buildEntity("ARTERIAL", 80);
        when(repository.findAll()).thenReturn(List.of(r));

        List<TrafficRecord> entities = service.findAllEntities();

        assertThat(entities).hasSize(1);
        assertThat(entities.get(0).getRoadType()).isEqualTo("ARTERIAL");
    }

    @Test
    @DisplayName("findAllEntities() deve retornar lista vazia quando banco está vazio")
    void findAllEntities_shouldReturnEmptyList() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.findAllEntities()).isEmpty();
    }
}
