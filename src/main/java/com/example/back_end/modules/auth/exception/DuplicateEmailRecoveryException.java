package com.example.back_end.modules.auth.exception;

/**
 * Special exception thrown when a duplicate email is detected during registration.
 * This exception signals that the transaction should rollback and recovery should
 * be attempted in a new transaction outside the current context.
 *
 * This is necessary because PostgreSQL marks the entire transaction as "aborted"
 * after a constraint violation, preventing any further SQL commands until rollback.
 */
public class DuplicateEmailRecoveryException extends RuntimeException {

    private final String email;

    public DuplicateEmailRecoveryException(String email) {
        super("Duplicate email detected - recovery needed: " + email);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
