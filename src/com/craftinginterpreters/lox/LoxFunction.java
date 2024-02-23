package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;
    private final Environment closure;
    private final boolean isInitializer;

    LoxFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
        this.closure = closure;
        this.declaration = declaration;
        this.isInitializer = isInitializer;
    }

    /*
     * "Binds" this function to a specific instance
     * of a class.  Binding simply creates a scope
     * around the function where the "this" variable
     * refers to the class to which the function is bound.
     * 
     * Note: this environment is created inside of the method's
     * original closure, so all variables in the original closure
     * are accessible from the new inner environment.
     */
    LoxFunction bind(LoxInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new LoxFunction(declaration, environment, isInitializer);
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments){
        // Use the closure environment as the call's parent
        Environment environment = new Environment(closure);

        // Safe to assume parameters and arguments have the same
        // length, because visitCallExpr() checks arity before
        // calling call()
        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        try {
            // executeBlock automatically restores previous environment
            // that was active when function was called
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            /*
             * If the function returns a value, it will be passed back
             * to us via a Return exception, which unwinds the call stack.
             * 
             * If this is an initializer, we force the return statement
             * to return "this."
             */
            return isInitializer ? closure.getAt(0, "this") : returnValue.value;
        }
        
        // Function did not return a value
        // If initializer, return 'this', otherwise return nil
        return isInitializer ? closure.getAt(0, "this") : null;
    }
}