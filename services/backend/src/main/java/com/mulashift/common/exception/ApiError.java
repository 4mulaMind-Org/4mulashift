package com.mulashift.common.exception;

import java.time.Instant;
import java.util.List;

/**
 * The single error format returned by every endpoint (see docs/02-design/api-spec.md).
 * correlationId is filled in once the request-correlation filter exists.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        String correlationId,
        List<String> details) {
}
