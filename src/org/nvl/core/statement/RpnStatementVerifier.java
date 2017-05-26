package src.org.nvl.core.statement;

import src.org.nvl.MessageConstants;
import src.org.nvl.core.input.split.SplitString;
import src.org.nvl.core.input.substituter.VariableSubstituter;
import src.org.nvl.core.input.tree.InputTree;
import src.org.nvl.core.input.type.SideType;
import src.org.nvl.core.input.white_space.InputSpaceFixer;
import src.org.nvl.core.rpn.AbstractRpnVerifier;
import src.org.nvl.core.rpn.Rpn;
import src.org.nvl.core.rpn.verifier.ArrayRpnVerifier;
import src.org.nvl.core.rpn.verifier.BooleanRpnVerifier;
import src.org.nvl.core.rpn.verifier.NumberRpnVerifier;
import src.org.nvl.core.rpn.verifier.StringRpnVerifier;
import src.org.nvl.core.variable.Type;
import src.org.nvl.core.variable.VariableType;
import src.org.nvl.core.variable.manager.VariableManager;

import static src.org.nvl.MessageConstants.INVALID_INPUT_MESSAGE;
import static src.org.nvl.MessageConstants.INVALID_OPERATOR_FORMAT;
import static src.org.nvl.core.input.invalid_operator_usage.InvalidOperatorUsage.endsWithOperator;
import static src.org.nvl.core.input.invalid_operator_usage.InvalidOperatorUsage.startsWithOperator;

public class RpnStatementVerifier implements StatementVerifier {

    private VariableManager variableManager;
    private boolean isBooleanOperation;
    private boolean isStringOperation;
    private boolean isIntegerOperation;
    private boolean isArrayOperation;
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

    public boolean containsUnevaluatedVariable() { return containsUnevaluatedVariable; }

    public RpnStatementVerifier(VariableManager variableManager) {
        this.variableManager = variableManager;
    }

    @Override
    public boolean verifyStatement(String statement) {
        VariableSubstituter variableSubstituter = new VariableSubstituter(variableManager);
        statement = InputSpaceFixer.fix(statement);
        checkInput(statement);
        InputTree inputTree = new InputTree();
        inputTree = variableSubstituter.substituteVariables(inputTree);
        inputTree = inputTree.createTree(statement);
        String result = verifyInput(inputTree);
        return result.equalsIgnoreCase("TRUE");
    }

    //must be called on substituted statement
    public void checkType(String statement) {
        isBooleanOperation = false;
        isStringOperation = false;
        isArrayOperation = false;
        isIntegerOperation = false;
        containsUnevaluatedVariable = false;
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

            /*if(typeLeft == SideType.UNEVALUATED || typeRight == SideType.UNEVALUATED) {
                throw new RuntimeException("Cannot be evaluated!");
            }*/
            if((typeLeft != typeRight) || (data.matches("<=|>=|<|>") && typeLeft == SideType.BOOLEAN)) {
                throw new RuntimeException(MessageConstants.INVALID_INPUT_MESSAGE);
            }

            rightSide = getSideValue(right, typeRight);
            leftSide = getSideValue(left, typeLeft);

            boolean result;
            if(typeRight == SideType.NUMBER) {
                result = AbstractRpnVerifier.compare(Integer.parseInt(leftSide), Integer.parseInt(rightSide), data);
            } else if(data.equals("||") || data.equals("&&")) {
                result = BooleanRpnVerifier.executeBooleanOperation(leftSide, rightSide, data);
            } else {
                result = AbstractRpnVerifier.compare(leftSide, rightSide, data);
            }
            return String.valueOf(result).toUpperCase();
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
        throw new RuntimeException(MessageConstants.OPERATION_MIX_MESSAGE);
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
}
