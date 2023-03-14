package src.SymbolTable;

import java.util.*;

public class SymbolTable {
    public LinkedHashMap<String, MyClass> classes;

    public SymbolTable() {
        classes = new LinkedHashMap<String, MyClass>();
    }

    public boolean ClassExists(String classname) {
        return classes.containsKey(classname);
    }

    public boolean MethodExists(String classname, String methodname) {
        if (classname.isEmpty() || methodname.isEmpty()) {
            return false;
        }
        return classes.get(classname).methods.containsKey(methodname);
    }

    public boolean InheritedMethodExists(String classname, String methodname) {
        if (classname.isEmpty() || methodname.isEmpty()) {
            return false;
        }
        while (!classes.get(classname).ParentClass.isEmpty()) {
            classname = classes.get(classname).ParentClass;
            if (MethodExists(classname, methodname)) {
                return true;
            }
        }
        return false;
    }

    public String getInheritedMethod(String classname, String methodname) {
        if (classname.isEmpty() || methodname.isEmpty()) {
            return " ";
        }
        while (!classes.get(classname).ParentClass.isEmpty()) {
            classname = classes.get(classname).ParentClass;
            if (MethodExists(classname, methodname)) {
                return classes.get(classname).methods.get(methodname).type;
            }
        }
        return " ";
    }

    public String getMethodType(String classname, String methodname) {
        if (classname.isEmpty() || methodname.isEmpty()) {
            return " ";
        }
        if (MethodExists(classname, methodname)) {
            return classes.get(classname).methods.get(methodname).type;
        } else if (InheritedMethodExists(classname, methodname)) {
            return getInheritedMethod(classname, methodname);
        } else {
            return " ";
        }
    }

    public boolean TypeExists(Var var) {
        String type = var.type;
        if (type.equals("boolean[]") || type.equals("int[]")
                || type.equals("boolean") || type.equals("int")
                || classes.containsKey(type)) {
            return true;
        } else {
            return false;
        }
    }

    public String getParentTypeIfExists(String classname, String curtype) {
        while (!classes.get(classname).ParentClass.isEmpty()) {
            classname = classes.get(classname).ParentClass;
            if (classname.equals(curtype)) {
                return classname;
            }
        }
        return " ";
    }

    public boolean MethodVariableExists(String classname, String methodname, String varname) {
        if (methodname.isEmpty()) {
            return false;
        }
        return classes.get(classname).methods.get(methodname).variables.containsKey(varname);
    }

    public boolean ArgExists(String classname, String methodname, String argname) {
        if (methodname.isEmpty()) {
            return false;
        }
        return classes.get(classname).methods.get(methodname).arguments.containsKey(argname);
    }

    public boolean FieldExists(String classname, String fieldname) {
        return classes.get(classname).fields.containsKey(fieldname);
    }

    public boolean InheritedFieldExists(String classname, String fieldname) {
        while (!classes.get(classname).ParentClass.isEmpty()) {
            classname = classes.get(classname).ParentClass;
            if (FieldExists(classname, fieldname)) {
                return true;
            }
        }
        return false;
    }

    public String getInheritedField(String classname, String fieldname) {
        while (!classes.get(classname).ParentClass.isEmpty()) {
            classname = classes.get(classname).ParentClass;
            if (FieldExists(classname, fieldname)) {
                return classes.get(classname).fields.get(fieldname).type;
            }
        }
        return " ";
    }

    public boolean VarExists(String classname, String methodname, String varname) {
        return MethodVariableExists(classname, methodname, varname)
                || ArgExists(classname, methodname, varname)
                || FieldExists(classname, varname)
                || InheritedFieldExists(classname, varname);
    }

    public String getVarType(String classname, String methodname, String varname) {
        if (MethodVariableExists(classname, methodname, varname)) {
            return classes.get(classname).methods.get(methodname).variables.get(varname);
        } else if (ArgExists(classname, methodname, varname)) {
            return classes.get(classname).methods.get(methodname).arguments.get(varname);
        } else if (FieldExists(classname, varname)) {
            return classes.get(classname).fields.get(varname).type;
        } else if (InheritedFieldExists(classname, varname)) {
            return getInheritedField(classname, varname);
        }
        return " ";
    }

    public String getMethodVariable(String classname, String methodname, String varname) {
        return classes.get(classname).methods.get(methodname).variables.get(varname);
    }

    public void PrintOffsets() {
        System.out.println("\n\t\033[1;97mPrinting Offsets\033[m");
        System.out.println("###############################");
        Set<String> classeskeySet = classes.keySet();
        for (String ckey : classeskeySet) {
            Set<String> fieldkeySet = classes.get(ckey).fields.keySet();
            for (String fkey : fieldkeySet) {
                System.out.println(ckey + "." + fkey + ": " + classes.get(ckey).fields.get(fkey).offset);
            }
            Set<String> methodkeySet = classes.get(ckey).methods.keySet();
            int index = 0;
            for (String mkey : methodkeySet) {
                // Skip main and methods that override other methods
                if (classes.get(ckey).methods.get(mkey).overrides || index == 0) {
                    continue;
                }
                System.out.println(ckey + "." + mkey + "(): " + classes.get(ckey).methods.get(mkey).offset);
                index++;
            }
        }
        System.out.println("###############################");
    }
}