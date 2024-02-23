package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

class Interpreter implements Expr.Visitor<Object>,
                             Stmt.Visitor<Void> {
    
    // Environment stores variable bindings
    // Stays in memory as long as interpreter is running
    final Environment globals = new Environment();
    private Environment environment = globals;
    
    // "Side table" to store the variable scoping levels
    private final Map<Expr, Integer> locals = new HashMap<>();

    Interpreter() {
        // Defines a native clock function
        globals.define("clock", new LoxCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });
    }

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
    
    /*
     * Interprets an if/else statement.
     * If the if condition is true, executes the "then"
     * branch of the statement, otherwise executes
     * the "else" branch, if it exists.
     */
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }

        return null;
    }

    /*
     * Interprets a while statement, executing the
     * while's body while the condition is true.
     */
    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }

        return null;
    }

    /*
     * Interprets a block by executing the block's
     * statements in a newly-scoped environment.
     */
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    /*
     * Interprets a class declaration by declaring
     * the class's name in the current environment,
     * then turning the class syntax node into the runtime
     * representation of a class.
     * 
     * Two-stage binding process allows a class to reference
     * itself inside its own methods.
     */
    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        environment.define(stmt.name.lexeme, null);
        LoxClass klass = new LoxClass(stmt.name.lexeme);
        environment.assign(stmt.name, klass);
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
     * Evaluates a function statement by defining
     * that function and adding it to the environment.
     */
    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        // Initialize the function with the environment current
        // as of the function declaration (not a function call)
        LoxFunction function = new LoxFunction(stmt, environment);
        environment.define(stmt.name.lexeme, function);
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
     * Evaluates a return statement.  If there
     * is a return value, we evaluate it; otherwise
     * we use nil.
     *
     * Throws an exception to unwind the call stack
     * all the way to the initial call() that called
     * the callable from which we are returning.
     */
    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = (stmt.value == null) ? null : evaluate(stmt.value);
        throw new Return(value);
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

    /*
     * Interprets a logical AND/OR expression.
     * Short circuits an OR expression if the left
     * operand is truthy, and short circuits an AND
     * expression if the right operand is falsy.
     * 
     * Returns a value with appropriate truthiness,
     * but the return value is not guaranteed to
     * be a boolean true/false.
     */
    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            // If left is truthy, then whole OR expression
            // evaluates to true
            if (isTruthy(left)) return left;
        } else {
            // If this is an AND expression
            // and left is falsy, the whole AND
            // expression evaluates to false
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookUpVariable(expr.name, expr);
    }

    /*
     * Retrieves a variable from the appropriate scope.

     * If the variable is stored in the locals table,
     * retrieves the variable from the appropriate environment.
     * 
     * Otherwise, assumes the variable must be global and
     * retrieves it from the global environment.
     * 
     * If the variable is not declared in the global environment,
     * a runtime error will be thrown (the variable is not defined).
     */
    private Object lookUpVariable(Token name, Expr expr) {
        Integer distance = locals.get(expr);
        return (distance == null)
            ? globals.get(name)
            : environment.getAt(distance, name.lexeme);
    }

    /*
     * Evaluates an assignment expression and
     * updates the binding in the appropriate environment.
     */
    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        
        Integer distance = locals.get(expr);
        if (distance != null) {
            environment.assignAt(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }

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
     * Interprets a Call expression.
     *
     * Evaluates the callee (typically a function identifier,
     * but could be anything) and evaluates each argument.
     * 
     * Then, performs the function call with the given args.
     */
    @Override
    public Object visitCallExpr(Expr.Call expr) {
        // Callee will usually be an identifier
        // (function name lookup), but could be anything
        Object callee = evaluate(expr.callee);
        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }


        // Interpret the arguments
        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        LoxCallable function = (LoxCallable)callee;

        // Validate the correct number of args were passed
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(
                expr.paren,
                "Expected " + function.arity() + " arguments but got " +
                arguments.size() + " .");
        }

        return function.call(this, arguments);
    }

    /*
     * Visits a get expression (access on instance property).
     * 
     * Evaluates the object being accessed, then gets the appropriate
     * property value from that object.
     */
    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object object = evaluate(expr.object);
        if (object instanceof LoxInstance) {
            return ((LoxInstance) object).get(expr.name);
        }

        throw new RuntimeError(expr.name, "Only instances have properties.");
    }

    /*
     * Visits a set expression (assignment to instance property).
     * 
     * Evaluates the object being accessed, and throws a runtime
     * error if that object is not a class instance.
     * 
     * Sets the property value to the result of evaluating the
     * assignment value.
     */
    @Override
    public Object visitSetExpr(Expr.Set expr) {
        Object object = evaluate(expr.object);

        if (!(object instanceof LoxInstance)) {
            throw new RuntimeError(expr.name, "Only instances have fields.");
        }

        Object value = evaluate(expr.value);
        ((LoxInstance)object).set(expr.name, value);
        return value;
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

    void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
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