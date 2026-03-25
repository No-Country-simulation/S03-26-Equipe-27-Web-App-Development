package com.smarttrafficflow.backend.api.dto;

import java.util.List;

public record TrafficInsightResponse(
        List<String> insights
) {
}
