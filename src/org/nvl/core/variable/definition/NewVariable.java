package src.org.nvl.core.variable.definition;

import src.org.nvl.MessageConstants;
import src.org.nvl.core.Pair;
import src.org.nvl.core.Substring;
import src.org.nvl.core.input.split.SplitString;
import src.org.nvl.core.input.type.SideType;
import src.org.nvl.core.responder.ResponderImpl;
import src.org.nvl.core.rpn.AbstractRpnVerifier;
import src.org.nvl.core.rpn.Rpn;
import src.org.nvl.core.rpn.verifier.BooleanRpnVerifier;
import src.org.nvl.core.rpn.verifier.NumberRpnVerifier;
import src.org.nvl.core.rpn.verifier.StringRpnVerifier;
import src.org.nvl.core.statement.RpnStatementVerifier;
import src.org.nvl.core.variable.Type;
import org.apache.commons.lang3.*;

import java.util.stream.IntStream;

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
    public static String replaceRightSideString(String rightSide, String leftSide, String varName, SideType type) {
        StringRpnVerifier stringRpnVerifier = new StringRpnVerifier();
        NumberRpnVerifier numberRpnVerifier = new NumberRpnVerifier();
        leftSide = leftSide.replaceAll(" \\* ", "*");
        Pair<String, Integer> front = getFront(leftSide, stringRpnVerifier);
        Pair<String, Integer> back = getBack(leftSide, stringRpnVerifier);
        String containVar = getMiddle(leftSide, front.second, back.second);
        String rpn = stringRpnVerifier.createRpn(rightSide + " - " + back.first);
        String calculatedRpn = stringRpnVerifier.calculateRpn(rpn);
        String simplifiedRightSide = removeBegin(front.first, calculatedRpn);
        SplitString splitString = new SplitString(containVar);
        Substring substrings[] = new Substring[rightSide.length()];
        int currentPosition = 0;
        int[] multiplyCoefficients = new int[splitString.getSplitInput().length];
        int nthCoeff = 0;
        int sum = 0;
        while (!splitString.isEmpty()) {
            String currentElement = splitString.getCurrentElement();
            if(currentElement.contains("'")) {
                rpn = stringRpnVerifier.createRpn(currentElement);
                calculatedRpn = stringRpnVerifier.calculateRpn(rpn);
                String var = StringUtils.substringBetween(currentElement, "'");
                substrings[currentPosition] = new Substring(sum, sum + calculatedRpn.length() - 2, var);
                sum += calculatedRpn.length() - 2;
                currentPosition++;
            } else if (!currentElement.equals("+")){
                Pair<String, String> coefficients = getCoefficients(currentElement, varName);
                rpn = stringRpnVerifier.createRpn(coefficients.second);
                calculatedRpn = numberRpnVerifier.calculateRpn(rpn);
                multiplyCoefficients[nthCoeff] = Integer.valueOf(calculatedRpn);
                nthCoeff++;
                sum += Integer.valueOf(calculatedRpn);
            }
            splitString.nextPosition();
        }

        return getValue(simplifiedRightSide, multiplyCoefficients, substrings, currentPosition);
    }

    private static String getValue(String simplifiedRightSide, int[] multiplyCoefficients,
                                   Substring[] positions, int positionsLength) {
        int currentPosition = 0, begin, end = 0;
        simplifiedRightSide = StringUtils.substringBetween(simplifiedRightSide, "'");
        int remainingLength = simplifiedRightSide.length();
        while(currentPosition < positionsLength) {
            String string = positions[currentPosition].val;
            for(int i = positions[currentPosition].begin; i < positions[currentPosition].end; i+=string.length()) {
                String correspondantStringRightSide = simplifiedRightSide.substring(i, i + string.length());
                if(!correspondantStringRightSide.equals(string)) {
                    throw new RuntimeException(MessageConstants.IMPOSSIBLE_INITIALIZATION_MESSAGE);
                }
                remainingLength -= string.length();
            }
            currentPosition++;
        }
        int sumCoefficients = IntStream.of(multiplyCoefficients).sum();
        if(remainingLength % sumCoefficients != 0) {
            throw new RuntimeException(MessageConstants.IMPOSSIBLE_INITIALIZATION_MESSAGE);
        }
        String value = "";
        StringRpnVerifier stringRpnVerifier = new StringRpnVerifier();
        for(int i = 0; i <= positionsLength; i++) {
            begin = end;
            if(i != positionsLength) {
                end = positions[i].begin;
            } else {
                end = simplifiedRightSide.length();
            }
            String toCheck = simplifiedRightSide.substring(begin, end);
            if(!value.equals("")) {
                String rpn = stringRpnVerifier.createRpn(value + " * " + multiplyCoefficients[i]);
                String result = stringRpnVerifier.calculateRpn(rpn);
                if(!result.equals("'" + toCheck + "'")) {
                    throw new RuntimeException(MessageConstants.IMPOSSIBLE_INITIALIZATION_MESSAGE);
                }
            } else {
                String rpn = stringRpnVerifier.createRpn("'" + toCheck + "'" + " / " + multiplyCoefficients[i]);
                value = stringRpnVerifier.calculateRpn(rpn);
            }
            if(i != positionsLength) {
                end = positions[i].end;
            }
        }
        return value;
    }

    private static Pair<String, Integer> getFront(String leftSide, StringRpnVerifier stringRpnVerifier) {
        StringBuilder front = new StringBuilder(leftSide.length());
        SplitString splitString = new SplitString(leftSide);
        Integer position = 0;
        while(!splitString.isEmpty() && (splitString.getCurrentElement().contains("'")
                || splitString.getCurrentElement().equals("+"))) {
            front.append(splitString.getCurrentElement()).append(' ');
            splitString.nextPosition();
            position++;
        }
        front.append("''");
        String rpn = stringRpnVerifier.createRpn(front.toString());
        return  new Pair(stringRpnVerifier.calculateRpn(rpn), position);
    }

    private static Pair<String, Integer> getBack(String leftSide, StringRpnVerifier stringRpnVerifier) {
        StringBuilder back = new StringBuilder(leftSide.length());
        SplitString splitString = new SplitString(leftSide);
        int size = splitString.getSplitInput().length - 1;
        while(size >= 0 && (splitString.getNthElement(size).contains("'") || splitString.getNthElement(size).equals("+"))) {
            back.append(splitString.getNthElement(size)).append(' ');
            size--;
        }
        back.append("''");
        String rpn = stringRpnVerifier.createRpn(back.toString());
        return  new Pair(stringRpnVerifier.calculateRpn(rpn), size);
    }

    private static String getMiddle(String leftSide, Integer begin, Integer end) {
        StringBuilder middle = new StringBuilder(leftSide.length());
        SplitString splitString = new SplitString(leftSide);
        while(begin <= end) {
            middle.append(splitString.getNthElement(begin)).append(' ');
            begin++;
        }
        if(middle.length() > 0) {
            return middle.toString().substring(0, middle.length() - 1);
        }
        return "";
    }

    private static String removeBegin(String begin, String calculatedRpn) {
        int matchingSymbols = 0;
        //begin.length - 1  because the final ' must be excluded
        while(matchingSymbols < begin.length() - 1 && calculatedRpn.charAt(matchingSymbols) == begin.charAt(matchingSymbols)) {
            matchingSymbols++;
        }
        if(matchingSymbols < begin.length() - 1) {
            throw new RuntimeException(MessageConstants.IMPOSSIBLE_INITIALIZATION_MESSAGE);
        }
        return "'" + calculatedRpn.substring(begin.length()- 1, calculatedRpn.length());
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
