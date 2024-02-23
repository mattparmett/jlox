package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

public class LoxClass implements LoxCallable {
    final String name;
    private final Map<String, LoxFunction> methods;

    LoxClass(String name, Map<String, LoxFunction> methods) {
        this.name = name;
        this.methods = methods;
    }

    LoxFunction findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }

        return null;
    }

    /*
     * Tells the interpreter how many arguments
     * to expect in the class constructor.
     */
    @Override
    public int arity() {
        LoxFunction initializer = findMethod("init");
        return (initializer == null) ? 0 : initializer.arity();
    }

    /*
     * Calling a class directly instantiates a new
     * class instance, and returns it.
     */
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        LoxInstance instance = new LoxInstance(this);

        // When the class is called, looks for an init constructor method
        // and immediately binds and calls it with the user's args.
        LoxFunction initializer = findMethod("init");
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }

        return instance;
    }

    @Override
    public String toString() {
        return name;
    }
    
}
