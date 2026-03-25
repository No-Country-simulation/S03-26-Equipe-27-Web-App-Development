package com.smarttrafficflow.backend.api.controller;

import com.smarttrafficflow.backend.api.dto.CreateTrafficRecordRequest;
import com.smarttrafficflow.backend.api.dto.TrafficRecordResponse;
import com.smarttrafficflow.backend.domain.trafficrecords.service.TrafficRecordService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/traffic-records")
public class TrafficRecordController {

    private static final Logger log = LoggerFactory.getLogger(TrafficRecordController.class);

    private final TrafficRecordService trafficRecordService;

    public TrafficRecordController(TrafficRecordService trafficRecordService) {
        this.trafficRecordService = trafficRecordService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrafficRecordResponse create(@Valid @RequestBody CreateTrafficRecordRequest request) {
        log.info("POST /api/traffic-records - creating traffic record for roadType={}, vehicleVolume={}",
                request.roadType(), request.vehicleVolume());
        TrafficRecordResponse response = trafficRecordService.create(request);
        log.info("POST /api/traffic-records - traffic record created with id={}", response.id());
        return response;
    }

    @GetMapping
    public List<TrafficRecordResponse> findAll() {
        List<TrafficRecordResponse> records = trafficRecordService.findAll();
        log.info("GET /api/traffic-records - returning {} records", records.size());
        return records;
    }
}
