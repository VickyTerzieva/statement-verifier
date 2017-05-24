package src.org.nvl.core.statement;

import src.org.nvl.core.input.split.SplitString;
import src.org.nvl.core.rpn.AbstractRpnVerifier;
import src.org.nvl.core.rpn.verifier.ArrayRpnVerifier;
import src.org.nvl.core.rpn.verifier.BooleanRpnVerifier;
import src.org.nvl.core.rpn.verifier.NumberRpnVerifier;
import src.org.nvl.core.rpn.verifier.StringRpnVerifier;
import src.org.nvl.core.variable.VariableType;
import src.org.nvl.core.variable.manager.VariableManager;

public class RpnStatementVerifier implements StatementVerifier {

    private VariableManager variableManager;
    private boolean isBooleanOperation;
    private boolean isStringOperation;
    private boolean isIntegerOperation;
    private boolean isArrayOperation;
    private boolean containsUnevaluatedVariable = false;
    private StringBuilder valueStatement;
    private int numberOfOperations = 0;

    public boolean isBooleanOperation() {
        return isBooleanOperation;
    }

    public boolean isStringOperation() {
        return isStringOperation;
    }

    public boolean isIntegerOperation() { return isIntegerOperation; }

    public boolean isArrayOperation() {
        return isArrayOperation;
    }

    public int getNumberOfOperations() { return numberOfOperations; }

    public boolean containsUnevaluatedVariable() { return containsUnevaluatedVariable; }

    public RpnStatementVerifier(VariableManager variableManager) {
        this.variableManager = variableManager;
    }

    @Override
    public boolean verifyStatement(String statement) {
        checkType(statement);

        AbstractRpnVerifier verify;

        if (isStringOperation) {        //we have string operations
            verify = new StringRpnVerifier();       //we verify the statement
            return verify.correct(valueStatement);
        }
        if (isArrayOperation) {
            verify = new ArrayRpnVerifier();
            return verify.correct(valueStatement);
        }
        if (isBooleanOperation) {                     //we have boolean operations
            verify = new BooleanRpnVerifier();
            return verify.correct(valueStatement);
        }
        if(isIntegerOperation) {
            verify = new NumberRpnVerifier();           //we have number operations
            return verify.correct(valueStatement);
        }
        throw new RuntimeException("Invalid input!");
    }

    //must be called on substituted statement
    public void checkType(String statement) {
        valueStatement = new StringBuilder(statement);
        isBooleanOperation = false;
        isStringOperation = false;
        isArrayOperation = false;
        isIntegerOperation = false;

        SplitString splitString = new SplitString(statement);
        while(!splitString.isEmpty()) {
            String currentElement = splitString.getCurrentElement();

            if(currentElement.matches("'[\\w\\s]+'") || isVariableOfType(currentElement, VariableType.STRING)) {
                if(!isStringOperation) {
                    numberOfOperations++;
                }
                isStringOperation = true;
            } else if(currentElement.matches("\\{\\d+(,\\d+)*\\}") || isVariableOfType(currentElement, VariableType.ARRAY)) {
                if(!isArrayOperation) {
                    numberOfOperations++;
                }
                isArrayOperation = true;
            } else if (currentElement.equalsIgnoreCase("FALSE") || currentElement.equalsIgnoreCase("TRUE") ||
                    isVariableOfType(currentElement, VariableType.BOOLEAN)) {
                if(!isBooleanOperation) {
                    numberOfOperations++;
                }
                isBooleanOperation = true;
            } else if (currentElement.matches("\\d+") || isVariableOfType(currentElement, VariableType.NUMBER)) {
                if(!isIntegerOperation) {
                    numberOfOperations++;
                }
                isIntegerOperation = true;
            } else if(currentElement.matches("[\\w]+") && !variableManager.containsVariable(currentElement)) {
                containsUnevaluatedVariable = true;
            } else if (!currentElement.matches("\\+|\\*|\\-|/|&&|\\|\\||!=|=|<|>|>=|<=|\\(|\\)")) { //already checked for matching brackets, see InputTree
                throw new RuntimeException("Invalid input!");
            }

            splitString.nextPosition();
        }
    }

    //TODO if a and b unevaluated and exp = a + b => exp not bool
    private boolean isVariableOfType(String currentElement, VariableType neededType) {
        boolean isVariable = variableManager.containsVariable(currentElement);
        return isVariable && variableManager.getVariable(currentElement).getType() == neededType;
    }
}
