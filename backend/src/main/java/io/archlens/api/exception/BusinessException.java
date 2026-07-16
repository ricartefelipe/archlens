package io.archlens.api.exception;

import jakarta.ws.rs.BadRequestException;

public class BusinessException extends BadRequestException {
    public BusinessException(String message) {
        super(message);
    }
}
