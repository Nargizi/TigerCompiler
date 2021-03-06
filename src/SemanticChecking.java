import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.RuleNode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class SemanticChecking extends TigerBaseListener {
    private final SymbolTable symbolTable;
    private final boolean save_table;
    private final Path table_path;
    private boolean semanticErrorOccurred;
    private final IRGenerator irGenerator;

    public SemanticChecking(boolean save_table, Path table_path) {
        symbolTable = new SymbolTable();
        irGenerator = new IRGenerator();
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

        irGenerator.startProgram(ctx.id);
    }

    @Override
    public void exitTiger_program(TigerParser.Tiger_programContext ctx) {
        symbolTable.popScope();
        try {
            if(save_table)
                symbolTable.toFile(table_path.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void enterFunct(TigerParser.FunctContext ctx) {
        if(checkSemantic(!ctx.hasReturn && !ctx.retType.equals(Type.VOID), ctx.getStop().getLine(), ErrorType.noReturnError)) {
            checkSemantic(!ctx.hasReturn, ctx.getStart().getLine(), ErrorType.noReturnError);
            ctx.semError = true;
        }
        for(var line: ctx.breakLines)
            checkSemantic(ctx.outsideBreak, line, ErrorType.outsideBreakError);
        checkSemantic(symbolTable.getLast().hasSymbol(ctx.id), ctx.getStart().getLine(), ErrorType.redefineError);
        Symbol function = new Symbol(ctx.id);
        ctx.retType = getBaseType(ctx.retType);
        if(checkSemantic(ctx.retType.isArray(), ctx.getStart().getLine(), ErrorType.arrayTypeError)) {
            ctx.retType = Type.ERROR;
            ctx.semError = true;
        }

        for(int i = 0; i < ctx.params.size(); ++i){
            if((getBaseType(ctx.params.get(i))).isArray())
                ctx.params.set(i, Type.ERROR);
        }

        function.attributes.put("returnType", ctx.retType);
        function.attributes.put("params", ctx.params);

        symbolTable.addSymbol(function);
        symbolTable.addScope(new FunctionScope(ctx.id)); // subroutine scope

        irGenerator.startFunction(ctx.id, ctx.retType);
    }

    @Override
    public void exitFunct(TigerParser.FunctContext ctx) {
        symbolTable.popScope();
        if(ctx.semError)
            symbolTable.getLast().removeSymbol(ctx.id);
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
            if(checkSemantic(ctx.storageClass.equalsIgnoreCase("var") && symbolTable.numScopes() == 1, line))
                return;
            if(checkSemantic(ctx.storageClass.equalsIgnoreCase("static") && symbolTable.numScopes() != 1, line))
                return;

            Symbol var = new Symbol(varName);
            var.attributes.put("varType", ctx.varType);
            var.attributes.put("storageClass", ctx.storageClass);

            symbolTable.addSymbol(var);

            if (ctx.varType.getBaseType().equals("int"))
                irGenerator.addInt(varName, ctx.varType.getArraySize());
            else
                irGenerator.addFloat(varName, ctx.varType.getArraySize());

            irGenerator.addCommand(Command.ASSIGN, varName, ctx.varValue);
        }
    }

    @Override
    public void enterParam(TigerParser.ParamContext ctx) {
        if (symbolTable.getLast().hasSymbol(ctx.id)) {
            throwError(ErrorType.badError, ctx.getStart().getLine());
            return;
        }

        if (checkSemantic(getBaseType(ctx.varType).isArray(), ctx.getStart().getLine(), ErrorType.arrayTypeError))
            return;
        Symbol param = new Symbol(ctx.id);
        param.attributes.put("varType", ctx.varType);

        symbolTable.addSymbol(param);

        irGenerator.addParam(ctx.id, ctx.varType);
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
        } catch (NullPointerException ignored){

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

    // TODO: WWTTTFFF for cicles generate
    @Override
    public void exitPrecedence_pow(TigerParser.Precedence_powContext ctx){
        if(ctx.getChildCount() != 1) {
            ctx.varType = ((TigerParser.Precedence_parenContext)((RuleNode)ctx.getChild(0)).getRuleContext()).varType;
            Type right = ((TigerParser.Precedence_powContext)((RuleNode)ctx.getChild(2)).getRuleContext()).varType;

            if (right.equals(Type.ERROR)) ctx.varType = Type.ERROR;

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
            if (left.equals(Type.ERROR) || right.equals(Type.ERROR))
                ctx.varType = Type.ERROR;
            else if (left.equals(Type.FLOAT) || right.equals(Type.FLOAT))
                ctx.varType = Type.FLOAT;
            else
                ctx.varType = Type.INT;
        }else{
            ctx.varType = ((TigerParser.Precedence_powContext)((RuleNode)ctx.getChild(0)).getRuleContext()).varType;
        }
    }

    @Override
    public void exitPrecedence_plus_minus(TigerParser.Precedence_plus_minusContext ctx) {
        if (ctx.getChildCount() != 1) {
            Type left = ((TigerParser.Precedence_plus_minusContext)((RuleNode)ctx.getChild(0)).getRuleContext()).varType;
            Type right = ((TigerParser.Precedence_mult_divContext)((RuleNode)ctx.getChild(2)).getRuleContext()).varType;

            if (left.equals(Type.ERROR) || right.equals(Type.ERROR))
                ctx.varType = Type.ERROR;
            else if (left.equals(Type.FLOAT) || right.equals(Type.FLOAT))
                ctx.varType = Type.FLOAT;
            else
                ctx.varType = Type.INT;

        } else {
            ctx.varType = ((TigerParser.Precedence_mult_divContext)((RuleNode)ctx.getChild(0)).getRuleContext()).varType;
        }
    }

    @Override
    public void exitPrecedence_compare(TigerParser.Precedence_compareContext ctx) {
        if(checkSemantic(ctx.getChildCount() > 3, ctx.getStart().getLine(), ErrorType.comparisonError)) {
            ctx.varType = Type.ERROR;
        }
        else if(ctx.getChildCount() != 1) {
            Type left = ((TigerParser.Precedence_plus_minusContext)((RuleNode)ctx.getChild(0)).getRuleContext()).varType;
            Type right = ((TigerParser.Precedence_plus_minusContext)((RuleNode)ctx.getChild(2)).getRuleContext()).varType;
            checkSemantic(!right.equals(left), ctx.getStart().getLine(), ErrorType.typeError);

            if (left.equals(Type.ERROR) || right.equals(Type.ERROR))
                ctx.varType = Type.ERROR;
            else
                ctx.varType = Type.INT;

        }else{
            ctx.varType = ((TigerParser.Precedence_plus_minusContext)((RuleNode)ctx.getChild(0)).getRuleContext()).varType;
        }
    }

    @Override
    public void exitPrecedence_and(TigerParser.Precedence_andContext ctx) {
        if (ctx.getChildCount() != 1) {
            Type left = ((TigerParser.Precedence_andContext)((RuleNode)ctx.getChild(0)).getRuleContext()).varType;
            Type right = ((TigerParser.Precedence_compareContext)((RuleNode)ctx.getChild(2)).getRuleContext()).varType;
            checkSemantic(right.equals(Type.FLOAT) || left.equals(Type.FLOAT), ctx.getStart().getLine());

            if (left.equals(Type.ERROR) || right.equals(Type.ERROR))
                ctx.varType = Type.ERROR;
            else
                ctx.varType = Type.INT;

        } else {
            ctx.varType = ((TigerParser.Precedence_compareContext)((RuleNode)ctx.getChild(0)).getRuleContext()).varType;
        }
    }

    @Override
    public void exitPrecedence_or(TigerParser.Precedence_orContext ctx) {
        if (ctx.getChildCount() != 1) {
            Type left = ((TigerParser.Precedence_orContext)((RuleNode)ctx.getChild(0)).getRuleContext()).varType;
            Type right = ((TigerParser.Precedence_andContext)((RuleNode)ctx.getChild(2)).getRuleContext()).varType;
            checkSemantic(right.equals(Type.FLOAT) || left.equals(Type.FLOAT), ctx.getStart().getLine());

            if (left.equals(Type.ERROR) || right.equals(Type.ERROR))
                ctx.varType = Type.ERROR;
            else
                ctx.varType = Type.INT;

        } else {
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
        checkSemantic(!(rType.equals(lType) || (rType.equals(Type.INT) && lType.equals(Type.FLOAT))), line, ErrorType.typeError);
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
        conditionError, outsideBreakError, comparisonError
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
        System.err.print("line " + Line + ": ");
        switch (error) {
            case badError:
                System.err.println("SEMANTIC ERROR OCCURRED");
                break;
            case typeError:
                System.err.println("Type mismatch");
                break;
            case narrowingError:
                System.err.println("Narrowing conversion on assignment");
                break;
            case redefineError:
                System.err.println("Redefinition in same scope");
                break;
            case notDefinedError:
                System.err.println("Not defined");
                break;
            case undefinedTypeError:
                System.err.println("Type not defined");
                break;
            case returnTypeError:
                System.err.println("Incorrect return type");
                break;
            case arrayTypeError:
                System.err.println("Illegal use of array type");
                break;
            case noReturnError:
                System.err.println("Missing return statement");
                break;
            case incorrectParameterError:
                System.err.println("Incorrect type or number of parameters");
                break;
            case conditionError:
                System.err.println("Condition must be of type int");
                break;
            case outsideBreakError:
                System.err.println("Break statement outside of loop");
                break;
            case comparisonError:
                System.err.println("Comparison is not associative operator");
            default:
        }
    }
}
