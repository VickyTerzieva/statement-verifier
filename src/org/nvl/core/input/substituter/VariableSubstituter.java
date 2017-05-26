package src.org.nvl.core.input.substituter;

import src.org.nvl.core.input.split.SplitString;
import src.org.nvl.core.input.tree.InputTree;
import src.org.nvl.core.variable.VariableType;
import src.org.nvl.core.variable.manager.VariableManager;

public class VariableSubstituter {
    private VariableManager variableManager;

    public VariableSubstituter(VariableManager variableManager) {
        this.variableManager = variableManager;
    }

    public InputTree substituteVariables(InputTree input) {
        if(input.isLeaf()) {
            StringBuilder result = new StringBuilder();
            SplitString splitString = new SplitString(input.toString());

            while (!splitString.isEmpty()) {
                String element = splitString.getCurrentElement();

                if (variableManager.containsVariable(element)) {
                    String value = variableManager.getVariable(element).getValue();
                    result.append(value).append(' ');
                } else {
                    result.append(element).append(' ');
                }

                splitString.nextPosition();
            }
            input.setValue(result.toString().substring(0, result.length() - 1));
        } else {
            input.setRightSide(substituteVariables(input.getRightSide()));
            input.setLeftSide(substituteVariables(input.getLeftSide()));
        }
        return input;
    }
}
