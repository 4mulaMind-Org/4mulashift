package com.mulashift.vehicle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.mulashift.vehicle.exception.LoadExceedsCapacityException;

/** Runs the real service with the real database (containers must be running). */
@SpringBootTest
class VehicleMatchingIntegrationTest {

    @Autowired
    private VehicleMatchingService matchingService;

    @Test
    void matchesATwoBhkLoadToATempo() {
        assertThat(matchingService.match(180, 900).vehicleType()).isEqualTo("TEMPO");
    }

    @Test
    void rejectsALoadLargerThanTheBiggestVehicle() {
        assertThatThrownBy(() -> matchingService.match(900, 1000))
                .isInstanceOf(LoadExceedsCapacityException.class);
    }
}
