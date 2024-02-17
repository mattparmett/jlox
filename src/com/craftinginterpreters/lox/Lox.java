package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
  /*
   * The main Lox interpreter.
   * Static so that successive calls to run()
   * inside a REPL session reuse the same interpreter,
   * preserving global variables that should persist
   * throughout the REPL session.
   */
  private static final Interpreter interpreter = new Interpreter();

  /*
   * Ensures we don't try to execute code
   * that has a known error, and lets us
   * exit with an appropriate exit code
   * if an error occurred.
   */
  static boolean hadError = false;
  static boolean hadRuntimeError = false;

  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: jlox [script]");
      System.exit(64);  // uses sysexit.h conventions
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

  /*
   * If the user starts jlox from the command line with
   * a path to a script file, runFile is called to read
   * and execute the file.
   *
   * @param path path to the Lox script file to run
   */
  public static void runFile(String path) throws IOException {
    byte[] bytes = Files.readAllBytes(Paths.get(path));
    run(new String(bytes, Charset.defaultCharset()));
    if (hadError) System.exit(65);
    if (hadRuntimeError) System.exit(70);
  }

  /*
   * If the user starts jlox from the command line
   * without passing a lox script file, runPrompt()
   * is called to present the user with an interactive REPL.
   *
   * The user can pass EOF (CTRL-D) to exit the REPL.
   */
  public static void runPrompt() throws IOException {
    InputStreamReader input = new InputStreamReader(System.in);
    BufferedReader reader = new BufferedReader(input);

    while (true) {
      System.out.print("> ");
      String line = reader.readLine();

      // If user passes EOF, we read null and exit
      if (line == null) {
        break;
      }

      run(line);

      // Reset error flag to allow user to enter next line
      hadError = false;
    }
  }

  /*
   * Eventually, run() will execute the lox source
   * code passed to it; for now, it simply prints
   * out the tokens of lox source it receives
   * from the REPL or source file.
   *
   * @param source Lox source code to execute
   */
  private static void run(String source) {
    // Scanner pass
    Scanner scanner = new Scanner(source);
    List<Token> tokens = scanner.scanTokens();

    // Parser pass
    Parser parser = new Parser(tokens);
    List<Stmt> statements = parser.parse();

    // Stop if there is a syntax error in parser
    if (hadError) return;

    // Resolver pass
    Resolver resolver = new Resolver(interpreter);
    resolver.resolve(statements);

    // Stop if there is an error in the resolver
    if (hadError) return;

    // Interpreter pass
    interpreter.interpret(statements);
  }

  /*
   * Tells the user that a syntax error occurred on
   * a given line.  This is the bare minimum that
   * we owe to our users!
   *
   * @param line Line on which the error occurred
   * @param message Human-readable error message
   */
  static void error(int line, String message) {
    report(line, "", message);
  }

  /*
   * Reports an error at a given token, showing the
   * token's location and the token itself.
   * 
   * @param token Token at which the error occurred
   * @param message Human-readable error message
   */
  static void error(Token token, String message) {
    if (token.type == TokenType.EOF) {
      report(token.line, " at end", message);
    } else {
      report(token.line, " at '"  + token.lexeme + "'", message);
    }
  }

  /*
   * Helper method to generate a syntax error message
   * that is printed to the console when a user executes
   * code with syntax violations.
   *
   * @param line Line at which the syntax violation occurred
   * @param where Additional information about the error?
   * @param message Human-readable error message
   */
  private static void report(int line, String where, String message) {
    System.err.println("[line " + line + "] Error" + where + ": " + message);
    hadError = true;
  }

  /*
   * Reports the given runtime error to the user.
   * Sets the hadRuntimeError flag to ensure the appropriate
   * exit code is reported on termination.
   */
  static void runtimeError(RuntimeError error) {
    System.err.println(error.getMessage() +
      "\n[line " + error.token.line + "]");
    hadRuntimeError = true;
  }

}
