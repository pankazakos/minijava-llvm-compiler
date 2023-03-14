package src.SymbolTable;

public class Var {
    public String type;
    public String name;

    public Var(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getType(Var var) {
        return var.type;
    }

    public String getName(Var var) {
        return var.name;
    }

    public String getVar(Var var) {
        return var.type + " " + var.name;
    }
}
