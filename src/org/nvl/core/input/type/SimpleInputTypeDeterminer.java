package src.org.nvl.core.input.type;

import src.org.nvl.MessageConstants;
import src.org.nvl.core.input.split.SplitString;
import src.org.nvl.core.input.tree.InputTree;
import src.org.nvl.core.variable.Type;
import src.org.nvl.core.variable.manager.VariableManager;

import java.util.HashSet;
import java.util.Set;

public class SimpleInputTypeDeterminer implements InputTypeDeterminer {
    private VariableManager variableManager;

    public SimpleInputTypeDeterminer(VariableManager variableManager) {
        this.variableManager = variableManager;
    }

    @Override
    public InputType determineType(InputTree input) {
        if (isDefinition(input)) {
            InputType[] variablesLeft = checkForVariable(input.getLeftSide().toString());
            InputType[] variablesRight = checkForVariable(input.getRightSide().toString());
            int numberOfUnevaluatedVariablesLeft = 0;
            int numberOfEvaluatedVariablesLeft = 0;
            for (int i = 0; i < variablesLeft.length && variablesLeft[i] != null; i++) {
                if (variablesLeft[i] == InputType.NEW_VARIABLE) {
                    numberOfUnevaluatedVariablesLeft++;
                } else {
                    numberOfEvaluatedVariablesLeft++;
                }
            }
            if (numberOfUnevaluatedVariablesLeft > 1) {
                throw new RuntimeException(MessageConstants.IMPOSSIBLE_MULTIPLE_DEFINITION_MESSAGE);
            }
            if(numberOfUnevaluatedVariablesLeft == 0 && numberOfEvaluatedVariablesLeft > 1) {
                throw new RuntimeException(MessageConstants.IMPOSSIBLE_MULTIPLE_REDEFINITION_MESSAGE);
            }
            if(numberOfUnevaluatedVariablesLeft == 0 && numberOfEvaluatedVariablesLeft == 0) {
                throw new RuntimeException(MessageConstants.INVALID_INPUT_MESSAGE);
            }
            for (int i = 0; i < variablesRight.length && variablesRight[i] != null; i++) {
                if (variablesRight[i] == InputType.NEW_VARIABLE) {
                    throw new RuntimeException(MessageConstants.UNEVALUATED_RIGHT_SIDE_MESSAGE);
                }
            }
            if(numberOfUnevaluatedVariablesLeft == 1) {
                return InputType.NEW_VARIABLE;
            }
            return InputType.EXISTING_VARIABLE;
        } else {
            return InputType.STATEMENT;
        }
    }

    private InputType[] checkForVariable(String input) {
        Set<String> unevaluatedVariables = new HashSet<>(), evaluatedVariables = new HashSet<>();
        SplitString splitString = new SplitString(input);
        InputType[] inputTypes = new InputType[splitString.getSplitInput().length];
        InputType inputType;

        int i = 0;
        while (!splitString.isEmpty()) {
            String element = splitString.getCurrentElement();
            if(Type.isWord(element) && !Type.isNumber(element) && !Type.isBoolean(element)) {
                inputType = determineVariableDefinition(element);
                if(inputType == InputType.NEW_VARIABLE && !unevaluatedVariables.contains(element)) {
                    inputTypes[i] = inputType;
                    unevaluatedVariables.add(element);
                    i++;
                } else if(inputType == InputType.EXISTING_VARIABLE && !evaluatedVariables.contains(element)) {
                    inputTypes[i] = inputType;
                    evaluatedVariables.add(element);
                    i++;
                }
            }
            splitString.nextPosition();
        }
        return inputTypes;
    }

    private InputType determineVariableDefinition(String variableName) {
        if (variableManager.containsVariable(variableName)) {
            return InputType.EXISTING_VARIABLE;
        } else {
            return InputType.NEW_VARIABLE;
        }
    }

    private boolean isDefinition(InputTree inputTree) {
       return inputTree.getValue().equals("=");
    }
}
