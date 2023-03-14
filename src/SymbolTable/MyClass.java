package src.SymbolTable;

import java.util.*;

public class MyClass {
    public String name;
    public String ParentClass;
    public int FieldOffset;
    public int MethodOffset;
    public LinkedHashMap<String, Field> fields; // key: name -> type
    public LinkedHashMap<String, Method> methods; // key: name -> Method

    public MyClass(String name, String ParentClass) {
        this.name = name;
        this.ParentClass = ParentClass;
        this.fields = new LinkedHashMap<String, Field>();
        this.methods = new LinkedHashMap<String, Method>();
        this.FieldOffset = 0;
        this.MethodOffset = 0;
    }

}