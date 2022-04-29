import java.util.Objects;

public class Type {
    public static Type FLOAT = new Type("FLOAT");
    public static Type INT = new Type("INT");
    public static Type VOID = new Type("VOID");
    public static Type ERROR = new Type("ERROR");

    private boolean isArray;
    private int arraySize;
    private String baseType;

    public Type(String baseType, int arraySize){
        isArray = true;
        this.baseType = baseType;
        this.arraySize = arraySize;
    }

    public Type(String baseType){
        isArray = false;
        this.baseType = baseType;
        this.arraySize = -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Type type = (Type) o;
        return isArray == type.isArray && arraySize == type.arraySize && Objects.equals(baseType, type.baseType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isArray, arraySize, baseType);
    }
}
