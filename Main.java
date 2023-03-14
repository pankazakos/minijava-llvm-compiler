import syntaxtree.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import src.SymbolTable.SymbolTable;
import src.MyVisitor.*;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: java Main [file1] [file2] ... [fileN]");
            System.exit(1);
        }

        for (String argument : args) {
            SymbolTable st;
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(argument);
                System.out.println(
                        "-------------------------------------------------------------------------------------");
                System.out.println("File: \033[1;97m" + argument + "\033[m\n");
                System.out.print("Parsing... ");
                MiniJavaParser parser = new MiniJavaParser(fis);
                Goal root = parser.Goal();
                System.out.println("\t \033[1;92mOK\033[m");

                st = new SymbolTable();

                // Call first visitor
                FirstVisitor evalfirst = new FirstVisitor(st);
                root.accept(evalfirst, null);
                System.out.println("Symbol Table \t \033[1;92mOK\033[m");

                // Call second visitor
                SecondVisitor second = new SecondVisitor(st);
                root.accept(second, null);
                System.out.println("Type checking \t \033[1;92mOK\033[m");

                // Print offsets
                st.PrintOffsets();
                System.out.println();

                // Call visitor that generates Intermidiate code
                System.out.print("LLVM code... ");
                IRVisitor irVisitor = new IRVisitor(st, argument);
                root.accept(irVisitor, null);
                System.out.println("\t \033[1;92mOK\033[m");

                System.out.println("\n\n");
            } catch (ParseException ex) {
                System.out.println(ex.getMessage());
            } catch (FileNotFoundException ex) {
                System.err.println(ex.getMessage());
            } finally {
                try {
                    if (fis != null)
                        fis.close();
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }
    }
}
