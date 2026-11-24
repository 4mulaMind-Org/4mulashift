package com.mulashift.city.service;

import java.util.List;

import com.mulashift.city.dto.CityResponse;
import com.mulashift.city.dto.CreateCityRequest;

/** Business operations for cities. Other modules depend on this interface, never on the repository. */
public interface CityService {

    CityResponse create(CreateCityRequest request);

    CityResponse getById(Long id);

    List<CityResponse> listActive();
}
