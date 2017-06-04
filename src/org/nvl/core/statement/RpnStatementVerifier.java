package src.org.nvl.core.statement;

import org.apache.commons.lang3.StringUtils;
import src.org.nvl.MessageConstants;
import src.org.nvl.core.Pair;
import src.org.nvl.core.input.split.SplitString;
import src.org.nvl.core.input.substituter.VariableSubstituter;
import src.org.nvl.core.input.tree.InputTree;
import src.org.nvl.core.input.type.SideType;
import src.org.nvl.core.input.white_space.InputSpaceFixer;
import src.org.nvl.core.responder.ResponderImpl;
import src.org.nvl.core.rpn.AbstractRpnVerifier;
import src.org.nvl.core.rpn.Rpn;
import src.org.nvl.core.rpn.verifier.BooleanRpnVerifier;
import src.org.nvl.core.rpn.verifier.NumberRpnVerifier;
import src.org.nvl.core.rpn.verifier.StringRpnVerifier;
import src.org.nvl.core.variable.Type;
import src.org.nvl.core.variable.VariableType;
import src.org.nvl.core.variable.definition.NewVariable;
import src.org.nvl.core.variable.manager.VariableManager;

import static src.org.nvl.MessageConstants.*;
import static src.org.nvl.core.input.invalid_operator_usage.InvalidOperatorUsage.endsWithOperator;
import static src.org.nvl.core.input.invalid_operator_usage.InvalidOperatorUsage.startsWithOperator;

public class RpnStatementVerifier implements StatementVerifier {

    private VariableManager variableManager;
    private boolean isBooleanOperation;
    private boolean isStringOperation;
    private boolean isIntegerOperation;
    private boolean isArrayOperation;
    private boolean sideContainsUnevaluatedVariable;
    private boolean containsUnevaluatedVariable;
    private int numberOfOperations = 0;

    public boolean isBooleanOperation() {
        return isBooleanOperation;
    }

    public boolean isStringOperation() {
        return isStringOperation;
    }

    public boolean isIntegerOperation() { return isIntegerOperation; }

    public boolean isArrayOperation() {
        return isArrayOperation;
    }

    public int getNumberOfOperations() { return numberOfOperations; }

    public boolean containsUnevaluatedVariable() { return sideContainsUnevaluatedVariable; }

    public RpnStatementVerifier(VariableManager variableManager) {
        this.variableManager = variableManager;
    }

    @Override
    public boolean verifyStatement(String statement) {
        VariableSubstituter variableSubstituter = new VariableSubstituter(variableManager);
        statement = InputSpaceFixer.fix(statement);
        checkInput(statement);
        InputTree inputTree = new InputTree();
        inputTree = inputTree.createTree(statement);
        if(inputTree.isLeaf() && !Type.isBoolean(inputTree.toString()) &&
                !variableManager.containsVariable(inputTree.toString())) {
            throw new RuntimeException(UNEVALUATABLE_EXPRESSION_MESSAGE);
        }
        inputTree = variableSubstituter.substituteVariables(inputTree);
        String result = verifyInput(inputTree);
        if(result.equalsIgnoreCase(UNDETERMINED_VALUE_MESSAGE)) {
            throw new RuntimeException(UNDETERMINED_VALUE_MESSAGE);
        }
        return result.equalsIgnoreCase("TRUE");
    }

    //must be called on substituted statement
    public void checkType(String statement) {
        isBooleanOperation = false;
        isStringOperation = false;
        isArrayOperation = false;
        isIntegerOperation = false;
        sideContainsUnevaluatedVariable = false;
        numberOfOperations = 0;

        SplitString splitString = new SplitString(statement);
        while(!splitString.isEmpty()) {
            String currentElement = splitString.getCurrentElement();

            if(Type.isString(currentElement) || isVariableOfType(currentElement, VariableType.STRING)) {
                if(!isStringOperation) {
                    numberOfOperations++;
                }
                isStringOperation = true;
            } else if(Type.isArray(currentElement) || isVariableOfType(currentElement, VariableType.ARRAY)) {
                if(!isArrayOperation) {
                    numberOfOperations++;
                }
                isArrayOperation = true;
            } else if (Type.isBoolean(currentElement) || isVariableOfType(currentElement, VariableType.BOOLEAN)
                    || currentElement.matches("&&|\\|\\|")) {
                if(!isBooleanOperation) {
                    numberOfOperations++;
                }
                isBooleanOperation = true;
            } else if (Type.isNumber(currentElement) || isVariableOfType(currentElement, VariableType.NUMBER)) {
                if(!isIntegerOperation) {
                    numberOfOperations++;
                }
                isIntegerOperation = true;
            } else if(Type.isWord(currentElement) && !variableManager.containsVariable(currentElement)) {
                sideContainsUnevaluatedVariable = true;
                containsUnevaluatedVariable = true;
            } else if (!currentElement.matches("\\+|\\*|\\-|/|!=|=|<|>|>=|<=|\\(|\\)|\\^|!")) { //already checked for matching brackets, see InputTree
                throw new RuntimeException(INVALID_INPUT_MESSAGE);
            }

            splitString.nextPosition();
        }
    }

