package src.org.nvl.core.responder.processor;

import src.org.nvl.core.statement.StatementVerifier;
import src.org.nvl.core.variable.EvaluatedVariable;
import src.org.nvl.core.variable.manager.VariableManager;

import java.util.Set;

public class RequestProcessorImpl implements RequestProcessor {
    private StatementVerifier statementVerifier;
    private VariableManager variableManager;

    public RequestProcessorImpl(StatementVerifier statementVerifier, VariableManager variableManager) {
        this.statementVerifier = statementVerifier;
        this.variableManager = variableManager;
    }

    public void addVariable(EvaluatedVariable evaluatedVariable) {
        variableManager.addVariable(evaluatedVariable);
    }

    public void updateVariable(EvaluatedVariable evaluatedVariable) {

        variableManager.updateVariable(evaluatedVariable);
    }

    public boolean verifyStatement(String statement) {
        return statementVerifier.verifyStatement(statement);
    }

    @Override
    public Set<EvaluatedVariable> variables() {
        return variableManager.variables();
    }
}
