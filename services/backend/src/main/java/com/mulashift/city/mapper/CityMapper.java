package com.mulashift.city.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.mulashift.city.dto.CityResponse;
import com.mulashift.city.entity.City;

/** Converts between entities and DTOs. A mapping library such as MapStruct can replace this later. */
@Component
public class CityMapper {

    public City toEntity(String name, String state) {
        return new City(name, state);
    }

    public CityResponse toResponse(City city) {
        return new CityResponse(city.getId(), city.getName(), city.getState(), city.isActive());
    }

    public List<CityResponse> toResponses(List<City> cities) {
        return cities.stream().map(this::toResponse).toList();
    }
}
