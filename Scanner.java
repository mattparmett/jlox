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
      start = current;
      scanToken();
    }

    // We are at the end of source; append EOF token
    // not strictly needed, but makes the parser cleaner.
    tokens.add(new Token(EOF, "", null, line));
    return tokens;
  }

  private void scanToken() {
    char c = advance();
    switch (c) {
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
        Lox.error(line, "Unexpected character.");
        break;
    }
  }

  /*
   * Helper function that tells us if we've reached
   * the end of our source code.
   *
   * @return true if scanner position is at or beyond end
   *          of source
   */
  private boolean isAtEnd() {
    return current >= this.source.length();
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
    return source.charAt(this.current++);
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
    String text = source.substring(this.start, this.current);
    this.tokens.add(new Token(type, text, literal, this.line));
  }
}
