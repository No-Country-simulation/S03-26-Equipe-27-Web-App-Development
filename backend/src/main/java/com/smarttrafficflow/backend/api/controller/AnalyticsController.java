package com.smarttrafficflow.backend.api.controller;

import com.smarttrafficflow.backend.api.dto.TrafficStatsResponse;
import com.smarttrafficflow.backend.domain.analytics.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/traffic-stats")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private static final List<String> VALID_GROUP_BY = List.of("hour", "roadType", "weekday");

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    public TrafficStatsResponse getStats(@RequestParam String groupBy) {
        log.info("GET /api/traffic-stats - requested groupBy={}", groupBy);

        if (!VALID_GROUP_BY.contains(groupBy)) {
            throw new IllegalArgumentException("Formato de groupBy invalido: " + groupBy);
        }

        TrafficStatsResponse response = analyticsService.getStats(groupBy);
        log.info("GET /api/traffic-stats - returning {} aggregated labels for groupBy={}",
                response.labels().size(), groupBy);
        return response;
    }
}