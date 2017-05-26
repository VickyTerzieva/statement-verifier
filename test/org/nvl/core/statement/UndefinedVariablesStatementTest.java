package org.nvl.core.statement;

import org.junit.Before;
import org.junit.Test;
import src.org.nvl.core.statement.RpnStatementVerifier;
import src.org.nvl.core.statement.StatementVerifier;
import src.org.nvl.core.variable.manager.MapVariableManager;

import java.util.HashMap;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static src.org.nvl.MessageConstants.INVALID_INPUT_MESSAGE;
import static src.org.nvl.MessageConstants.NEW_VARIABLE_MESSAGE;
import static src.org.nvl.MessageConstants.UNDETERMINED_VALUE_MESSAGE;

/**
 * Created by Vicky on 27.5.2017 г..
 */
public class UndefinedVariablesStatementTest {
    private StatementVerifier statementVerifier;
    private MapVariableManager variableManager;

    @Before
    public void setUp() {
        variableManager = new MapVariableManager(new HashMap<>());
        statementVerifier = new RpnStatementVerifier(variableManager);
    }

    @Test
    public void testUndefinedVariableStatement_trueComplex() {
        assertTrue(statementVerifier.verifyStatement("a + a + 19 > 3 * a - a + 17"));
    }

    @Test
    public void testUndefinedVariableStatement_falseComplex() {
        assertFalse(statementVerifier.verifyStatement("a + a + 19 < 2 * a + 17"));
    }

    @Test
    public void testUndefinedVariableStatement_true() {
        assertTrue(statementVerifier.verifyStatement("a + 19 > 17"));
    }

    @Test
    public void testUndefinedVariableStatement_false() {
        assertFalse(statementVerifier.verifyStatement("a + 19 < 17"));
    }

    @Test(expected = RuntimeException.class)
    public void testUndefinedVariableStatement_undeterminedValue() {
        try {
            statementVerifier.verifyStatement("a + 17 < 19");
        }
        catch(RuntimeException re) {
            assertEquals(UNDETERMINED_VALUE_MESSAGE, re.getMessage());
            throw re;
        }
        fail("Exception did not throw!");
    }
}
