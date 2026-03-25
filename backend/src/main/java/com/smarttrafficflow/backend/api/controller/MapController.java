package com.smarttrafficflow.backend.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/traffic-map")
public class MapController {

    @GetMapping
    public List<Map<String, Object>> getMapData() {
        return List.of(
                Map.of("region", "DEFAULT_REGION", "lat", -23.5505, "lng", -46.6333)
        );
    }
}
