package com.urva.myfinance.coinTrack;

/**
 * Custom exception for unauthorized access attempts
 * Returns HTTP 401 status code
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnauthorizedException() {
        super("Unauthorized access");
    }
}