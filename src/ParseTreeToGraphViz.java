import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

public class ParseTreeToGraphViz extends TigerBaseListener{
    private GraphVizBuilder builder;
    public ParseTreeToGraphViz(GraphVizBuilder builder){
        super();
        this.builder = builder;
    }

    @Override
    public void enterEveryRule(ParserRuleContext ctx) {

        String text = ctx.getClass().getSimpleName();
        text = text.substring(0, text.length() - 7); // 7 length of the word "Context"
        Integer id = ctx.hashCode();
        builder.addAttribute(id.toString(), "label", String.format("\"%s\"", text));
        for(int i = 0; i < ctx.getChildCount(); ++i){
            builder.addArrow(id.toString(), String.valueOf(ctx.getChild(i).hashCode()));
        }
        super.enterEveryRule(ctx);
    }


    @Override
    public void visitTerminal(TerminalNode node) {
        Integer id = node.hashCode();
        String text = node.getText();
        builder.addAttribute(id.toString(), "shape", "box");
        builder.addAttribute(id.toString(), "label", String.format("\"%s\"", text));
//        return my_id;
    }
}
