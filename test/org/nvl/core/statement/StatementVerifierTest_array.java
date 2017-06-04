package src.org.nvl.core.statement;

import org.junit.Before;
import org.junit.Test;
import src.org.nvl.core.statement.RpnStatementVerifier;
import src.org.nvl.core.statement.StatementVerifier;
import src.org.nvl.core.variable.EvaluatedVariable;
import src.org.nvl.core.variable.VariableType;
import src.org.nvl.core.variable.manager.MapVariableManager;

import java.util.HashMap;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

public class StatementVerifierTest_array {
    private StatementVerifier statementVerifier;
    private MapVariableManager variableManager;

    @Before
    public void setUp() {
        variableManager = new MapVariableManager(new HashMap<>());
        statementVerifier = new RpnStatementVerifier(variableManager);
    }

    @Test
    public void testVerifyStatement_simple() {
        assertTrue(statementVerifier.verifyStatement("{1,2} == {1,2}"));
    }

    @Test
    public void testVerifyStatement_lessDigits() {
        assertTrue(statementVerifier.verifyStatement("{1} < {244312}"));
    }

    @Test
    public void testVerifyStatement_greaterDifferentLengths() {
        assertTrue(statementVerifier.verifyStatement("{1,2} < {2}"));
    }

    @Test
    public void testVerifyStatement_additionWithNumber() {
        assertTrue(statementVerifier.verifyStatement("{1,2} + 10 == {11,12}"));
    }

    @Test
    public void testVerifyStatement_additionWithArray() {
        assertTrue(statementVerifier.verifyStatement("{1,2} + {10,10} == {11,12}"));
    }

    @Test
    public void testVerifyStatement_additionWithArrayDifferentLength() {
        assertFalse(statementVerifier.verifyStatement("{1,2} + {10} == {11,12}"));
    }

    @Test
    public void testVerifyStatement_multiplicationWithNumber() {
        assertTrue(statementVerifier.verifyStatement("{1,2} * 10 == {10,20}"));
    }

    @Test
    public void testVerifyStatement_multiplicationWithArray() {
        assertTrue(statementVerifier.verifyStatement("{3,2} * {10,1} == {30,2}"));
    }

    @Test
    public void testVerifyStatement_multiplicationWithArrayDifferentSizes() {
        assertTrue(statementVerifier.verifyStatement("{3,2} * {10,1,2} == {30,2,2}"));
    }

    @Test
    public void testVerifyStatement_complexWithVariables() {
        variableManager.addVariable(new EvaluatedVariable("s", "{1,2,3}", VariableType.ARRAY));
        assertFalse(statementVerifier.verifyStatement("( {5,5,5} + s ) * s < {1,1,1}"));
    }

    @Test
    public void testVerifyStatement_shorter() {
        variableManager.addVariable(new EvaluatedVariable("s", "{1,1,1}", VariableType.STRING));
        assertTrue(statementVerifier.verifyStatement("s < {2,3}"));
    }
}
