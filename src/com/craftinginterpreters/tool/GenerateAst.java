package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
  static String indent = "  ";

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("Usage: generate_ast <output directory>");
      System.exit(64);
    }

    String outputDir = args[0];
    defineAst(outputDir, "Expr", Arrays.asList(
      // Name of class : fields with types
      "Assign   : Token name, Expr value",
      "Binary   : Expr left, Token operator, Expr right",
      "Grouping : Expr expression",
      "Literal  : Object value",
      "Unary    : Token operator, Expr right",
      "Variable : Token name"
    ));

    defineAst(outputDir, "Stmt", Arrays.asList(
      "Block      : List<Stmt> statements",
      "Expression : Expr expression",
      "Print      : Expr expression",
      "Var        : Token name, Expr initializer"
    ));
  }

  private static void defineAst(
      String outputDir, String baseName, List<String> types)
      throws IOException {
    String path = outputDir + "/" + baseName + ".java";
    PrintWriter writer = new PrintWriter(path, "UTF-8");

    writer.println("package com.craftinginterpreters.lox;");
    writer.println();
    writer.println("import java.util.List;");
    writer.println();
    writer.println("abstract class " + baseName + " {");

    /*
     * Define the Visitor interface for Expr, used in this
     * case to pretty print our AST.
     *
     * Implements the Visitor design pattern, which lets us
     * implement a function that applies to a set of related
     * classes in a single place; the subclasses themselves
     * know which method of the interface to call, so we can
     * simply pass them the interface and they will call the
     * appropriate function on themselves.
     */
    defineVisitor(writer, baseName, types);

    // Define the abstract accept() method for the Visitor interface.
    writer.println(indent + "abstract <R> R accept(Visitor<R> visitor);");
    writer.println();

    // Define each of the AST subclasses.
    for (String type: types) {
      String className = type.split(":")[0].trim();
      String fields = type.split(":")[1].trim();
      defineType(writer, baseName, className, fields);
    }

    writer.println("}");
    writer.close();
  }


  private static void defineType(
      PrintWriter writer, String baseName, String className, String fieldList) {

    // Define class.
    writer.println(
      indent + "static class " + className + " extends " + baseName + " {"
    );

    // Define fields.
    String[] fields = fieldList.split(", ");
    for (String field : fields) {
      writer.println(indent + indent + "final " + field + ";");
    }

    // Define constructor.
    writer.println();
    writer.println(indent + indent + className + "(" + fieldList + ") {");

    // Store each parameter for the class in a field.
    for (String field : fields) {
      String name = field.split(" ")[1];
      writer.println(
        indent + indent + indent + "this." + name + " = " + name + ";"
      );
    }

    // End constructor.
    writer.println(indent + indent + "}");

    // Implement the appropriate visit method for the Visitor interface.
    writer.println();
    writer.println(indent + indent + "@Override");
    writer.println(indent + indent + "<R> R accept(Visitor<R> visitor) {");
    writer.println(
      indent + indent + indent +
      "return visitor.visit" + className + baseName + "(this);"
    );
    writer.println(indent + indent + "}");


    // End class.
    writer.println(indent + "}");
    writer.println();
  }

  private static void defineVisitor(
      PrintWriter writer, String baseName, List<String> types) {
    writer.println(indent + "interface Visitor<R> {");

    // Define "visit" interface method for each subtype.
    for (String type: types) {
      String typeName = type.split(":")[0].trim();
      writer.println(
        indent + indent + "R visit" + typeName + baseName + "(" +
        typeName + " " + baseName.toLowerCase() + ");"
      );
    }

    // End visitor interface.
    writer.println(indent + "}");
    writer.println();
  }
}
