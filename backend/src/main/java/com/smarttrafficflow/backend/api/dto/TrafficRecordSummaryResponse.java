package com.smarttrafficflow.backend.api.dto;

import java.time.OffsetDateTime;

public record TrafficRecordSummaryResponse(
        long recordCount,
        long totalVehicleVolume,
        long uniqueStreetCount,
        int averageVehicleVolume,
        OffsetDateTime latestTimestamp
) {
}
