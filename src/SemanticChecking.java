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

    public SemanticChecking(boolean save_table, Path table_path) {
        symbolTable = new SymbolTable();
        this.save_table = save_table;
        this.table_path = table_path;
        semanticErrorOccurred = false;
    }

    public boolean semanticErrorOccurred() {
        return semanticErrorOccurred;
    }

    @Override
    public void enterTiger_program(TigerParser.Tiger_programContext ctx) {
        symbolTable.addScope(new GenericScope()); // global scope
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
        checkSemantic(!ctx.hasReturn, ctx.getStart().getLine(), ErrorType.noReturnError);
        for(var line: ctx.breakLines)
            checkSemantic(ctx.outsideBreak, line, ErrorType.outsideBreakError);
        checkSemantic(symbolTable.getLast().hasSymbol(ctx.id), ctx.getStart().getLine(), ErrorType.redefineError);
        Symbol function = new Symbol(ctx.id);
        ctx.retType = getBaseType(ctx.retType);
        checkSemantic(ctx.retType.isArray(), ctx.getStart().getLine(), ErrorType.arrayTypeError);

        for(var param : ctx.params){
            checkSemantic(getBaseType(param).isArray(), ctx.getStart().getLine(), ErrorType.arrayTypeError);
        }

        function.attributes.put("returnType", ctx.retType);
        function.attributes.put("params", ctx.params);
        symbolTable.getLast().addSymbol(function);
        symbolTable.addScope(new FunctionScope(ctx.id)); // subroutine scope

    }

    @Override
    public void exitFunct(TigerParser.FunctContext ctx) {
        symbolTable.popScope();
    }

    @Override
    public void enterLet_stat(TigerParser.Let_statContext ctx) {
        symbolTable.addScope(new GenericScope()); // local scope;
    }

    @Override
    public void exitLet_stat(TigerParser.Let_statContext ctx) {
        symbolTable.popScope();
    }

    @Override
    public void exitType_declaration(TigerParser.Type_declarationContext ctx) {
        if (checkSemantic(symbolTable.getLast().hasSymbol(ctx.id), ctx.getStart().getLine(), ErrorType.redefineError)) {
            return;
        }

        Symbol type = new Symbol(ctx.id);
        type.attributes.put("varType", ctx.varType);

        symbolTable.addSymbol(type);
    }

    @Override
    public void exitVar_declaration(TigerParser.Var_declarationContext ctx) {
        int line = ctx.getStart().getLine();
        if(checkSemantic(!((Type.isBuiltIn(ctx.varType) && !ctx.varType.isArray()) || symbolTable.getSymbol(ctx.varType.getBaseType()) != null), line, ErrorType.undefinedTypeError))
            return;
        for (int i = 0; i < ctx.idList.size(); i++) {
            String varName = ctx.idList.get(i);

            if (checkSemantic(symbolTable.getLast().hasSymbol(varName), line, ErrorType.redefineError)) {
                continue;
            }
            checkSemantic(ctx.storageClass.equalsIgnoreCase("var") && symbolTable.numScopes() == 1, line);
            checkSemantic(ctx.storageClass.equalsIgnoreCase("static") && symbolTable.numScopes() != 1, line);

            Symbol var = new Symbol(varName);
            var.attributes.put("varType", ctx.varType);
            var.attributes.put("storageClass", ctx.storageClass);

            symbolTable.addSymbol(var);
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
    }


    @Override
    public void enterValue(TigerParser.ValueContext ctx) {
        try {
            Symbol symbol = symbolTable.getSymbol(ctx.id);
            if (checkSemantic(symbol == null, ctx.getStart().getLine(), ErrorType.notDefinedError)) {
                ctx.varType = Type.ERROR;
            }
            ctx.varType = (Type) symbol.attributes.get("varType");
            if (ctx.isSubscript) {
                ctx.varType = new Type(((Type) symbolTable.getSymbol(ctx.varType.getBaseType()).attributes.get("varType")).getBaseType());
            }
            ctx.varType = getBaseType(ctx.varType);
        } catch (NullPointerException np){

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
            checkSemantic(right.equals(Type.FLOAT), ctx.getStart().getLine(), ErrorType.typeError);
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
            checkSemantic(!right.equals(left), ctx.getStart().getLine(), ErrorType.typeError);
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
        if (rType.equals(Type.ERROR) || lType.equals(Type.ERROR))
            return;
        int line = ctx.getStart().getLine();
        checkSemantic(!(rType.equals(lType) || (rType.equals(Type.INT) && rType.equals(Type.FLOAT))), line, ErrorType.typeError);
    }

    @Override
    public void exitIf_else_stat(TigerParser.If_else_statContext ctx) {
        TigerParser.ExprContext exprContext  = ((TigerParser.ExprContext)((RuleNode)ctx.getChild(1)).getRuleContext());
        checkSemantic(!exprContext.varType.equals(Type.INT), ctx.getStart().getLine(), ErrorType.conditionError);
    }

    @Override
    public void exitIf_stat(TigerParser.If_statContext ctx) {
        TigerParser.ExprContext exprContext  = ((TigerParser.ExprContext)((RuleNode)ctx.getChild(1)).getRuleContext());
        checkSemantic(!exprContext.varType.equals(Type.INT), ctx.getStart().getLine(), ErrorType.conditionError);
    }

    @Override
    public void exitWhile_stat(TigerParser.While_statContext ctx) {
        TigerParser.ExprContext exprContext  = ((TigerParser.ExprContext)((RuleNode)ctx.getChild(1)).getRuleContext());
        checkSemantic(!exprContext.varType.equals(Type.INT), ctx.getStart().getLine(), ErrorType.conditionError);
    }

    @Override
    public void exitFor_stat(TigerParser.For_statContext ctx) {
        TigerParser.ExprContext exprContext1  = ((TigerParser.ExprContext)((RuleNode)ctx.getChild(3)).getRuleContext());
        TigerParser.ExprContext exprContext2  = ((TigerParser.ExprContext)((RuleNode)ctx.getChild(5)).getRuleContext());
        checkSemantic(!exprContext1.varType.equals(Type.INT), ctx.getStart().getLine(), ErrorType.conditionError);
        checkSemantic(!exprContext2.varType.equals(Type.INT), ctx.getStart().getLine(), ErrorType.conditionError);
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
        if(checkSemantic(symbol == null, line, ErrorType.notDefinedError))
            return;
        List<Type> params = (List<Type>) symbol.attributes.get("params");
        if(checkSemantic(params.size() != args.size(), line, ErrorType.incorrectParameterError))
            return;
        for (int i = 0; i < params.size(); ++i) {
            Type param = getBaseType(params.get(i));
            checkSemantic(!(param.equals(args.get(i)) || (param.equals(Type.FLOAT) && args.get(i).equals(Type.INT))), line, ErrorType.incorrectParameterError);
        }

        Type returnType = (Type) symbol.attributes.get("returnType");
        checkSemantic(!(lType.equals(Type.VOID) || lType.equals(returnType) || (lType.equals(Type.FLOAT) && returnType.equals(Type.INT))), line);
    }

    @Override
    public void exitRet_stat(TigerParser.Ret_statContext ctx) {
        TigerParser.OptreturnContext optreturnContext  = ((TigerParser.OptreturnContext)((RuleNode)ctx.getChild(1)).getRuleContext());
        String funcName = symbolTable.getCurrentFunction();
        Type returnType = (Type) symbolTable.get(0).getSymbol(funcName).attributes.get("returnType");
        if(returnType == null){
            returnType = Type.VOID;
        }
        Type actualType = optreturnContext.varType;
        if(checkSemantic(returnType.equals(Type.INT) && actualType.equals(Type.FLOAT), ctx.getStart().getLine(), ErrorType.narrowingError))
            return;
        checkSemantic(!(actualType.equals(returnType) || (returnType.equals(Type.FLOAT) && actualType.equals(Type.INT))), ctx.getStart().getLine(), ErrorType.returnTypeError);

    }

    @Override
    public void exitValue_tail(TigerParser.Value_tailContext ctx){
        if (ctx.getChildCount() != 0) {
            TigerParser.ExprContext exprContext = ((TigerParser.ExprContext) ((RuleNode) ctx.getChild(1)).getRuleContext());
            checkSemantic(!exprContext.varType.equals(Type.INT), ctx.getStart().getLine(), ErrorType.typeError);
        }
    }

    enum ErrorType {
        badError, typeError, narrowingError,
        redefineError, notDefinedError, undefinedTypeError,
        returnTypeError, arrayTypeError, noReturnError, incorrectParameterError,
        conditionError, outsideBreakError;
    }

    private Type getBaseType(Type currType){
        Type baseType = currType;
        while (!Type.isBuiltIn(baseType)) {
            baseType = (Type) symbolTable.getSymbol(baseType.getBaseType()).attributes.get("varType");
        }
        return baseType;
    }

    private boolean checkSemantic(boolean cond, int line, ErrorType error){
        if (cond){
            throwError(error, line);
            return true;
        }
        return false;
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
                break;
            case redefineError:
                System.out.println("Redefinition in same scope");
                break;
            case notDefinedError:
                System.out.println("Not defined");
                break;
            case undefinedTypeError:
                System.out.println("Type not defined");
                break;
            case returnTypeError:
                System.out.println("Incorrect return type");
                break;
            case arrayTypeError:
                System.out.println("Illegal use of array type");
                break;
            case noReturnError:
                System.out.println("Missing return statement");
                break;
            case incorrectParameterError:
                System.out.println("Incorrect type or number of parameters");
                break;
            case conditionError:
                System.out.println("Condition must be of type int");
                break;
            case outsideBreakError:
                System.out.println("Break statement outside of loop");
                break;
            default:
        }
    }
}
