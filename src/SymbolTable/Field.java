package src.SymbolTable;

public class Field {
    public String type;
    public String name;
    public int offset;
    public boolean inherited;

    public Field(String type, String name) {
        this.type = type;
        this.name = name;
        this.offset = 0;
        this.inherited = false;
    }
}