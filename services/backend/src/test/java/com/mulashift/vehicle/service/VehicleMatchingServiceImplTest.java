package com.mulashift.vehicle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mulashift.vehicle.entity.VehicleType;
import com.mulashift.vehicle.exception.LoadExceedsCapacityException;
import com.mulashift.vehicle.mapper.VehicleTypeMapper;
import com.mulashift.vehicle.repository.VehicleTypeRepository;

/** Unit test of the matching rule: no Spring and no database. */
@ExtendWith(MockitoExtension.class)
class VehicleMatchingServiceImplTest {

    @Mock
    private VehicleTypeRepository vehicleTypeRepository;

    private VehicleMatchingServiceImpl matchingService;

    @BeforeEach
    void setUp() {
        matchingService = new VehicleMatchingServiceImpl(vehicleTypeRepository, new VehicleTypeMapper());
    }

    /** Deliberately returned in a mixed order, to prove the result does not depend on row order. */
    private void stubVehicleTypes() {
        when(vehicleTypeRepository.findAll()).thenReturn(List.of(
                new VehicleType("TRUCK", 800, 7000),
                new VehicleType("MINI", 50, 500),
                new VehicleType("TEMPO", 200, 1500)));
    }

    @ParameterizedTest(name = "{0} cft and {1} kg -> {2}")
    @CsvSource({
            "10,   100,  MINI",
            "50,   500,  MINI",
            "51,   100,  TEMPO",
            "40,   600,  TEMPO",
            "200,  1500, TEMPO",
            "201,  100,  TRUCK",
            "800,  7000, TRUCK"
    })
    void picksTheSmallestVehicleThatFits(int volumeCft, int weightKg, String expectedType) {
        stubVehicleTypes();

        assertThat(matchingService.match(volumeCft, weightKg).vehicleType()).isEqualTo(expectedType);
    }

    @Test
    void failsWhenVolumeExceedsTheLargestVehicle() {
        stubVehicleTypes();

        assertThatThrownBy(() -> matchingService.match(801, 100))
                .isInstanceOf(LoadExceedsCapacityException.class);
    }

    @Test
    void failsWhenWeightExceedsTheLargestVehicle() {
        stubVehicleTypes();

        assertThatThrownBy(() -> matchingService.match(100, 7001))
                .isInstanceOf(LoadExceedsCapacityException.class);
    }

    @Test
    void failsWhenNoVehicleTypesAreConfigured() {
        when(vehicleTypeRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> matchingService.match(10, 10))
                .isInstanceOf(LoadExceedsCapacityException.class);
    }

    @Test
    void rejectsNonPositiveInput() {
        assertThatThrownBy(() -> matchingService.match(0, 100)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> matchingService.match(100, -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
