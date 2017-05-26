package src.org.nvl.core.input.tree;

import src.org.nvl.core.input.invalid_operator_usage.InvalidOperatorUsage;
import src.org.nvl.core.input.split.SplitString;
import src.org.nvl.core.variable.EvaluatedVariable;
import src.org.nvl.core.variable.Type;
import src.org.nvl.core.variable.VariableType;
import src.org.nvl.core.variable.manager.MapVariableManager;
import src.org.nvl.core.variable.manager.VariableManager;

import java.util.*;

import static src.org.nvl.MessageConstants.*;

/**
 * Created by Vicky on 13.5.2017 г..
 */

public class InputTree {
    private InputTree leftSide = null;
    private InputTree rightSide = null;
    private String data;
    private static VariableManager bracketVariables = new MapVariableManager(new HashMap<>());

    public InputTree() {
        this.data = "";
    }

    public InputTree getRightSide() {
        InputTree rightSideCopy;
        rightSideCopy = copyTree(rightSide);
        return rightSideCopy;
    }

    public void setRightSide(InputTree rightSide) {
        this.rightSide = copyTree(rightSide);
    }

    public InputTree getLeftSide() {
        InputTree leftSideCopy;
        leftSideCopy = copyTree(leftSide);
        return leftSideCopy;
    }

