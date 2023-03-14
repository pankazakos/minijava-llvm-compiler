## Minijava LLVM Compiler

### Description
Minijava LLVM compiler is my implementation of a compiler for a subset of Java called Minijava, written in Java, that can generate LLVM IR code. The grammar is defined by `./lib/minijava.jj` file. You can also view the grammar of Minijava in BNF from `BNF.html` file. The language supports custom types from classes and inheritance. The code was written for the assignment of Compilers class of Spring 2022.

### Parser
I used [javaCC](https://javacc.github.io/javacc/) (javacc5.jar) a tool for generating top-down LL(k) parsers and lexical analyzers for the Java programming language. Also we were given a Java Tree Builder (jtb132di.jar) that transforms javacc grammars in Java class hierarchies.

### Visitors
One visitor has been created to fill the Symbol Table of the compiler. A second visitor is responsible for type checking (semantic analysis) using the Symbol Table and a thrid one is used to generate IR code of LLVM.

### Build project
```
make
```

### compile tests
To compile the default tests run
```
make compile-tests
```
To compile your custom tests run
```
java Main <file1.java> <file2.java> ...
```
where <file.java> is the filename of your minijava file.

### run tests
To see the output of the default compiled files run
```
make run-llvm
```
for your custom files run 
```
clang <file.ll> -o <file>
./file
```
You need to install clang compiler in order to compile .ll files to machine code. Once compiled, you can run the executable from the command line.

### Some rules of Minijava in text
MiniJava is fully object-oriented, like Java. It does not allow global functions, only classes, fields and methods. The basic types are int, boolean, int [] which is an array of int, and boolean [] which is an array of boolean. You can build classes that contain fields of these basic types or of other classes. Classes contain methods with arguments of basic or class types, etc.

MiniJava supports single inheritance but not interfaces. It does not support function overloading, which means that each method name must be unique. In addition, all methods are inherently polymorphic (i.e., “virtual” in C++ terminology). This means that foo can be defined in a subclass if it has the same return type and argument types (ordered) as in the parent, but it is an error if it exists with other argument types or return type in the parent. Also all methods must have a return type--there are no void methods. Fields in the base and derived class are allowed to have the same names, and are essentially different fields.

All MiniJava methods are “public” and all fields “protected”. A class method cannot access fields of another class, with the exception of its superclasses. Methods are visible, however. A class's own methods can be called via “this”. E.g., this.foo(5) calls the object's own foo method, a.foo(5) calls the foo method of object a. Local variables are defined only at the beginning of a method. A name cannot be repeated in local variables (of the same method) and cannot be repeated in fields (of the same class). A local variable x shadows a field x of the surrounding class.
In MiniJava, constructors and destructors are not defined. The new operator calls a default void constructor. In addition, there are no inner classes and there are no static methods or fields. By exception, the pseudo-static method “main” is handled specially in the grammar. A MiniJava program is a file that begins with a special class that contains the main method and specific arguments that are not used. The special class has no fields. After it, other classes are defined that can have fields and methods.
Notably, an A class can contain a field of type B, where B is defined later in the file. But when we have "class B extends A”, A must be defined before B. As you'll notice in the grammar, MiniJava offers very simple ways to construct expressions and only allows < comparisons. There are no lists of operations, e.g., 1 + 2 + 3, but a method call on one object may be used as an argument for another method call. In terms of logical operators, MiniJava allows the logical and ("&&") and the logical not ("!"). For int and boolean arrays, the assignment and [] operators are allowed, as well as the a.length expression, which returns the size of array a. We have “while” and “if” code blocks. The latter are always followed by an “else”. Finally, the assignment "A a = new B();" when B extends A is correct, and the same applies when a method expects a parameter of type A and a B instance is given instead.
