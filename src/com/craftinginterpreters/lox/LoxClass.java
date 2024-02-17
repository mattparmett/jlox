package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

public class LoxClass implements LoxCallable {
    final String name;

    LoxClass(String name) {
        this.name = name;
    }

    /*
     * Tells the interpreter how many arguments
     * to expect in the class constructor.
     * 
     * TODO: make dynamic for user-defined constructors
     */
    @Override
    public int arity() {
        return 0;
    }

    /*
     * Calling a class directly instantiates a new
     * class instance, and returns it.
     */
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        LoxInstance instance = new LoxInstance(this);
        return instance;
    }

    @Override
    public String toString() {
        return name;
    }
    
}
