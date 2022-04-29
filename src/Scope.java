import java.util.HashMap;
import java.util.Map;

public class Scope {

    // variable name, attribute name, attribute value
    private Map<String, Symbol> symbols;
    private String name;

    public Scope() {
        this("");
    }

    public Scope(String name) {
        symbols = new HashMap<>();
        this.name = name;
    }
    public void addSymbol(Symbol symbol) {
        symbols.put(symbol.name, symbol);
    }

    public Symbol getSymbol(String symbolName) {
        return symbols.get(symbolName);
    }

    public boolean hasSymbol(String symbolName) {
        return symbols.containsKey(symbolName);
    }

    public String getName() {
        return name;
    }
}

class Symbol {
    public String name;
    public Map<String, Object> attributes;

    public Symbol(String name) {
        this.name = name;
        attributes = new HashMap<>();
    }
}