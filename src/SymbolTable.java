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
        }

        public void addScope(){
            addScope("");
        }

        public void addScope(String scopeName){
            table.add(new Scope(scopeName));
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
            return table.size();
        }

        public Scope getLast(){
            return get(table.size() - 1);
        }

        public Scope get(int index){
            return table.get(index);
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

}
