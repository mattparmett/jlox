package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("Usage: generate_ast <output directory>");
      System.exit(64);
    }

    String outputDir = args[0];
    defineAst(outputDir, "Expr", Arrays.asList(
      // Name of class : fields with types
      "Binary   : Expr left, Token operator, Expr right",
      "Grouping : Expr expression",
      "Literal  : Object value",
      "Unary    : Token operator, Expr right"
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
    String indent = "  ";

    // Define class.
    writer.println(
      indent + "static class " + className + " extends " + baseName + " {"
    );

    // Define constructor.
    writer.println(indent + indent + className + "(" + fieldList + ") {");

    // Store each parameter for the class in a field.
    String[] fields = fieldList.split(", ");
    for (String field : fields) {
      String name = field.split(" ")[1];
      writer.println(
        indent + indent + indent + "this." + name + " = " + name + ";"
      );
    }

    // End constructor.
    writer.println(indent + indent + "}");

    // Define fields.
    writer.println();
    for (String field : fields) {
      writer.println(indent + indent + "final " + field + ";");
    }

    // End class.
    writer.println(indent + "}");
    writer.println();
  }
}
