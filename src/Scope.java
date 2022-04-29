import java.util.HashMap;
import java.util.Map;

public class Scope {

    // variable name, attribute name, attribute value
    private Map<String, Symbol> symbols;

    public Scope() {
        symbols = new HashMap<>();
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

}

class Symbol {
    public String name;
    public Map<String, Object> attributes;

    public Symbol(String name) {
        this.name = name;
        attributes = new HashMap<>();
    }
}