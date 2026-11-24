package com.mulashift.city.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCityRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String state) {
}
