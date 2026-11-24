package com.mulashift.vehicle.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mulashift.vehicle.dto.VehicleTypeResponse;
import com.mulashift.vehicle.entity.VehicleType;
import com.mulashift.vehicle.mapper.VehicleTypeMapper;
import com.mulashift.vehicle.repository.VehicleTypeRepository;

@Service
public class VehicleTypeServiceImpl implements VehicleTypeService {

    private final VehicleTypeRepository vehicleTypeRepository;
    private final VehicleTypeMapper vehicleTypeMapper;

    public VehicleTypeServiceImpl(VehicleTypeRepository vehicleTypeRepository, VehicleTypeMapper vehicleTypeMapper) {
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.vehicleTypeMapper = vehicleTypeMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleTypeResponse> listAll() {
        List<VehicleType> sorted = vehicleTypeRepository.findAll().stream()
                .sorted(Comparator.comparingInt(VehicleType::getMaxCft).thenComparingInt(VehicleType::getMaxKg))
                .toList();
        return vehicleTypeMapper.toResponses(sorted);
    }
}
