import java.util.Objects;

public class Type {
    public static Type FLOAT = new Type("float");
    public static Type INT = new Type("int");
    public static Type VOID = new Type("void");
    public static Type ERROR = new Type("error");

    private boolean isArray;
    private int arraySize;
    private String baseType;

    public static boolean isBuiltIn(Type type){
        if (type.getBaseType().equals("float") ||
                type.getBaseType().equals("int") ||
                type.getBaseType().equals("error") ||
                type.getBaseType().equals("void")){
            return true;
        }
        return false;
    }

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

    public String getBaseType() {
        return baseType;
    }

    public boolean isArray() {
        return isArray;
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

    @Override
    public String toString() {
        if (isArray)
            return baseType + "[" + arraySize + "]";
        else
            return baseType;
    }
}
