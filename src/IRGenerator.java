import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

enum Command {
    ASSIGN, EXPR, WHILE_LOOP, FOR_LOOP, RETURN
}

public class IRGenerator {
    public IRProgram program;
    public IRFunction activeFunction = null;

    public void startProgram(String programName) {
        program = new IRProgram(programName);
    }

    public void startFunction(String functionName, Type returnType) {
        activeFunction = new IRFunction(functionName, returnType);
        program.addFunction(activeFunction);
    }

    public void addInt(String intVar, int arrSize) {
        IRScope activeScope = (activeFunction == null) ? program : activeFunction;
        activeScope.addInt(intVar, arrSize);
    }

    public void addFloat(String floatVar, int arrSize) {
        IRScope activeScope = (activeFunction == null) ? program : activeFunction;
        activeScope.addFloat(floatVar, arrSize);
    }

    public void addParam(String paramName, Type type) {
        activeFunction.addParam(paramName, type);
    }

    public void addCommand(Command command, String... args) {
//         activeFunction.addCommand(command, args);
    }

}

abstract class IRScope {
    public abstract void addInt(String intVar, int arrSize);
    public abstract void addFloat(String floatVar, int arrSize);

    protected String varsListToString(List<Map.Entry<String, Integer>> vars) {
        StringBuilder listBuilder = new StringBuilder();

        for (int i = 0; i < vars.size(); i++) {
            if (i != 0) listBuilder.append(", ");
            listBuilder.append(vars.get(i).getKey());

            if (vars.get(i).getValue() != 0)
                listBuilder.append("[").append(vars.get(i).getValue()).append("]");
        }

        return listBuilder.toString();
    }
}

class IRProgram extends IRScope {
    private final String name;

    private final List<Map.Entry<String, Integer>> intList;
    private final List<Map.Entry<String, Integer>> floatList;
    private final List<IRFunction> functionList;

    public IRProgram(String programName) {
        this.name = programName;

        intList = new ArrayList<>();
        floatList = new ArrayList<>();
        functionList = new ArrayList<>();
    }

    @Override
    public void addInt(String intVar, int arrSize) {
        intList.add(new AbstractMap.SimpleEntry<>(intVar, arrSize));
    }

    @Override
    public void addFloat(String floatVar, int arrSize) {
        floatList.add(new AbstractMap.SimpleEntry<>(floatVar, arrSize));
    }

    public void addFunction(IRFunction function) {
        functionList.add(function);
    }

    @Override
    public String toString() {
        StringBuilder programBuilder = new StringBuilder();

        programBuilder.append("start-program ").append(this.name).append('\n');
        programBuilder.append("\tstatic-int-list: ").append(varsListToString(intList)).append("\n");
        programBuilder.append("\tstatic-float-list: ").append(varsListToString(floatList)).append("\n");

        for (IRFunction function : functionList) {
            programBuilder.append('\n');
            programBuilder.append(function.toString());
        }

        programBuilder.append("\nend-program ").append(this.name);

        return programBuilder.toString();
    }
}

class IRFunction extends IRScope {
    private final String name;
    private final List<Map.Entry<String, String>> paramList;
    private final String returnType;

    private final List<Map.Entry<String, Integer>> intList;
    private final List<Map.Entry<String, Integer>> floatList;
    private final List<String> commandList;

    private int temp_count;

    public IRFunction(String name, Type returnType) {
        this.name = name;
        this.returnType = returnType.getBaseType();
        this.paramList = new ArrayList<>();

        intList = new ArrayList<>();
        floatList = new ArrayList<>();
        commandList = new ArrayList<>();

        temp_count = 0;
    }

    @Override
    public void addInt(String intVar, int arrSize) {
        intList.add(new AbstractMap.SimpleEntry<>(intVar, arrSize));
    }

    @Override
    public void addFloat(String floatVar, int arrSize) {
        floatList.add(new AbstractMap.SimpleEntry<>(floatVar, arrSize));
    }

    public void addParam(String paramName, Type paramType) {
        String type = paramType.getBaseType();
        paramList.add(new AbstractMap.SimpleEntry<>(paramName, type));

        List<Map.Entry<String, Integer>> varList = (type.equals("int")) ? intList : floatList;
        varList.add(new AbstractMap.SimpleEntry<>(paramName, 0));
    }

    public void addCommand(Command command, String... args) {
        StringBuilder commandBuilder = new StringBuilder();

//        switch (command) {
//            case ASSIGN:
//                String secondArg = args[1];
//                if (args[1] == "temp") secondArg = "_t" + temp_count;
//                commandBuilder.append("assign ").append(args[0]).append(secondArg);
//                break;
//        }

        commandList.add(commandBuilder.toString());
    }

    @Override
    public String toString() {
        StringBuilder funcBuilder = new StringBuilder();

        funcBuilder.append("start-function ").append(this.name).append('\n');
        funcBuilder.append("\t").append(returnType).append(" ").append(name).
                    append(" (").append(paramListToString()).append(")\n");
        funcBuilder.append("\tstatic-int-list: ").append(varsListToString(intList)).append("\n");
        funcBuilder.append("\tstatic-float-list: ").append(varsListToString(floatList)).append("\n");
        funcBuilder.append("\t").append(name).append(":\n");

        for (String command : commandList)
            funcBuilder.append(command);

        funcBuilder.append("end-function ").append(this.name);

        return funcBuilder.toString();
    }

    public String paramListToString() {
        StringBuilder listBuilder = new StringBuilder();

        for (int i = 0; i < paramList.size(); i++) {
            if (i != 0) listBuilder.append(", ");
            listBuilder.append(paramList.get(i).getValue()); // type
            listBuilder.append(paramList.get(i).getKey()); // var name
        }

        return listBuilder.toString();
    }
}
