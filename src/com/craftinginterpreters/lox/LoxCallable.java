package com.craftinginterpreters.lox;

import java.util.List;

interface LoxCallable {
    // The number of arguments this Callable expects
    int arity();

    /*
     * Returns the value that the Call expression produces from
     * the list of arguments.
     * 
     * Interpeter is passed in case it is needed during
     * the function call.
     */
    Object call(Interpreter interpreter, List<Object> arguments);
}