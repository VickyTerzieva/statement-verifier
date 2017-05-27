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
        assertTrue(statementVerifier.verifyStatement("3 * a + a + 19 > 4 * a + 17"));
    }

    @Test
    public void testUndefinedVariableStatement_falseComplex() {
        assertFalse(statementVerifier.verifyStatement("a + a + 19 < 2 * a + 17"));
    }

    @Test
    public void testUndefinedVariableStatement_falseAndTrue() {
        assertFalse(statementVerifier.verifyStatement("a + 19 < 17 && b < b + 15"));
    }

    @Test
    public void testUndefinedVariableStatement_true() {
        assertTrue(statementVerifier.verifyStatement("a + 19 > 17"));
    }

    @Test
    public void testUndefinedVariableStatement_false() {
        assertFalse(statementVerifier.verifyStatement("a + 19 <= 17"));
    }

    @Test
    public void testUndefinedVariableStatement_trueBool() {
        assertTrue(statementVerifier.verifyStatement("a || true"));
    }

    @Test
    public void testUndefinedVariableStatement_trueSimple() {
        assertTrue(statementVerifier.verifyStatement("a < 3 * a"));
    }

    @Test
    public void testUndefinedVariableStatement_falseBool() {
        assertFalse(statementVerifier.verifyStatement("a && false"));
    }

    @Test
    public void testUndefinedVariableStatement_trueBool2() {
        assertTrue(statementVerifier.verifyStatement("a || !a"));
    }

    @Test
    public void testUndefinedVariableStatement_falseBool2() {
        assertFalse(statementVerifier.verifyStatement("a && !a"));
    }

    @Test
    public void testUndefinedVariableStatement_undeterminedAndFalse() {
        assertFalse(statementVerifier.verifyStatement("a + 17 < 19 && b + 16 <= b + 15"));
    }

    @Test
    public void testUndefinedVariableStatement_trueArray() {
        assertTrue(statementVerifier.verifyStatement("{1,1,1} + a + {2,3,4} < a + {3,4,5} * 3"));
    }

    @Test
    public void testUndefinedVariableStatement_trueArray2() {
        assertTrue(statementVerifier.verifyStatement("{3,3,3} / 3 + a <= 2 * a + {1,1,1}"));
    }

    @Test
    public void testUndefinedVariableStatement_falseArraySimple() {
        assertFalse(statementVerifier.verifyStatement("a + {1} < a + {1}"));
    }

    @Test
    public void testUndefinedVariableStatement_falseArray() {
        assertFalse(statementVerifier.verifyStatement("{1,1,1} + a + {2,3,4} < a"));
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

    @Test(expected = RuntimeException.class)
    public void testUndefinedVariableStatement_undeterminedAndTrue() {
        try {
            statementVerifier.verifyStatement("a + 17 < 19 && b < b + 15 / 4");
        }
        catch(RuntimeException re) {
            assertEquals(UNDETERMINED_VALUE_MESSAGE, re.getMessage());
            throw re;
        }
        fail("Exception did not throw!");
    }

    @Test(expected = RuntimeException.class)
    public void testUndefinedVariableStatement_falseOrUndetermined() {
        try {
            statementVerifier.verifyStatement("a && false && true || b && true");
        }
        catch(RuntimeException re) {
            assertEquals(UNDETERMINED_VALUE_MESSAGE, re.getMessage());
            throw re;
        }
        fail("Exception did not throw!");
    }

    @Test(expected = RuntimeException.class)
    public void testUndefinedVariableStatement_undeterminedArray() {
        try {
            statementVerifier.verifyStatement("{1,1,1} + a + {2,3,4} < 3 * a");
        }
        catch(RuntimeException re) {
            assertEquals(UNDETERMINED_VALUE_MESSAGE, re.getMessage());
            throw re;
        }
        fail("Exception did not throw!");
    }
}
