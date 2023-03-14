package src.MyVisitor;

import java.util.*;

import src.SymbolTable.*;
import syntaxtree.*;
import visitor.GJDepthFirst;

import java.io.*;

public class IRVisitor extends GJDepthFirst<String, String> {

    private SymbolTable st;
    private LinkedHashMap<String, List<String>> vtables; // key: classname -> list of method names
    private int iflabelcount = 0;
    private int whilelabel = 0;
    private int andlabel = 0;
    private int arr_alloc = 0;
    private int oob = 0;
    private String path[];
    private String folder;
    private String filename;
    private File file;

    public IRVisitor(SymbolTable st, String filename) {
        this.st = st;
        this.path = filename.split("/");

        this.vtables = new LinkedHashMap<String, List<String>>();
        Set<String> classkeySet = st.classes.keySet();

        for (String ckey : classkeySet) {
            Set<String> methodkeySet = st.classes.get(ckey).methods.keySet();
            List<String> methods = new ArrayList<String>();

            for (String mkey : methodkeySet) {
                if (mkey.equals("main")) {
                    continue;
                }
                methods.add(mkey);
            }
            this.vtables.put(ckey, methods);
        }

        // Example: minijava-extra/Add.java converts to Add.ll
        this.filename = this.path[path.length - 1].split("\\.")[0] + ".ll";

        if (path.length > 1) {
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i <= path.length - 2; i++) {
                buffer.append(path[i] + "/");
            }
            this.folder = buffer.toString();
        } else {
            this.folder = "./";
        }

