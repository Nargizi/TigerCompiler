import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.*;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;


public class Main {

    public static File[] getTigerFiles(String path) throws Exception {
        File folder = new File(path);
        return folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".tiger");
            }
        });
    }

    public static void compile(File file, boolean build_graphviz, boolean write_tokens, boolean build_ir, boolean save_symbol_table) throws IOException {
        CharStream codePointCharStream = CharStreams.fromPath(Path.of(file.getAbsolutePath()));
        TigerLexer lexer = new TigerLexer(codePointCharStream);
        lexer.addErrorListener(new ErrorHandler(Error.LEXICAL_ERROR));
        TigerParser parser = new TigerParser(new CommonTokenStream(lexer));
        parser.addErrorListener(new ErrorHandler(Error.SYNTAX_ERROR));
        ParseTree tree = parser.tiger_program();
        ParseTreeWalker walker = new ParseTreeWalker();

        File folder = file.getParentFile();
        String name = file.getName();
        name = name.substring(0, name.lastIndexOf("tiger"));
        SemanticChecking semanticChecking = new SemanticChecking(save_symbol_table, Path.of(folder.getAbsolutePath(), name + "st"));
        walker.walk(semanticChecking, tree);
        if(write_tokens) {
            walker.walk(new TokensLogger(Path.of(folder.getAbsolutePath(), name + "tokens").toString(),
                    lexer.getVocabulary()), tree);
        }
        if(build_graphviz){
            GraphVizBuilder builder = new GraphVizBuilder();
            builder.startDigraph();
            ParseTreeToGraphViz converter = new ParseTreeToGraphViz(builder);
            walker.walk(converter, tree);
            builder.endDigraph();
            builder.toFile(Path.of(folder.getAbsolutePath(), name + "gv").toString());
        }
        if(semanticChecking.semanticErrorOccurred()){
            System.exit(Error.SEMANTIC_ERROR.getValue());
        }
    }

    public static void main(String[] args) throws IOException {
        String source_path = null;
        boolean write_tokens = false;
        boolean build_graphviz = false;
        boolean save_symbol_table = false;
        boolean build_ir = false;
        for(int i = 0; i < args.length; ++i){
            if(args[i].equals("-i")){
                source_path = args[i + 1];
            }
            else if(args[i].equals("-l")){
                write_tokens = true;
            }
            if(args[i].equals("-p")){
                build_graphviz = true;
            }
            if (args[i].equals("-st")){
                save_symbol_table = true;
            }
            if(args[i].equals("-ir")){
                build_ir = true;
            }
        }
        if (source_path == null){
            System.exit(Error.ARGUMENT_ERROR.getValue());
        }
        File file = new File(source_path);
        compile(file, build_graphviz, write_tokens, build_ir, save_symbol_table);
//        File[] files = getTigerFiles(source_path);
//        for(File f: files){
//            compile(f, build_graphviz, write_tokens);
//        }

        System.exit(Error.NO_ERROR.getValue());
    }
}