    public void setLeftSide(InputTree leftSide) {
        this.leftSide = copyTree(leftSide);
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

    private InputTree copyTree(InputTree inputTree) {
        InputTree copyOfTree = null;
        if(inputTree != null) {
            copyOfTree = new InputTree();
            copyOfTree.setValue(inputTree.getValue());
            copyOfTree.leftSide = copyTree(inputTree.leftSide);
            copyOfTree.rightSide = copyTree(inputTree.rightSide);
        }
        return copyOfTree;
    }

    public InputTree createTree(String input) {
        InputTree inputTree;
        input = replaceBracketExpressions(input);
        inputTree = splitInput(input);
        if(inputTree.isLeaf() && !Type.isBoolean(inputTree.toString())) {
            throw new RuntimeException(UNEVALUATABLE_EXPRESSION_MESSAGE);
        }
        return inputTree;
    }

    private InputTree splitInput(String spaceFixedInput) {
        InputTree inputTree = new InputTree();
        String[] operatorsByPriority = {"=", "||", "&&", "!=", "==", "<=", "<", ">=", ">"};
        int count = 9;
        int opNum = 0;
        while(opNum < count) {
            String operator = " " + operatorsByPriority[opNum] + " ";
            if(spaceFixedInput.contains(operator)) {
                if(operator.equals(" || ")) {
                    operator = " \\|\\| ";
                }
                String[] parts = spaceFixedInput.split(operator, 2);
                String left = parts[0];
                String right = parts[1];
                if(InvalidOperatorUsage.startsWithOperator(left) || InvalidOperatorUsage.endsWithOperator(left) ||
                        InvalidOperatorUsage.startsWithOperator(right) || InvalidOperatorUsage.endsWithOperator(right)) {
                    throw new RuntimeException(String.format(INVALID_OPERATOR_FORMAT, operatorsByPriority[opNum]));
                }
                inputTree.setValue(operatorsByPriority[opNum]);
                inputTree.setLeftSide(splitInput(left));
                inputTree.setRightSide(splitInput(right));
                break;
            } else {
                opNum++;
            }
        }
        if (inputTree.isLeaf()) {
            if(containBracketExpression(spaceFixedInput)) {
                spaceFixedInput = replaceBracketVariables(spaceFixedInput);
                spaceFixedInput = removeOuterBrackets(spaceFixedInput);
                inputTree = splitInput(spaceFixedInput);
            }
            else {
                inputTree.setValue(spaceFixedInput);
            }
        }
        return inputTree;
    }

    private String removeOuterBrackets(String spaceFixedInput) {
        if(spaceFixedInput.charAt(0) == '(' && spaceFixedInput.charAt(spaceFixedInput.length() - 1) == ')') {
            spaceFixedInput = spaceFixedInput.substring(2,spaceFixedInput.length() - 2);
        }
        return spaceFixedInput;
    }

    private String replaceBracketVariables(String spaceFixedInput) {
        StringBuilder result = new StringBuilder();
        SplitString splitString = new SplitString(spaceFixedInput);
        while(!splitString.isEmpty()) {
            String var = splitString.getCurrentElement();
            if(bracketVariables.containsVariable(var)) {
                String bracketExpression = bracketVariables.getVariable(var).getValue();
                result.append(bracketExpression).append(' ');
            } else {
                result.append(var).append(' ');
            }
            splitString.nextPosition();
        }
        return result.toString().substring(0, result.length() - 1);
    }

    private boolean containBracketExpression(String spaceFixedInput) {
        SplitString splitString = new SplitString(spaceFixedInput);
        while(!splitString.isEmpty()) {
            String a = splitString.getCurrentElement();
            if(bracketVariables.containsVariable(a)) {
                return true;
            }
            splitString.nextPosition();
        }
        return false;
    }

    // (a == b) != (c == d)  ->  y != z
    private String replaceBracketExpressions(String input) {
        char[] charInput = input.toCharArray();
        StringBuilder newVariable = new StringBuilder();
        Stack<String> stackInput = new Stack<>();
        boolean isPartOfBracketsExpression = false;
        for(int i = 0; i < charInput.length; i++)
        {
            if(charInput[i] == '(')
            {
                isPartOfBracketsExpression = true;
                stackInput.push(charInput[i]+"");
            }
            else if(charInput[i] == ')')
            {
                if(stackInput.isEmpty()) {
                    throw new RuntimeException(INVALID_INPUT_MESSAGE);
                }
                newVariable.append(")");
                String next = stackInput.pop();
                while(!next.equals("("))
                {
                    newVariable.append(next);
                    if(stackInput.isEmpty()) { // no '(' in stack
                        throw new RuntimeException(INVALID_INPUT_MESSAGE);
                    }
                    next = stackInput.pop();
                }
                newVariable.append("(");
                String bracketVarValue = newVariable.reverse().toString();
                String name;
                Set<String> variablesInInput = getVariablesInInput(input);
                name = bracketVariables.freeNameOfVariable(variablesInInput);
                EvaluatedVariable bracketVariable = new EvaluatedVariable(name, bracketVarValue, VariableType.BRACKET_EXPRESSION);
                bracketVariables.addVariable(bracketVariable);
                if(stackInput.isEmpty()) { //no more bracket expressions
                    isPartOfBracketsExpression = false;
                }
                bracketVarValue = replaceForRegex(bracketVarValue);
                input = input.replaceAll(bracketVarValue, name);
                stackInput.push(name);
                newVariable.setLength(0);
            }
            else if(isPartOfBracketsExpression)
            {
                 stackInput.push(charInput[i]+"");
            }
        }
        return input;
    }

    private String replaceForRegex(String bracketVarValue) {
        bracketVarValue = bracketVarValue.replace("(", "\\(");
        bracketVarValue = bracketVarValue.replace(")", "\\)");
        bracketVarValue = bracketVarValue.replace("+", "\\+");
        bracketVarValue = bracketVarValue.replace("*", "\\*");
        bracketVarValue = bracketVarValue.replace("|", "\\|");
        bracketVarValue = bracketVarValue.replace("{", "\\{");
        bracketVarValue = bracketVarValue.replace("}", "\\}");
        return bracketVarValue;
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
        boolean isBooleanReservedWord = Type.isBoolean(input);
        boolean isWord = Character.isLetter(input.charAt(0));
        return isWord && !isBooleanReservedWord;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(this.data.equals("")) {
            return "";
        }
        if(this.isLeaf()) {
            return this.getValue();
        }
        sb.append(this.getLeftSide().toString());
        sb.append(" ");
        sb.append(this.getValue());
        sb.append(" ");
        sb.append(this.getRightSide().toString());
        return String.valueOf(sb);
    }
}
