package src.org.nvl.core.variable.definition.value;

import src.org.nvl.core.variable.UnevaluatedVariable;

/**
 * Replaces variables with their values in the right side of the definition
 */
public interface VariableValueParser {
    UnevaluatedVariable parse(UnevaluatedVariable variable);
}
