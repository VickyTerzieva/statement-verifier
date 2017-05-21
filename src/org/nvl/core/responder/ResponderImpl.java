package src.org.nvl.core.responder;

import src.org.nvl.core.input.split.SplitString;
import src.org.nvl.core.input.substituter.VariableSubstituter;
import src.org.nvl.core.input.tree.InputTree;
import src.org.nvl.core.input.type.InputType;
import src.org.nvl.core.input.type.InputTypeDeterminer;
import src.org.nvl.core.input.type.SideType;
import src.org.nvl.core.input.validator.InputValidator;
import src.org.nvl.core.input.white_space.InputSpaceFixer;
import src.org.nvl.core.responder.processor.RequestProcessor;
import src.org.nvl.core.rpn.verifier.ArrayRpnVerifier;
import src.org.nvl.core.rpn.verifier.BooleanRpnVerifier;
import src.org.nvl.core.rpn.verifier.NumberRpnVerifier;
import src.org.nvl.core.rpn.verifier.StringRpnVerifier;
import src.org.nvl.core.statement.RpnStatementVerifier;
import src.org.nvl.core.statement.type.InputTypeMatcher;
import src.org.nvl.core.variable.EvaluatedVariable;
import src.org.nvl.core.variable.VariableType;
import src.org.nvl.core.variable.manager.VariableManager;

import java.util.Set;

import static src.org.nvl.MessageConstants.*;
import static src.org.nvl.core.input.invalid_operator_usage.InvalidOperatorUsage.endsWithOperator;
import static src.org.nvl.core.input.invalid_operator_usage.InvalidOperatorUsage.startsWithOperator;

public class ResponderImpl implements Responder {
    private InputTypeDeterminer typeDeterminer;
    private RequestProcessor requestProcessor;
    private InputValidator inputValidator;
    private VariableSubstituter variableSubstituter;
    private VariableManager variableManager;
    private Divider divider;
    private String[] substitutedInput;
    private static final int OPERATION = 0;
    private static final int LEFT_SIDE = 1;
    private static final int RIGHT_SIDE = 2;

    public ResponderImpl(InputTypeDeterminer typeDeterminer, RequestProcessor requestProcessor, InputValidator inputValidator, VariableManager variableManager) {
        this.typeDeterminer = typeDeterminer;
        this.requestProcessor = requestProcessor;
        this.inputValidator = inputValidator;
        this.variableManager = variableManager;
        this.variableSubstituter = new VariableSubstituter(variableManager);
        this.divider = new Divider();
    }

    @Override
    public String process(String userInput) {
        String response = "";

        userInput = InputSpaceFixer.fix(userInput);
        checkInput(userInput);
        InputTree inputTree = new InputTree();
        inputTree = inputTree.createTree(userInput);
        verifyInput(inputTree);


        //InputType inputType = typeDeterminer.determineType(substitutedInput.toString());

        /*if (inputType == InputType.NEW_VARIABLE) {
            computeRightSide(substitutedInput);
            requestProcessor.addVariable(substitutedInput.toString());
            response = NEW_VARIABLE_MESSAGE;
        } else if (inputType == InputType.EXISTING_VARIABLE) {
            computeRightSide(substitutedInput);
            requestProcessor.updateVariable(substitutedInput.toString());
            response = EXISTING_VARIABLE_MESSAGE;
        } else if (inputType == InputType.STATEMENT) {
            boolean validStatement = requestProcessor.verifyStatement(substitutedInput.toString());
            response = String.format(STATEMENT_FORMAT, dividedInput, Boolean.toString(validStatement).toUpperCase());
        }*/

        return response;
    }

