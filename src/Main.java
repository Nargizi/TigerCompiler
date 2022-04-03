import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.io.IOException;

public class Main {
    public static String SOURCE_PATH;
    public static boolean WRITE_TOKENS = false;
    public static boolean BUILD_GRAPHVIZ = false;

    public static void main(String[] args) throws IOException {
        for(int i = 0; i < args.length; ++i){
            if(args[i].equals("-i")){
                SOURCE_PATH = args[i + 1];
            }
            else if(args[i].equals("-l")){
                WRITE_TOKENS = true;
            }
            if(args[i].equals("-p")){
                BUILD_GRAPHVIZ = true;
            }
        }
        CharStream codePointCharStream = CharStreams.fromFileName(SOURCE_PATH);
        TigerLexer lexer = new TigerLexer(codePointCharStream);
        TigerParser parser = new TigerParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.prog();
        ParseTreeWalker walker = new ParseTreeWalker();
        if(WRITE_TOKENS) {
//            walker.walk(new TokensLogger(TOKEN_PATH, lexer.getVocabulary()), tree);
        }
        if(BUILD_GRAPHVIZ){
            GraphVizBuilder builder = new GraphVizBuilder();
            builder.startDigraph();
            ParseTreeToGraphViz converter = new ParseTreeToGraphViz(builder);
            walker.walk(converter, tree);
            builder.endDigraph();
            System.out.println(builder);
        }
        System.out.println(tree.toStringTree());

    }
}
