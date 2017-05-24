package src.org.nvl.core.input.white_space;

public class InputSpaceFixer {
    private static final String ISOLATION_FORMAT = " %s ";
    private static final String NON_ISOLATION_FORMAT = "%s";

    public static String fix(String input) {
        input = doNotIsolate(input);
        input = isolate(input);
        return input.trim();
    }

    private static String doNotIsolate(String input) {
        String[] shouldNotBeIsolatedElements = {",", "\\{", "\\}"};
        input = singleIsolate(input, shouldNotBeIsolatedElements, NON_ISOLATION_FORMAT);
        return input;
    }

    private static String isolate(String input) {
        String[] specialElements = {"\\(", "\\)", "\\+", "\\*", "-", "\\^", "/", "&&", "\\|\\|", "\\!", "=", "<", ">"};

        input = singleIsolate(input, specialElements, ISOLATION_FORMAT);

        input = input.replaceAll("[\\s]+", " ");

        input = input.replaceAll("> =", ">=");
        input = input.replaceAll("< =", "<=");
        input = input.replaceAll("=" + "[\\s]+" + "=", "==");
        return input.replaceAll("! =", "!=");
    }

    private static String singleIsolate(String input, String[] specialElements, String format) {
        for (String element : specialElements) {
            input = input.replaceAll("[\\s]*" + element + "[\\s]*", String.format(format, element));
        }

        return input;
    }
}
