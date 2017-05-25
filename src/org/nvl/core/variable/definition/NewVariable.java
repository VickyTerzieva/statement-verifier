package src.org.nvl.core.variable.definition;

import jdk.nashorn.internal.runtime.regexp.RegExp;
import src.org.nvl.core.Pair;
import src.org.nvl.core.input.type.SideType;
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
        toMultiply = toMultiply.replaceAll("\\*", " * ");
        AbstractRpnVerifier rpn = Rpn.makeRpn(type);
        NumberRpnVerifier numberRpnVerifier = new NumberRpnVerifier();
        if(!toAdd.equals("")) {
            String rpnSubtract = rpn.createRpn(toAdd);
            String toSubtract = rpn.calculateRpn(rpnSubtract);
            replacedRightSide = "( " + rightSide + " - " + toSubtract + " )";
        }
        if(!toMultiply.equals("")) {
            String rpnDivideBy = numberRpnVerifier.createRpn(toMultiply);
            String toDivideBy = numberRpnVerifier.calculateRpn(rpnDivideBy);
            replacedRightSide = replacedRightSide + " / " + toDivideBy;
        }
        replacedRightSide = replacedRightSide.replaceAll("[\\s]*\\+ \\-[\\s]*"," - ");
        replacedRightSide = replacedRightSide.replaceAll("[\\s]*\\- \\-[\\s]*"," + ");
        return replacedRightSide;
    }

    private static Pair<String, String> getCoefficients(String leftSide, String varName) {
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
                    throw new RuntimeException("Only linear definitions supported!");
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

    public static String replaceRightSideBoolean(String rightSide, String leftSide, String varName) {
        Pair<String, String> coefficients = getCoefficientsBool(leftSide, varName);
        String containVar = coefficients.first;
        String dontContainVar = coefficients.second;

        if(rightSide.equalsIgnoreCase("FALSE") && dontContainVar.equalsIgnoreCase("TRUE")) {
            // sth || true cannot be false
            throw new RuntimeException("Impossible operation!");
        }
        if(rightSide.equalsIgnoreCase("FALSE")) { //dontContainVar is also false or is empty
            if(!containVar.toLowerCase().contains("true")) {
                // containVar like: false && varName || varName && false; always false; then varName can be both true or false
                throw new RuntimeException("Multiple possible answers!");
            }
            //containVar like: false && varName ... || true && varName; true && varName = false only if varName = false
            return "false";
        }
        if(dontContainVar.equalsIgnoreCase("TRUE")) { //rightSide is also true
            // sth || true = true -> doesn't matter if 'sth' is true or false => varName can be either true or false
            throw new RuntimeException("Multiple possible answers!");
        }
        //rightSide is true, dontContainVar is false or is empty, then containVar must be true
        if(!containVar.toLowerCase().contains("true")) {
            //all disjunctions are gonna be false no matter the value of varName
            throw new RuntimeException("Impossible operation!");
        }
        //rightSide is true, dontContainVar is false or empty and containVar has disjunction containing 'true' so varName must be true
        return "true";
    }

    private static Pair<String, String> getCoefficientsBool(String leftSide, String varName) {
        String[] expressions = leftSide.split(" \\|\\| ");
        StringBuilder containVar = new StringBuilder(leftSide.length());
        StringBuilder dontContainVar = new StringBuilder(leftSide.length());
        BooleanRpnVerifier booleanRpnVerifier = new BooleanRpnVerifier();
        for(int i = 0; i < expressions.length; i++) {
            int index = expressions[i].indexOf(varName);
            String toCalculate = "";
            if(index != -1) {
                if(!containVar.toString().equals("")) {
                    containVar.append(" || ");
                }
                if(expressions[i].length() > 1 && index == expressions[i].length() - 1){
                    toCalculate = expressions[i].substring(0, expressions[i].length() - 2);
                } else if (expressions[i].length() > 1) {
                    String first = expressions[i].substring(0, index);
                    String second = expressions[i].substring(index + varName.length() + 3, expressions[i].length());
                    toCalculate = first + second;
                }
                if(!toCalculate.equals("")) {
                    String rpn = booleanRpnVerifier.createRpn(toCalculate);
                    containVar.append(varName).append(" && ").append(booleanRpnVerifier.calculateRpn(rpn));
                } else {
                    containVar.append(varName);
                }
            }
            else if(expressions[i].equalsIgnoreCase("FALSE") || expressions[i].equalsIgnoreCase("TRUE")) {
                if(!dontContainVar.toString().equals("")) {
                    dontContainVar.append(" && ");
                }
                dontContainVar.append(expressions[i]);
            }
        }
        String rpn = booleanRpnVerifier.createRpn(dontContainVar.toString());
        String freeOfVariablesPart = !rpn.equals("") ? booleanRpnVerifier.calculateRpn(rpn) : "";
        return new Pair(containVar.toString(), freeOfVariablesPart);
    }
}
