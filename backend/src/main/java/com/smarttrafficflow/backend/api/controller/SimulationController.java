package com.smarttrafficflow.backend.api.controller;

import com.smarttrafficflow.backend.api.dto.SimulationRequest;
import com.smarttrafficflow.backend.api.dto.TrafficRecordResponse;
import com.smarttrafficflow.backend.domain.simulations.service.SimulationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/simulations")
public class SimulationController {

    private static final Logger log = LoggerFactory.getLogger(SimulationController.class);

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public List<TrafficRecordResponse> generate(@Valid @RequestBody SimulationRequest request) {
        log.info("POST /api/simulations/generate - generating {} records for scenario={}",
                request.recordsToGenerate(), request.scenarioName());
        List<TrafficRecordResponse> generated = simulationService.generate(request);
        log.info("POST /api/simulations/generate - generated {} records", generated.size());
        return generated;
    }
}