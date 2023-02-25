JCC = javac
JFLAGS = -d $(OUTPUT_DIR)
RMFLAGS = -r
SRC_DIR = src/com/craftinginterpreters/lox/
TOOL_DIR = src/com/craftinginterpreters/tool/
OUTPUT_DIR = bin/
JLOX_BIN = com.craftinginterpreters.lox.Lox
AST_BIN = com.craftinginterpreters.tool.GenerateAst

default: all

all: jlox, tool

jlox:
	$(JCC) $(JFLAGS) $(SRC_DIR)*.java

tool:
	$(JCC) $(JFLAGS) $(TOOL_DIR)*.java

run: jlox
	java -cp $(OUTPUT_DIR) $(JLOX_BIN)

gen_ast:
	java -cp $(OUTPUT_DIR) $(AST_BIN) $(SRC_DIR)

clean:
	$(RM) $(RMFLAGS) $(OUTPUT_DIR)*