    private String verifyInput(InputTree inputTree) {
        if(!inputTree.isLeaf()) {
            String data = inputTree.getValue();
            InputTree left = inputTree.getLeftSide();
            InputTree right = inputTree.getRightSide();
            SideType typeLeft = getType(left);
            SideType typeRight = getType(right);
            String rightSide, leftSide;

            checkIfInvalidInput(left, right, typeLeft, typeRight, data);

            if(containsUnevaluatedVariable) {
                return tryToCalculate(left, right, data).toUpperCase();
            }
            else {
                rightSide = getSideValue(right, typeRight);
                leftSide = getSideValue(left, typeLeft);

                String result;
                if(typeRight == SideType.NUMBER) {
                    result = String.valueOf(AbstractRpnVerifier.compare(Integer.parseInt(leftSide), Integer.parseInt(rightSide), data));
                } else if(typeLeft == SideType.BOOLEAN || typeRight == SideType.BOOLEAN) {
                    result = BooleanRpnVerifier.executeBooleanOperation(leftSide, rightSide, data);
                } else {
                    result = String.valueOf(AbstractRpnVerifier.compare(leftSide, rightSide, data));
                }
                return result.toUpperCase();
            }
        }
        return inputTree.toString();
    }

    private void checkIfInvalidInput(InputTree left, InputTree right, SideType typeLeft, SideType typeRight, String data) {
        if((!typesMatch(left.toString(), right.toString(), typeLeft, typeRight)
                && typeLeft != SideType.UNEVALUATED && typeRight != SideType.UNEVALUATED)
                || (data.matches("<=|>=|<|>") && (typeLeft == SideType.BOOLEAN || typeRight == SideType.BOOLEAN))) {
            throw new RuntimeException(INVALID_INPUT_MESSAGE);
        }
    }

    private String getSideValue(InputTree tree, SideType type) {
        String response = "";
        String rpn;
        AbstractRpnVerifier rpnVerifier;
        if(tree.isLeaf()) {
            rpnVerifier = Rpn.makeRpn(type);
            rpn = rpnVerifier.createRpn(tree.toString());
            response = rpnVerifier.calculateRpn(rpn);
        } else {
            response = verifyInput(tree);
        }
        return response;
    }

    private SideType getType(InputTree inputTree) {
        if(!inputTree.isLeaf()) {  // value of inputTree data is an operator
            if(inputTree.getValue().equals("&&") || inputTree.getValue().equals("||")) {
                SideType left = getType(inputTree.getLeftSide());
                SideType right = getType(inputTree.getRightSide());
                if((left != SideType.BOOLEAN  && left != SideType.UNEVALUATED) ||
                        (right != SideType.BOOLEAN && right != SideType.UNEVALUATED)) {
                    throw new RuntimeException(INVALID_INPUT_MESSAGE);
                }
            }
            return SideType.BOOLEAN;
        }
        String expression = inputTree.getValue();
        checkType(expression);
        if(isBooleanOperation() && numberOfOperations == 1) {
            return SideType.BOOLEAN;
        }
        if((isStringOperation() && numberOfOperations == 1) ||
                (isStringOperation() && isIntegerOperation() &&
                        numberOfOperations == 2 && (expression.contains("*") || expression.contains("/")))) {
            return SideType.STRING;
        }
        if((isArrayOperation() && isIntegerOperation() && numberOfOperations == 2) ||
                (isArrayOperation() && numberOfOperations == 1)) {
            return SideType.ARRAY;
        }
        if(isIntegerOperation() && numberOfOperations == 1) {
            return SideType.NUMBER;
        }
        if(containsUnevaluatedVariable() && numberOfOperations == 0) {
            return SideType.UNEVALUATED;
        }
        throw new RuntimeException(INVALID_INPUT_MESSAGE);
    }

