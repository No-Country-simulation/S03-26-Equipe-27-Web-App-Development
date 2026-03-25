package com.smarttrafficflow.backend.domain.trafficrecords.service;

import com.smarttrafficflow.backend.api.dto.CreateTrafficRecordRequest;
import com.smarttrafficflow.backend.api.dto.TrafficRecordResponse;
import com.smarttrafficflow.backend.domain.trafficrecords.entity.TrafficRecord;
import com.smarttrafficflow.backend.domain.trafficrecords.repository.TrafficRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrafficRecordService {

    private static final Logger log = LoggerFactory.getLogger(TrafficRecordService.class);

    private final TrafficRecordRepository repository;

    public TrafficRecordService(TrafficRecordRepository repository) {
        this.repository = repository;
    }

    public TrafficRecordResponse create(CreateTrafficRecordRequest request) {
        TrafficRecord record = new TrafficRecord();
        record.setTimestamp(request.timestamp());
        record.setRoadType(request.roadType());
        record.setVehicleVolume(request.vehicleVolume());
        record.setEventType(request.eventType());
        record.setWeather(request.weather());
        record.setRegion(request.region());

        TrafficRecord saved = repository.save(record);
        log.info("TrafficRecord saved id={}, roadType={}, timestamp={}",
                saved.getId(), saved.getRoadType(), saved.getTimestamp());
        return toResponse(saved);
    }

    public List<TrafficRecordResponse> findAll() {
        List<TrafficRecordResponse> records = repository.findAll().stream().map(this::toResponse).toList();
        log.debug("TrafficRecord findAll returned {} records", records.size());
        return records;
    }

    public List<TrafficRecord> findAllEntities() {
        List<TrafficRecord> records = repository.findAll();
        log.debug("TrafficRecord findAllEntities returned {} records", records.size());
        return records;
    }

    private TrafficRecordResponse toResponse(TrafficRecord record) {
        return new TrafficRecordResponse(
                record.getId(),
                record.getTimestamp(),
                record.getRoadType(),
                record.getVehicleVolume(),
                record.getEventType(),
                record.getWeather(),
                record.getRegion()
        );
    }
}
