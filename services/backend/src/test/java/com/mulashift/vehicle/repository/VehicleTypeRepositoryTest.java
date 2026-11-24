package com.mulashift.vehicle.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.mulashift.vehicle.entity.VehicleType;

/**
 * Integration test against the real PostgreSQL from docker-compose (containers must be running).
 * Checks the rows seeded by migration V2 and that the entity mapping matches the table.
 * Only the volume limits are asserted, because the weight limits are still placeholders.
 */
@SpringBootTest
class VehicleTypeRepositoryTest {

    @Autowired
    private VehicleTypeRepository vehicleTypeRepository;

    @Test
    void readsTheSeededVehicleTypes() {
        assertThat(vehicleTypeRepository.findAll())
                .extracting(VehicleType::getCode, VehicleType::getMaxCft)
                .containsExactlyInAnyOrder(
                        tuple("MINI", 50),
                        tuple("TEMPO", 200),
                        tuple("TRUCK", 800));
    }
}
