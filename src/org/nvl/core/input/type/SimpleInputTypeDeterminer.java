package src.org.nvl.core.input.type;

import src.org.nvl.core.variable.manager.VariableManager;

public class SimpleInputTypeDeterminer implements InputTypeDeterminer {
    private VariableManager variableManager;

    public SimpleInputTypeDeterminer(VariableManager variableManager) {
        this.variableManager = variableManager;
    }

    @Override
    public InputType determineType(String input) {
        if (isDefinition(input)) {
            String variableName = input.substring(0, input.indexOf('=')).trim();
            return determineVariableDefinition(variableName);
        } else {
            return InputType.STATEMENT;
        }
    }

    private InputType determineVariableDefinition(String variableName) {
        if (variableManager.containsVariable(variableName)) {
            return InputType.EXISTING_VARIABLE;
        } else {
            return InputType.NEW_VARIABLE;
        }
    }

    private boolean isDefinition(String input) {
        char[] charInput = input.toCharArray();
        int i = 0;
        while (charInput[i] != '=' && charInput[i] != '>' && charInput[i] != '<' && (charInput[i] != '!' || charInput[i + 1] != '='))  //input must contain at least =, >, <
        {
            i++;
        }
        return !(charInput[i] == '>' || charInput[i] == '<' || charInput[i + 1] == '=');
    }
}
