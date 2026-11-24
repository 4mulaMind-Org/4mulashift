package com.mulashift.vehicle.service;

import java.util.Comparator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mulashift.vehicle.dto.VehicleTypeResponse;
import com.mulashift.vehicle.entity.VehicleType;
import com.mulashift.vehicle.exception.LoadExceedsCapacityException;
import com.mulashift.vehicle.mapper.VehicleTypeMapper;
import com.mulashift.vehicle.repository.VehicleTypeRepository;

@Service
public class VehicleMatchingServiceImpl implements VehicleMatchingService {

    private final VehicleTypeRepository vehicleTypeRepository;
    private final VehicleTypeMapper vehicleTypeMapper;

    public VehicleMatchingServiceImpl(VehicleTypeRepository vehicleTypeRepository, VehicleTypeMapper vehicleTypeMapper) {
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.vehicleTypeMapper = vehicleTypeMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleTypeResponse match(int volumeCft, int weightKg) {
        if (volumeCft <= 0 || weightKg <= 0) {
            throw new IllegalArgumentException("Volume and weight must be positive.");
        }
        // Sorted in memory (smallest first) so the result does not depend on database row order.
        return vehicleTypeRepository.findAll().stream()
                .sorted(Comparator.comparingInt(VehicleType::getMaxCft).thenComparingInt(VehicleType::getMaxKg))
                .filter(type -> type.canCarry(volumeCft, weightKg))
                .findFirst()
                .map(vehicleTypeMapper::toResponse)
                .orElseThrow(() -> new LoadExceedsCapacityException(volumeCft, weightKg));
    }
}
