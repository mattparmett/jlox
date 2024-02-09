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
    if (match(IF)) return ifStatement();
    if (match(PRINT)) return printStatement();
    if (match(WHILE)) return whileStatement();
    if (match(LEFT_BRACE)) return new Stmt.Block(block());
    return expressionStatement();
  }

  /*
   * Parses an If statement.
   * Detects an else clause via the "else" keyword.
   * 
   * Note: we bind "else" statements to the nearest
   * (innermost) "if" statement preceding the else,
   * since we greedily look forward for an "else"
   * clause each time we encounter an "if" clause.
   *
   * Returns the syntax tree for the If statement.
   */
  private Stmt ifStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'if'.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after if condition.");

    Stmt thenBranch = statement();

    Stmt elseBranch = null;
    if (match(ELSE)) {
      elseBranch = statement();
    }

    return new Stmt.If(condition, thenBranch, elseBranch);

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
   * Parses a while statement, and returns the syntax
   * tree for the while statement including the condition
   * and body.
   */
  private Stmt whileStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'while'.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after condition.");
    Stmt body = statement();

    return new Stmt.While(condition, body);
  }

  /*
   * Parses a block as a list of statements
   * (declarations) within curly braces.
   * 
   * Returns a list of statements within the block.
   */
  private List<Stmt> block() {
    List<Stmt> statements = new ArrayList<>();

    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration());
    }

    consume(RIGHT_BRACE, "Expect '}' after block.");
    return statements;
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
   * Parses a variable assignment by parsing the
   * left hand side (which can be any expression
   * of higher precedence); if we find an '=',
   * we parse the RHS and return an assignment expression
   * tree node.
   * 
   * We parse the LHS as if it were an expression,
   * and if we see an equals sign we can perform
   * an assignment to it.
   */
  private Expr assignment() {
    Expr expr = or();

    if (match(EQUAL)) {
      Token equals = previous();
      Expr value = assignment();

      if (expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable)expr).name;
        return new Expr.Assign(name, value);
      }

      // Report but don't throw, since we don't need
      // to synchronize the parser.
      error(equals, "Invalid assingment target.");
    }

    return expr;
  }

  /*
   * Expression matches any expression
   * at any precedence level.
   */
  private Expr expression() {
    return assignment();
  }

  /*
   * Parses a logical OR expression,
   * treating each operand as an AND
   * expression per the formal grammar.
   */
  private Expr or() {
    Expr expr = and();

    while (match(OR)) {
      Token operator = previous();
      Expr right = and();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }

  /*
   * Parses a logical AND expression,
   * which has higher precedence over OR.
   * Treats each operand as an equality,
   * per the formal grammar.
   */
  private Expr and() {
    Expr expr = equality();

    while (match(AND)) {
      Token operator = previous();
      Expr right = equality();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
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
