JCC = javac
JFLAGS = -d $(OUTPUT_DIR)
RMFLAGS = -r
SRC_DIR = src/com/craftinginterpreters/lox/
OUTPUT_DIR = bin/
JLOX_BIN = com.craftinginterpreters.lox.Lox

default: all

all:
	$(JCC) $(JFLAGS) $(SRC_DIR)*.java

run: all
	java -cp $(OUTPUT_DIR) $(JLOX_BIN)

clean:
	$(RM) $(RMFLAGS) $(OUTPUT_DIR)*
