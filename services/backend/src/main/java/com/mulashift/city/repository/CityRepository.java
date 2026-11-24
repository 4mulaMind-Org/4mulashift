package com.mulashift.city.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mulashift.city.entity.City;

public interface CityRepository extends JpaRepository<City, Long> {

    Optional<City> findByNameAndState(String name, String state);

    List<City> findByActiveTrue();
}
