package com.mulashift.city.exception;

import org.springframework.http.HttpStatus;

import com.mulashift.common.exception.ApiException;

public class DuplicateCityException extends ApiException {

    public DuplicateCityException(String name, String state) {
        super(HttpStatus.CONFLICT, "CITY_ALREADY_EXISTS", "City already exists: " + name + ", " + state);
    }
}
