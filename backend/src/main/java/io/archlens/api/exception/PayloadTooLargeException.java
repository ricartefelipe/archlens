package io.archlens.api.exception;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

public class PayloadTooLargeException extends ClientErrorException {
    public PayloadTooLargeException(String message) {
        super(message, 413);
    }
}
