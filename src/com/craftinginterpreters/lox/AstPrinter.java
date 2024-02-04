package com.craftinginterpreters.lox;

class AstPrinter implements Expr.Visitor<String> {
  String print(Expr expr) {
    return expr.accept(this);
  }

  @Override
  public String visitVariableExpr(Expr.Variable expr) {
    return expr.name.lexeme;
  }

  @Override
  public String visitBinaryExpr(Expr.Binary expr) {
    return parenthesize(
      expr.operator.lexeme,
      expr.left,
      expr.right
    );
  }

  @Override
  public String visitGroupingExpr(Expr.Grouping expr) {
    return parenthesize("group", expr.expression);
  }

  @Override
  public String visitLiteralExpr(Expr.Literal expr) {
    if (expr.value == null) {
      return "nil";
    }

    return expr.value.toString();
  }

  @Override
  public String visitUnaryExpr(Expr.Unary expr) {
    return parenthesize(expr.operator.lexeme, expr.right);
  }

  /*
   * Takes an expression name and a list of subexpressions,
   * and wraps the expression in parenthesis with appropriate
   * formatting.  Recursively renders subexpressions
   * by calling AstPrinter.print() on each subexpression.
   *
   * @param name name of the expression (1st argument in parens)
   * @param exprs list of expressions to parenthesize
   * @return parenthesized expression
   */
  private String parenthesize(String name, Expr... exprs) {
    StringBuilder builder = new StringBuilder();

    builder.append("(").append(name);

    for (Expr expr : exprs) {
      builder.append(" ");
      builder.append(expr.accept(this));  // recursive step
    }

    builder.append(")");
    return builder.toString();
  }
}