    private void checkInput(String userInput) {
        if (startsWithOperator(userInput) || endsWithOperator(userInput)) {
            throw new RuntimeException(String.format(INVALID_OPERATOR_FORMAT, userInput));
        }
    }

    private boolean isVariableOfType(String currentElement, VariableType neededType) {
        boolean isVariable = variableManager.containsVariable(currentElement);
        return isVariable && variableManager.getVariable(currentElement).getType() == neededType;
    }

    private String tryToCalculate(InputTree left, InputTree right, String operation) {
        SideType typeLeft = getType(left);
        SideType typeRight = getType(right);
        checkIfInvalidInput(left, right, typeLeft, typeRight, operation);

        String rightVal, leftVal;
        String var = ResponderImpl.getVariable(right.toString());
        if(var.equals("")) {
            var = ResponderImpl.getVariable(left.toString());
        }

        if(left.isLeaf() && right.isLeaf()) {
            String res;

            if(typeRight != SideType.BOOLEAN && typeLeft != SideType.BOOLEAN && !operation.equals("&&")
                    && !operation.equals("||")) {
                if(typeRight == SideType.NUMBER && typeLeft != SideType.UNEVALUATED) { //the other type migth be string or array, so we take it
                    res = computeNonBoolean(left, right, operation, var, typeLeft);
                }
                else {
                    res = computeNonBoolean(left, right, operation, var, typeRight);
                }
            } else {
                res = computeBoolean(left, operation, right, var);
            }
            return res.toUpperCase();
        } else if (left.isLeaf()) {
            rightVal = tryToCalculate(right.getLeftSide(), right.getRightSide(), right.getValue());
            leftVal = left.getValue();
            if(!Type.isBoolean(leftVal)) {
                leftVal = UNDETERMINED_VALUE_MESSAGE;
            }
        } else if (right.isLeaf()) {
            leftVal = tryToCalculate(left.getLeftSide(), left.getRightSide(), left.getValue());
            rightVal = right.getValue();
            if(!Type.isBoolean(rightVal)) {
                rightVal = UNDETERMINED_VALUE_MESSAGE;
            }
        } else {
            rightVal = tryToCalculate(right.getLeftSide(), right.getRightSide(), right.getValue());
            leftVal = tryToCalculate(left.getLeftSide(), left.getRightSide(), left.getValue());
        }
        return BooleanRpnVerifier.executeBooleanOperation(leftVal, rightVal, operation);
    }

    private String getValue(String side, String var, SideType type, String operation) {
        Pair<String, String> coefficients = NewVariable.getCoefficients(side, var);
        String toAdd = coefficients.first;
        String toMultiply = coefficients.second;
        String calculated, signSubtraction = "", signMultiplication = "";
        try {
            calculated = NewVariable.getToSubtract(toAdd, type);
            if(calculated.equals("0") || calculated.equals("{0}") || calculated.equals("")) {
                signSubtraction = "=";
            } else {
                signSubtraction = ">";
            }
        } catch (Exception e) {
            if(e.getMessage().equals(NEGATIVE_NUMBERS_MESSAGE) || e.getMessage().equals(EMPTY_STACK_MESSAGE)) {
                signSubtraction = "<";
            }
        }
        try {
            calculated = NewVariable.getToDivideBy(toMultiply);
            if(calculated.equals("0") || calculated.equals("")) {
                signMultiplication = "=";
            } else {
                signMultiplication = ">";
            }
        } catch (Exception e) {
            if(e.getMessage().equals(NEGATIVE_NUMBERS_MESSAGE) || e.getMessage().equals(EMPTY_STACK_MESSAGE)) {
                signMultiplication = "<";
            }
        }
        if(operation.equals("==")) {
            return String.valueOf(signMultiplication.equals("=") && signSubtraction.equals("="));
        } if((signMultiplication.equals(">") && signSubtraction.equals("<"))
                || (signMultiplication.equals("<") && signSubtraction.equals(">"))) {
            return UNDETERMINED_VALUE_MESSAGE;
        } if(signMultiplication.equals(">") || signSubtraction.equals(">")) {
            return "TRUE";
        } if(signMultiplication.equals("=") && signSubtraction.equals("=") &&
                (operation.equals(">=") || operation.equals("<="))) {
            return "TRUE";
        }
        return "FALSE";
    }

