package com.craftinginterpreters.lox;

import java.util.List;

class Interpreter implements Expr.Visitor<Object>,
                             Stmt.Visitor<Void> {
    
    // Environment stores variable bindings
    // Stays in memory as long as interpreter is running
    private Environment environment = new Environment();

    /*
     * Accepts a syntax tree for an expression and
     * evaluates it.
     * 
     * If the evaluation is not successful,
     * reports a runtime error to the user.
     */
    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    /*
     * Visitor methods for Stmt
     */

    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    /*
     * Evaluates an expression statement.
     * Statements produce no values, so return type is void.
     */
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);  // discard value
        return null;
    }

    /*
     * Evaluates a print statement by evaluating
     * the inner expression, converting it to string,
     * and printing to stdout.
     */
    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    /*
     * Evaluates a variable declaration
     * and stores the new binding in the
     * environment.
     * 
     * If an initializer is provided, the variable
     * is set to the value of the evaluated
     * initializer.  Otherwise, the variable
     * is initialized with a value of nil.
     */
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    /*
     * Visitor methods for Expr
     */

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    /*
     * Evaluates an assignment expression and
     * updates the binding in the environment.
     */
    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    /*
     * Evaluates a unary expression by first
     * evaluating the operand, then applying
     * the unary operator to the operand.
     */
    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
        }

        // Unreachable
        return null;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        /*
         * A Grouping node (result of using explicit parenthesis
         * in an expression) has a reference to an inner node
         * for the expression contained within the parenthesis.
         * 
         * We can simply extract this node, and visit it with
         * the interpreter.
         */
        return evaluate(expr.expression);
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            // Arithmetic operators
            case PLUS:
                // Allow for numeric addition and string concatenation
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }

                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }

                throw new RuntimeError(
                    expr.operator,
                    "Operands must be two numbers or two strings."
                );
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;

            // Comparison operators
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;

            // Equality operators
            // Support operands of any type (can be mixed)
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
        }

        // Unreachable
        return null;
    }

    /*
     * Helper methods
     */

    /*
     * Helper method that sends a statement
     * back into the interpreter's visitor.
     */
    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    /*
     * Executes a list of statements in the context of
     * the given environment (scope).
     * 
     * Updates the interpreter's environment to the given
     * environment, visits all of the statements in the block,
     * and then resets the interpreter's environment to the
     * previous state.
     */
    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;
            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            // Reset the environment even if exception is thrown
            this.environment = previous;
        }
    }

    /*
     * Helper method that sends an expression
     * back into the interpreter's visitor.
     */
    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    /*
     * Determines whether the given object is
     * truthy or falsey.
     * 
     * Treats false and nil as falsey, and everything
     * else as truthy.
     */
    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    /*
     * Checks whether the given operand is a number (Double),
     * and throws a Lox RuntimeError if not.
     * 
     * Can be used to guard casts and evaluations that require
     * one or more numeric operators.
     */
    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    /*
     * Determines whether two objects are equal,
     * according to Lox's definition.
     */
    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            
            // We use doubles internally to represent Lox ints;
            // need to chop off Java's trailing decimal.
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }

            return text;
        }

        return object.toString();
    }

}