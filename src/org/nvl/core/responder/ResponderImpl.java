package src.org.nvl.core.responder;

import src.org.nvl.core.input.split.SplitString;
import src.org.nvl.core.input.substituter.VariableSubstituter;
import src.org.nvl.core.input.tree.InputTree;
import src.org.nvl.core.input.type.InputType;
import src.org.nvl.core.input.type.InputTypeDeterminer;
import src.org.nvl.core.input.type.SideType;
import src.org.nvl.core.input.white_space.InputSpaceFixer;
import src.org.nvl.core.responder.processor.RequestProcessor;
import src.org.nvl.core.rpn.AbstractRpnVerifier;
import src.org.nvl.core.rpn.Rpn;
import src.org.nvl.core.rpn.verifier.BooleanRpnVerifier;
import src.org.nvl.core.statement.RpnStatementVerifier;
import src.org.nvl.core.variable.EvaluatedVariable;
import src.org.nvl.core.variable.Type;
import src.org.nvl.core.variable.VariableType;
import src.org.nvl.core.variable.definition.NewVariable;
import src.org.nvl.core.variable.manager.VariableManager;

import java.util.Set;

import static src.org.nvl.MessageConstants.*;
import static src.org.nvl.core.input.invalid_operator_usage.InvalidOperatorUsage.endsWithOperator;
import static src.org.nvl.core.input.invalid_operator_usage.InvalidOperatorUsage.startsWithOperator;

public class ResponderImpl implements Responder {
    private InputTypeDeterminer typeDeterminer;
    private RequestProcessor requestProcessor;
    private VariableSubstituter variableSubstituter;
    private VariableManager variableManager;

    public ResponderImpl(InputTypeDeterminer typeDeterminer, RequestProcessor requestProcessor,  VariableManager variableManager) {
        this.typeDeterminer = typeDeterminer;
        this.requestProcessor = requestProcessor;
        this.variableSubstituter = new VariableSubstituter(variableManager);
        this.variableManager = variableManager;
    }

    @Override
    public String process(String userInput) {
        String response = "";

        String spaceFixedInput = InputSpaceFixer.fix(userInput);
        checkInput(spaceFixedInput);
        InputTree inputTree = new InputTree();
        inputTree = inputTree.createTree(spaceFixedInput);
        InputType inputType = typeDeterminer.determineType(inputTree);

        if (inputType == InputType.NEW_VARIABLE || inputType == InputType.EXISTING_VARIABLE) {
            InputTree rightSide = inputTree.getRightSide();
            InputTree leftSide = inputTree.getLeftSide();
            String left = leftSide.toString();
            if(left.contains("/") || left.contains("^") || left.contains("(")) {
                throw new RuntimeException(INVALID_INPUT_MESSAGE);
            }
            if(!leftSide.isLeaf() && !leftSide.getValue().matches("&&|\\|\\|")) { //the variable is part of comparison expression
                throw new RuntimeException(IMPOSSIBLE_INITIALIZATION_MESSAGE);
            }
            rightSide = variableSubstituter.substituteVariables(rightSide);
            if(inputType == InputType.NEW_VARIABLE) {
                leftSide = variableSubstituter.substituteVariables(leftSide);
            }
            SideType typeRight = getType(rightSide);
            SideType typeLeft = getType(leftSide);
            if(!typesMatch(leftSide.toString(), typeLeft, typeRight) && typeLeft != SideType.UNEVALUATED) {
                throw new RuntimeException(IMPOSSIBLE_INITIALIZATION_MESSAGE);
            }
            String rightSideValue = getSideValue(rightSide, typeRight);
            addUpdateVar(typeRight, rightSideValue, leftSide.toString(), inputType);
            response = (inputType == InputType.NEW_VARIABLE) ?  NEW_VARIABLE_MESSAGE : EXISTING_VARIABLE_MESSAGE;
        } else if (inputType == InputType.STATEMENT) {
            boolean verifiedInput = requestProcessor.verifyStatement(spaceFixedInput);
            response = String.format(STATEMENT_FORMAT, userInput, String.valueOf(verifiedInput).toUpperCase());
        }

        return response;
    }