    private String changeSigns(String s) {
        char[] charArr = s.toCharArray();
        for(int i = 0; i < s.length(); i++) {
            if(charArr[i] == '+') {
                charArr[i] = '-';
            } else if (charArr[i] == '-') {
                charArr[i] = '+';
            }
        }
        return String.valueOf(charArr);
    }

    private String computeBoolean(InputTree left, String operation, InputTree right, String var) {
        String res;
        Pair<String, String> bool = NewVariable.isAlwaysTrue(left.toString() + " " + operation + " "+ right.toString(),
                var);
        String resultTrue = bool.first;
        String resultFalse = bool.second;
        if(resultTrue.equalsIgnoreCase(resultFalse)) {
            res = resultTrue;
        } else {
            res = UNDETERMINED_VALUE_MESSAGE;
        }
        return res;
    }

    private String computeNonBoolean(InputTree left, InputTree right, String operation, String var, SideType type) {
        if(type == SideType.STRING) {
            return computeString(left, right, operation, var);
        }
        String minusSide, minuend, subtrahend;
        if(operation.equals(">=") || operation.equals(">")) {
            minuend = left.toString();
            subtrahend = right.toString();
        } else {
            minuend = right.toString();
            subtrahend = left.toString();
        }

        minusSide = changeSigns(subtrahend);
        return getValue(minuend + " - " + minusSide, var, type, operation);
    }

    private String computeString(InputTree left, InputTree right, String operation, String var) {
        StringRpnVerifier stringRpnVerifier = new StringRpnVerifier();
        String leftToString = left.toString();
        String rightToString = right.toString();
        String coefficientLeft, coefficientRight;
        while(!leftToString.isEmpty() && !rightToString.isEmpty()) {
            Pair<String, Integer> frontLeft = NewVariable.getFront(leftToString, stringRpnVerifier);
            Pair<String, Integer> frontRight = NewVariable.getFront(rightToString, stringRpnVerifier);
            if (frontLeft.first.equals(frontRight.first)) {
                if(!frontLeft.first.isEmpty() && !frontLeft.first.equals("''")) {
                    leftToString = leftToString.substring(StringUtils.ordinalIndexOf(leftToString, " ", frontLeft.second) - 1);
                    rightToString = rightToString.substring(StringUtils.ordinalIndexOf(rightToString, " ", frontRight.second) - 1);
                    final int POSITION_OF_OPERATOR = 1;
                    final int POSITION_AFTER_OPERATOR = 2;
                    if ((leftToString.length() > 0 && leftToString.charAt(POSITION_OF_OPERATOR) == '-') ||
                            (rightToString.length() > 0 && rightToString.charAt(POSITION_OF_OPERATOR) == '-')) {
                        return INVALID_OPERATION_MESSAGE; // e.g. not allowed '...' - 2*a
                    }
                    leftToString = leftToString.substring(POSITION_AFTER_OPERATOR);
                    rightToString = rightToString.substring(POSITION_AFTER_OPERATOR);
                }
                Pair<String, String> removeVariablesFrontLeft = removeVariablesFront(leftToString, var);
                Pair<String, String> removeVariablesFrontRight = removeVariablesFront(rightToString, var);
                coefficientLeft = removeVariablesFrontLeft.second;
                coefficientRight = removeVariablesFrontRight.second;
                leftToString = removeVariablesFrontLeft.first;
                rightToString = removeVariablesFrontRight.first;
                if(!coefficientLeft.equals(coefficientRight)) {
                    if(!leftToString.isEmpty() && !rightToString.isEmpty()) {
                        return UNDETERMINED_VALUE_MESSAGE;
                    }
                    return makeComparisson(leftToString, rightToString, coefficientLeft, coefficientRight, operation);
                }
            } else if(!containsUnevaluatedVariable(leftToString) && !containsUnevaluatedVariable(rightToString)){
                return String.valueOf(AbstractRpnVerifier.compare(frontLeft.first, frontRight.first, operation));
            }
        }
        if(operation.matches(">=|>")) {
            return String.valueOf(rightToString.isEmpty()).toUpperCase();
        } else if(operation.matches("<=|<")) {
            return String.valueOf(leftToString.isEmpty()).toUpperCase();
        }
        return "FALSE";
    }

