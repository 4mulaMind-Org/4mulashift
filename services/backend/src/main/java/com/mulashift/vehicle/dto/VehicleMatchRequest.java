package com.mulashift.vehicle.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record VehicleMatchRequest(
        @NotNull @Positive Integer volumeCft,
        @NotNull @Positive Integer weightKg) {
}
