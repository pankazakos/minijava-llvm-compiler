all:
	java -jar ./lib/jtb132di.jar -te ./lib/minijava.jj
	java -jar ./lib/javacc5.jar ./lib/minijava-jtb.jj
	javac Main.java

Folder = minijava-extra

TESTS = $(wildcard ./tests/minijava/*.java)

.PHONY: clean clean-tests

# compile all examples and generate LLVM code
compile-tests:
	@java Main $(TESTS)

# Run LLVM code of all tests
run-llvm:
	@for i in $(TESTS); \
	do \
	    echo "clang: " $$i; \
	    clang $${i%.java}.ll -o $${i%.java}; \
	    ./$${i%.java}; \
	    echo "\n"; \
	done

# Remove all class files
# Remove other generated files and folders from the compiler
clean:
	find . -name \*.class -type f -delete
	rm -rf ./syntaxtree ./visitor
	rm JavaCharStream.java MiniJavaParser.java \
	MiniJavaParserConstants.java MiniJavaParserTokenManager.java \
	ParseException.java ./lib/minijava-jtb.jj Token.java TokenMgrError.java

# Remove all llvm files and executables from tests folder
clean-tests:
	find ./tests/minijava/ -type f -executable -exec rm '{}' \;
	rm ./tests/minijava/*.ll