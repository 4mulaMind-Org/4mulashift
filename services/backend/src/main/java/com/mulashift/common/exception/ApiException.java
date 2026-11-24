package com.mulashift.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for business exceptions that map to an HTTP error response.
 * Each module defines its own subclasses (for example CityNotFoundException).
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
