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

        userInput = InputSpaceFixer.fix(userInput);
        checkInput(userInput);
        InputTree inputTree = new InputTree();
        inputTree = inputTree.createTree(userInput);
        InputType inputType = typeDeterminer.determineType(inputTree);

        if (inputType == InputType.NEW_VARIABLE || inputType == InputType.EXISTING_VARIABLE) {
            InputTree rightSide = inputTree.getRightSide();
            InputTree leftSide = inputTree.getLeftSide();
            String left = leftSide.toString();
            if(left.contains("/") || left.contains("^") || left.contains("(")) {
                throw new RuntimeException(INVALID_INPUT_MESSAGE);
            }
            if(!leftSide.isLeaf() && !leftSide.getValue().matches("&&|\\|\\|")) { //the variable is part of comparison expression
                throw new RuntimeException("Impossible to initialize the variable!");
            }
            rightSide = variableSubstituter.substituteVariables(rightSide);
            if(inputType == InputType.NEW_VARIABLE) {
                leftSide = variableSubstituter.substituteVariables(leftSide);
            }
            SideType typeRight = getType(rightSide);
            SideType typeLeft = getType(leftSide);
            if(!typesMatch(leftSide.toString(), typeLeft, typeRight) && typeLeft != SideType.UNEVALUATED) {
                throw new RuntimeException("Impossible to initialize the variable!");
            }
            String rightSideValue = getSideValue(rightSide, typeRight);
            addUpdateVar(typeRight, rightSideValue, leftSide.toString(), inputType);
            response = (inputType == InputType.NEW_VARIABLE) ?  NEW_VARIABLE_MESSAGE : EXISTING_VARIABLE_MESSAGE;
        } else if (inputType == InputType.STATEMENT) {
            inputTree = variableSubstituter.substituteVariables(inputTree);
            boolean verifiedInput = requestProcessor.verifyStatement(userInput);
            response = String.format(STATEMENT_FORMAT, inputTree.toString(), verifiedInput);
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
            if(!currentElement.matches("[\\d]+") && currentElement.matches("[\\w]+") && !variableManager.containsVariable(currentElement)) {
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

            /*if(typeLeft == SideType.UNEVALUATED || typeRight == SideType.UNEVALUATED) {
                throw new RuntimeException("Cannot be evaluated!");
            }*/
            if((typeLeft != typeRight) || (data.matches("<=|>=|<|>") && typeLeft == SideType.BOOLEAN)) {
                throw new RuntimeException("Invalid operation!");
            }

            rightSide = getSideValue(right, typeRight);
            leftSide = getSideValue(left, typeLeft);

            if(rightSide.matches("(.*)Cannot be evaluated!") || rightSide.matches("(.*)Invalid operation!")) {
                return  rightSide;
            }
            if(leftSide.matches("(.*)Cannot be evaluated!") || leftSide.matches("(.*)Invalid operation!")) {
                return  leftSide;
            }
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
            return SideType.BOOLEAN;
        }
        String expression = inputTree.getValue();
        RpnStatementVerifier rpnStatementVerifier = new RpnStatementVerifier(variableManager);
        rpnStatementVerifier.checkType(expression);
        int numberOfOperations = rpnStatementVerifier.getNumberOfOperations();
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
        throw new RuntimeException("Invalid mix of operations!");
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

    private String getVariable(String leftSide) {
        String var = "";
        for(int i = 0; i < leftSide.length(); i++) {
            if((leftSide.charAt(i) >= 'a' && leftSide.charAt(i) <= 'z') ||
                    (leftSide.charAt(i) >= 'A' && leftSide.charAt(i) <= 'Z')) {
               int indexEnd = leftSide.indexOf(" ", i);
               indexEnd = (indexEnd == -1) ? leftSide.length() : indexEnd;
                var = leftSide.substring(i, indexEnd);
                break;
            }
        }
        return var;
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
