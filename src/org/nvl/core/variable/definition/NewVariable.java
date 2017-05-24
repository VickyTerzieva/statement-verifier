package src.org.nvl.core.variable.definition;

import jdk.nashorn.internal.runtime.regexp.RegExp;
import src.org.nvl.core.rpn.verifier.NumberRpnVerifier;

/**
 * Created by Vicky on 23.5.2017 г..
 */
public class NewVariable {

    public static String replaceRightSideNumber(String rightSide, String leftSide, String varName) {
        String replacedRightSide = rightSide;
        leftSide = leftSide.replaceAll(" \\* ", "*");
        leftSide = leftSide.replaceAll(" \\- ", " + -");
        String[] splitLeftSide = leftSide.split(" \\+ ");
        String[] containVar = new String[splitLeftSide.length];
        String[] dontContainVar = new String[splitLeftSide.length];
        int indexContainVar = 0, indexDontContainVar = 0;
        for(int i = 0; i < splitLeftSide.length; i++) {
            if(splitLeftSide[i].contains(varName)) {
                String toCalculate = replacePlusMinusVar(splitLeftSide[i], varName);
                int index = toCalculate.indexOf(varName);
                if(toCalculate.indexOf(varName, index + 1) != - 1) {
                    throw new RuntimeException("Only linear definitions supported!");
                }
                if(toCalculate.length() > 1 && index == toCalculate.length() - 1){
                    toCalculate = toCalculate.substring(0, toCalculate.length() - 2);
                } else if (toCalculate.length() > 1) {
                    String first = toCalculate.substring(0, index - 1);
                    String second = toCalculate.substring(index + 1, toCalculate.length());
                    toCalculate = first + second;
                }
                containVar[indexContainVar] = toCalculate;
                indexContainVar++;
            } else {
                dontContainVar[indexDontContainVar] = splitLeftSide[i];
                indexDontContainVar++;
            }
        }
        String toAdd = concatenate(dontContainVar);
        String toMultiply = concatenate(containVar);
        toMultiply = toMultiply.replaceAll("\\*", " * ");
        NumberRpnVerifier rpn = new NumberRpnVerifier();
        if(!toAdd.equals("")) {
            String rpnSubtract = rpn.createRpn(toAdd);
            String toSubtract = rpn.calculateRpn(rpnSubtract);
            replacedRightSide = "( " + rightSide + " - " + toSubtract + " )";
        }
        if(!toMultiply.equals("")) {
            String rpnDivideBy = rpn.createRpn(toMultiply);
            String toDivideBy = rpn.calculateRpn(rpnDivideBy);
            replacedRightSide = replacedRightSide + " / " + toDivideBy;
        }
        return replacedRightSide;
    }

    private static String concatenate(String[] expressions) {
        StringBuilder concatenatedExpression = new StringBuilder();
        concatenatedExpression.append("");
        if(expressions.length > 0 && expressions[0] != null) {
            concatenatedExpression.append(expressions[0]);
        }
        for(int i = 1; i < expressions.length && expressions[i] != null; i++) {
            if(expressions[i].startsWith("-")) {
                concatenatedExpression.append(" - ");
                concatenatedExpression.append(expressions[i].substring(1, expressions[i].length()));
            } else {
                concatenatedExpression.append(" + ");
                concatenatedExpression.append(expressions[i]);
            }
        }
        return concatenatedExpression.toString();
    }

    private static String replacePlusMinusVar(String expression, String varName) {
        int index = expression.indexOf(varName);
        String result = expression;
        if(index > 0) {
            char previousChar = expression.charAt(index - 1);
            if(previousChar == '-') {
                result = result.replaceAll("\\-", "-1*");
            }
        } else { // a * ... or only a
            result = "1*" + result;
        }
        return result;
    }

    public static String replaceRightSideArray(String rightSide, String leftSide, String varName) {

        return "";
    }

    public static String replaceRightSideBoolean(String rightSide, String leftSide, String varName) {

        return "";
    }


    public static String replaceRightSideString(String rightSide, String leftSide, String varName) {
        String[] expressions = leftSide.split("\\+");
        for(int i = 0; i < expressions.length; i++) {
            int indexOfVar = expressions[i].indexOf(varName);
            if (expressions[i].indexOf(varName, indexOfVar + 1) != -1) {
                throw new RuntimeException("Invalid input!");
            }
        }
        return rightSide;
    }


}
