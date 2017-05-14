package src.org.nvl.core.variable.type;

import src.org.nvl.core.variable.EvaluatedVariable;
import src.org.nvl.core.variable.UnevaluatedVariable;

/**
 * Parses the variable value to find its type
 */
public interface VariableTypeParser {
    EvaluatedVariable parse(UnevaluatedVariable variable);
}