    private boolean typesMatch(String leftSide, SideType typeLeft, SideType typeRight) {
        if(typeLeft == typeRight) {
            return true;
        }
        if(typeRight == SideType.ARRAY && typeLeft == SideType.NUMBER && containsUnevaluatedVariable(leftSide)) {
            return true;
        }
        if(typeRight == SideType.STRING && typeLeft == SideType.NUMBER && containsUnevaluatedVariable(leftSide)
                && numberIsPartOfMultiplication(leftSide)) {
            return true;
        }
        return false;
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

    private String verifyInput(InputTree inputTree) {
        if(!inputTree.isLeaf()) {
            String data = inputTree.getValue();
            InputTree left = inputTree.getLeftSide();
            InputTree right = inputTree.getRightSide();
            SideType typeLeft = getType(left);
            SideType typeRight = getType(right);
            String rightSide, leftSide;

            if(typeRight == SideType.UNEVALUATED) {
                throw new RuntimeException("Cannot be evaluated!");
            }
            if((typeLeft != typeRight) || (data.matches("<=|>=|<|>") && typeLeft == SideType.BOOLEAN)) {
                throw new RuntimeException(INVALID_OPERATION_MESSAGE);
            }

            rightSide = getSideValue(right, typeRight);
            leftSide = getSideValue(left, typeLeft);

            String result;
            if(typeRight == SideType.NUMBER) {
                result = String.valueOf(AbstractRpnVerifier.compare(Integer.parseInt(leftSide), Integer.parseInt(rightSide), data));
            } else if(typeRight == SideType.BOOLEAN) {
                result = BooleanRpnVerifier.executeBooleanOperation(leftSide, rightSide, data);
            } else {
                result = String.valueOf(AbstractRpnVerifier.compare(leftSide, rightSide, data));
            }
            return result.toUpperCase();
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
        String expression = inputTree.getValue();
        RpnStatementVerifier rpnStatementVerifier = new RpnStatementVerifier(variableManager);
        rpnStatementVerifier.checkType(expression);
        int numberOfOperations = rpnStatementVerifier.getNumberOfOperations();
        if(!inputTree.isLeaf()) {  // value of inputTree data is an operator
            if(inputTree.getValue().equals("&&") || inputTree.getValue().equals("||")) {
                rpnStatementVerifier.checkType(inputTree.getValue());
                if(numberOfOperations != 1 || !rpnStatementVerifier.isBooleanOperation()) {
                    throw new RuntimeException(INVALID_INPUT_MESSAGE);
                }
            }
            return SideType.BOOLEAN;
        }

        if(rpnStatementVerifier.isBooleanOperation() && numberOfOperations == 1) {
            return SideType.BOOLEAN;
        }
        if((rpnStatementVerifier.isStringOperation() && numberOfOperations == 1) ||
                (rpnStatementVerifier.isStringOperation() && rpnStatementVerifier.isIntegerOperation() &&
                        numberOfOperations == 2 && (expression.contains("*") || expression.contains("/")))) {
            return SideType.STRING;
        }
        if((rpnStatementVerifier.isArrayOperation() && rpnStatementVerifier.isIntegerOperation() && numberOfOperations == 2) ||
                (rpnStatementVerifier.isArrayOperation() && numberOfOperations == 1)) {
            return SideType.ARRAY;
        }
        if(rpnStatementVerifier.isIntegerOperation() && numberOfOperations == 1) {
            return SideType.NUMBER;
        }
        if(rpnStatementVerifier.containsUnevaluatedVariable() && numberOfOperations == 0) {
            return SideType.UNEVALUATED;
        }
        throw new RuntimeException(OPERATION_MIX_MESSAGE);
    }

    private void addUpdateVar(SideType typeRight, String rightSideValue, String leftSide, InputType inputType) {
        VariableType type;
        AbstractRpnVerifier rpn = Rpn.makeRpn(typeRight);
        String rightSide;
        String name = getVariable(leftSide);
        if(typeRight == SideType.ARRAY) {
            rightSide = NewVariable.replaceRightSide(rightSideValue, leftSide, name, typeRight);
            type = VariableType.ARRAY;
        } else if(typeRight == SideType.BOOLEAN) {
            rightSide = NewVariable.replaceRightSideBoolean(rightSideValue, leftSide, name);
            type = VariableType.BOOLEAN;
        } else if(typeRight == SideType.NUMBER) {
            rightSide = NewVariable.replaceRightSide(rightSideValue, leftSide, name, typeRight);
            type = VariableType.NUMBER;
        } else {
            rightSide = NewVariable.replaceRightSide(rightSideValue, leftSide, name, typeRight);
            type = VariableType.STRING;
        }
        String rpnRightSide = rpn.createRpn(rightSide);
        String value = rpn.calculateRpn(rpnRightSide);
        EvaluatedVariable newVar = new EvaluatedVariable(name, value, type);
        if(inputType == InputType.NEW_VARIABLE) {
            requestProcessor.addVariable(newVar);
        } else {
            requestProcessor.updateVariable(newVar);
        }
    }

    public static String getVariable(String leftSide) {
        SplitString splitString = new SplitString(leftSide);
        while(!splitString.isEmpty()) {
            String currentElement = splitString.getCurrentElement();
            if(Type.isWord(currentElement) && !Type.isNumber(currentElement) && !Type.isBoolean(currentElement)) {
                return currentElement;
            }
            splitString.nextPosition();
        }
        return "";
    }

    private void checkInput(String userInput) {
        if (startsWithOperator(userInput) || endsWithOperator(userInput)) {
            throw new RuntimeException(String.format(INVALID_OPERATOR_FORMAT, userInput));
        }
    }

    @Override
    public Set<EvaluatedVariable> variables() {
        return requestProcessor.variables();
    }
}
