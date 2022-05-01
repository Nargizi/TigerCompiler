import java.util.HashMap;
import java.util.Map;

public interface Scope{

    void addSymbol(Symbol symbol);

    Symbol getSymbol(String symbolName);

    boolean hasSymbol(String symbolName);

    String getName();
}
class GenericScope implements Scope{

    // variable name, attribute name, attribute value
    public Map<String, Symbol> symbols;
    private String name;

    public GenericScope() {
        this("");
    }

    public GenericScope(String name) {
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

    @Override
    public String getName() {
        return name;
    }


}

class FunctionScope extends GenericScope{
    public FunctionScope(String name) {
        super(name);
    }
}


class Symbol {
    public String name;
    public Map<String, Object> attributes;

    public Symbol(String name) {
        this.name = name;
        attributes = new HashMap<>();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(name).append(": ");
        for(var pair: attributes.entrySet()){

            if (pair.getValue() instanceof String)
                builder.append((String)pair.getValue()).append(", ");
            else
                builder.append(pair.getValue().toString()).append(", ");
        }
        return builder.toString();
    }
}