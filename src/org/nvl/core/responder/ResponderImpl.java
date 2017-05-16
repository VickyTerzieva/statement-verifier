package src.org.nvl.core.responder;

import src.org.nvl.core.input.substituter.VariableSubstituter;
import src.org.nvl.core.input.tree.InputTree;
import src.org.nvl.core.input.type.InputType;
import src.org.nvl.core.input.type.InputTypeDeterminer;
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

        //TODO remove - only testing
        InputTree inputTree = new InputTree();
        inputTree.createTree(userInput);
        //String replacedInput = InputTree.replaceBracketExpressions(userInput);
        //TODO remove

        String[] dividedInput = checkInput(userInput);
        InputType inputType = typeDeterminer.determineType(substitutedInput.toString());

        if (inputType == InputType.NEW_VARIABLE) {
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
        }

        return response;
    }

    private String[] checkInput(String userInput) {
        String[] dividedInput;

        try {
            String spaceFixedInput = InputSpaceFixer.fix(userInput);
            dividedInput = divider.splitInputByMainDivider(spaceFixedInput);
            substitutedInput = variableSubstituter.substitute(dividedInput);
            validateInput(substitutedInput);
        } catch (Exception e) {
            if (e.getMessage().contains(INVALID_INPUT_FORMAT.substring(0, 10))) {
                throw e;
            } else {
                throw new RuntimeException(String.format(INVALID_INPUT_FORMAT, userInput, "Try again"));
            }
        }

        return dividedInput;
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

    private void validateInput(String[] input) {
        boolean validSides = true;
        boolean validTypes;

        //multiple sides possible
        if (!input[OPERATION].equals("=")) {
            for(int side = 0; side < input.length; side++) {
                validSides = inputValidator.isValid(input[side]);
                if(!validSides) {
                    break;
                }
                validTypes = new InputTypeMatcher(variableManager).sidesTypeMatches(input[LEFT_SIDE], input[side]);
                if (!validTypes) {
                    throw new RuntimeException(String.format(INVALID_INPUT_FORMAT, input, "Incompatible value types"));
                }
            }
        }
        //only two sides allowed if operation equals "="
        else {
            boolean isExisting = variableManager.containsVariable(input[LEFT_SIDE]);
            EvaluatedVariable variable = variableManager.getVariable(input[LEFT_SIDE]);
            if (isExisting && !sameTypes(variable.getType(), input[RIGHT_SIDE])) {
                throw new RuntimeException(String.format(INVALID_INPUT_FORMAT, input, "Variable type change is not permitted"));
            } else {
                if (!input[LEFT_SIDE].matches("\\w+")) {
                    throw new RuntimeException(String.format(INVALID_INPUT_FORMAT, input, "Only letters, digits and underscores are permitted in variable names"));
                }
            }
        }

        if (!validSides) {
            throw new RuntimeException(String.format(INVALID_INPUT_FORMAT, input, "Try again"));
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
