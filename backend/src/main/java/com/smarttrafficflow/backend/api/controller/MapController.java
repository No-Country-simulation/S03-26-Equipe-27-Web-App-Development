package com.smarttrafficflow.backend.api.controller;

import com.smarttrafficflow.backend.domain.trafficrecords.repository.TrafficRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/traffic-map")
public class MapController {

    private static final Logger log = LoggerFactory.getLogger(MapController.class);

    private static final Map<String, double[]> REGION_COORDINATES = Map.of(
            "DEFAULT_REGION", new double[]{-23.5505, -46.6333},
            "CENTRO",         new double[]{-23.5430, -46.6393},
            "NORTE",          new double[]{-23.4900, -46.6260},
            "SUL",            new double[]{-23.6200, -46.6350},
            "LESTE",          new double[]{-23.5500, -46.5200},
            "OESTE",          new double[]{-23.5400, -46.7200}
    );

    private static final double[] FALLBACK_COORDS = {-23.5505, -46.6333};

    private final TrafficRecordRepository repository;

    public MapController(TrafficRecordRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Map<String, Object>> getMapData() {
        List<String> regions = repository.findDistinctRegions();
        log.info("GET /api/traffic-map - found {} distinct regions", regions.size());

        if (regions.isEmpty()) {
            return List.of(Map.of("region", "DEFAULT_REGION",
                    "lat", FALLBACK_COORDS[0], "lng", FALLBACK_COORDS[1]));
        }

        return regions.stream()
                .map(region -> {
                    double[] coords = REGION_COORDINATES.getOrDefault(region, FALLBACK_COORDS);
                    return (Map<String, Object>) Map.<String, Object>of(
                            "region", region, "lat", coords[0], "lng", coords[1]);
                })
                .toList();
    }
}