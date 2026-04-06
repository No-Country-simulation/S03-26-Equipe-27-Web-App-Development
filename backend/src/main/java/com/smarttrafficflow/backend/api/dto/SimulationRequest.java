package com.smarttrafficflow.backend.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SimulationRequest(
        @NotNull @Min(1) @Max(500) Integer recordsToGenerate,
        @NotBlank String scenarioName
) {
}