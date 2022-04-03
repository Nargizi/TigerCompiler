import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class TokensLogger extends TigerBaseListener{
    private BufferedWriter writer;
    private Vocabulary vocabulary;
    public TokensLogger(String token_path, Vocabulary vocabulary) throws IOException {
        super();
        writer = new BufferedWriter(new FileWriter(token_path));
        this.vocabulary = vocabulary;
    }

    @Override
    public void visitTerminal(TerminalNode node) {
        String symbol = vocabulary.getSymbolicName(node.getSymbol().getType());
        String text = node.getText();
        try {
            writer.append(String.format("<%s, \"%s\">\n", symbol, text));
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
