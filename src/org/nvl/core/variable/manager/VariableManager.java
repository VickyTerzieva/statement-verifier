package src.org.nvl.core.variable.manager;

import src.org.nvl.core.variable.EvaluatedVariable;

import java.util.Set;

/**
 * Manages the variables in the current environment
 */
public interface VariableManager {
    void addVariable(EvaluatedVariable variable);

    void updateVariable(EvaluatedVariable variable);

    boolean containsVariable(String name);

    EvaluatedVariable getVariable(String name);

    Set<EvaluatedVariable> variables();

    String freeNameOfVariable(Set<String> variableInInput);
}
