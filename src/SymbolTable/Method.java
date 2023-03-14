package src.SymbolTable;

import java.util.*;

public class Method {
    public String type;
    public String name;
    public int offset;
    public boolean overrides;
    public boolean inherited;
    public LinkedHashMap<String, String> arguments; // key: name -> type
    public LinkedHashMap<String, String> variables; // key: name -> type
    public LinkedHashMap<String, Integer> registers; // key: variable_name -> register_number
    public LinkedHashMap<String, String> callocRegs; // key: class_name -> register_number
    public LinkedHashMap<String, String> regTypes; // key: %register_number -> type (needed for class types)
    public int RegCounter;

    public Method(String type, String name) {
        this.type = type;
        this.name = name;
        this.offset = 0;
        this.overrides = false;
        this.inherited = false;
        this.arguments = new LinkedHashMap<String, String>();
        this.variables = new LinkedHashMap<String, String>();
        this.registers = new LinkedHashMap<String, Integer>();
        this.callocRegs = new LinkedHashMap<String, String>();
        this.regTypes = new LinkedHashMap<String, String>();
        this.RegCounter = 1;
    }

    public int genReg() {
        return RegCounter++;
    }
}