        try {
            this.file = new File(folder + this.filename);
            if (!file.createNewFile()) {
                // If file already exists, delete it and make a new one
                if (file.delete()) {
                    file.createNewFile();
                } else {
                    System.out.println("Unable to ovewrite to file " + this.filename);
                    System.exit(-1);
                }
            }
        } catch (IOException ioe) {
            System.out.println("IOException: " + ioe.getMessage());
            ioe.printStackTrace();
        }
    }

    // ClassMethod: is a string in "Class::Method" format (c++ like) in order to
    // determine the scope
    public String getClass(String ClassMethod) {
        String[] str = ClassMethod.split("::");
        return str[0];
    }

    public String getMethod(String ClassMethod) {
        String[] str = ClassMethod.split("::");
        if (str.length == 2)
            return str[1];
        else
            return "";
    }

    public void emit(String str) {
        try {
            FileWriter flw = new FileWriter(file, true);
            BufferedWriter bfw = new BufferedWriter(flw);
            bfw.write(str);
            bfw.close();
        } catch (IOException ioe) {
            System.out.println("IOException: " + ioe.getMessage());
            ioe.printStackTrace();
        }
    }

    public String getIRtype(String type) {
        if (type.equals("boolean")) {
            return "i1";
        } else if (type.equals("int")) {
            return "i32";
        } else if (type.equals("boolean[]")) {
            return "i8*";
        } else if (type.equals("int[]")) {
            return "i32*";
        } else {
            return "i8*"; // Class type
        }
    }

    private String getIFlabel() {
        return "if" + iflabelcount++;
    }

    private String getWhilelabel() {
        return "loop" + whilelabel++;
    }

    private String getAndLabel() {
        return "andclause" + andlabel++;
    }

    private String getArr_alloc() {
        return "arr_alloc" + arr_alloc++;
    }

    private String getoob() {
        return "oob" + oob++;
    }

    public int ComputeOffset(String type) {
        if (type.equals("boolean")) {
            return 1;
        } else if (type.equals("int")) {
            return 4;
        } else {
            return 8;
        }
    }

    public int Class_Size(String classname) {
        Set<String> fieldkeySet = st.classes.get(classname).fields.keySet();
        // Check for inherited Classes. If the are any, use their offset
        int parent_offset = 0;
        String parentname = classname;
        if (!st.classes.get(parentname).ParentClass.isEmpty()) {
            parentname = st.classes.get(parentname).ParentClass;
            parent_offset = st.classes.get(parentname).FieldOffset;
        }
        int curr_offset = 0;
        int index = 0;
        int ret = 0;
        String previoustype = " ";
        for (String key : fieldkeySet) {
            // last key
            if (index == fieldkeySet.size() - 1) {
                Field fld = st.classes.get(classname).fields.get(key);
                curr_offset = fld.offset;
                previoustype = st.classes.get(classname).fields.get(key).type;
                if (curr_offset == 0) {
                    ret = parent_offset + ComputeOffset(previoustype);
                } else {
                    ret = curr_offset + ComputeOffset(previoustype);
                }
            }
            index++;
        }
        return ret;
    }

    public void vtables() {
        Set<String> classkeySet = st.classes.keySet();
        // For each class
        for (String ckey : classkeySet) {
            emit("@." + st.classes.get(ckey).name + "_vtable = global [" + vtables.get(ckey).size() + " x i8*] [");
            Set<String> methodkeySet = st.classes.get(ckey).methods.keySet();

            // For each method of the class
            int index = 0;
            for (String mkey : methodkeySet) {
                if (mkey.equals("main")) {
                    break;
                }
                // append to file the return value of the method and "this" pointer
                emit("i8* bitcast (" + getIRtype(st.classes.get(ckey).methods.get(mkey).type) + " (i8*");

                // Append type of remaining arguments if they exist
                Set<String> argumentkeySet = st.classes.get(ckey).methods.get(mkey).arguments.keySet();
                for (String argType : argumentkeySet) {
                    emit("," + getIRtype(st.getVarType(ckey, mkey, argType)));
                }
                if (st.classes.get(ckey).methods.get(mkey).inherited) {
                    String parentclass = ckey;
                    while (!st.classes.get(parentclass).ParentClass.isEmpty()
                            && st.MethodExists(st.classes.get(parentclass).ParentClass, mkey)) {
                        parentclass = st.classes.get(parentclass).ParentClass;
                    }
                    emit(")* @" + parentclass + "." + st.classes.get(ckey).methods.get(mkey).name + " to i8*)");
                } else {
                    emit(")* @" + st.classes.get(ckey).name + "." + st.classes.get(ckey).methods.get(mkey).name
                            + " to i8*)");
                }
                if (index < methodkeySet.size() - 1) {
                    emit(", ");
                }
                index++;
            }

            emit("]\n\n");

        }
    }

    public void initial_emit() {

        String initialemit = "target triple = \"x86_64-pc-linux-gnu\"\n"
                + "declare i8* @calloc(i32, i32)\n"
                + "declare i32 @printf(i8*, ...)\n"
                + "declare void @exit(i32)\n\n"
                + "@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n"
                + "@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n"
                + "define void @print_int(i32 %i) {\n"
                + "\t%_str = bitcast [4 x i8]* @_cint to i8*\n"
                + "\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n"
                + "\tret void\n"
                + "}\n\n"
                + "define void @throw_oob() {\n"
                + "\t%_str = bitcast [15 x i8]* @_cOOB to i8*\n"
                + "\tcall i32 (i8*, ...) @printf(i8* %_str)\n"
                + "\tcall void @exit(i32 1)\n"
                + "\tret void\n"
                + "}\n\n";

        emit(initialemit);
        vtables();
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    @Override
    public String visit(MainClass n, String argu) throws Exception {
        initial_emit();
        emit("define i32 @main() {\n");

        String classname = n.f1.accept(this, argu);
        n.f14.accept(this, classname + "::main");
        n.f15.accept(this, classname + "::main");

        emit("\tret i32 0\n}\n\n");
        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    @Override
    public String visit(ClassDeclaration n, String argu) throws Exception {
        String classname = n.f1.accept(this, argu);
        n.f4.accept(this, classname);
        return null;
    }

    // f0 -> "class"
    // f1 -> Identifier()
    // f2 -> "extends"
    // f3 -> Identifier()
    // f4 -> "{"
    // f5 -> ( VarDeclaration() )*
    // f6 -> ( MethodDeclaration() )*
    // f7 -> "}"
    @Override
    public String visit(ClassExtendsDeclaration n, String argu) throws Exception {
        String classname = n.f1.accept(this, argu);
        n.f6.accept(this, classname);
        return null;
    }

    // f0 -> "public"
    // f1 -> Type()
    // f2 -> Identifier()
    // f3 -> "("
    // f4 -> ( FormalParameterList() )?
    // f5 -> ")"
    // f6 -> "{"
    // f7 -> ( VarDeclaration() )*
    // f8 -> ( Statement() )*
    // f9 -> "return"
    // f10 -> Expression()
    // f11 -> ";"
    // f12 -> "}"
    @Override
    public String visit(MethodDeclaration n, String argu) throws Exception {
        String methodType = n.f1.accept(this, argu);
        String methodName = n.f2.accept(this, argu);
        String classname = getClass(argu);

        emit("define " + getIRtype(methodType) + " @" + classname + "." + methodName + "(i8* %this");
        n.f4.accept(this, classname + "::" + methodName);
        emit(") {\n");

        // Alocate arguments of method
        Set<String> argumentkeySet = st.classes.get(classname).methods.get(methodName).arguments.keySet();
        for (String arg : argumentkeySet) {
            String argType = st.classes.get(classname).methods.get(methodName).arguments.get(arg);
            emit("\t%" + arg + " = alloca " + getIRtype(argType) + "\n");
            emit("\tstore " + getIRtype(argType) + " %." + arg + ", " + getIRtype(argType) + "* %" + arg + "\n");
        }

        n.f7.accept(this, classname + "::" + methodName);
        n.f8.accept(this, classname + "::" + methodName);

        String returnType = st.classes.get(classname).methods.get(methodName).type;
        String returnValue = n.f10.accept(this, classname + "::" + methodName);
        emit("\tret " + getIRtype(returnType) + " " + returnValue + "\n}\n\n");
        return null;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    @Override
    public String visit(FormalParameterList n, String argu) throws Exception {
        String ret = n.f0.accept(this, null);

        if (n.f1 != null) {
            ret += n.f1.accept(this, null);
        }

        return ret;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    @Override
    public String visit(FormalParameterTerm n, String argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    @Override
    public String visit(FormalParameterTail n, String argu) throws Exception {
        String ret = "";
        for (Node node : n.f0.nodes) {
            ret += ", " + node.accept(this, null);
        }

        return ret;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public String visit(FormalParameter n, String argu) throws Exception {
        String paramType = n.f0.accept(this, argu);
        String paramName = n.f1.accept(this, argu);
        emit(", " + getIRtype(paramType) + " %." + paramName);
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    @Override
    public String visit(VarDeclaration n, String argu) throws Exception {
        String varType = n.f0.accept(this, argu);
        String varName = n.f1.accept(this, argu);
        String classname = getClass(argu);
        String methodname = getMethod(argu);

        // Alocate space to stack only if identifier is a local variable of the method
        if (st.VarExists(classname, methodname, varName)) {
            Method method = st.classes.get(classname).methods.get(methodname);
            // generate register for the specific variable
            int reg = method.genReg();
            // save register number of variable to the method registers
            method.registers.put(varName, reg);
            emit("\t%" + reg + " = alloca " + getIRtype(varType) + "\n");
        }

        return null;
    }

    /**
     * f0 -> ArrayType()
     * | BooleanType()
     * | IntegerType()
     * | Identifier()
     */
    @Override
    public String visit(Type n, String argu) throws Exception {
        return n.f0.accept(this, null);
    }

    /**
     * f0 -> BooleanArrayType()
     * | IntegerArrayType()
     */
    @Override
    public String visit(ArrayType n, String argu) throws Exception {
        return n.f0.accept(this, null);
    }

    @Override
    public String visit(BooleanArrayType n, String argu) {
        return "boolean[]";
    }

    @Override
    public String visit(IntegerArrayType n, String argu) {
        return "int[]";
    }

    @Override
    public String visit(BooleanType n, String argu) {
        return "boolean";
    }

    @Override
    public String visit(IntegerType n, String argu) {
        return "int";
    }

    @Override
    public String visit(Identifier n, String argu) {
        return n.f0.toString();
    }

    /**
     * f0 -> Block()
     * | AssignmentStatement()
     * | ArrayAssignmentStatement()
     * | IfStatement()
     * | WhileStatement()
     * | PrintStatement()
     */
    @Override
    public String visit(Statement n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "{"
     * f1 -> ( Statement() )*
     * f2 -> "}"
     */
    @Override
    public String visit(Block n, String argu) throws Exception {
        n.f1.accept(this, argu);
        return null;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    @Override
    public String visit(AssignmentStatement n, String argu) throws Exception {
        String lvalue = n.f0.accept(this, argu);
        String rvalue = n.f2.accept(this, argu);
        String classname = getClass(argu);
        String methodname = getMethod(argu);
        Method method = st.classes.get(classname).methods.get(methodname);

        emit("\t; AssignmentStatement\n");
        if (st.MethodVariableExists(classname, methodname, lvalue)) {
            // If identifier is a local variable of the method
            String ltype = st.getVarType(classname, methodname, lvalue);

            int reg = method.registers.get(lvalue);
            emit("\tstore " + getIRtype(ltype) + " " + rvalue + ", " + getIRtype(ltype) + "* %" + reg + "\n");

        } else if (st.ArgExists(classname, methodname, lvalue)) {
            // If it is a parameter of the method
            String ltype = st.getVarType(classname, methodname, lvalue);
            emit("\tstore " + getIRtype(ltype) + " " + rvalue + ", " + getIRtype(ltype) + "* %" + rvalue + "\n");

        } else {
            // Else: it is a Field of the method. Note: No need to check if the field exists
            // due to secondVisitor
            Field fld = st.classes.get(classname).fields.get(lvalue);

            // getelementprt: Find pointer of field with offset
            int offset = 8 + fld.offset;
            int greg1 = method.genReg();
            emit("\t%" + greg1 + " = getelementptr i8, i8* %this, i32 " + offset + "\n");

            int greg2 = method.genReg();
            String fldtype = st.getVarType(classname, methodname, lvalue);
            // need to bitcast to appropriate IRtype
            emit("\t%" + greg2 + " = bitcast i8* %" + greg1 + " to " + getIRtype(fldtype) + "*\n");
            // store
            emit("\tstore " + getIRtype(fldtype) + " " + rvalue + ", " + getIRtype(fldtype) + "* %" + greg2 + "\n");
        }
        return null;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "["
     * f2 -> Expression()
     * f3 -> "]"
     * f4 -> "="
     * f5 -> Expression()
     * f6 -> ";"
     */
    @Override
    public String visit(ArrayAssignmentStatement n, String argu) throws Exception {
        String arr = n.f0.accept(this, argu);
        String classname = getClass(argu);
        String methodname = getMethod(argu);
        Method method = st.classes.get(classname).methods.get(methodname);
        String arrType = st.getVarType(classname, methodname, arr);

        // Load size of array
        int size;
        if (st.MethodVariableExists(classname, methodname, arr)) {
            // Local variable
            int reg = method.genReg();
            emit("\t%" + reg + " = load " + getIRtype(arrType) + ", " + getIRtype(arrType) + "* %"
                    + method.registers.get(arr) + "\n");
            size = reg;
        } else if (st.ArgExists(classname, methodname, arr)) {
            // Parameter of method
            int reg = method.genReg();
            emit("\t%" + reg + " = load " + getIRtype(arrType) + ", " + getIRtype(arrType) + "* %" + arr + "\n");
            size = reg;
        } else {
            // Field
            Field fld = st.classes.get(classname).fields.get(arr);

            // getelementprt: Find pointer of field with offset
            int offset = 8 + fld.offset;
            int reg1 = method.genReg();
            emit("\t%" + reg1 + " = getelementptr i8, i8* %this, i32 " + offset + "\n");

            // need to bitcast to appropriate IRtype
            int reg2 = method.genReg();
            emit("\t%" + reg2 + " = bitcast i8* %" + reg1 + " to " + getIRtype(arrType) + "*\n");

            // Load field
            int reg3 = method.genReg();
            emit("\t%" + reg3 + " = load " + getIRtype(arrType) + ", " + getIRtype(arrType) + "* %" + reg2 + "\n");
            size = reg3;
        }
        // Second load to change type
        int SizeReg = method.genReg();
        emit("\t%" + SizeReg + " = load i32, i32* %" + size + "\n");

        String index = n.f2.accept(this, argu);

        int reg4 = method.genReg();
        emit("\t%" + reg4 + " = icmp ult i32 " + index + ", %" + SizeReg + "\n");

        String label1 = getoob();
        String label2 = getoob();
        String label3 = getoob();
        emit("\tbr i1 %" + reg4 + ", label %" + label1 + ", label %" + label2 + "\n");

        // Label1
        emit("\n" + label1 + ": \n");
        int reg5 = method.genReg();
        emit("\t%" + reg5 + " = add i32 " + index + ", 1\n");

        int reg6 = method.genReg();
        emit("\t%" + reg6 + " = getelementptr " + "i32" + ", " + getIRtype(arrType) + " %" + size + ", i32 %" + reg5
                + "\n");

        String exprReg = n.f5.accept(this, argu);
        emit("\t" + "store " + "i32" + " " + exprReg + ", " + getIRtype(arrType) + " %" + reg6 + "\n");
        emit("\tbr label %" + label3 + "\n");

        // Label 2
        emit("\n" + label2 + ": \n");
        emit("\tcall void @throw_oob()\n");
        emit("\tbr label %" + label1 + "\n");

        // Label3
        emit("\n" + label3 + ": \n");

        return null;
    }

    /**
     * f0 -> "if"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     * f5 -> "else"
     * f6 -> Statement()
     */
    @Override
    public String visit(IfStatement n, String argu) throws Exception {
        String iflabel = getIFlabel();
        String elselabel = getIFlabel();
        String endlabel = getIFlabel();
        String exprReg = n.f2.accept(this, argu);

        // Define if-else labels
        emit("\tbr i1 " + exprReg + ", label %" + iflabel + ", " + "label %" + elselabel + "\n\n");

        emit(iflabel + ": \n");
        n.f4.accept(this, argu); // if body
        emit("\t\tbr label %" + endlabel + "\n\n");

        emit(elselabel + ": \n");
        n.f6.accept(this, argu); // else body
        emit("\t\tbr label %" + endlabel + "\n\n");

        emit(endlabel + ":" + "\n"); // define end label
        return null;
    }

    /**
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    @Override
    public String visit(WhileStatement n, String argu) throws Exception {
        String condlabel = getWhilelabel();
        String whilebody = getWhilelabel();
        String endlabel = getWhilelabel();
        // goto to first lable of while in order to check the condition
        emit("\tbr label %" + condlabel + "\n");

        // First label of while
        emit(condlabel + ": \n");
        String exprReg = n.f2.accept(this, argu); // condition
        emit("\tbr i1 " + exprReg + ", label %" + whilebody + ", label %" + endlabel + "\n\n");

        // Second label of while
        emit(whilebody + ": \n");
        n.f4.accept(this, argu); // while body
        emit("\tbr label %" + condlabel + "\n");

        // Define endlabel
        emit("\t" + endlabel + ": \n");
        return null;
    }

    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    @Override
    public String visit(PrintStatement n, String argu) throws Exception {
        String exprReg = n.f2.accept(this, argu);
        emit("\t" + "call void (i32) @print_int(i32 " + exprReg + ")" + "\n");
        return null;
    }

    /**
     * f0 -> AndExpression()
     * | CompareExpression()
     * | PlusExpression()
     * | MinusExpression()
     * | TimesExpression()
     * | ArrayLookup()
     * | ArrayLength()
     * | MessageSend()
     * | Clause()
     */
    @Override
    public String visit(Expression n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    @Override
    public String visit(AndExpression n, String argu) throws Exception {
        String label1 = getAndLabel();
        String label2 = getAndLabel();
        String label3 = getAndLabel();
        String label4 = getAndLabel();
        Method method = st.classes.get(getClass(argu)).methods.get(getMethod(argu));

        emit("\tbr label %" + label1 + "\n\n");

        // Define label1
        emit("\n" + label1 + ": \n");
        String reg1 = n.f0.accept(this, argu);
        emit("\tbr i1 " + reg1 + ", label %" + label3 + ", label %" + label2 + "\n\n");

        // Define label2
        emit("\n" + label2 + ": \n");
        emit("\tbr label %" + label4 + "\n\n");

        // Define label3
        emit("\n" + label3 + ": \n");
        String reg3 = n.f2.accept(this, argu);
        emit("\tbr label %" + label4 + "\n\n");

        // Define label4
        emit("\n" + label4 + ": \n");
        int reg4 = method.genReg();
        emit("\t%" + reg4 + " = phi i1 [ 0, %" + label2 + " ], [ " + reg3 + ", %" + label3 + " ]\n");

        return "%" + reg4;
    }

    /**
     * f0 -> NotExpression()
     * | PrimaryExpression()
     */
    @Override
    public String visit(Clause n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    @Override
    public String visit(NotExpression n, String argu) throws Exception {
        String clause = n.f1.accept(this, argu);
        int reg = st.classes.get(getClass(argu)).methods.get(getMethod(argu)).genReg();
        emit("\t%" + reg + " = xor i1 1, " + clause + "\n");
        return "%" + reg;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(CompareExpression n, String argu) throws Exception {
        String lprimary = n.f0.accept(this, argu);
        String rprimary = n.f2.accept(this, argu);
        Method method = st.classes.get(getClass(argu)).methods.get(getMethod(argu));

        int regnum = method.genReg();
        emit("\t%" + regnum + " = icmp slt i32 " + lprimary + ", " + rprimary + "\n");

        return "%" + regnum;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(PlusExpression n, String argu) throws Exception {
        String lprimary = n.f0.accept(this, argu);
        String rprimary = n.f2.accept(this, argu);

        int regnum = st.classes.get(getClass(argu)).methods.get(getMethod(argu)).genReg();
        emit("\t%" + regnum + " = add i32 " + lprimary + ", " + rprimary + "\n");

        return "%" + regnum;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(MinusExpression n, String argu) throws Exception {
        String lprimary = n.f0.accept(this, argu);
        String rprimary = n.f2.accept(this, argu);

        int regnum = st.classes.get(getClass(argu)).methods.get(getMethod(argu)).genReg();
        emit("\t%" + regnum + " = sub i32 " + lprimary + ", " + rprimary + "\n");

        return "%" + regnum;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(TimesExpression n, String argu) throws Exception {
        String lprimary = n.f0.accept(this, argu);
        String rprimary = n.f2.accept(this, argu);

        int regnum = st.classes.get(getClass(argu)).methods.get(getMethod(argu)).genReg();
        emit("\t%" + regnum + " = mul i32 " + lprimary + ", " + rprimary + "\n");

        return "%" + regnum;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public String visit(ArrayLookup n, String argu) throws Exception {
        String arr = n.f0.accept(this, argu);
        String index = n.f2.accept(this, argu);
        String classname = getClass(argu);
        String methodname = getMethod(argu);
        Method method = st.classes.get(classname).methods.get(methodname);

        // Load size of array
        int reg1 = method.genReg();
        emit("\t%" + reg1 + " = load i32, i32* " + arr + "\n");

        int reg2 = method.genReg();
        emit("\t%" + reg2 + " = icmp ult i32 " + index + ", %" + reg1 + "\n");

        String label1 = getoob();
        String label2 = getoob();
        String label3 = getoob();
        emit("\tbr i1 %" + reg2 + ", label %" + label1 + ", label %" + label2 + "\n");

        // Label1
        emit("\n" + label1 + ": \n");
        int reg3 = method.genReg();
        emit("\t%" + reg3 + " = add i32 " + index + ", 1\n");
        int reg4 = method.genReg();
        emit("\t%" + reg4 + " = getelementptr i32, i32* " + arr + ", i32 %" + reg3 + "\n");
        int reg5 = method.genReg();
        emit("\t%" + reg5 + " = load i32, i32* %" + reg4 + "\n");
        emit("\tbr label %" + label3 + "\n");

        // Label2
        emit("\n" + label2 + ": \n");
        emit("\tcall void @throw_oob()\n");
        emit("\tbr label %" + label3);

        // Label3
        emit("\n" + label3 + ": \n");

        return "%" + reg5;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public String visit(ArrayLength n, String argu) throws Exception {
        String arr = n.f0.accept(this, argu);
        String classname = getClass(argu);
        String methodname = getMethod(argu);
        Method method = st.classes.get(classname).methods.get(methodname);

        int reg = method.genReg();
        emit("\t%" + reg + " = load i32, i32* " + arr + "\n");

        return "%" + reg;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    public String visit(MessageSend n, String argu) throws Exception {
        String objectReg = n.f0.accept(this, argu);
        Method method = st.classes.get(getClass(argu)).methods.get(getMethod(argu));

        String msgmethodname = n.f2.accept(this, argu);
        String msgclassname = method.regTypes.get(objectReg);
        Method msgmethod = st.classes.get(msgclassname).methods.get(msgmethodname);
        emit("\t; " + msgclassname + "." + msgmethodname + " : \n");

        int reg1 = method.genReg();
        emit("\t%" + reg1 + " = bitcast i8* " + objectReg + " to i8***\n");
        int reg2 = method.genReg();
        emit("\t%" + reg2 + " = load i8**, i8*** %" + reg1 + "\n");

        int reg3 = method.genReg();
        emit("\t%" + reg3 + " = getelementptr i8*, i8** %" + reg2 + ", i32 " + msgmethod.offset / 8 + "\n");

        int reg4 = method.genReg();
        emit("\t%" + reg4 + " = load i8*, i8** %" + reg3 + "\n");

        int reg5 = method.genReg();
        emit("\t%" + reg5 + " = bitcast i8* %" + reg4 + " to " + getIRtype(msgmethod.type) + " (i8*");

        Set<String> argkeySet = msgmethod.arguments.keySet();
        for (String arg : argkeySet) {
            emit(", " + getIRtype(msgmethod.arguments.get(arg)));
        }
        emit(")* \n");

        int reg6 = method.genReg();
        emit("\t%" + reg6 + " = call " + getIRtype(msgmethod.type) + " %" + reg5 + "(i8* "
                + method.callocRegs.get(msgclassname));

        if (n.f4.present()) {
            String strparameters = n.f4.accept(this, argu);
            String[] parameters = strparameters.split(", ");
            int index = 0;
            for (String arg : argkeySet) {
                emit(", " + getIRtype(msgmethod.arguments.get(arg)) + " " + parameters[index]);
                index++;
            }
        }
        emit(")\n");
        return "%" + reg6;
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    public String visit(ExpressionList n, String argu) throws Exception {
        String ret0 = n.f0.accept(this, argu);
        String ret1 = n.f1.accept(this, argu);
        if (ret1.equals(", null")) {
            ret1 = "";
        }
        return ret0 + ret1;
    }

    /**
     * f0 -> ( ExpressionTerm() )*
     */
    @Override
    public String visit(ExpressionTail n, String argu) throws Exception {
        String _ret = "";
        for (Node node : n.f0.nodes)
            _ret += ", " + node.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    public String visit(ExpressionTerm n, String argu) throws Exception {
        String ret = n.f1.accept(this, argu);
        return ret;
    }

    /**
     * f0 -> IntegerLiteral()
     * | TrueLiteral()
     * | FalseLiteral()
     * | Identifier()
     * | ThisExpression()
     * | ArrayAllocationExpression()
     * | AllocationExpression()
     * | BracketExpression()
     */
    @Override
    public String visit(PrimaryExpression n, String argu) throws Exception {
        String primary = n.f0.accept(this, argu);

        // Node choice: if it is identifier
        if (n.f0.which == 3) {
            String classname = getClass(argu);
            String methodname = getMethod(argu);
            Method method = st.classes.get(classname).methods.get(methodname);
            if (st.MethodVariableExists(classname, methodname, primary)
                    || st.ArgExists(classname, methodname, primary)) {
                // Local variable or parameter of method
                String loadreg;
                if (st.ArgExists(classname, methodname, primary)) {
                    loadreg = "%" + primary;
                } else {
                    loadreg = "%" + method.registers.get(primary);
                }
                String vartype = st.getVarType(classname, methodname, primary);

                int reg = method.genReg();
                emit("\t; PrimaryExpresion : \n");
                emit("\t%" + reg + " = load " + getIRtype(vartype) + ", " + getIRtype(vartype) + "* " + loadreg + "\n");

                method.regTypes.put("%" + reg, method.variables.get(primary));

                return "%" + reg;
            } else if (st.FieldExists(classname, primary)) {
                // Field
                Field fld = st.classes.get(classname).fields.get(primary);
                int offset = 8 + fld.offset; // Added pointer "this"
                String type = st.getVarType(classname, methodname, primary);

                int reg1 = method.genReg();
                emit("\t%" + reg1 + " = getelementptr i8, i8* %this, i32 " + offset + "\n");

                int reg2 = method.genReg();
                emit("\t%" + reg2 + " = bitcast i8* %" + reg1 + " to " + getIRtype(type) + "*\n");

                int reg3 = method.genReg();
                emit("\t%" + reg3 + " = load " + getIRtype(type) + ", " + getIRtype(type) + "* %" + reg2 + "\n");

                method.regTypes.put("%" + reg3, st.classes.get(classname).fields.get(primary).type);

                return "%" + reg3;
            }
        } else {
            return primary;
        }
        return null;
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    @Override
    public String visit(IntegerLiteral n, String argu) throws Exception {
        return n.f0.toString();
    }

    /**
     * f0 -> "true"
     */
    @Override
    public String visit(TrueLiteral n, String argu) throws Exception {
        return "true";
    }

    /**
     * f0 -> "false"
     */
    @Override
    public String visit(FalseLiteral n, String argu) throws Exception {
        return "false";
    }

    /**
     * Grammar production:
     * f0 -> "this"
     */
    @Override
    public String visit(ThisExpression n, String argu) throws Exception {
        Method method = st.classes.get(getClass(argu)).methods.get(getMethod(argu));
        method.regTypes.put("%this", getClass(argu));
        return "%this";
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    @Override
    public String visit(BracketExpression n, String argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public String visit(IntegerArrayAllocationExpression n, String argu) throws Exception {
        String ExprReg = n.f3.accept(this, argu);
        String classname = getClass(argu);
        String methodname = getMethod(argu);
        Method method = st.classes.get(classname).methods.get(methodname);

        int reg1 = method.genReg();
        emit("\t%" + reg1 + " = icmp slt i32 " + ExprReg + ", 0\n");

        String label1 = getArr_alloc();
        String label2 = getArr_alloc();
        emit("\tbr i1 %" + reg1 + ", label %" + label1 + ", label %" + label2 + "\n");

        // Negative size error
        emit("\n" + label1 + ": \n");
        emit("\tcall void @throw_oob()\n");
        emit("\tbr label %" + label2 + "\n");

        // Label2
        emit("\n" + label2 + ": \n");

        int reg2 = method.genReg();
        emit("\t%" + reg2 + " = add i32 " + ExprReg + ", 1\n");

        int reg3 = method.genReg();
        emit("\t%" + reg3 + " = call i8* @calloc(i32 4, i32 %" + reg2 + ")\n");

        int reg4 = method.genReg();
        emit("\t%" + reg4 + " = bitcast i8* %" + reg3 + " to i32*\n");

        emit("\tstore i32 " + ExprReg + ", i32* %" + reg4 + "\n");

        return "%" + reg4;
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    @Override
    public String visit(AllocationExpression n, String argu) throws Exception {
        emit("\t; AllocationExpression\n");
        String new_classname = n.f1.accept(this, argu);

        // added pointer 'this'
        int size = 8 + Class_Size(new_classname);
        Method method = st.classes.get(getClass(argu)).methods.get(getMethod(argu));

        int reg1 = method.genReg();
        emit("\t%" + reg1 + " = call i8* @calloc(i32 1, i32 " + size + ")\n");
        method.callocRegs.put(new_classname, "%" + reg1);
        method.regTypes.put("%" + reg1, new_classname);

        int reg2 = method.genReg();
        emit("\t%" + reg2 + " = bitcast i8* %" + reg1 + " to i8***\n");

        int reg3 = method.genReg();
        int numOfmethods = vtables.get(new_classname).size();
        emit("\t%" + reg3 + " = getelementptr [" + numOfmethods + " x i8*], [" + numOfmethods + " x i8*]* @."
                + new_classname + "_vtable, i32 0, i32 0\n");
        emit("\tstore i8** %" + reg3 + ", i8*** %" + reg2 + "\n");

        return "%" + reg1;
    }
}