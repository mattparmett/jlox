package com.craftinginterpreters.lox;

import java.util.List;
import java.util.ArrayList;
import static com.craftinginterpreters.lox.TokenType.*;

class Parser {
  // Sentinel class used to unwind the parser
  private static class ParseError extends RuntimeException {}

  private final List<Token> tokens;
  private int current = 0;

  /*
   * The parser consumes a flat input sequence
   * of tokens (rather than characters, like the scanner).
   *
   * We store the list of tokens, and use current
   * to keep track of the next token to be parsed.
   */
  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  /*
   * Parses a program into a list of statements.
   */
  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
      statements.add(declaration());
    }

    return statements;
  }

  /*
   * Parser Rules
   * Implemented in ascending order of precedence.
   */

  /*
   * Parses a declaration.  If the current token
   * is the "var" keyword, treats the following token
   * as a declaration.  Otherwise, falls through to
   * statement.
   * 
   * If we encounter a parse error, catches the
   * error and synchronizes the parser so it
   * can continue parsing at the next declaration.
   */
  private Stmt declaration() {
    try {
      if (match(VAR)) return varDeclaration();
      return statement();
    } catch (ParseError error) {
      synchronize();
      return null;
    }
  }

  /*
   * Parses a variable declaration.
   * Note that it is optional to initialize
   * variables when they are declared.
   */
  private Stmt varDeclaration() {
    // We have already parsed the "var" keyword, so the
    // next token should be a variable name
    Token name = consume(IDENTIFIER, "Expect variable name.");

    Expr initializer = null;
    if (match(EQUAL)) {
      initializer = expression();
    }

    consume(SEMICOLON, "Expect ';' after variable declaration.");
    return new Stmt.Var(name, initializer);
  }

  /*
   * Statement can be an expression or a print statement.
   * If the next token doesn't look like a known statement,
   * we assume it is an expression statement.
   */
  private Stmt statement() {
    if (match(PRINT)) return printStatement();
    return expressionStatement();
  }

  /*
   * Parses the expression after the print
   * token, consumes the terminating semicolon,
   * and returns the syntax tree.
   */
  private Stmt printStatement() {
    // Note: we have already matched and consumed
    // the print token before getting here
    Expr value = expression();
    consume(SEMICOLON, "Expect ';' after value.");
    return new Stmt.Print(value);
  }

  /*
   * Parses an expression and consumes the terminating
   * semicolon, returning the syntax tree.
   */
  private Stmt expressionStatement() {
    Expr expr = expression();
    consume(SEMICOLON, "Expect ';' after expression.");
    return new Stmt.Expression(expr);
  }

  /*
   * Expression matches any expression
   * at any precedence level.
   *
   * Since Equality has the lowest precedence,
   * we can match that and cover everything with
   * higher precedence.
   */
  private Expr expression() {
    return equality();
  }

  /*
   * Equality matches:
   *  comparison ( ( "!=" | "==" ) comparison )*
   *
   * Our parser needs to store the left and right
   * comparison, as well as the operator, and
   * return a Binary expression.
   *
   * Because there may be more than one chained
   * equality expression (that's the asterisk in
   * the grammar above), we use a while loop to
   * chain the binary expressions until we don't
   * find an equality operator.
   *
   * We also need to match the case where there
   * are no equality operators, and we have a single
   * Comparison expression.  We do that by first
   * calling comparison to get our left expression,
   * and we return that if there are no equality operators.
   */
  private Expr equality() {
    Expr expr = comparison();

    while (match(BANG_EQUAL, EQUAL_EQUAL)) {
      // Match just consumed the equality operator
      Token operator = previous();
      Expr right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr comparison() {
    Expr expr = term();

    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      // Match just consumed the comparison operator
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr term() {
    Expr expr = factor();

    while (match(MINUS, PLUS)) {
      // Match just consumed the add/sub operator
      Token operator = previous();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr factor() {
    Expr expr = unary();

    while (match(SLASH, STAR)) {
      // Match just consumed the div/mult operator
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  /*
   * To parse a unary expression, we first examine
   * the current token.  If it's a ! or -, we must
   * have a unary expression.
   *
   * If we have a unary expression, we grab the token
   * and then recursively parse the right term as another
   * unary expression.
   */
  private Expr unary() {
    if (match(BANG, MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }
    
    return primary();
  }

  private Expr primary() {
    if (match(FALSE)) {
      return new Expr.Literal(false);
    }

    if (match(TRUE)) {
      return new Expr.Literal(true);
    }

    if (match(NIL)) {
      return new Expr.Literal(null);
    }

    if (match(NUMBER, STRING)) {
      return new Expr.Literal(previous().literal);
    }

    if (match(IDENTIFIER)) {
      return new Expr.Variable(previous());
    }

    /*
     * After we match an opening paren and parse the
     * expression inside, we must find a closing paren.
     *
     * If we don't, we need to raise an error.
     */
    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expected ')' after expression.");
      return new Expr.Grouping(expr);
    }

    throw error(peek(), "Expect expression.");
  }

  /*
   * Checks to see if the current token is
   * a token of any of the given types.
   *
   * If it is, we advance the parser (consume
   * that token) and return true.  Otherwise,
   * we return false without consuming the token.
   */
  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }

    return false;
  }

  /*
   * Checks to see if the next token is of the
   * expected type.
   * 
   * If so, consumes the token.  Otherwise,
   * we've hit an error and report it.
   */
  private Token consume(TokenType type, String message) {
    if (check(type)) return advance();

    throw error(peek(), message);
  }

  /*
   * Checks if the current token is the given
   * type without consuming that token.
   */
  private boolean check(TokenType type) {
    if (this.isAtEnd()) {
      return false;
    }

    return peek().type == type;
  }

  /*
   * Consumes the current token and
   * returns that token to the caller.
   */
  private Token advance() {
    if (!this.isAtEnd()) {
      current++;
    }

    return previous();
  }

  /*
   * Returns true if the parser is at
   * the end of the token list.
   */
  private boolean isAtEnd() {
    return peek().type == EOF;
  }

  /*
   * Returns the current token
   * without consuming.
   */
  private Token peek() {
    return tokens.get(current);
  }

  /*
   * Returns the previous token.
   */
  private Token previous() {
    return tokens.get(current - 1);
  }

  /*
   * Shows the user a parser error and returns a
   * ParserError sentinel, which is used to unwind the parser.
   */
  private ParseError error(Token token, String message) {
    Lox.error(token, message);
    return new ParseError();
  }

  /*
   * Synchronizes the parser's state
   * after a parsing error is encountered.
   * 
   * Discards tokens that would have likely
   * caused cascading parsing errors, and lets
   * us continue parsing the rest of the file
   * starting at the next statement after the error.
   */
  private void synchronize() {
    advance();

    // Discard tokens until we find a statement bounary
    // (semicolon, keyword)
    while (!isAtEnd()) {
      if (previous().type == SEMICOLON) return;

      switch (peek().type) {
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
      }

      advance();
    }
  }
}