import java.util.ArrayList;
import java.util.List;

public class SemanticChecking extends TigerBaseListener {
    private List<Scope> symbolTable;

    public SemanticChecking() {
        symbolTable = new ArrayList<>();
    }

    @Override
    public void enterTiger_program(TigerParser.Tiger_programContext ctx) {
        symbolTable.add(new Scope()); // global scope
    }

    @Override
    public void exitTiger_program(TigerParser.Tiger_programContext ctx) {
        symbolTable.remove(symbolTable.size() - 1);
    }

    @Override
    public void enterFunct(TigerParser.FunctContext ctx) {
        Symbol function = new Symbol(ctx.id);
        function.attributes.put("returnType", ctx.retType);
        function.attributes.put("params", ctx.params);
        symbolTable.get(0).addSymbol(function);

        symbolTable.add(new Scope()); // subroutine scope
    }

    @Override
    public void exitFunct(TigerParser.FunctContext ctx) {
        symbolTable.remove(symbolTable.size() - 1);
    }

    @Override
    public void enterLet_stat(TigerParser.Let_statContext ctx) {
        symbolTable.add(new Scope()); // local scope;
    }

    @Override
    public void exitLet_stat(TigerParser.Let_statContext ctx) {
        symbolTable.remove(symbolTable.size() - 1);
    }

    @Override
    public void enterType_declaration(TigerParser.Type_declarationContext ctx) {
        Symbol type = new Symbol(ctx.id);
        type.attributes.put("varType", ctx.varType);
        type.attributes.put("varSize", ctx.varSize);
        type.attributes.put("isArray", ctx.isArray);

        symbolTable.get(symbolTable.size() - 1).addSymbol(type);
    }

    @Override
    public void enterVar_declaration(TigerParser.Var_declarationContext ctx) {
        for (int i = 0; i < ctx.idList.size(); i++) {
            String varName = ctx.idList.get(i);

            Symbol var = new Symbol(varName);
            var.attributes.put("varType", ctx.varType);
            var.attributes.put("varSize", ctx.varSize);
            var.attributes.put("isArray", ctx.isArray);

            symbolTable.get(symbolTable.size() - 1).addSymbol(var);
        }
    }

    @Override
    public void enterParam(TigerParser.ParamContext ctx) {
        Symbol param = new Symbol(ctx.id);
        param.attributes.put("varType", ctx.varType);
    }
}
