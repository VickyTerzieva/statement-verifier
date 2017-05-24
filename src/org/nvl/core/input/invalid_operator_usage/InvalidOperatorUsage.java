package src.org.nvl.core.input.invalid_operator_usage;

/**
 * Created by Vicky on 18.5.2017 г..
 */
public class InvalidOperatorUsage {
    public static boolean endsWithOperator(String userInput) {
        return userInput.matches(".*(\\+|\\*|\\-|/|&&|\\|\\||!=|=|<|>)");
    }

    public static boolean startsWithOperator(String userInput) {
        return userInput.matches("(\\*|/|&&|\\|\\||!=|=|<|>).*");
    }
}
