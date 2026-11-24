package com.mulashift.vehicle.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.mulashift.vehicle.dto.VehicleTypeResponse;
import com.mulashift.vehicle.entity.VehicleType;

@Component
public class VehicleTypeMapper {

    public VehicleTypeResponse toResponse(VehicleType type) {
        return new VehicleTypeResponse(type.getCode(), type.getMaxCft(), type.getMaxKg());
    }

    public List<VehicleTypeResponse> toResponses(List<VehicleType> types) {
        return types.stream().map(this::toResponse).toList();
    }
}
