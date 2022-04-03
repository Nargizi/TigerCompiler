import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;


public class Main {

    public static File[] getTigerFiles(String path){
        File folder = new File(path);
        return folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".tiger");
            }
        });
    }

    public static void compile(File file, boolean build_graphviz, boolean write_tokens) throws IOException {
        CharStream codePointCharStream = CharStreams.fromPath(Path.of(file.getAbsolutePath()));
        TigerLexer lexer = new TigerLexer(codePointCharStream);
        TigerParser parser = new TigerParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.prog();
        ParseTreeWalker walker = new ParseTreeWalker();

        File folder = file.getParentFile();
        String name = file.getName();
        name = name.substring(0, name.lastIndexOf("tiger"));
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
        System.out.println(tree.toStringTree());
    }

    public static void main(String[] args) throws IOException {
        String source_path = "";
        boolean write_tokens = false;
        boolean build_graphviz = false;
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
        }
        File[] files = getTigerFiles(source_path);
        for(File f: files){
            compile(f, build_graphviz, write_tokens);
        }
    }
}
