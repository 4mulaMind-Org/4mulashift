package com.mulashift.vehicle.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mulashift.vehicle.entity.VehicleType;

public interface VehicleTypeRepository extends JpaRepository<VehicleType, String> {
}