    private String makeComparisson(String leftToString, String rightToString, String coefficientLeft,
                                   String coefficientRight, String operation) {
        if(leftToString.isEmpty() && rightToString.isEmpty()) {
            return String.valueOf(AbstractRpnVerifier.compare(coefficientLeft, coefficientRight, operation));
        } else if (!rightToString.isEmpty()) {
            if(AbstractRpnVerifier.compare(coefficientLeft, coefficientRight, ">")) {
                return UNDETERMINED_VALUE_MESSAGE;
            } else {
                return String.valueOf(AbstractRpnVerifier.compare(coefficientLeft, coefficientRight, operation));
            }
        } else {
            if(AbstractRpnVerifier.compare(coefficientLeft, coefficientRight, "<")) {
                return UNDETERMINED_VALUE_MESSAGE;
            } else {
                return String.valueOf(AbstractRpnVerifier.compare(coefficientLeft, coefficientRight, operation));
            }
        }
    }

    //result.first = string without variables at beginning, result.second = coefficient of variables at the beginning
    private Pair<String,String> removeVariablesFront(String leftToString, String var) {
        leftToString = leftToString.replaceAll(" \\* ", "*");
        NumberRpnVerifier numberRpnVerifier = new NumberRpnVerifier();
        SplitString left = new SplitString(leftToString);
        StringBuilder containVarLeft = new StringBuilder();
        String leftRest;
        int numberOfOperations = 0;
        while(!left.isEmpty() && ((!Type.isString(left.getCurrentElement()) && left.getCurrentElement().contains(var))
                || (left.getCurrentElement().matches("\\+|\\-")))) {
            containVarLeft.append(left.getCurrentElement()).append(' ');
            left.nextPosition();
            numberOfOperations++;
        }
        if(containVarLeft.charAt(containVarLeft.length() - 2) == '-') {
            throw new RuntimeException(INVALID_OPERATION_MESSAGE); // 'a' - a (not supported)
        }
        leftRest = concatenate(left);
        String containVar;
        if(numberOfOperations > 1) { // remove last operation (+ / -)
            containVar = containVarLeft.substring(0, containVarLeft.length() - 2);
        } else if(numberOfOperations > 0) { // remove last ' '
            containVar = containVarLeft.substring(0, containVarLeft.length() - 1);
        } else {
            containVar = "";
        };
        Pair<String, String> coefficientsLeft = NewVariable.getCoefficients(containVar, var);
        String rpn = numberRpnVerifier.createRpn(coefficientsLeft.second);
        String calculatedRpn = numberRpnVerifier.calculateRpn(rpn);
        return new Pair(leftRest, calculatedRpn);
    }

    private String concatenate(SplitString left) {
        StringBuilder result = new StringBuilder();
        while(!left.isEmpty()) {
            result.append(left.getCurrentElement()).append(' ');
            left.nextPosition();
        }
        return result.length() > 0 ? result.toString().substring(0, result.length() - 1) : "";
    }

    private boolean typesMatch(String leftSide, String rightSide, SideType typeLeft, SideType typeRight) {
        if(typeLeft == typeRight) {
            return true;
        }
        if((typeRight == SideType.ARRAY && typeLeft == SideType.NUMBER && containsUnevaluatedVariable(leftSide))
                || (typeLeft == SideType.ARRAY && typeRight == SideType.NUMBER && containsUnevaluatedVariable(rightSide))) {
            return true;
        }
        return (typeRight == SideType.STRING && typeLeft == SideType.NUMBER && containsUnevaluatedVariable(leftSide)
                && numberIsPartOfMultiplication(leftSide)) || (typeLeft == SideType.STRING && typeRight == SideType.NUMBER
                && containsUnevaluatedVariable(rightSide) && numberIsPartOfMultiplication(rightSide));
    }

    private boolean numberIsPartOfMultiplication(String leftSide) {
        return leftSide.matches(".* \\* [\\d]+.*") || leftSide.matches(".*[\\d]+ \\* .*");
    }

    private boolean containsUnevaluatedVariable(String leftSide) {
        SplitString splitString = new SplitString(leftSide);
        while(!splitString.isEmpty()) {
            String currentElement = splitString.getCurrentElement();
            if(!Type.isNumber(currentElement) && Type.isWord(currentElement) && !variableManager.containsVariable(currentElement)) {
                return true;
            }
            splitString.nextPosition();
        }
        return false;
    }
}
