import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.RuleNode;

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
        if (checkSemantic(symbolTable.get(symbolTable.size() - 1).hasSymbol(ctx.id), ctx.getStart().getLine())) {
            return;
        }

        Symbol function = new Symbol(ctx.id);
        function.attributes.put("returnType", ctx.retType);
        function.attributes.put("params", ctx.params);
        symbolTable.get(0).addSymbol(function);

        symbolTable.add(new Scope(ctx.id)); // subroutine scope
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
        if (checkSemantic(symbolTable.get(symbolTable.size() - 1).hasSymbol(ctx.id), ctx.getStart().getLine())) {
            return;
        }

        Symbol type = new Symbol(ctx.id);
        type.attributes.put("varType", ctx.varType);

        symbolTable.get(symbolTable.size() - 1).addSymbol(type);
    }

    @Override
    public void enterVar_declaration(TigerParser.Var_declarationContext ctx) {
        int line = ctx.getStart().getLine();
        for (int i = 0; i < ctx.idList.size(); i++) {
            String varName = ctx.idList.get(i);

            if (checkSemantic(symbolTable.get(symbolTable.size() - 1).hasSymbol(varName), line)) {
                continue;
            }

            checkSemantic(ctx.storageClass.equals("VAR") && symbolTable.size() == 1, line);
            checkSemantic(ctx.storageClass.equals("STATIC") && symbolTable.size() != 1, line);

            Symbol var = new Symbol(varName);
            var.attributes.put("varType", ctx.varType);

            symbolTable.get(symbolTable.size() - 1).addSymbol(var);
        }
    }

    @Override
    public void enterParam(TigerParser.ParamContext ctx) {
        if (symbolTable.get(symbolTable.size() - 1).hasSymbol(ctx.id)) {
            throwError(ErrorType.badError, ctx.getStart().getLine());
            return;
        }

        Symbol param = new Symbol(ctx.id);
        param.attributes.put("varType", ctx.varType);

        symbolTable.get(symbolTable.size() - 1).addSymbol(param);
    }

    @Override
    public void enterValue(TigerParser.ValueContext ctx) {
        Symbol symbol = getSymbol(ctx.id);
        if(checkSemantic(symbol == null, ctx.getStart().getLine())){
            ctx.varType = Type.ERROR;
        }
        ctx.varType = (Type) symbol.attributes.get("varType");
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
        int line = ctx.getStart().getLine();
        checkSemantic(!(rType.equals(lType) || (rType.equals(Type.INT) && rType.equals(Type.FLOAT))), line);
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
        Symbol symbol = getSymbol(ctx.ID().getText());
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
        TigerParser.OptreturnContext optreturnContext  = ((TigerParser.OptreturnContext)((RuleNode)ctx.getChild(0)).getRuleContext());
        String funcName = symbolTable.get(symbolTable.size() - 1).getName();
        Type returnType = (Type) symbolTable.get(0).getSymbol(funcName).attributes.get("returnType");
        Type actualType = optreturnContext.varType;
        checkSemantic(!(actualType.equals(returnType) || (returnType.equals(Type.FLOAT) && actualType.equals(Type.INT))), ctx.getStart().getLine());

    }

    enum ErrorType {
        badError;
    }

    private Symbol getSymbol(String name){
        for(int i = symbolTable.size() - 1; i >=0; --i){
            if(symbolTable.get(i).hasSymbol(name))
                return symbolTable.get(i).getSymbol(name);
        }
        return null;
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
            return false;
        }
        return true;
    }

    private void throwError(ErrorType error, int Line) {
        switch (error) {
            case badError:
                break;
            default:
        }
    }
}
