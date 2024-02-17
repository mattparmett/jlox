# jlox

This repo contains the code for the `jlox` tree-walk interpreter, outlined in the first part of Bob Nystrom's book *Crafting Interpreters*.

To run the interpreter:
- `git clone`
- `java -cp bin/ com.craftinginterpreters.lox.Lox [optional file]`

If no `jlox` file is provided, the interpreter will open to a REPL.

## What I Learned: *Crafting Interpreters Part I*

The sections below summarize some of the key concepts I found interesting in the first section of the book, where we build the Lox interpreter in Java.

This content is in blog format, and doesn't contain information about running the code in this repo.  Sections will be added as I continue to work through the Java section of the book.

### Language Grammar and Syntax Trees

Nystrom does a very great job of softly introducing the complex-sounding terminology around language grammar.  Specifically, I enjoyed his presentation of the different types of grammar, and the use of bite-sized examples that were large enough to drive the key points home.

His comparison of lexical and syntactic grammars stood out as a great example of the clarity he brings to the discussion.  Lexical grammar is implemented by the Scanner; its primitives are characters, and it uses grammar rules to generate lexemes and tokens.  Syntactic grammar is implemented by the Parser; its primitives are tokens, and it uses grammar rules to generate expressions.  I found the comparison table in the book to be a great resource for quickly digesting the differences between types of grammar.

I also enjoyed the short discussion of Abstract Syntax Trees (ASTs).  Our study in this section of the book formalized work I had done professionally at Kanary, where we implemented an AST for a small domain-specific language that was used to drive a selenium web scraper.  We wrote a parser in Go, and used YAML to manually create ASTs that contained expressions for the parser to assemble into instructions for our selenium web scraper.  At Kanary, I simply wrote the expressions and assembled them into the AST.  Here, I learned exactly *why* our Go parser required input in that form, and gained an appreciation for what our parser was actually doing behind the scenes.

### Metaprogramming

Whenever I find myself writing tedious, repetitive code, I always do mental calculus about how much time it will take to finish writing that code versus writing a metaprogram to automatically generate the necessary code files.

In this case, Nystrom guides us through writing a metaprogram to generate the subclass definitions for each type of expression in our grammar.  For the four or five subclasses we had at the time, it seemed like overkill, but I came to appreciate it later once I could automatically generate this AST code for future expression types without having to break my concentration on the task at hand to manually write those classes.

In this case, the author certainly had the benefit of knowing how much code would need to be generated, and thus the ultimate ROI for this specific metaprogram.  While it seems difficult to know which tasks will recur in the future in my own projects, I think that over time I'll gain a better understanding of when an upfront investment of time writing a metaprogram will yield sufficient returns later in the project.

### Visitor Interface

This one was fun - Nystrom does a great job of setting up the motivating example for *why* we require a design pattern, and it only took a few minutes for me to build a mental model of the design pattern.  In particular, I really loved his illustrations of the tables and row/column logic - they vastly accelerated my understanding of the problem and solution.

Essentially, we have many types of expressions in our language, and each expression type is a subclass of a parent Expr class.  Expression subtypes will all need to implement the same high-level operations, but those operations look different for every expression.  (This is where the table comes in: think of each subclass as a row, and each operation as a column.)

There are two ways we can structure this code:

1.  Implement each operation as an abstract method in the `Expr` parent class, and `@Override` that method with a concrete implementation in each subclass.  This requires editing each subclass when we add a new operation.  (This method fits the OOP paradigm.)

2.  Implement each operation as a standalone function that takes an `Expr` object.  Each operation function will use pattern matching to determine which subclass it received, and take the appropriate action.  This requires editing each function and its pattern matching logic when we add a new class.  Additionally, this can be slow for classes lower in the "pattern matching hierarchy," which will have to go through many type checks before their operation is executed.  (This method fits the functional programming paradigm.)

Both of these methods require, as Nystrom puts it, "cracking open" existing code to add new functionality - if we want to add a class, we have to update all our operations; if we want to add an operation, we must update all our classes.

