package com.mulashift.vehicle.service;

import com.mulashift.vehicle.dto.VehicleTypeResponse;

/** Rule-based vehicle matching (feature F2). The lead module calls this interface when a request is created. */
public interface VehicleMatchingService {

    /**
     * Returns the smallest vehicle type that can carry the load.
     *
     * @throws com.mulashift.vehicle.exception.LoadExceedsCapacityException if no single vehicle fits
     * @throws IllegalArgumentException if volume or weight is not positive
     */
    VehicleTypeResponse match(int volumeCft, int weightKg);
}
