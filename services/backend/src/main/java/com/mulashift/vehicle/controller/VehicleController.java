package com.mulashift.vehicle.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mulashift.vehicle.dto.VehicleMatchRequest;
import com.mulashift.vehicle.dto.VehicleTypeResponse;
import com.mulashift.vehicle.service.VehicleMatchingService;
import com.mulashift.vehicle.service.VehicleTypeService;

/** HTTP layer only. Lets the customer UI estimate the vehicle before creating a request. */
@RestController
@RequestMapping("/api/v1")
public class VehicleController {

    private final VehicleTypeService vehicleTypeService;
    private final VehicleMatchingService vehicleMatchingService;

    public VehicleController(VehicleTypeService vehicleTypeService, VehicleMatchingService vehicleMatchingService) {
        this.vehicleTypeService = vehicleTypeService;
        this.vehicleMatchingService = vehicleMatchingService;
    }

    @GetMapping("/vehicle-types")
    public List<VehicleTypeResponse> listVehicleTypes() {
        return vehicleTypeService.listAll();
    }

    @PostMapping("/vehicle-matches")
    public VehicleTypeResponse match(@Valid @RequestBody VehicleMatchRequest request) {
        return vehicleMatchingService.match(request.volumeCft(), request.weightKg());
    }
}
