package com.mulashift.city.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.mulashift.city.entity.City;

/**
 * Integration test against the real PostgreSQL from docker-compose, so the containers must be running.
 * Each test is rolled back, so the database is left unchanged.
 * Test data uses random names, so it can never collide with real cities already in the development database.
 * Starting the context also makes Hibernate validate the City entity against the cities table.
 */
@SpringBootTest
@Transactional
class CityRepositoryTest {

    @Autowired
    private CityRepository cityRepository;

    private static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void savesAndFindsCityByNameAndState() {
        String name = unique("City");
        cityRepository.saveAndFlush(new City(name, "Test State"));

        var found = cityRepository.findByNameAndState(name, "Test State");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isNotNull();
        assertThat(found.get().isActive()).isTrue();
    }

    @Test
    void rejectsDuplicateNameAndState() {
        String name = unique("City");
        cityRepository.saveAndFlush(new City(name, "Test State"));

        assertThatThrownBy(() -> cityRepository.saveAndFlush(new City(name, "Test State")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsOnlyActiveCities() {
        String closedName = unique("Closed");
        String openName = unique("Open");
        City closed = new City(closedName, "Test State");
        closed.deactivate();
        cityRepository.save(closed);
        cityRepository.saveAndFlush(new City(openName, "Test State"));

        var active = cityRepository.findByActiveTrue();

        assertThat(active).extracting(City::getName)
                .contains(openName)
                .doesNotContain(closedName);
    }
}
