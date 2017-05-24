package src.org.nvl.core.responder.processor;

import src.org.nvl.core.variable.EvaluatedVariable;

import java.util.Set;

/**
 * Processes the given requests
 */
public interface RequestProcessor {
    void addVariable(EvaluatedVariable evaluatedVariable);

    void updateVariable(EvaluatedVariable variableDefinition);

    boolean verifyStatement(String statement);

    Set<EvaluatedVariable> variables();
}
