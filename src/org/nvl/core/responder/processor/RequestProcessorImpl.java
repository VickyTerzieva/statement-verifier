package src.org.nvl.core.responder.processor;

import src.org.nvl.core.statement.StatementVerifier;
import src.org.nvl.core.variable.EvaluatedVariable;
import src.org.nvl.core.variable.UnevaluatedVariable;
import src.org.nvl.core.variable.definition.VariableDefinitionParser;
import src.org.nvl.core.variable.manager.VariableManager;
import src.org.nvl.core.variable.type.VariableTypeParser;

import java.util.Set;

public class RequestProcessorImpl implements RequestProcessor {
    private StatementVerifier statementVerifier;
    private VariableDefinitionParser variableDefinitionParser;
    private VariableManager variableManager;
    private VariableTypeParser variableTypeParser;

    public RequestProcessorImpl(StatementVerifier statementVerifier, VariableTypeParser variableTypeParser, VariableDefinitionParser variableDefinitionParser,
                                VariableManager variableManager) {
        this.statementVerifier = statementVerifier;
        this.variableTypeParser = variableTypeParser;
        this.variableDefinitionParser = variableDefinitionParser;
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
