package com.smarttrafficflow.backend.api.dto;

import java.util.List;

public record PagedTrafficRecordResponse(
        List<TrafficRecordResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {
}
