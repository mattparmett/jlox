package com.craftinginterpreters.lox;

class Interpreter implements Expr.Visitor<Object> {
    /*
     * Accepts a syntax tree for an expression and
     * evaluates it, returning the string representation
     * of the result Object returned by evaluate().
     * 
     * If the evaluation is not successful,
     * reports a runtime error to the user.
     */
    void interpret(Expr expression) {
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
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

    /*
     * Helper method that sends an expression
     * back into the interpreter's visitor.
     */
    private Object evaluate(Expr expr) {
        return expr.accept(this);
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
}