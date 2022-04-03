public enum Error {
    NO_ERROR(0),
    ARGUMENT_ERROR(1),
    LEXICAL_ERROR(2),
    SYNTAX_ERROR(3);

    private final int value;

    Error(final int newValue) {
        value = newValue;
    }

    public int getValue() { return value; }
}
