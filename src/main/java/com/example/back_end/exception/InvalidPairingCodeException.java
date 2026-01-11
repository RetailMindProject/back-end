package com.example.back_end.exception;

/**
 * Exception thrown when pairing code is invalid, expired, or already used
 */
public class InvalidPairingCodeException extends RuntimeException {
    public InvalidPairingCodeException(String message) {
        super(message);
    }
}