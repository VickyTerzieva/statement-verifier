package src.org.nvl.core.responder;

import src.org.nvl.core.input.substituter.VariableSubstituter;
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
import src.org.nvl.MessageConstants;

import java.util.Set;

public class ResponderImpl implements Responder {
    private InputTypeDeterminer typeDeterminer;
    private RequestProcessor requestProcessor;
    private InputValidator inputValidator;
    private InputSpaceFixer inputSpaceFixer;
    private VariableSubstituter variableSubstituter;
    private VariableManager variableManager;
    private Divider divider;
    private DividedInput substitutedInput;

    public ResponderImpl(InputTypeDeterminer typeDeterminer, RequestProcessor requestProcessor, InputValidator inputValidator, VariableManager variableManager) {
        this.typeDeterminer = typeDeterminer;
        this.requestProcessor = requestProcessor;
        this.inputValidator = inputValidator;
        this.variableManager = variableManager;
        this.inputSpaceFixer = new InputSpaceFixer();
        this.variableSubstituter = new VariableSubstituter(variableManager);
        this.divider = new Divider();
    }

    @Override
    public String process(String userInput) {
        String response = "";

        DividedInput dividedInput = checkInput(userInput);
        InputType inputType = typeDeterminer.determineType(substitutedInput.toString());

        if (inputType == InputType.NEW_VARIABLE) {
            computeRightSide(substitutedInput);
            requestProcessor.addVariable(substitutedInput.toString());
            response = MessageConstants.NEW_VARIABLE_MESSAGE;
        } else if (inputType == InputType.EXISTING_VARIABLE) {
            computeRightSide(substitutedInput);
            requestProcessor.updateVariable(substitutedInput.toString());
            response = MessageConstants.EXISTING_VARIABLE_MESSAGE;
        } else if (inputType == InputType.STATEMENT) {
            boolean validStatement = requestProcessor.verifyStatement(substitutedInput.toString());
            response = String.format(MessageConstants.STATEMENT_FORMAT, dividedInput, Boolean.toString(validStatement).toUpperCase());
        }

        return response;
    }

    private DividedInput checkInput(String userInput) {
        DividedInput dividedInput;

        try {
            String spaceFixedInput = inputSpaceFixer.fix(userInput);
            dividedInput = divider.divide(spaceFixedInput);
            substitutedInput = variableSubstituter.substitute(dividedInput);
            validateInput(substitutedInput);
        } catch (Exception e) {
            if (e.getMessage().contains(MessageConstants.INVALID_INPUT_FORMAT.substring(0, 10))) {
                throw e;
            } else {
                throw new RuntimeException(String.format(MessageConstants.INVALID_INPUT_FORMAT, userInput, "Try again"));
            }
        }

        return dividedInput;
    }

    private void computeRightSide(DividedInput dividedInput) {
        if (sameTypes(VariableType.ARRAY, dividedInput.getRightSide())) {
            ArrayRpnVerifier rpnVerifier = new ArrayRpnVerifier();
            String rpn = rpnVerifier.createRPN(dividedInput.getRightSide());
            dividedInput.setRightSide(rpnVerifier.calculateRpn(rpn));
        } else if (sameTypes(VariableType.STRING, dividedInput.getRightSide())) {
            StringRpnVerifier rpnVerifier = new StringRpnVerifier();
            String rpn = rpnVerifier.createRPN(dividedInput.getRightSide());
            dividedInput.setRightSide(rpnVerifier.calculateRpnForString(rpn));
        } else if (sameTypes(VariableType.NUMBER, dividedInput.getRightSide())) {
            NumberRpnVerifier rpnVerifier = new NumberRpnVerifier();
            String rpn = rpnVerifier.createRPN(dividedInput.getRightSide());
            dividedInput.setRightSide(String.valueOf(rpnVerifier.calculateRPN(rpn).intValue()));
        } else if (sameTypes(VariableType.BOOLEAN, dividedInput.getRightSide())) {
            BooleanRpnVerifier rpnVerifier = new BooleanRpnVerifier();
            String rpn = rpnVerifier.createRPN(dividedInput.getRightSide());
            dividedInput.setRightSide(rpnVerifier.calculateRPN(rpn).toString());
        }
    }

    private void validateInput(DividedInput input) {
        boolean validRightSide = inputValidator.isValid(input.getRightSide());
        boolean validLeftSide = true;

        if (!input.getOperation().equals("=")) {
            validLeftSide = inputValidator.isValid(input.getLeftSide());

            boolean validTypes = new InputTypeMatcher(variableManager).sidesTypeMatches(input.getLeftSide(), input.getRightSide());

            if (!validTypes) {
                throw new RuntimeException(String.format(MessageConstants.INVALID_INPUT_FORMAT, input, "Incompatible value types"));
            }
        } else {
            boolean isExisting = variableManager.containsVariable(input.getLeftSide());
            EvaluatedVariable variable = variableManager.getVariable(input.getLeftSide());
            if (isExisting && !sameTypes(variable.getType(), input.getRightSide())) {
                throw new RuntimeException(String.format(MessageConstants.INVALID_INPUT_FORMAT, input, "Variable type change is not permitted"));
            } else {
                if (!input.getLeftSide().matches("\\w+")) {
                    throw new RuntimeException(String.format(MessageConstants.INVALID_INPUT_FORMAT, input, "Only letters, digits and underscores are permitted in variable names"));
                }
            }
        }

        if (!validLeftSide || !validRightSide) {
            throw new RuntimeException(String.format(MessageConstants.INVALID_INPUT_FORMAT, input, "Try again"));
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
