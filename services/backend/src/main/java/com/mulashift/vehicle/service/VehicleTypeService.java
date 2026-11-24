package com.mulashift.vehicle.service;

import java.util.List;

import com.mulashift.vehicle.dto.VehicleTypeResponse;

public interface VehicleTypeService {

    /** All vehicle types, from the smallest to the largest. */
    List<VehicleTypeResponse> listAll();
}
