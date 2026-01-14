package com.poultry.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;
    private final Object details;

    public BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = HttpStatus.BAD_REQUEST;
        this.details = null;
    }

    public BusinessException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.details = null;
    }

    public BusinessException(String message, String errorCode, HttpStatus httpStatus, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.details = details;
    }

    // Common exceptions
    public static BusinessException notFound(String entity, Object id) {
        return new BusinessException(
                entity + " not found with id: " + id,
                "NOT_FOUND",
                HttpStatus.NOT_FOUND
        );
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(message, "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(message, "FORBIDDEN", HttpStatus.FORBIDDEN);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(message, "CONFLICT", HttpStatus.CONFLICT);
    }

    public static BusinessException rateLimitExceeded(String message) {
        return new BusinessException(message, "RATE_LIMIT_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS);
    }

    public static BusinessException invalidState(String message) {
        return new BusinessException(message, "INVALID_STATE", HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