    //TODO
    private boolean verifyInput(InputTree inputTree) {
        if(!inputTree.isLeaf()) {
            String data = inputTree.getValue();
            InputTree left = inputTree.getLeftSide();
            InputTree right = inputTree.getRightSide();
            SideType typeLeft = getType(left);
            SideType typeRight = getType(right);
            if(typeLeft != typeRight) {
                throw new RuntimeException(String.format(INVALID_INPUT_FORMAT, inputTree, "Incompatible value types"));
            }
            if(!verifyInput(inputTree.getLeftSide())) {
                return false;
            }
            if(!verifyInput(inputTree.getRightSide())) {
                return false;
            }
        }
        //TODO check if sides have equal values
        return true;
    }

    private SideType getType(InputTree inputTree) {
        if(!inputTree.isLeaf()) {  // value of inputTree data is an operator
            return SideType.BOOLEAN;
        }
        String expression = inputTree.getValue();
        RpnStatementVerifier rpnStatementVerifier = new RpnStatementVerifier(variableManager);
        rpnStatementVerifier.checkType(expression);
        if(rpnStatementVerifier.isBooleanOperation()) {
            return SideType.BOOLEAN;
        }
        if(rpnStatementVerifier.isStringOperation()) {
            return SideType.STRING;
        }
        if(rpnStatementVerifier.isArrayOperation()) {
            return SideType.INTEGER;
        }
            //case isArray(expression) : return SideType.ARRAY;
        throw new RuntimeException("You found a bug. Invalid input.");
    }

    private void checkInput(String userInput) {
        if (startsWithOperator(userInput) || endsWithOperator(userInput)) {
            throw new RuntimeException(String.format(INVALID_OPERATOR_FORMAT, userInput));
        }
    }

    private void computeRightSide(String[] dividedInput) {
        for(int i = 0; i < dividedInput.length; i++) {
            if (sameTypes(VariableType.ARRAY, dividedInput[i])) {
                ArrayRpnVerifier rpnVerifier = new ArrayRpnVerifier();
                String rpn = rpnVerifier.createRPN(dividedInput[i]);
                dividedInput[i] = rpnVerifier.calculateRpn(rpn);
            } else if (sameTypes(VariableType.STRING, dividedInput[i])) {
                StringRpnVerifier rpnVerifier = new StringRpnVerifier();
                String rpn = rpnVerifier.createRPN(dividedInput[i]);
                dividedInput[i] = rpnVerifier.calculateRpnForString(rpn);
            } else if (sameTypes(VariableType.NUMBER, dividedInput[i])) {
                NumberRpnVerifier rpnVerifier = new NumberRpnVerifier();
                String rpn = rpnVerifier.createRPN(dividedInput[i]);
                dividedInput[i] = String.valueOf(rpnVerifier.calculateRPN(rpn).intValue());
            } else if (sameTypes(VariableType.BOOLEAN, dividedInput[i])) {
                BooleanRpnVerifier rpnVerifier = new BooleanRpnVerifier();
                String rpn = rpnVerifier.createRPN(dividedInput[i]);
                dividedInput[i] = rpnVerifier.calculateRPN(rpn).toString();
            }
        }
    }

    private boolean sameTypes(VariableType type, String rightSide) {
        RpnStatementVerifier rpnStatementVerifier = new RpnStatementVerifier(variableManager);
        rpnStatementVerifier.checkType(rightSide);

        boolean bothAreArrays = type == VariableType.ARRAY && rpnStatementVerifier.isArrayOperation();
        boolean bothAreStrings = type == VariableType.STRING && rpnStatementVerifier.isStringOperation();
        boolean bothAreBooleans = type == VariableType.BOOLEAN && rpnStatementVerifier.isBooleanOperation();
        boolean bothAreNumbers =
                type == VariableType.NUMBER && !rpnStatementVerifier.isBooleanOperation() && !rpnStatementVerifier.isStringOperation() && !rpnStatementVerifier.isArrayOperation();
        return bothAreArrays || bothAreStrings || bothAreBooleans || bothAreNumbers;
    }

    @Override
    public Set<EvaluatedVariable> variables() {
        return requestProcessor.variables();
    }
}
