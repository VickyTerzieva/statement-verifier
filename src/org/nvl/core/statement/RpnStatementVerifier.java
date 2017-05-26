package src.org.nvl.core.statement;

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
        inputTree = variableSubstituter.substituteVariables(inputTree);
        String result = verifyInput(inputTree);
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

            if((typeLeft != typeRight && typeLeft != SideType.UNEVALUATED && typeRight != SideType.UNEVALUATED)
                    || (data.matches("<=|>=|<|>") && (typeLeft == SideType.BOOLEAN || typeRight == SideType.BOOLEAN))) {
                throw new RuntimeException(INVALID_INPUT_MESSAGE);
            }

            if(containsUnevaluatedVariable) {
                return tryToCalculate(left, right, data);
            }
            else {
                rightSide = getSideValue(right, typeRight);
                leftSide = getSideValue(left, typeLeft);

                boolean result;
                if (typeRight == SideType.NUMBER) {
                    result = AbstractRpnVerifier.compare(Integer.parseInt(leftSide), Integer.parseInt(rightSide), data);
                } else if (data.equals("||") || data.equals("&&")) {
                    result = BooleanRpnVerifier.executeBooleanOperation(leftSide, rightSide, data);
                } else {
                    result = AbstractRpnVerifier.compare(leftSide.toLowerCase(), rightSide.toLowerCase(), data);
                }
                return String.valueOf(result).toUpperCase();
            }
        }
        return "";
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
                checkType(inputTree.getValue());
                if(numberOfOperations != 1 || !isBooleanOperation) {
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
        throw new RuntimeException(OPERATION_MIX_MESSAGE);
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
        if((typeLeft != typeRight && typeLeft != SideType.UNEVALUATED && typeRight != SideType.UNEVALUATED)
                || (operation.matches("<=|>=|<|>") && (typeLeft == SideType.BOOLEAN || typeRight == SideType.BOOLEAN))) {
            throw new RuntimeException(INVALID_INPUT_MESSAGE);
        }

        String rightVal, leftVal;
        String var = ResponderImpl.getVariable(right.toString());
        if(var.equals("")) {
            var = ResponderImpl.getVariable(left.toString());
        }

        if(left.isLeaf() && right.isLeaf()) {
            String res;

            if(typeRight != SideType.BOOLEAN) {
                res = computeNonBoolean(left, right, operation, var, typeRight);
            } else {
                res = computeBoolean(left, operation, right, var);
            }
            return res.toUpperCase();
        } else if (left.isLeaf()) {
            rightVal = tryToCalculate(right.getLeftSide(), right.getRightSide(), operation);
            leftVal = computeBoolean(left, operation, right, var);
        } else if (right.isLeaf()) {
            leftVal = tryToCalculate(left.getLeftSide(), left.getRightSide(), operation);
            rightVal = computeBoolean(left, operation, right, var);
        } else {
            rightVal = tryToCalculate(right.getLeftSide(), right.getRightSide(), operation);
            leftVal = tryToCalculate(left.getLeftSide(), left.getRightSide(), operation);
        }
        boolean result = BooleanRpnVerifier.executeBooleanOperation(leftVal, rightVal, operation);
        return String.valueOf(result).toUpperCase();
    }

    private String getValue(String side, String var, SideType type) {
        Pair<String, String> coefficients = NewVariable.getCoefficients(side, var);
        String toAdd = coefficients.first;
        String toMultiply = coefficients.second;
        String res = "";
        try {
            NewVariable.getToSubtract(toAdd, type);
            res = "TRUE";
        } catch (Exception e) {
            if(e.getMessage().equals(NEGATIVE_NUMBERS_MESSAGE)) {
                res = "FALSE";
            }
        }
        try {
            NewVariable.getToDivideBy(toMultiply);
            return res;
        } catch (Exception e) {
            if(e.getMessage().equals(EMPTY_STACK_MESSAGE)) {
                if(res.equals("TRUE")) {
                    throw new RuntimeException(UNDETERMINED_VALUE_MESSAGE);
                } else {
                    return res;
                }
            }
        }
        return res;
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
        if(resultTrue.equalsIgnoreCase("true") && resultFalse.equalsIgnoreCase("true")) {
            res = "TRUE";
        } else {
            res = "FALSE";
        }
        return res;
    }

    private String computeNonBoolean(InputTree left, InputTree right, String operation, String var, SideType type) {
        String minusSide, minuend, subtrahend;
        if(operation.equals(">=") || operation.equals(">")) {
            minuend = left.toString();
            subtrahend = right.toString();
        } else {
            minuend = right.toString();
            subtrahend = left.toString();
        }

        minusSide = changeSigns(subtrahend);
        return getValue(minuend + " - " + minusSide, var, type);
    }
}
