package src.org.nvl.core.input.tree;

import src.org.nvl.core.input.split.SplitString;
import src.org.nvl.core.input.white_space.InputSpaceFixer;
import src.org.nvl.core.variable.EvaluatedVariable;
import src.org.nvl.core.variable.VariableType;
import src.org.nvl.core.variable.manager.MapVariableManager;
import src.org.nvl.core.variable.manager.VariableManager;

import java.util.*;

/**
 * Created by Vicky on 13.5.2017 г..
 */

public class InputTree {
    private InputTree leftSide = null;
    private InputTree rightSide = null;
    private String data;
    private static VariableManager bracketVariables;

    public InputTree() {
        this.data = "";
        bracketVariables = new MapVariableManager(new HashMap<>());
    }

    public InputTree getRightSide() {
        return rightSide;
    }

    public void setRightSide(InputTree rightSide) {
        this.rightSide = rightSide;
    }

    public InputTree getLeftSide() {
        return leftSide;
    }

    public void setLeftSide(InputTree leftSide) {
        this.leftSide = leftSide;
    }

    public String getValue() {
        return data;
    }

    public void setValue(String value) {
        this.data = value;
    }

    public boolean isLeaf() {
        return leftSide == null && rightSide == null;
    }

    public InputTree createTree(String input) {
        InputTree inputTree;
        String spaceFixedInput = InputSpaceFixer.fix(input);
        spaceFixedInput = replaceBracketExpressions(spaceFixedInput);
        inputTree = splitInput(spaceFixedInput);
        return inputTree;
    }

    private InputTree splitInput(String spaceFixedInput) {
        InputTree inputTree = new InputTree();
        String[] operatorsByPriority = {"=", "\\|\\|", "&&", "!=", "==", "<=", "<", ">=", ">"};
        int count = 9;
        int opNum = 0;
        while(opNum < count) {
            String operator = " " + operatorsByPriority[opNum] + " ";
            if(spaceFixedInput.contains(operator)) {
                String[] parts = spaceFixedInput.split(operator, 2);
                String left = parts[0];
                String right = parts[1];
                inputTree.setValue(operatorsByPriority[opNum]);
                inputTree.setLeftSide(splitInput(left));
                inputTree.setRightSide(splitInput(right));
                break;
            } else {
                opNum++;
            }
        }
        if (inputTree.isLeaf()) {
            inputTree.setValue(spaceFixedInput);
        }
        return inputTree;
    }

    // (a == b) != (c == d)  ->  y != z
    private String replaceBracketExpressions(String input) {
        char[] charInput = input.toCharArray();
        char[] newVariable = new char[input.length()];
        Stack<Character> stackInput = new Stack<>();
        boolean isPartOfBracketsExpression = false;
        for(int i = 0; i < charInput.length; i++)
        {
            if(charInput[i] == '(')
            {
                isPartOfBracketsExpression = true;
                stackInput.push(charInput[i]);
            }
            else if(charInput[i] == ')')
            {
                if(stackInput.isEmpty()) {
                    throw new RuntimeException("Invalid input! ");
                }
                int j = 1;
                newVariable[0] = ')';
                while((newVariable[j] = stackInput.pop()) != '(')
                {
                    if(stackInput.isEmpty()) { // no '(' in stack
                        throw new RuntimeException("Invalid input! ");
                    }
                    j++;
                }
                if(j + 1 < input.length()) {
                    newVariable[++j] = '\0';
                }
                String newVarString = new String(newVariable, 0, j);
                String bracketVarValue = new StringBuilder(newVarString).reverse().toString();
                String name;
                Set<String> variablesInInput = getVariablesInInput(input);
                name = bracketVariables.freeNameOfVariable(variablesInInput);
                EvaluatedVariable bracketVariable = new EvaluatedVariable(name, bracketVarValue, VariableType.BRACKET_EXPRESSION);
                bracketVariables.addVariable(bracketVariable);
                if(stackInput.isEmpty()) { //no more bracket expressions
                    isPartOfBracketsExpression = false;
                }
                bracketVarValue = bracketVarValue.replace("(", "\\(");
                bracketVarValue = bracketVarValue.replace(")", "\\)");
                input = input.replaceAll(bracketVarValue, name);
            }
            else if(isPartOfBracketsExpression)
            {
                stackInput.push(charInput[i]);
            }
        }
        return input;
    }

    private Set<String> getVariablesInInput(String input) {
        Set<String> result = new HashSet<>();
        SplitString splitString = new SplitString(input);
        String[] splitInput = splitString.getSplitInput();
        for(int i = 0; i < splitInput.length; i++)
        {
            if(isVariable(splitInput[i]))
            {
                result.add(splitInput[i]);
            }
        }
        return result;
    }

    private boolean isVariable(String input) {
        boolean isBooleanReservedWord = input.equalsIgnoreCase("FALSE") || input.equalsIgnoreCase("TRUE");
        boolean isWord = Character.isLetter(input.charAt(0));
        return isWord && !isBooleanReservedWord;
    }
}