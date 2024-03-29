package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    private final Interpreter interpreter;
    
    /*
     * Tracks scopes that are currently "in scope".
     * Each map represents a single block scope, and maps
     * variable names to values (booleans).
     * 
     * Note: only includes local block scopes (anything not tracked here is global).
     */
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();

    // Track whether we are currently inside a function declaration
    private FunctionType currentFunction = FunctionType.NONE;

    // Track whether we are currently inside a class declaration
    private ClassType currentClass = ClassType.NONE;

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    private enum FunctionType {
        NONE,
        FUNCTION,
        INITIALIZER,
        METHOD
    }

    private enum ClassType {
        NONE,
        CLASS,
        SUBCLASS
    }
    
    /*
     * Resolves a block statement by creating a new scope
     * and resolving the block statements within that scope.
     */
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    /*
     * Resolves a class declaration.
     * 
     * The name of the class is bound in the
     * surrounding score, where the class is declared.
     * 
     * Declares and defines the name of the class in
     * the current scope.  The class name is declared
     * before it is defined, allowing the class definition
     * to refer to itself.
     * 
     * Resolves each method defined inside the class.
     */
    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        ClassType enclosingClass = currentClass;
        currentClass = ClassType.CLASS;

        declare(stmt.name);
        define(stmt.name);

        if (stmt.superclass != null) {
            if (stmt.name.lexeme.equals(stmt.superclass.name.lexeme)) {
                Lox.error(stmt.superclass.name, "A class can't inherit from itself.");
            }

            currentClass = ClassType.SUBCLASS;
            resolve(stmt.superclass);

            // If class has a superclass, create a new scope surrounding
            // all its methods, where "super" is defined
            beginScope();
            scopes.peek().put("super", true);
        }

        beginScope();
        scopes.peek().put("this", true);

        for (Stmt.Function method : stmt.methods) {
            FunctionType declaration = FunctionType.METHOD;
            if (method.name.lexeme.equals("init")) {
                declaration = FunctionType.INITIALIZER;
            }
            resolveFunction(method, declaration);
        }

        endScope();
        if (stmt.superclass != null) endScope();  // end "super" scope
        currentClass = enclosingClass;
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    /*
     * Resolves a function declaration.
     * 
     * The name of the function is bound in the
     * surrounding scope, where the function is declared.
     * 
     * The function parameters are bound into the inner
     * function scope.
     * 
     * Declares and defines the name of the function in
     * the current scope, but the function's name is defined
     * before the function body is resolved -- this allows
     * the function to recursively refer to itself in the
     * function body.
     */
    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);
        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    /*
     * Resolves an if-else statement.
     * 
     * Because this is a static analysis, does not
     * follow control flow (both branches are resolved).
     */
    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) resolve(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can't return from top-level code.");
        }

        if (stmt.value != null) {
            if (currentFunction == FunctionType.INITIALIZER) {
                Lox.error(stmt.keyword, "Can't return a value from an initializer.");
            }

            resolve(stmt.value);
        }
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }

    /*
     * Resolves a variable initialization.
     * 
     * Separates the declaration and initialization
     * of the variable to avoid shadowing errors
     * (variable intiializations cannot refer to the
     * variable being declared).
     */
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);

        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }

        define(stmt.name);
        return null;
    }

    /*
     * Resolves an assignment expression by
     * resolving the assignment value (which may
     * contain references to other variables) and
     * then resolving the variable that is assigned to.
     */
    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.right);
        resolve(expr.left);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);
        for (Expr argument : expr.arguments) resolve(argument);
        return null;
    }

    @Override
    public Void visitGetExpr(Expr.Get expr) {
        // Since properties are looked up dynamically,
        // they don't get resolved (only the object does).
        // Actual property access happens in the interpreter.
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;  // nothing to resolve
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitSetExpr(Expr.Set expr) {
        // Instance property is dynamically resolved
        // in the interpreter
        resolve(expr.value);
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitSuperExpr(Expr.Super expr) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword, "Can't use 'super' outside of a class.");
        } else if (currentClass != ClassType.SUBCLASS) {
            Lox.error(expr.keyword, "Can't use 'super' in a class with no superclass.");
        }

        // Treats 'super' similarly to other local
        // variables, since we manually set 'super'
        // in an enclosing scope
        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitThisExpr(Expr.This expr) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword, "Can't use 'this' outside of a class.");
            return null;
        }

        // Treats 'this' similarly to other local
        // variables, since we manually set 'this'
        // in the local scope
        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    /*
     * Resolves a variable expression by:
     *  1. Checking if the variable is accessed in its own initializer
     *      (has been declared but not initialized)
     *  2. Resolve the variable
     */
    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
            Lox.error(expr.name, "Can't read local variable in its own initializer.");
        }

        resolveLocal(expr, expr.name);
        return null;
    }

    void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }

    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }

    /*
     * Resolves a function body.
     * 
     * Creates a new scope for the body, and
     * binds the variables for each of the function
     * parameters inside the new block scope.
     * 
     * Then, the function body is resolved inside the
     * function scope.
     */
    private void resolveFunction(Stmt.Function function, FunctionType type) {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;

        beginScope();

        for (Token param : function.params) {
            declare(param);
            define(param);
        }

        resolve(function.body);
        endScope();
        currentFunction = enclosingFunction;
    }

    private void beginScope() {
        // While interpreter represents scopes as a linked list of envs,
        // resolver uses an actual stack
        scopes.push(new HashMap<String, Boolean>());
    }

    private void endScope() {
        scopes.pop();
    }

    /*
     * Declare a variable in the current (innermost) local scope.
     */
    private void declare(Token name) {
        if (scopes.isEmpty()) return;
        Map<String, Boolean> scope = scopes.peek();

        // Report error if user tries to re-declare a variable
        // in the same scope
        if (scope.containsKey(name.lexeme)) {
            Lox.error(name, "A variable with name " + name.lexeme +
                " already exists in this scope.");
        }

        // Bind to false to indicate initializer not yet resolved 
        scope.put(name.lexeme, false);
    }

    /*
     * "Defining" a variable in a scope essentially
     * marks that the variable has been initialized (true)
     */
    private void define(Token name) {
        if (scopes.isEmpty()) return;
        scopes.peek().put(name.lexeme, true);
    }

    /*
     * Resolves a variable by looking at scopes from the
     * innermost scope outward to the most global scope on the stack.
     * 
     * Once the variable is found in a scope, resolve it and take note
     * of the numebr of "steps" we took from the innermost scope.
     * 
     * If the variable is not found in any block scope, leave it unresolved
     * and assume the variable is global.
     */
    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }
}
