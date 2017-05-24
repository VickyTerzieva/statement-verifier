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
import src.org.nvl.core.rpn.verifier.ArrayRpnVerifier;
import src.org.nvl.core.rpn.verifier.BooleanRpnVerifier;
import src.org.nvl.core.rpn.verifier.NumberRpnVerifier;
import src.org.nvl.core.rpn.verifier.StringRpnVerifier;
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
        this.variableManager = variableManager;
        this.variableSubstituter = new VariableSubstituter(variableManager);
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
            if(left.contains("/")) {
                throw new RuntimeException("Invalid input!");
            }
            if(!leftSide.isLeaf()) { //the variable is part of comparison expression
                throw new RuntimeException("Impossible to initialize the variable!");
            }
            rightSide = variableSubstituter.substituteVariables(rightSide);
            if(inputType == InputType.NEW_VARIABLE) {
                leftSide = variableSubstituter.substituteVariables(leftSide);
            }
            SideType typeRight = getType(rightSide);
            SideType typeLeft = getType(leftSide);
            if(typeRight != typeLeft && typeLeft != SideType.UNEVALUATED) {
                throw new RuntimeException("Impossible to initialize the variable!");
            }
            String rightSideValue = getSideValue(rightSide, typeRight);
            addUpdateVar(typeRight, rightSideValue, leftSide.toString(), inputType);
            response = (inputType == InputType.NEW_VARIABLE) ?  NEW_VARIABLE_MESSAGE : EXISTING_VARIABLE_MESSAGE;
        } else if (inputType == InputType.STATEMENT) {
            inputTree = variableSubstituter.substituteVariables(inputTree);
            String verifiedInput = verifyInput(inputTree);
            if(verifiedInput.matches("(.*)Cannot be evaluated!") || verifiedInput.matches("(.*)Incompatible value types!")) {
                response = verifiedInput;
            } else {
                response = String.format(STATEMENT_FORMAT, inputTree.toString(), verifiedInput);
            }
        }

        return response;
    }

    private void addUpdateVar(SideType typeRight, String rightSideValue, String leftSide, InputType inputType) {
        VariableType type;
        AbstractRpnVerifier rpn = Rpn.makeRpn(typeRight);
        String rightSide;
        String name = getVariable(leftSide);
        if(typeRight == SideType.ARRAY) {
            rightSide = NewVariable.replaceRightSideArray(rightSideValue, leftSide, name);
            type = VariableType.ARRAY;
        } else if(typeRight == SideType.BOOLEAN) {
            rightSide = NewVariable.replaceRightSideBoolean(rightSideValue, leftSide, name);
            type = VariableType.BOOLEAN;
        } else if(typeRight == SideType.NUMBER) {
            rightSide = NewVariable.replaceRightSideNumber(rightSideValue, leftSide, name);
            type = VariableType.NUMBER;
        } else {
            rightSide = NewVariable.replaceRightSideString(rightSideValue, leftSide, name);
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

    private String verifyInput(InputTree inputTree) {
        if(!inputTree.isLeaf()) {
            String data = inputTree.getValue();
            InputTree left = inputTree.getLeftSide();
            InputTree right = inputTree.getRightSide();
            SideType typeLeft = getType(left);
            SideType typeRight = getType(right);
            String rightSide, leftSide;

            if(typeLeft == SideType.UNEVALUATED || typeRight == SideType.UNEVALUATED) {
                return String.format(INVALID_INPUT_FORMAT, inputTree, "Cannot be evaluated!");
            }
            if((typeLeft != typeRight) || (data.matches("<=|>=|<|>") && typeLeft == SideType.BOOLEAN)) {
                return String.format(INVALID_INPUT_FORMAT, inputTree, "Incompatible value types");
            }

            rightSide = getSideValue(right, typeRight);
            leftSide = getSideValue(left, typeLeft);

            if(rightSide.matches("(.*)Cannot be evaluated!") || rightSide.matches("(.*)Incompatible value types!")) {
                return  rightSide;
            }
            if(leftSide.matches("(.*)Cannot be evaluated!") || leftSide.matches("(.*)Incompatible value types!")) {
                return  leftSide;
            }
            if(compare(leftSide, rightSide, data)) {
                return "TRUE";
            }
            return "FALSE";
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
        if(rpnStatementVerifier.isStringOperation() && numberOfOperations == 1) {
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

    private void checkInput(String userInput) {
        if (startsWithOperator(userInput) || endsWithOperator(userInput)) {
            throw new RuntimeException(String.format(INVALID_OPERATOR_FORMAT, userInput));
        }
    }

    protected <E extends Comparable<E>> boolean compare(E left, E right, String operation) {
        switch (operation) {
            case "==":
                return left.compareTo(right) == 0;
            case ">":
                return left.compareTo(right) > 0;
            case "<":
                return left.compareTo(right) < 0;
            case ">=":
                return left.compareTo(right) >= 0;
            case "<=":
                return left.compareTo(right) <= 0;
            case "!=":
                return left.compareTo(right) != 0;
            default:
                return false;
        }
    }

    @Override
    public Set<EvaluatedVariable> variables() {
        return requestProcessor.variables();
    }
}
