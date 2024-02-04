package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {
    /*
     * Stores variable bindings.
     * Note: keys are strings so that all identifier
     * tokens with the same name refer to the same variable.
     */
    private final Map<String, Object> values = new HashMap<>();

    /*
     * Stores a variable binding.
     * Permits overwriting a binding
     * if the provided name already
     * exists in the environment.
     */
    void define(String name, Object value) {
        values.put(name, value);
    }

    /*
     * Retrieves a variable value from the
     * environment.
     * 
     * Throws a RuntimeError if the variable
     * is not found.  Therefore, it is OK
     * to reference a variable before it is
     * defined as long as that reference
     * is not evaluated.
     */
    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        throw new RuntimeError(
            name,
            "Undefined variable '" + name.lexeme + "'."
        );
    }
}