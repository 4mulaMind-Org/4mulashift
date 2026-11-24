package com.mulashift.city.exception;

import org.springframework.http.HttpStatus;

import com.mulashift.common.exception.ApiException;

public class CityNotFoundException extends ApiException {

    public CityNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", "City not found: " + id);
    }
}
