package com.smarttrafficflow.backend.api.dto;

import java.util.List;

public record StreetSearchResponse(
        List<StreetResponse> items,
        int limit,
        int offset,
        long total
) {
}
