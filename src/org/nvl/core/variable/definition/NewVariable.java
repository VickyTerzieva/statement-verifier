package src.org.nvl.core.variable.definition;

import src.org.nvl.MessageConstants;
import src.org.nvl.core.Pair;
import src.org.nvl.core.input.split.SplitString;
import src.org.nvl.core.input.type.SideType;
import src.org.nvl.core.responder.ResponderImpl;
import src.org.nvl.core.rpn.AbstractRpnVerifier;
import src.org.nvl.core.rpn.Rpn;
import src.org.nvl.core.rpn.verifier.BooleanRpnVerifier;
import src.org.nvl.core.rpn.verifier.NumberRpnVerifier;

/**
 * Created by Vicky on 23.5.2017 г..
 */
public class NewVariable {

    public static String replaceRightSide(String rightSide, String leftSide, String varName, SideType type) {
        String replacedRightSide = rightSide;
        Pair<String, String> coefficients = getCoefficients(leftSide, varName);
        String toAdd = coefficients.first;
        String toMultiply = coefficients.second;
        Pair<String, String> calculatedCoefficients = getCalculatedCoefficients(toAdd, toMultiply, type);
        String toSubtract = calculatedCoefficients.first;
        String toDivideBy = calculatedCoefficients.second;

        if(!toSubtract.equals("")) {
            replacedRightSide = "( " + rightSide + " - " + toSubtract + " )";
        }
        if(!toDivideBy.equals("")) {
            replacedRightSide = replacedRightSide + " / " + toDivideBy;
        }

        replacedRightSide = replacedRightSide.replaceAll("[\\s]*\\+ \\-[\\s]*"," - ");
        replacedRightSide = replacedRightSide.replaceAll("[\\s]*\\- \\-[\\s]*"," + ");
        return replacedRightSide;
    }

    public static Pair<String, String> getCoefficients(String leftSide, String varName) {
        leftSide = leftSide.replaceAll(" \\* ", "*");
        leftSide = leftSide.replaceAll(" \\- ", " + -");
        String[] splitLeftSide = leftSide.split(" \\+ ");
        String[] containVar = new String[splitLeftSide.length];
        String[] dontContainVar = new String[splitLeftSide.length];
        int indexContainVar = 0, indexDontContainVar = 0;
        for(int i = 0; i < splitLeftSide.length; i++) {
            //if varName is not part of a string
            if(splitLeftSide[i].contains(varName) && !splitLeftSide[i].contains("'")) {
                String toCalculate = replacePlusMinusVar(splitLeftSide[i], varName);
                int index = toCalculate.indexOf(varName);
                if(toCalculate.indexOf(varName, index + 1) != -1) {
                    throw new RuntimeException(MessageConstants.LINEAR_DEFINITION_MESSAGE);
                }
                if(toCalculate.length() > 1 && index == toCalculate.length() - 1){
                    toCalculate = toCalculate.substring(0, toCalculate.length() - 2);
                } else if (toCalculate.length() > 1) {
                    String first = toCalculate.substring(0, index - 1);
                    String second = toCalculate.substring(index + varName.length(), toCalculate.length());
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
        return new Pair(toAdd, toMultiply);
    }

    private static Pair<String,String> getCalculatedCoefficients(String toAdd, String toMultiply, SideType type) {
        if(!ResponderImpl.getVariable(toAdd).equals("") || !ResponderImpl.getVariable(toMultiply).equals("")) {
            throw new RuntimeException(MessageConstants.UNDETERMINED_VALUE_MESSAGE);
        }
        toMultiply = toMultiply.replaceAll("\\*", " * ");
        String toDivideBy = getToDivideBy(toMultiply);
        String toSubtract = getToSubtract(toAdd, type);

        return new Pair(toSubtract, toDivideBy);
    }

    public static String getToDivideBy(String toMultiply) {
        NumberRpnVerifier numberRpnVerifier = new NumberRpnVerifier();
        String toDivideBy = "";
        if(!toMultiply.equals("")) {
            String rpnDivideBy = numberRpnVerifier.createRpn(toMultiply);
            toDivideBy = numberRpnVerifier.calculateRpn(rpnDivideBy);
        }
        return toDivideBy;
    }

    public static String getToSubtract(String toAdd, SideType type) {
        AbstractRpnVerifier rpn = Rpn.makeRpn(type);
        String toSubtract = "";
        if(!toAdd.equals("")) {
            String rpnSubtract = rpn.createRpn(toAdd);
            toSubtract = rpn.calculateRpn(rpnSubtract);
        }
        return toSubtract;
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

    //only two possibilities, we try with both
    public static String replaceRightSideBoolean(String rightSide, String leftSide, String varName) {
        Pair<String, String> bool = isAlwaysTrue(leftSide, varName);
        String resultTrue = bool.first;
        String resultFalse = bool.second;
        if(resultTrue.equalsIgnoreCase(rightSide) && resultFalse.equalsIgnoreCase(rightSide)) {
            throw new RuntimeException(MessageConstants.MULTIPLE_POSSIBLE_ANSWERS_MESSAGE);
        }
        if(resultTrue.equalsIgnoreCase(rightSide)) {
            return "true";
        }
        if(resultFalse.equalsIgnoreCase(rightSide)) {
            return "false";
        }
        throw new RuntimeException(MessageConstants.IMPOSSIBLE_OPERATION_MESSAGE);
    }

    //if varName = f and expression contains 'false', the 'f' in 'false' should not be replaced
    private static String replace(String leftSide, String varName, String bool) {
        SplitString splitString = new SplitString(leftSide);
        StringBuilder result = new StringBuilder();
        while(!splitString.isEmpty()) {
            String currentElement = splitString.getCurrentElement();
            if(currentElement.equals(varName)) {
                result.append(bool);
            } else {
                result.append(currentElement);
            }
            splitString.nextPosition();
        }
        return result.toString();
    }

    public static Pair<String, String> isAlwaysTrue(String side, String varName) {
        String leftSideTrue = replace(side, varName, "true");
        String leftSideFalse = replace(side, varName, "false");
        BooleanRpnVerifier rpnVerifier = new BooleanRpnVerifier();
        String rpnTrue = rpnVerifier.createRpn(leftSideTrue);
        String rpnFalse = rpnVerifier.createRpn(leftSideFalse);
        String resultTrue = rpnVerifier.calculateRpn(rpnTrue);
        String resultFalse = rpnVerifier.calculateRpn(rpnFalse);
        return new Pair(resultTrue, resultFalse);
    }
}
