package com.mulashift.vehicle.exception;

import org.springframework.http.HttpStatus;

import com.mulashift.common.exception.ApiException;

/** Raised when no single vehicle type can carry the load. The load must be split or reviewed manually. */
public class LoadExceedsCapacityException extends ApiException {

    public LoadExceedsCapacityException(int volumeCft, int weightKg) {
        super(HttpStatus.valueOf(422), "LOAD_EXCEEDS_CAPACITY",
                "No single vehicle can carry this load (" + volumeCft + " cft, " + weightKg
                        + " kg). Split the load or request a manual review.");
    }
}
