package src.org.nvl.core.variable;

/**
 * Created by Vicky on 26.5.2017 г..
 */
public class Type {

    public static boolean isNumber(String input) {
        return input.matches("[\\d]+");
    }

    public static boolean isString(String input) {
        return input.matches("'[\\w\\s]+'");
    }

    public static boolean isArray(String input) {
        return input.matches("\\{\\d+(,\\d+)*\\}");
    }

    public static boolean isBoolean(String input) {
        return input.equalsIgnoreCase("FALSE") || input.equalsIgnoreCase("TRUE");
    }

    public static boolean isWord(String input) { return input.matches("[\\w]+"); }
}
