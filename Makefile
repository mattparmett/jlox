default: classes

classes: Lox.java
	javac Lox.java

clean:
	$(RM) *.class
