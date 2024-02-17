package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;

    LoxFunction(Stmt.Function declaration) {
        this.declaration = declaration;
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
        // Create new scope for each function call
        Environment environment = new Environment(interpreter.globals);

        // Safe to assume parameters and arguments have the same
        // length, because visitCallExpr() checks arity before
        // calling call()
        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        // executeBlock automatically restores previous environment
        // that was active when function was called
        interpreter.executeBlock(declaration.body, environment);
        return null;  // TODO: add return values
    }
}