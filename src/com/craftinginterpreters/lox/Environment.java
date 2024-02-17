package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {
    /*
     * Stores a reference to the Environment
     * that is immediately closing this Environment.
     * 
     * Used for lexical scoping; we first look up
     * identifiers in this Environment, and if not found,
     * we expand our search to the enclosing Environment.
     */
    final Environment enclosing;

    /*
     * Stores variable bindings.
     * Note: keys are strings so that all identifier
     * tokens with the same name refer to the same variable.
     */
    private final Map<String, Object> values = new HashMap<>();

    // Use this constructor for the global Environment,
    // which should have no enclosing Environment.
    Environment() {
        enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

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
     * Retrieves the enclosing environment
     * [distance] scopes outside of the current.
     */
    Environment ancestor(int distance) {
        Environment environment = this;
        while (distance-- > 0) environment = environment.enclosing;
        return environment;
    }

    /*
     * Retrieves a variable in the environment
     * [distance] scopes outside of the current.
     * 
     * No need to check if the variable exists,
     * because the resolver pass already found it.
     */
    Object getAt(int distance, String name) {
        return ancestor(distance).values.get(name);
    }

    /*
     * Inserts a variable into the environment
     * [distance] scopes outside of the current.
     */
    void assignAt(int distance, Token name, Object value) {
        ancestor(distance).values.put(name.lexeme, value);
    }

    /*
     * Updates an assignment in the environment.
     * If the variable isn't in this environment,
     * tries to update the variable in the enclosing
     * environment.
     * 
     * Throws a RuntimeError if the given token
     * is not currently stored in the environment.
     */
    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(
            name,
            "Undefined variable '" + name.lexeme + "'."
        );
    }

    /*
     * Retrieves a variable value from the
     * environment.  If the variable is not
     * found in this environment, we try to
     * find it in the enclosing environment.
     * 
     * Throws a RuntimeError if the variable
     * is not found and there is no enclosing
     * environment.  Therefore, it is OK
     * to reference a variable before it is
     * defined as long as that reference
     * is not evaluated.
     */
    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        if (enclosing != null) return enclosing.get(name);

        throw new RuntimeError(
            name,
            "Undefined variable '" + name.lexeme + "'."
        );
    }
}