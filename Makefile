JCC = javac
JFLAGS = -d $(OUTPUT_DIR)
RMFLAGS = -r

SRC_DIR = src/com/craftinginterpreters/lox/
TOOL_DIR = src/com/craftinginterpreters/tool/
OUTPUT_DIR = bin/
AST_SRC_DIR = $(SRC_DIR)Expr.java

JLOX_BIN = com.craftinginterpreters.lox.Lox
AST_BIN = com.craftinginterpreters.tool.GenerateAst

all: jlox

run: jlox
	java -cp $(OUTPUT_DIR) $(JLOX_BIN)

jlox: gen_ast
	$(JCC) $(JFLAGS) $(SRC_DIR)*.java

gen_ast: tool
	java -cp $(OUTPUT_DIR) $(AST_BIN) $(SRC_DIR)

tool:
	$(JCC) $(JFLAGS) $(TOOL_DIR)*.java

clean:
	$(RM) $(RMFLAGS) $(OUTPUT_DIR)*

clean_ast:
	$(RM) $(RMFLAGS) $(AST_SRC_DIR)

clobber: clean clean_ast
