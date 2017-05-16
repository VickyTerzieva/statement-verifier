package src.org.nvl.core.responder;

import static src.org.nvl.MessageConstants.INVALID_INPUT_FORMAT;

//v100 - more than one operation supported (e.g a < 5 == 7 > 3 == 3 < 9) - main operation - first position
//get dividers together (splitInputByMainDivider)

//TODO

public class Divider {
    /*public String[] divide(String input) {
        String[] split = sideSplitInput(input);
        return new String[](split[0], split[2], split[1]);
    }*/

    public String[] splitInputByMainDivider(String input) {
        String[] dividers = new String[] {"<=", ">=", "==", "!=", "<", ">", "="};
        String[] sideSplit;

        sideSplit = splitBy(input, dividers);

        return sideSplit;
    }

    private String[] splitBy(String input, String[] delimiters) {
        String[] result = null;
        boolean foundDivider = false;
        String actualDelimiter = "";

        for (String delimiter : delimiters) {
            if (input.contains(delimiter)) {
                if (foundDivider) {
                    throw new RuntimeException(String.format(INVALID_INPUT_FORMAT, input, "Unallowed combination of operators"));
                }

                actualDelimiter = delimiter;
                result = input.split(" " + delimiter + " ");
                foundDivider = true;
            }
        }

        if (!foundDivider || result.length != 2) {
            throw new RuntimeException(String.format(INVALID_INPUT_FORMAT, input, "Try again "));
        }

        return new String[] {result[0], actualDelimiter, result[1]};
    }
}
