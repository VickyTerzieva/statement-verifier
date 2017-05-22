package src.org.nvl.core.input.type;

import src.org.nvl.core.input.tree.InputTree;

/**
 * Determines the variable type from its value
 */
public interface InputTypeDeterminer {
    InputType determineType(InputTree input);
}
