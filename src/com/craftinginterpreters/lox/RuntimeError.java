package com.craftinginterpreters.lox;

class RuntimeError extends RuntimeException {
    /*
     * Tracks the token that identifies the location
     * of the RuntimeError in the user's Lox code.
     */
    final Token token;

    RuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }
}