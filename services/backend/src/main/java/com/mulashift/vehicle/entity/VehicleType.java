package com.mulashift.vehicle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A vehicle size class with its capacity limits (the volume matrix from the PRD).
 * Maps to the vehicle_types table created by Flyway migration V2.
 */
@Entity
@Table(name = "vehicle_types")
public class VehicleType {

    @Id
    @Column(name = "vehicle_type", nullable = false, length = 20)
    private String code;

    @Column(name = "max_cft", nullable = false)
    private int maxCft;

    @Column(name = "max_kg", nullable = false)
    private int maxKg;

    /** Required by JPA. Not for application code. */
    protected VehicleType() {
    }

    public VehicleType(String code, int maxCft, int maxKg) {
        this.code = code;
        this.maxCft = maxCft;
        this.maxKg = maxKg;
    }

    public String getCode() {
        return code;
    }

    public int getMaxCft() {
        return maxCft;
    }

    public int getMaxKg() {
        return maxKg;
    }

    /** A load fits only if both its volume and its weight are within the limits. */
    public boolean canCarry(int volumeCft, int weightKg) {
        return volumeCft <= maxCft && weightKg <= maxKg;
    }
}
