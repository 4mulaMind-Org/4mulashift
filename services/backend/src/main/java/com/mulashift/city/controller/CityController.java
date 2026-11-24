package com.mulashift.city.controller;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mulashift.city.dto.CityResponse;
import com.mulashift.city.dto.CreateCityRequest;
import com.mulashift.city.service.CityService;

/**
 * HTTP layer only: validates input, calls the service, returns DTOs.
 * The admin endpoint is open for now; it is restricted to the ADMIN role when authentication is added.
 */
@RestController
@RequestMapping("/api/v1")
public class CityController {

    private final CityService cityService;

    public CityController(CityService cityService) {
        this.cityService = cityService;
    }

    @GetMapping("/cities")
    public List<CityResponse> listActive() {
        return cityService.listActive();
    }

    @GetMapping("/cities/{id}")
    public CityResponse getById(@PathVariable Long id) {
        return cityService.getById(id);
    }

    @PostMapping("/admin/cities")
    public ResponseEntity<CityResponse> create(@Valid @RequestBody CreateCityRequest request) {
        CityResponse created = cityService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/cities/" + created.id())).body(created);
    }
}
