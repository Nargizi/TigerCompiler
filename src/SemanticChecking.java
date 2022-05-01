import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.RuleNode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SemanticChecking extends TigerBaseListener {
    private SymbolTable symbolTable;
    private boolean save_table;
    private Path table_path;
    private boolean semanticErrorOccurred;
    private IRGenerator irGenerator;

    public SemanticChecking(boolean save_table, Path table_path) {
        symbolTable = new SymbolTable();
        irGenerator = new IRGenerator();
        this.save_table = save_table;
        this.table_path = table_path;
        semanticErrorOccurred = false;
        init_builtins();
    }

    private void init_builtins(){
        symbolTable.addScope(); // built_in scope
        Symbol printi = new Symbol("printi");
        printi.attributes.put("returnType", Type.VOID);
        printi.attributes.put("params", List.of(Type.INT));
        symbolTable.addSymbol(printi);

        Symbol printf = new Symbol("printf");
        printi.attributes.put("returnType", Type.VOID);
        printi.attributes.put("params", List.of(Type.FLOAT));
        symbolTable.addSymbol(printf);

        Symbol not = new Symbol("not");
        printi.attributes.put("returnType", Type.INT);
        printi.attributes.put("params", List.of(Type.INT));
        symbolTable.addSymbol(not);

        Symbol exit = new Symbol("exit");
        printi.attributes.put("returnType", Type.VOID);
        printi.attributes.put("params", List.of(Type.INT));
        symbolTable.addSymbol(exit);
    }

    public boolean semanticErrorOccurred() {
        return semanticErrorOccurred;
    }

    @Override
    public void enterTiger_program(TigerParser.Tiger_programContext ctx) {
        symbolTable.addScope(); // global scope

        irGenerator.startProgram(ctx.id);
    }

    @Override
    public void exitTiger_program(TigerParser.Tiger_programContext ctx) {
        symbolTable.popScope();
        try {
            symbolTable.toFile(table_path.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void enterFunct(TigerParser.FunctContext ctx) {
        if (checkSemantic(symbolTable.getLast().hasSymbol(ctx.id), ctx.getStart().getLine())) {
            return;
        }
        Symbol function = new Symbol(ctx.id);
        function.attributes.put("returnType", ctx.retType);
        function.attributes.put("params", ctx.params);
        symbolTable.get(0).addSymbol(function);
        symbolTable.addScope(ctx.id); // subroutine scope

        irGenerator.startFunction(ctx.id, ctx.retType);
    }

    @Override
    public void exitFunct(TigerParser.FunctContext ctx) {
        symbolTable.popScope();
    }

    @Override
    public void enterLet_stat(TigerParser.Let_statContext ctx) {
        symbolTable.addScope(); // local scope;
    }

    @Override
    public void exitLet_stat(TigerParser.Let_statContext ctx) {
        symbolTable.popScope();
    }

    @Override
    public void exitType_declaration(TigerParser.Type_declarationContext ctx) {
        if (checkSemantic(symbolTable.getLast().hasSymbol(ctx.id), ctx.getStart().getLine())) {
            return;
        }

        Symbol type = new Symbol(ctx.id);
        type.attributes.put("varType", ctx.varType);

        symbolTable.addSymbol(type);
    }

    @Override
    public void exitVar_declaration(TigerParser.Var_declarationContext ctx) {
        int line = ctx.getStart().getLine();
        for (int i = 0; i < ctx.idList.size(); i++) {
            String varName = ctx.idList.get(i);

            if (checkSemantic(symbolTable.getLast().hasSymbol(varName), line)) {
                continue;
            }
            System.out.println(ctx.storageClass);
            System.out.println(ctx.varType);
            checkSemantic(ctx.storageClass.equalsIgnoreCase("var") && symbolTable.numScopes() == 1, line);
            checkSemantic(ctx.storageClass.equalsIgnoreCase("static") && symbolTable.numScopes() != 1, line);

            Symbol var = new Symbol(varName);
            var.attributes.put("varType", ctx.varType);
            var.attributes.put("storageClass", ctx.storageClass);

            symbolTable.addSymbol(var);

            if (ctx.varType.getBaseType().equals("int"))
                irGenerator.addInt(varName, ctx.varType.getArraySize());
            else
                irGenerator.addFloat(varName, ctx.varType.getArraySize());
        }
    }

    @Override
    public void enterParam(TigerParser.ParamContext ctx) {
        if (symbolTable.getLast().hasSymbol(ctx.id)) {
            throwError(ErrorType.badError, ctx.getStart().getLine());
            return;
        }

        Symbol param = new Symbol(ctx.id);
        param.attributes.put("varType", ctx.varType);

        symbolTable.addSymbol(param);

        irGenerator.addParam(ctx.id, ctx.varType);
    }


    @Override
    public void enterValue(TigerParser.ValueContext ctx) {
        Symbol symbol = symbolTable.getSymbol(ctx.id);
        if(checkSemantic(symbol == null, ctx.getStart().getLine())){
            ctx.varType = Type.ERROR;
        }
        ctx.varType = (Type) symbol.attributes.get("varType");
        if(ctx.isSubscript){
            ctx.varType = new Type(((Type)symbolTable.getSymbol(ctx.varType.getBaseType()).attributes.get("varType")).getBaseType());
        }
        while(!Type.isBuiltIn(ctx.varType)) {
            ctx.varType = (Type) symbolTable.getSymbol(ctx.varType.getBaseType()).attributes.get("varType");
        }
    }

    @Override
    public void exitPrecedence_trail(TigerParser.Precedence_trailContext ctx){
        RuleContext childContext = ((RuleNode)ctx.getChild(0)).getRuleContext();
        if (childContext.getRuleIndex() == TigerParser.RULE_value){
            ctx.varType = ((TigerParser.ValueContext)childContext).varType;
        }else{
            ctx.varType =  ((TigerParser.Const_Context)childContext).varType;
        }
    }

    @Override
    public void exitPrecedence_paren(TigerParser.Precedence_parenContext ctx){
        if(ctx.getChildCount() != 1) {
            ctx.varType = ((TigerParser.ExprContext)((RuleNode)ctx.getChild(1)).getRuleContext()).varType;
        }else{
            ctx.varType = ((TigerParser.Precedence_trailContext)((RuleNode)ctx.getChild(0)).getRuleContext()).varType;
        }
    }

    @Override
    public void exitPrecedence_pow(TigerParser.Precedence_powContext ctx){
        if(ctx.getChildCount() != 1) {
            ctx.varType = ((TigerParser.Precedence_parenContext)((RuleNode)ctx.getChild(0)).getRuleContext()).varType;
            Type right = ((TigerParser.Precedence_powContext)((RuleNode)ctx.getChild(2)).getRuleContext()).varType;
            checkSemantic(right.equals(Type.FLOAT), ctx.getStart().getLine());
        }else{
            ctx.varType = ((TigerParser.Precedence_parenContext)((RuleNode)ctx.getChild(0)).getRuleContext()).varType;
        }
    }

    @Override
    public void exitPrecedence_mult_div(TigerParser.Precedence_mult_divContext ctx) {
        if(ctx.getChildCount() != 1) {
            Type left = ((TigerParser.Precedence_mult_divContext)((RuleNode)ctx.getChild(0)).getRuleContext()).varType;
            Type right = ((TigerParser.Precedence_powContext)((RuleNode)ctx.getChild(2)).getRuleContext()).varType;
            if (left.equals(Type.FLOAT) || right.equals(Type.FLOAT))
                ctx.varType = Type.FLOAT;
            else
                ctx.varType = Type.INT;
        }else{
            ctx.varType = ((TigerParser.Precedence_powContext)((RuleNode)ctx.getChild(0)).getRuleContext()).varType;
        }
    }

    @Override
    public void exitPrecedence_plus_minus(TigerParser.Precedence_plus_minusContext ctx) {
        if(ctx.getChildCount() != 1) {
            Type left = ((TigerParser.Precedence_plus_minusContext)((RuleNode)ctx.getChild(0)).getRuleContext()).varType;
            Type right = ((TigerParser.Precedence_mult_divContext)((RuleNode)ctx.getChild(2)).getRuleContext()).varType;
            if (left.equals(Type.FLOAT) || right.equals(Type.FLOAT))
                ctx.varType = Type.FLOAT;
            else
                ctx.varType = Type.INT;
        }else{
            ctx.varType = ((TigerParser.Precedence_mult_divContext)((RuleNode)ctx.getChild(0)).getRuleContext()).varType;
        }
    }

    @Override
    public void exitPrecedence_compare(TigerParser.Precedence_compareContext ctx) {
        if(ctx.getChildCount() != 1) {
            Type left = ((TigerParser.Precedence_plus_minusContext)((RuleNode)ctx.getChild(0)).getRuleContext()).varType;
            Type right = ((TigerParser.Precedence_plus_minusContext)((RuleNode)ctx.getChild(2)).getRuleContext()).varType;
            checkSemantic(!right.equals(left), ctx.getStart().getLine());
            ctx.varType = Type.INT;
        }else{
            ctx.varType = ((TigerParser.Precedence_plus_minusContext)((RuleNode)ctx.getChild(0)).getRuleContext()).varType;
        }
    }

    @Override
    public void exitPrecedence_and(TigerParser.Precedence_andContext ctx) {
        if(ctx.getChildCount() != 1) {
            Type left = ((TigerParser.Precedence_andContext)((RuleNode)ctx.getChild(0)).getRuleContext()).varType;
            Type right = ((TigerParser.Precedence_compareContext)((RuleNode)ctx.getChild(2)).getRuleContext()).varType;
            checkSemantic(right.equals(Type.FLOAT) || left.equals(Type.FLOAT), ctx.getStart().getLine());
            ctx.varType = Type.INT;
        }else{
            ctx.varType = ((TigerParser.Precedence_compareContext)((RuleNode)ctx.getChild(0)).getRuleContext()).varType;
        }
    }

    @Override
    public void exitPrecedence_or(TigerParser.Precedence_orContext ctx) {
        if(ctx.getChildCount() != 1) {
            Type left = ((TigerParser.Precedence_orContext)((RuleNode)ctx.getChild(0)).getRuleContext()).varType;
            Type right = ((TigerParser.Precedence_andContext)((RuleNode)ctx.getChild(2)).getRuleContext()).varType;
            checkSemantic(right.equals(Type.FLOAT) || left.equals(Type.FLOAT), ctx.getStart().getLine());
            ctx.varType = Type.INT;
        }else{
            ctx.varType = ((TigerParser.Precedence_andContext)((RuleNode)ctx.getChild(0)).getRuleContext()).varType;
        }
    }

    @Override
    public void exitExpr(TigerParser.ExprContext ctx) {
        ctx.varType = ((TigerParser.Precedence_orContext)((RuleNode)ctx.getChild(0)).getRuleContext()).varType;
    }

    @Override
    public void exitOptreturn(TigerParser.OptreturnContext ctx) {
        if(ctx.getChildCount() == 1){
            ctx.varType = ((TigerParser.ExprContext)((RuleNode)ctx.getChild(0)).getRuleContext()).varType;
        }else{
            ctx.varType = Type.VOID;
        }
    }

    @Override
    public void exitOptprefix(TigerParser.OptprefixContext ctx) {
        if(ctx.getChildCount() != 0){
            TigerParser.ValueContext valueContext = ((TigerParser.ValueContext)((RuleNode)ctx.getChild(0)).getRuleContext());
            ctx.varType = valueContext.varType;
            ctx.id = valueContext.id;
        }else{
            ctx.varType = Type.VOID;
        }
    }

    @Override
    public void exitValue_stat(TigerParser.Value_statContext ctx) {
        TigerParser.ValueContext valueContext = ((TigerParser.ValueContext)((RuleNode)ctx.getChild(0)).getRuleContext());
        TigerParser.ExprContext exprContext  = ((TigerParser.ExprContext)((RuleNode)ctx.getChild(2)).getRuleContext());
        Type rType = exprContext.varType;
        Type lType = valueContext.varType;
//        System.out.println(rType);
//        System.out.println(lType);
        int line = ctx.getStart().getLine();
        checkSemantic(!(rType.equals(lType) || (rType.equals(Type.INT) && rType.equals(Type.FLOAT))), line, ErrorType.typeError);
    }

    @Override
    public void exitIf_else_stat(TigerParser.If_else_statContext ctx) {
        TigerParser.ExprContext exprContext  = ((TigerParser.ExprContext)((RuleNode)ctx.getChild(1)).getRuleContext());
        checkSemantic(!exprContext.varType.equals(Type.INT), ctx.getStart().getLine());
    }

    @Override
    public void exitIf_stat(TigerParser.If_statContext ctx) {
        TigerParser.ExprContext exprContext  = ((TigerParser.ExprContext)((RuleNode)ctx.getChild(1)).getRuleContext());
        checkSemantic(!exprContext.varType.equals(Type.INT), ctx.getStart().getLine());
    }

    @Override
    public void exitWhile_stat(TigerParser.While_statContext ctx) {
        TigerParser.ExprContext exprContext  = ((TigerParser.ExprContext)((RuleNode)ctx.getChild(1)).getRuleContext());
        checkSemantic(!exprContext.varType.equals(Type.INT), ctx.getStart().getLine());
    }

    @Override
    public void exitFor_stat(TigerParser.For_statContext ctx) {
        TigerParser.ExprContext exprContext1  = ((TigerParser.ExprContext)((RuleNode)ctx.getChild(3)).getRuleContext());
        TigerParser.ExprContext exprContext2  = ((TigerParser.ExprContext)((RuleNode)ctx.getChild(5)).getRuleContext());
        checkSemantic(!exprContext1.varType.equals(Type.INT), ctx.getStart().getLine());
        checkSemantic(!exprContext2.varType.equals(Type.INT), ctx.getStart().getLine());
    }

    @Override
    public void exitExpr_list(TigerParser.Expr_listContext ctx) {
        if(ctx.getChildCount() != 0){
            TigerParser.ExprContext exprContext = ((TigerParser.ExprContext)((RuleNode)ctx.getChild(0)).getRuleContext());
            TigerParser.Expr_list_tailContext exprListTailContext  = ((TigerParser.Expr_list_tailContext)((RuleNode)ctx.getChild(1)).getRuleContext());

            ctx.params.add(exprContext.varType);
            ctx.params.addAll(exprListTailContext.params);
        }
    }

    @Override
    public void exitExpr_list_tail(TigerParser.Expr_list_tailContext ctx) {
        if(ctx.getChildCount() != 0){
            TigerParser.ExprContext exprContext = ((TigerParser.ExprContext)((RuleNode)ctx.getChild(1)).getRuleContext());
            TigerParser.Expr_list_tailContext exprListTailContext  = ((TigerParser.Expr_list_tailContext)((RuleNode)ctx.getChild(2)).getRuleContext());

            ctx.params.add(exprContext.varType);
            ctx.params.addAll(exprListTailContext.params);
        }
    }

    @Override
    public void exitFunc_call_stat(TigerParser.Func_call_statContext ctx) {
        TigerParser.OptprefixContext optprefixContext  = ((TigerParser.OptprefixContext)((RuleNode)ctx.getChild(0)).getRuleContext());
        Type lType = optprefixContext.varType;
        TigerParser.Expr_listContext exprListContext = ((TigerParser.Expr_listContext)((RuleNode)ctx.getChild(3)).getRuleContext());
        List<Type> args = exprListContext.params;
        Symbol symbol = symbolTable.getSymbol(ctx.ID().getText());
        int line = ctx.getStart().getLine();
        if(checkSemantic(symbol == null, line))
            return;
        List<Type> params = (List<Type>) symbol.attributes.get("params");
        if(checkSemantic(params.size() != args.size(), line))
            return;
        for (int i = 0; i < params.size(); ++i)
            checkSemantic(!(params.get(i).equals(args.get(i)) || (params.get(i).equals(Type.FLOAT) && args.get(i).equals(Type.INT))), line);

        Type returnType = (Type) symbol.attributes.get("returnType");
        checkSemantic(!(lType.equals(Type.VOID) || lType.equals(returnType) || (lType.equals(Type.FLOAT) && returnType.equals(Type.INT))), line);
    }

    @Override
    public void exitRet_stat(TigerParser.Ret_statContext ctx) {
        TigerParser.OptreturnContext optreturnContext  = ((TigerParser.OptreturnContext)((RuleNode)ctx.getChild(1)).getRuleContext());
        String funcName = symbolTable.getLast().getName();
        Type returnType = (Type) symbolTable.get(0).getSymbol(funcName).attributes.get("returnType");
        Type actualType = optreturnContext.varType;
        checkSemantic(!(actualType.equals(returnType) || (returnType.equals(Type.FLOAT) && actualType.equals(Type.INT))), ctx.getStart().getLine());

    }

    enum ErrorType {
        badError, typeError, narrowingError;
    }


    private boolean checkSemantic(boolean cond, int line, ErrorType error){
        if (cond){
            throwError(error, line);
            return false;
        }
        return true;
    }

    private boolean checkSemantic(boolean cond, int line){
        if (cond){
            throwError(ErrorType.badError, line);
            return true;
        }
        return false;
    }

    private void throwError(ErrorType error, int Line) {
        semanticErrorOccurred = true;
        System.out.print("line " + Line + ": ");
        switch (error) {
            case badError:
                System.out.println("SEMANTIC ERROR OCCURRED");
                break;
            case typeError:
                System.out.println("Type mismatch");
                break;
            case narrowingError:
                System.out.println("Narrowing conversion on assignment");
            default:
        }
    }
}
