import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class SymbolTable {
    private List<Scope> table;
    public StringBuilder savedTable;
    private int indentation = 0;

        public SymbolTable(){
            table = new ArrayList<>();
            savedTable = new StringBuilder();
            init_builtins();
        }

    private void init_builtins(){
        table.add(new GenericScope()); // built_in scope
        Symbol printi = new Symbol("printi");
        printi.attributes.put("returnType", Type.VOID);
        printi.attributes.put("params", List.of(Type.INT));
        table.get(0).addSymbol(printi);

        Symbol printf = new Symbol("printf");
        printf.attributes.put("returnType", Type.VOID);
        printf.attributes.put("params", List.of(Type.FLOAT));
        table.get(0).addSymbol(printf);

        Symbol not = new Symbol("not");
        not.attributes.put("returnType", Type.INT);
        not.attributes.put("params", List.of(Type.INT));
        table.get(0).addSymbol(not);

        Symbol exit = new Symbol("exit");
        exit.attributes.put("returnType", Type.VOID);
        exit.attributes.put("params", List.of(Type.INT));
        table.get(0).addSymbol(exit);
    }

        public void addScope(Scope scope){
            table.add(scope);
            indentation++;
            savedTable.append("\t".repeat(indentation))
                    .append("Scope ").append(table.size()).append(":").append("\n");
        }

        public void popScope(){
            table.remove(table.size() - 1);
            indentation--;
        }

        public void addSymbol(Symbol symbol){
            getLast().addSymbol(symbol);
            savedTable.append("\t".repeat(indentation + 1))
                    .append(symbol.toString()).append("\n");
        }

        public int numScopes(){
            return table.size() - 1;
        }

        public Scope getLast(){
            return get(table.size() - 2);
        }

        public Scope get(int index){
            return table.get(index + 1);
        }

        public String toString(){
            return savedTable.toString();
        }

        public void toFile(String path) throws IOException {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            writer.append(this.toString());
            writer.flush();
        }

        public Symbol getSymbol(String name){
            for(int i = table.size() - 1; i >=0; --i){
                if(table.get(i).hasSymbol(name))
                    return table.get(i).getSymbol(name);
            }
            return null;
        }

        public String getCurrentFunction(){
            for(int i = table.size() - 1; i >=0; --i){
                if(table.get(i) instanceof FunctionScope)
                    return table.get(i).getName();
            }
            return null;
        }

}
