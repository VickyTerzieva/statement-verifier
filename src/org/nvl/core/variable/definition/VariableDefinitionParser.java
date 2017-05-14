package src.org.nvl.core.variable.definition;

import src.org.nvl.core.variable.UnevaluatedVariable;

/**
 * Parses a variable definition to it's parts in string form
 */
public interface VariableDefinitionParser {
    UnevaluatedVariable parse(String definition);
}