The Visitor pattern allows us to define all the behavior for an operation in one place, while minimizing the need to update all existing classes when we add a new operation.

We start by implementing a `Visitor` interface for the parent class (in this case, `Expr`).  The `Visitor` interface tells the user that any operation acting on a member of the parent class must implement actions for each subclass.

Next, we write an abstract `accept` function for the `Expr` parent class.  This requires that each subclass must include a way of "accepting" a visit from the `Visitor`.  The `accept` function takes the `Visitor` as a parameter, so the subclass knows which `Visitor` to address when it invokes its specific operation function (which resides within the implementation of the `Visitor` interface).

In each subclass, the `accept` function calls the appropriate action from within the `Visitor` implementation passed to it.  Thus, the `accept` function acts as the dispatch mechanism, allowing us to avoid pattern matching within our implementation of the `Visitor`.

That's it.  Now, we can implement a new operation on our set of classes as a `Visitor`, and that operation should contain actions to perform for every subclass.  There is no additional work needed to update our existing subclasses - they already know how to interact with our new `Visitor`.

(When we add a new class, we will in fact need to update our existing operations - after all, they will need to know what actions to perform on the new, unfamiliar object.)

### Error Productions

I found the idea of **error productions** fascinating -- these are instances where the parser author extends the grammar of the language to accomodate common programming errors in the language, all to provide the programmer useful feedback when mistakes are made.

Nystrom gives the example of a leading unary operator.  Lox doesn't support this, but we could in theory extend the grammar to allow it, and catch instances of this operation during parsing.  The parser would consume the erroneous code, but reports it as an error to the user.

It seems to me that including error productions in a language grammar would necessitate deep knowledge of the proper grammar among the language's parser authors, since the grammar implemented in the parser would differ from the grammar published in the language's documentation.

### Desugaring

Nystrom introduces us to the concept of **desugaring** when we implement for loops.  Rather than building new machinery to handle C-style for loops, we simply translate the for loop syntax (initializer, condition, incrementer) into an imperative form: a block containing the initializer, a while loop with the given condition, and the incrementer inserted at the end of the while loop.

This desugared form is exactly equivalent to the nicer for loop syntax, but our interpreter already knows how to handle it.  Therefore, we just have to update our parser to translate the syntactic sugar to the desugared form - much easier than updating the interpreter as well.

The ease of this technique makes me wonder what other syntactic sugar could be implemented over existing interpreter functionality - I will save that as an exercise for after I complete the initial implementation of the language.

### Using Exceptions to Unwind the Call Stack

When we return a value from a function, we need to unwind the interpeter call stack all the way back to the initial stack frame where the function (or Callable) was called.  There may be an arbitrary number of frames on the stack when we return, so tracking our position in the call stack would be very difficult.

Rather than manually setting breadcrumbs in the stack, Nystrom has us use a custom runtime exception defined in a Return class to unwind the stack to precisely the desired point - the original function call.  As a bonus, since a Java exception is just a class, we can store the return value in the exception, which allows us to pass the return value directly back to the function caller.

While perhaps this may seem trivial to a more experienced engineer, I found this to be a very elegant solution to a problem that seems irritatingly complex to a more junior developer like myself.  I'll try to keep this in mind as a pattern to use in similar situations in the future.

### Interpreter Passes

While implementing the resolver, it became clear to me that the jlox "interpreter" is actually comprised of several different passes over the source code and the resulting syntax tree.  If we wanted to incorporate other interpreter features (such as typing, or static analysis like "unreachable code"), we could simply write additional passes over the AST that are run before our final interpreter pass.

This is very similar to work I did in my Software Analysis course at Penn, where we instrumented LLVM to insert static and dynamic analysis passes that operated on LLVM IR (an intermediate representation of the code during compilation).  We developed analyses that checked for divide-by-zero errors and performed hybrid analyses to detect security vulnerabilities like SQL injections or integer overflows.

It's very cool to see this design pattern come up again, especially when we've built the infrastructure and the AST "intermediate representation" from scratch!
