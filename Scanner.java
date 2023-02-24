package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Static import to avoid typing "TokenType." everywhere
import static com.craftinginterpreters.lox.TokenType.*;

class Scanner {
  // Scanner stores raw source code as simple string
  // and generates tokens as we parse through the source
  private final String source;
  private final List<Token> tokens = new ArrayList<>();
  private int start = 0;  // tracks start of current lexeme
  private int current = 0;  // tracks current position in source
  private int line = 1;  // tracks line in source

  Scanner(String source) {
    this.source = source;
  }

  /*
   * Scans through the given Lox source code,
   * building a list of Tokens.
   *
   * @return list of Tokens in the given source code
   */
  List<Token> scanTokens() {
    while (!isAtEnd()) {
      // We are at the beginning of the next lexeme.
      // Update the lexeme start position to current position 
      this.start = this.current;
      scanToken();
    }

    // We are at the end of source; append EOF token
    // not strictly needed, but makes the parser cleaner.
    tokens.add(new Token(EOF, "", null, this.line));
    return tokens;
  }

  /*
   * Consumes the next character in the source code
   * and generates a Token based on that character.
   *
   * Supports single- and multi-character lexemes;
   * performs a lookahead and conditional consumption
   * for lexemes that span more than one character.
   *
   * Adds generated Tokens to the Scanner's token list.
   */
  private void scanToken() {
    char c = advance();
    switch (c) {
      // Single-character tokens.
      case '(': addToken(LEFT_PAREN); break;
      case ')': addToken(RIGHT_PAREN); break;
      case '{': addToken(LEFT_BRACE); break;
      case '}': addToken(RIGHT_BRACE); break;
      case ',': addToken(COMMA); break;
      case '.': addToken(DOT); break;
      case '-': addToken(MINUS); break;
      case '+': addToken(PLUS); break;
      case ';': addToken(SEMICOLON); break;
      case '*': addToken(STAR); break;

      // One- or two-character operators.
      case '!':
        addToken(match('=') ? BANG_EQUAL : BANG);
        break;
      case '=':
        addToken(match('=') ? EQUAL_EQUAL : EQUAL);
        break;
      case '<':
        addToken(match('=') ? LESS_EQUAL : LESS);
        break;
      case '>':
        addToken(match('=') ? GREATER_EQUAL : GREATER);
        break;
      case '/':
        // If we encounter '//', we have a comment
        // which goes until the end of the line.
        if (match('/')) {
          while (peek() != '\n' && !isAtEnd()) {
            advance();
          }
        } else {
          addToken(SLASH);
        }
        break;

      // Newlines and whitespace.
      case '\n':
        line++;  // flow-through intentional
      case ' ':
      case '\r':
      case '\t':
        // Ignore all whitespace; returns back to
        // beginning of loop, creating a new lexeme.
        break;


      
      default:
        /*
         * Note: we still consume the erroneous
         * character prior to the switch statement;
         * this allows us to continue scanning the
         * source code to find other errors, if any.
         *
         * Since hadError gets set, we will never
         * try to execute any of the errneous code.
         */
        Lox.error(this.line, "Unexpected character.");
        break;
    }
  }

  /*
   * Helper function that acts as a conditional advance().
   *
   * If the next character in the source code matches
   * expected, consumes that next character and returns true.
   *
   * If the Scanner is at the end of the source code (i.e.
   * there is no next character) or the next character does
   * not match the expected character, returns false
   * without consuming the next character.
   *
   * @return true if next character matches expected,
   *          false if not or if the Scanner is at the end of source
   */
  private boolean match(char expected) {
    if (!isAtEnd()) {
      return false;
    }

    if (this.source.charAt(this.current) != expected) {
      return false;
    }

    this.current++;
    return true;
  }

  /*
   * Performs a lookahead to the next character in the
   * source code.  Does not consume the next character
   * under any circumstances.
   *
   * @return next character in the source code, or
   *          '\0' if the Scanner is at the end of the source
   */
  private char peek() {
    if (isAtEnd()) {
      return '\0';
    }

    return this.source.charAt(this.current);
  }

  /*
   * Helper function that tells us if we've reached
   * the end of our source code.
   *
   * @return true if scanner position is at or beyond end
   *          of source
   */
  private boolean isAtEnd() {
    return this.current >= this.source.length();
  }

  /*
   * Helper function that consumes the current
   * character in the Lox source code.
   *
   * (Returns the current character and moves
   * the current pointer to the next character.)
   *
   * @return character at the current position
   *          in the source code
   */
  private char advance() {
    return this.source.charAt(this.current++);
  }

  /*
   * Creates a new Token with the given TokenType
   * from the text of the current lexeme and no literal,
   * and adds that Token to the Scanner's token list.
   */
  private void addToken(TokenType type) {
    addToken(type, null);
  }

  /*
   * Creates a new Token with the given TokenType
   * and literal object, and adds that Token to
   * the Scanner's token list.
   */
  private void addToken(TokenType type, Object literal) {
    String text = this.source.substring(this.start, this.current);
    this.tokens.add(new Token(type, text, literal, this.line));
  }
}
