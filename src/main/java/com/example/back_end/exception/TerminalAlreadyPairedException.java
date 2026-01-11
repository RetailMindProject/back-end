package com.example.back_end.exception;

/**
 * Exception thrown when terminal is already paired with another browser
 */
public class TerminalAlreadyPairedException extends RuntimeException {
    public TerminalAlreadyPairedException(String message) {
        super(message);
    }
}