package src.org.nvl.core.input.substituter;

import src.org.nvl.core.input.split.SplitString;
import src.org.nvl.core.variable.VariableType;
import src.org.nvl.core.variable.manager.VariableManager;

public class VariableSubstituter {
    private VariableManager variableManager;

    public VariableSubstituter(VariableManager variableManager) {
        this.variableManager = variableManager;
    }
    public String[] substitute(String[] input) {
        String[] result = new String[input.length];

        for(int i = 0; i < input.length; i++) {
            result[i] = substituteVariables(input[i]);
        }
        return result;
    }

    private String substituteVariables(String input) {
        SplitString splitString = new SplitString(input);

        while (!splitString.isEmpty()) {
            String element = splitString.getCurrentElement();

            if (variableManager.containsVariable(element)) {
                String value = variableManager.getVariable(element).getValue();
                splitString.setCurrentElement(value);
            }

            splitString.nextPosition();
        }

        return concatenate(splitString.getSplitInput());
    }

    private String concatenate(String[] splitInput) {
        StringBuilder result = new StringBuilder();
        result.append(splitInput[0]);

        for (int i = 1; i < splitInput.length; ++i) {
            result.append(" ").append(splitInput[i]);
        }

        return result.toString();
    }

    private boolean isVariableOfType(String currentElement, VariableType neededType) {
        boolean isVariable = variableManager.containsVariable(currentElement);
        return isVariable && variableManager.getVariable(currentElement).getType() == neededType;
    }
}
