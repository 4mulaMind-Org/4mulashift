package com.mulashift.city.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import com.mulashift.city.dto.CityResponse;
import com.mulashift.city.dto.CreateCityRequest;
import com.mulashift.city.entity.City;
import com.mulashift.city.exception.CityNotFoundException;
import com.mulashift.city.exception.DuplicateCityException;
import com.mulashift.city.mapper.CityMapper;
import com.mulashift.city.repository.CityRepository;

/** Unit test: no Spring and no database, so it runs in milliseconds. */
@ExtendWith(MockitoExtension.class)
class CityServiceImplTest {

    @Mock
    private CityRepository cityRepository;

    private CityServiceImpl cityService;

    @BeforeEach
    void setUp() {
        cityService = new CityServiceImpl(cityRepository, new CityMapper());
    }

    @Test
    void createTrimsInputAndReturnsSavedCity() {
        when(cityRepository.findByNameAndState("Mathura", "Uttar Pradesh")).thenReturn(Optional.empty());
        when(cityRepository.saveAndFlush(any(City.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CityResponse response = cityService.create(new CreateCityRequest("  Mathura ", "Uttar Pradesh"));

        assertThat(response.name()).isEqualTo("Mathura");
        assertThat(response.state()).isEqualTo("Uttar Pradesh");
        assertThat(response.active()).isTrue();
    }

    @Test
    void createRejectsDuplicateBeforeSaving() {
        when(cityRepository.findByNameAndState("Agra", "Uttar Pradesh"))
                .thenReturn(Optional.of(new City("Agra", "Uttar Pradesh")));

        assertThatThrownBy(() -> cityService.create(new CreateCityRequest("Agra", "Uttar Pradesh")))
                .isInstanceOf(DuplicateCityException.class);
        verify(cityRepository, never()).saveAndFlush(any());
    }

    @Test
    void createTranslatesDatabaseUniqueViolationIntoDuplicateCity() {
        when(cityRepository.findByNameAndState("Agra", "Uttar Pradesh")).thenReturn(Optional.empty());
        when(cityRepository.saveAndFlush(any(City.class))).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> cityService.create(new CreateCityRequest("Agra", "Uttar Pradesh")))
                .isInstanceOf(DuplicateCityException.class);
    }

    @Test
    void getByIdReturnsCity() {
        City city = new City("Noida", "Uttar Pradesh");
        ReflectionTestUtils.setField(city, "id", 7L);
        when(cityRepository.findById(7L)).thenReturn(Optional.of(city));

        CityResponse response = cityService.getById(7L);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.name()).isEqualTo("Noida");
    }

    @Test
    void getByIdThrowsWhenCityIsMissing() {
        when(cityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cityService.getById(99L)).isInstanceOf(CityNotFoundException.class);
    }

    @Test
    void listActiveMapsRepositoryResult() {
        when(cityRepository.findByActiveTrue()).thenReturn(List.of(new City("Delhi", "Delhi")));

        assertThat(cityService.listActive()).extracting(CityResponse::name).containsExactly("Delhi");
    }
}
