package com.example.back_end.exception;

/**
 * Exception thrown when browser is not paired with any terminal
 */
public class NoTerminalPairedException extends RuntimeException {
    public NoTerminalPairedException(String message) {
        super(message);
    }
}