package com.mulashift.city.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mulashift.city.dto.CityResponse;
import com.mulashift.city.dto.CreateCityRequest;
import com.mulashift.city.entity.City;
import com.mulashift.city.exception.CityNotFoundException;
import com.mulashift.city.exception.DuplicateCityException;
import com.mulashift.city.mapper.CityMapper;
import com.mulashift.city.repository.CityRepository;

@Service
public class CityServiceImpl implements CityService {

    private final CityRepository cityRepository;
    private final CityMapper cityMapper;

    public CityServiceImpl(CityRepository cityRepository, CityMapper cityMapper) {
        this.cityRepository = cityRepository;
        this.cityMapper = cityMapper;
    }

    @Override
    @Transactional
    public CityResponse create(CreateCityRequest request) {
        String name = request.name().trim();
        String state = request.state().trim();

        // Friendly early check. The unique constraint in the database is the real guarantee,
        // because two requests can pass this check at the same moment.
        if (cityRepository.findByNameAndState(name, state).isPresent()) {
            throw new DuplicateCityException(name, state);
        }
        try {
            City saved = cityRepository.saveAndFlush(cityMapper.toEntity(name, state));
            return cityMapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateCityException(name, state);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CityResponse getById(Long id) {
        return cityRepository.findById(id)
                .map(cityMapper::toResponse)
                .orElseThrow(() -> new CityNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CityResponse> listActive() {
        return cityMapper.toResponses(cityRepository.findByActiveTrue());
    }
}
