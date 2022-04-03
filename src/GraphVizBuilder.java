
public class GraphVizBuilder {
    StringBuilder builder;
    public GraphVizBuilder(){
        builder = new StringBuilder();
    }

    public void startDigraph(String name){
        builder.append("digraph ").append(name).append(" {").append("\n");
    }

    public void startDigraph(){
        this.startDigraph("");
    }

    public void endDigraph(){
        builder.append("}");
    }

    public void addAttribute(String node, String attribute, String value){
        builder.append("\"").append(node).append("\"")
                .append(" [").append(attribute).append(" = ").append(value).append(" ]").append("\n");
    }

    public void addArrow(String node1, String node2){
        builder.append("\"").append(node1).append("\"")
                .append(" -> ").append("\"").append(node2).append("\"").append("\n");
    }
    public String toString(){
        return builder.toString();
    }
}
