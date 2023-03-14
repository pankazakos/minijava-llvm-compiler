package src.MyVisitor;

import java.util.*;

import src.SymbolTable.*;
import syntaxtree.*;
import visitor.GJDepthFirst;

public class SecondVisitor extends GJDepthFirst<String, String> {
    private SymbolTable st;

    public SecondVisitor(SymbolTable st) {
        this.st = st;
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
        String classname = n.f1.accept(this, null);
        n.f15.accept(this, classname + "::main");
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
        String classname = n.f1.accept(this, null);
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
        String classname = n.f1.accept(this, null);
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
        String MethodType = n.f1.accept(this, null);
        String MethodName = n.f2.accept(this, null);
        n.f8.accept(this, getClass(argu) + "::" + MethodName);
        String ReturnType = n.f10.accept(this, getClass(argu) + "::" + MethodName);

        // Check if return statement matches the return type
        if (!ReturnType.equals(MethodType)) {
            throw new Exception("cannot convert from " + ReturnType + " to " + MethodType);
        }
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

        // Check the type of argument
        String ArgType = n.f0.accept(this, argu);
        String ArgName = n.f1.accept(this, argu);
        Var arg = new Var(ArgType, ArgName);
        if (!st.TypeExists(arg)) {
            throw new Exception(ArgType + " cannot be resolved to any type");
        }
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    @Override
    public String visit(VarDeclaration n, String argu) throws Exception {

        // Check the type of variable
        String VarType = n.f0.accept(this, argu);
        String VarName = n.f1.accept(this, argu);
        Var var = new Var(VarType, VarName);
        if (!st.TypeExists(var)) {
            throw new Exception(VarType + " cannot be resolved to any type");
        }
        n.f2.accept(this, argu);
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

        // Check if variable is declared
        String lname = n.f0.accept(this, argu);
        String classname = getClass(argu);
        String methodname = getMethod(argu);
        if (!st.VarExists(classname, methodname, lname)) {
            throw new Exception(lname + " cannot be resolved to any type");
        }
        String ltype = st.getVarType(classname, methodname, lname);
        String rtype = n.f2.accept(this, argu);
        // Check whether the type of rvalue matches the type of lvalue or matches a type
        // of parentclass
        if (!ltype.equals(rtype)) {
            String ParentType = st.getParentTypeIfExists(rtype, ltype);
            if (!ParentType.equals(ltype)) {
                throw new Exception("cannot convert from " + rtype + " to " + ltype);
            }
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
        // Check if variable is declared
        String lname = n.f0.accept(this, argu);
        String classname = getClass(argu);
        String methodname = getMethod(argu);
        if (!st.VarExists(classname, methodname, lname)) {
            throw new Exception(lname + " cannot be resolved to any type");
        }
        // Array type can be only boolean[] or int[] in MiniJava
        String ltype = st.getVarType(classname, methodname, lname);
        if (!ltype.equals("boolean[]") && !ltype.equals("int[]")) {
            throw new Exception(lname + " cannot be resolved to any type");
        }
        // Check if index is an integer
        String indextype = n.f2.accept(this, argu);
        if (!indextype.equals("int")) {
            throw new Exception("cannot convert from " + indextype + " to int");
        }
        String rtype = n.f5.accept(this, argu);
        // Check whether the type of lvalue matches the type of rvalue
        if (ltype.equals("boolean[]") && !rtype.equals("boolean")) {
            throw new Exception("cannot convert from int[] to boolean[]");
        } else if (ltype.equals("int[]") && !rtype.equals("int")) {
            throw new Exception("cannot convert from boolean[] to int[]");
        }
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
        // Check if the type inside IfStatement is boolean
        String ifClauseType = n.f2.accept(this, argu);
        if (!ifClauseType.equals("boolean")) {
            throw new Exception("cannot convert from " + ifClauseType + " to boolean");
        }
        n.f4.accept(this, argu);
        n.f6.accept(this, argu);
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
        // Check if the type inside WhileStatement is boolean
        String WhileType = n.f2.accept(this, argu);
        if (!WhileType.equals("boolean")) {
            throw new Exception("cannot convert from " + WhileType + " to boolean");
        }
        n.f4.accept(this, argu);
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
        // Only ints are printable in MiniJava
        String PrintType = n.f2.accept(this, argu);
        if (!PrintType.equals("int")) {
            throw new Exception("cannot convert from " + PrintType + " to int");
        }
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
        // Check if Clauses are of boolean type
        String ltype = n.f0.accept(this, argu);
        String rtype = n.f2.accept(this, argu);
        if (!ltype.equals("boolean") && !rtype.equals("boolean")) {
            throw new Exception("The operator && is undefined for the argument type(s) " + ltype + ", " + rtype);
        }
        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(CompareExpression n, String argu) throws Exception {
        // Check if types are ints
        String ltype = n.f0.accept(this, argu);
        String rtype = n.f2.accept(this, argu);
        if (!ltype.equals("int") && !rtype.equals("int")) {
            throw new Exception("The operator < is undefined for the argument type(s) " + ltype + ", " + rtype);
        }
        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(PlusExpression n, String argu) throws Exception {
        // Check if types are ints
        String ltype = n.f0.accept(this, argu);
        String rtype = n.f2.accept(this, argu);
        if (!ltype.equals("int") && !rtype.equals("int")) {
            throw new Exception("The operator + is undefined for the argument type(s) " + ltype + ", " + rtype);
        }
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(MinusExpression n, String argu) throws Exception {
        // Check if types are ints
        String ltype = n.f0.accept(this, argu);
        String rtype = n.f2.accept(this, argu);
        if (!ltype.equals("int") && !rtype.equals("int")) {
            throw new Exception("The operator - is undefined for the argument type(s) " + ltype + ", " + rtype);
        }
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(TimesExpression n, String argu) throws Exception {
        // Check if types are ints
        String ltype = n.f0.accept(this, argu);
        String rtype = n.f2.accept(this, argu);
        if (!ltype.equals("int") && !rtype.equals("int")) {
            throw new Exception("The operator - is undefined for the argument type(s) " + ltype + ", " + rtype);
        }
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    @Override
    public String visit(ArrayLookup n, String argu) throws Exception {
        // Check if ltype is boolean[] or int[]
        String ltype = n.f0.accept(this, argu);
        if (!ltype.equals("boolean[]") && !ltype.equals("int[]")) {
            throw new Exception(ltype + " cannot be resolved to any type");
        }
        // Check if index is integer
        String indexType = n.f2.accept(this, argu);
        if (!indexType.equals("int")) {
            throw new Exception("cannot convert from " + indexType + " to int");
        }
        if (ltype.equals("boolean[]")) {
            return "boolean";
        } else {
            return "int";
        }
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    @Override
    public String visit(ArrayLength n, String argu) throws Exception {
        // Check if type is boolean[] or int[]
        String type = n.f0.accept(this, argu);
        if (!type.equals("boolean[]") && !type.equals("int[]")) {
            throw new Exception("The primitive type" + type + "int does not have a field length");
        }
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    @Override
    public String visit(MessageSend n, String argu) throws Exception {
        // Note: Type defined by a class is the same as the name of the class
        String ClassType = n.f0.accept(this, argu);
        String methodname = n.f2.accept(this, argu);
        // Check if ClassType is not a basic type
        if (ClassType.equals("int") || ClassType.equals("boolean")
                || ClassType.equals("boolean[]") || ClassType.equals("int[]")) {
            throw new Exception("cannot invoke " + methodname + " on the primitive type " + ClassType);
        }
        // Check if method exists in this particular class
        if (!st.MethodExists(ClassType, methodname) && !st.InheritedMethodExists(ClassType, methodname)) {
            throw new Exception(methodname + " is undefined for the type " + ClassType);
        }
        // Check if number of arguments is the same with the number of arguments of the
        // method protocol
        if (n.f4.present()) {
            String ArgTypes[] = n.f4.accept(this, argu).split(",");
            if (ArgTypes.length != st.classes.get(ClassType).methods.get(methodname).arguments.size()) {
                throw new Exception(
                        "The method " + methodname + " is not applicable for the number of arguments given");
            }
            // Convert LinkeHashMap values to List
            List<String> lstArgs = new ArrayList<String>(
                    st.classes.get(ClassType).methods.get(methodname).arguments.values());

            // For each argument check whether the type of the argument given matches the
            // type of the class or a type of a parent class
            int index = 0;
            for (String argtype : ArgTypes) {
                String protocol_type = lstArgs.get(index);
                if (!argtype.equals(protocol_type)) {
                    String ParentType = st.getParentTypeIfExists(argtype, lstArgs.get(index));
                    if (!ParentType.equals(protocol_type)) {
                        throw new Exception(argtype + " cannot be resolved to any type");
                    }
                }
                index++;
            }
        }
        return st.getMethodType(ClassType, methodname);
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    @Override
    public String visit(ExpressionList n, String argu) throws Exception {
        return n.f0.accept(this, argu) + n.f1.accept(this, argu);
    }

    // f0 -> ( ExpressionTerm() )*
    @Override
    public String visit(ExpressionTail n, String argu) throws Exception {
        String _ret = "";
        for (Node node : n.f0.nodes)
            _ret += "," + node.accept(this, argu);
        return _ret;
    }

    // f0 -> ","
    // f1 -> Expression()
    @Override
    public String visit(ExpressionTerm n, String argu) throws Exception {
        return n.f1.accept(this, argu);
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
        // Rtype has to be boolean
        String rtype = n.f1.accept(this, argu);
        if (!rtype.equals("boolean")) {
            throw new Exception("The operator ! is undefined for the type " + rtype);
        }
        return "boolean";
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
    public String visit(PrimaryExpression n, String argu) throws Exception {
        String prExprType = n.f0.accept(this, argu);
        // Check if it is one of the basic types
        if (prExprType.equals("boolean[]") || prExprType.equals("int[]")
                || prExprType.equals("boolean") || prExprType.equals("int")) {
            return prExprType;
        } else if (prExprType.equals("this")) {
            return getClass(argu);
        } else if (n.f0.which == 3) {
            String type = st.getVarType(getClass(argu), getMethod(argu), prExprType);
            return type;
        } else {
            return prExprType;
        }
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    @Override
    public String visit(IntegerLiteral n, String argu) throws Exception {
        return "int";
    }

    /**
     * f0 -> "true"
     */
    @Override
    public String visit(TrueLiteral n, String argu) throws Exception {
        return "boolean";
    }

    /**
     * f0 -> "false"
     */
    @Override
    public String visit(FalseLiteral n, String argu) throws Exception {
        return "boolean";
    }

    /**
     * f0 -> "this"
     */
    @Override
    public String visit(ThisExpression n, String argu) throws Exception {
        return getClass(argu);
    }

    /**
     * f0 -> BooleanArrayAllocationExpression()
     * | IntegerArrayAllocationExpression()
     */
    @Override
    public String visit(ArrayAllocationExpression n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "new"
     * f1 -> "boolean"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    @Override
    public String visit(BooleanArrayAllocationExpression n, String argu) throws Exception {
        // Check if expression value type is an int
        String indextype = n.f3.accept(this, argu);
        if (!indextype.equals("int")) {
            throw new Exception("cannot convert from " + indextype + " to int");
        }
        return "boolean[]";
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    @Override
    public String visit(IntegerArrayAllocationExpression n, String argu) throws Exception {
        // Check if expression value type is an int
        String indextype = n.f3.accept(this, argu);
        if (!indextype.equals("int")) {
            throw new Exception("cannot convert from " + indextype + " to int");
        }
        return "int[]";
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    @Override
    public String visit(AllocationExpression n, String argu) throws Exception {
        String classname = n.f1.accept(this, argu);
        if (!st.ClassExists(classname)) {
            throw new Exception(classname + " cannot be resolved to any type");
        }
        return classname;
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
}
