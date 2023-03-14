package src.MyVisitor;

import java.util.*;

import src.SymbolTable.*;
import syntaxtree.*;
import visitor.GJDepthFirst;

public class FirstVisitor extends GJDepthFirst<String, String> {

    private SymbolTable st;

    public FirstVisitor(SymbolTable st) {
        this.st = st;
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

    public int ComputeFieldOffset(String classname, String fieldtype, String fieldname) {
        Set<String> fieldkeySet = st.classes.get(classname).fields.keySet();
        // Check for inherited Classes. If the are any, use their offset
        int parent_offset = 0;
        String parentname = classname;
        if (!st.classes.get(parentname).ParentClass.isEmpty()) {
            parentname = st.classes.get(parentname).ParentClass;
            parent_offset = st.classes.get(parentname).FieldOffset;
        }
        if (fieldkeySet.size() == 1) {
            return parent_offset + 0;
        }
        int curr_offset = 0;
        int index = 0;
        int ret = 0;
        String previoustype = " ";
        for (String key : fieldkeySet) {
            // Previous key of last key
            if (index == fieldkeySet.size() - 2) {
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

    public int ComputeMethodOffset(String classname, String methodname) {
        Set<String> methodkeySet = st.classes.get(classname).methods.keySet();
        int parent_offset = 0;
        String parentname = classname;
        while (!st.classes.get(parentname).ParentClass.isEmpty()) {
            parentname = st.classes.get(parentname).ParentClass;
            if (!st.MethodExists(parentname, methodname)) {
                break;
            }
            // If it overrides inherited method, return parent offset
            st.classes.get(classname).methods.get(methodname).overrides = true;
            return st.classes.get(parentname).methods.get(methodname).offset;
        }
        // Check for Parent Class. If there is one get its offset
        if (!st.classes.get(parentname).ParentClass.isEmpty()) {
            parentname = st.classes.get(parentname).ParentClass;
            parent_offset = st.classes.get(parentname).MethodOffset;
        }
        if (methodkeySet.size() == 1) {
            return parent_offset + 0;
        }
        int curr_offset = 0;
        int index = 0;
        int ret = 0;
        int overridenMethods = 0;
        for (String key : methodkeySet) {
            if (st.classes.get(classname).methods.get(key).overrides) {
                overridenMethods++;
            } else {
                overridenMethods = 0;
            }
            // Previous key of last key
            if (index == methodkeySet.size() - 2 - overridenMethods) {
                Method mthd = st.classes.get(classname).methods.get(key);
                curr_offset = mthd.offset;
                if (curr_offset == 0) {
                    ret = parent_offset + 8;
                } else {
                    ret = curr_offset + 8;
                }
            }
            index++;
        }
        return ret;
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

        // Insert name of main class into the symbol table. Then insert the main method.
        String classname = n.f1.accept(this, argu);
        MyClass MainClass = new MyClass(classname, "");
        st.classes.put(classname, MainClass);
        Method MainMethod = new Method("void", "main");
        st.classes.get(classname).methods.put("main", MainMethod);

        // Insert argument of the main function
        String arg_name = n.f11.accept(this, argu);
        st.classes.get(classname).methods.get("main").arguments.put(arg_name, "String[]");

        // variables of main method inside classname Class
        n.f14.accept(this, classname + "::main");
        // statements of main method inside classname Class
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

        // Insert name of class into the symbol table
        String classname = n.f1.accept(this, argu);
        MyClass newClass = new MyClass(classname, "");
        if (st.ClassExists(classname)) {
            throw new Exception("Duplicate name of class \"" + classname + "\"");
        }
        st.classes.put(classname, newClass);

        // Variables of newClass
        n.f3.accept(this, classname);

        // Methods of newClass
        n.f4.accept(this, classname);
        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    @Override
    public String visit(ClassExtendsDeclaration n, String argu) throws Exception {

        // Insert name of child class into the symbol table
        String classname = n.f1.accept(this, argu);
        String parent_classname = n.f3.accept(this, argu);
        if (st.ClassExists(classname)) {
            throw new Exception("Duplicate name of class \"" + classname + "\"");
        }
        if (!st.ClassExists(parent_classname)) {
            throw new Exception(parent_classname + " cannot be resolved to any type");
        }
        MyClass childClass = new MyClass(classname, parent_classname);
        st.classes.put(classname, childClass);

        // copy all inherited classes of child class to parent class
        Set<String> methodkeySet = st.classes.get(parent_classname).methods.keySet();
        for (String mkey : methodkeySet) {
            String mtype = st.classes.get(parent_classname).methods.get(mkey).type;
            Method method = new Method(mtype, mkey);
            st.classes.get(classname).methods.put(mkey, method);
            st.classes.get(classname).methods.get(mkey).inherited = true;
            st.classes.get(classname).methods.get(mkey).offset = st.classes.get(parent_classname).methods
                    .get(mkey).offset;
        }

        // copy all inherited fields of child class from parent class
        Set<String> fieldkeySet = st.classes.get(parent_classname).fields.keySet();
        for (String fkey : fieldkeySet) {
            String ftype = st.classes.get(parent_classname).fields.get(fkey).type;
            Field fld = new Field(ftype, fkey);
            st.classes.get(classname).fields.put(fkey, fld);
            st.classes.get(classname).fields.get(fkey).inherited = true;
            st.classes.get(classname).fields.get(fkey).offset = st.classes.get(parent_classname).fields
                    .get(fkey).offset;
        }

        // Variables of child class
        n.f5.accept(this, classname);
        n.f6.accept(this, classname);
        return null;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    @Override
    public String visit(MethodDeclaration n, String argu) throws Exception {

        // Insert type and name of method
        String MethodType = n.f1.accept(this, argu);
        String MethodName = n.f2.accept(this, argu);
        Method tempMethod = new Method(MethodType, MethodName);
        String classname = getClass(argu);
        if (st.MethodExists(classname, MethodName) && !st.classes.get(classname).methods.get(MethodName).inherited) {
            throw new Exception("Duplicate method name \"" + MethodName + "\"");
        }
        st.classes.get(classname).methods.put(MethodName, tempMethod);
        int tmp_offset = ComputeMethodOffset(classname, MethodName);
        st.classes.get(classname).methods.get(MethodName).offset = tmp_offset;
        // Update MethodOffset until last one
        if (!st.classes.get(classname).methods.get(MethodName).overrides) {
            st.classes.get(classname).MethodOffset = tmp_offset + ComputeOffset(MethodType);
        }

        // Insert arguments of method
        if (n.f4.present()) {
            n.f4.accept(this, classname + "::" + MethodName);
        }
        n.f7.accept(this, classname + "::" + MethodName);
        n.f8.accept(this, classname + "::" + MethodName);

        // Expresions of the method
        n.f10.accept(this, classname + "::" + MethodName);
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    @Override
    public String visit(VarDeclaration n, String argu) throws Exception {
        // Insert field or variable whether it is in the scope of a method or not
        String type = n.f0.accept(this, argu);
        String name = n.f1.accept(this, argu);
        String classname = getClass(argu);
        String methodname = getMethod(argu);
        if (st.ArgExists(classname, methodname, name)) {
            throw new Exception("Duplicate variable " + name);
        }
        if (methodname.isEmpty()) {
            Field tempField = new Field(type, name);
            st.classes.get(classname).fields.put(name, tempField);
            int tmp_offset = ComputeFieldOffset(classname, type, name);
            st.classes.get(classname).fields.get(name).offset = tmp_offset;
            // Update class FieldOffset until the last one
            if (!st.classes.get(classname).fields.get(name).inherited) {
                st.classes.get(classname).FieldOffset = tmp_offset + ComputeOffset(type);
            }
        } else {
            st.classes.get(classname).methods.get(methodname).variables.put(name, type);
        }
        return null;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    @Override
    public String visit(FormalParameterList n, String argu) throws Exception {
        String ret = n.f0.accept(this, argu);

        if (n.f1 != null) {
            ret += n.f1.accept(this, argu);
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
            ret += ", " + node.accept(this, argu);
        }

        return ret;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public String visit(FormalParameter n, String argu) throws Exception {
        // Insert argument
        String type = n.f0.accept(this, argu);
        String name = n.f1.accept(this, argu);
        if (st.ArgExists(getClass(argu), getMethod(argu), name)) {
            throw new Exception("Duplicate parameter " + name);
        }
        st.classes.get(getClass(argu)).methods.get(getMethod(argu)).arguments.put(name, type);
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
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> BooleanArrayType()
     * | IntegerArrayType()
     */
    @Override
    public String visit(ArrayType n, String argu) throws Exception {
        return n.f0.accept(this, argu);
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
}
