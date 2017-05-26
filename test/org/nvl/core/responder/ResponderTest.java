package src.org.nvl.core.responder;

import org.junit.Before;
import org.junit.Test;
import src.org.nvl.MessageConstants;
import src.org.nvl.core.input.type.InputTypeDeterminer;
import src.org.nvl.core.input.type.SimpleInputTypeDeterminer;
import src.org.nvl.core.responder.processor.RequestProcessor;
import src.org.nvl.core.responder.processor.RequestProcessorImpl;
import src.org.nvl.core.statement.RpnStatementVerifier;
import src.org.nvl.core.statement.StatementVerifier;
import src.org.nvl.core.variable.EvaluatedVariable;
import src.org.nvl.core.variable.VariableType;
import src.org.nvl.core.variable.manager.MapVariableManager;
import src.org.nvl.core.variable.manager.VariableManager;

import java.util.HashMap;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static src.org.nvl.MessageConstants.*;

public class ResponderTest {
    private Responder responder;
    private VariableManager variableManager;

    @Before
    public void setUp() {
        variableManager = new MapVariableManager(new HashMap<>());

        InputTypeDeterminer typeDeterminer = new SimpleInputTypeDeterminer(variableManager);
        StatementVerifier statementVerifier = new RpnStatementVerifier(variableManager);
        RequestProcessor requestProcessor = new RequestProcessorImpl(statementVerifier, variableManager);

        responder = new ResponderImpl(typeDeterminer, requestProcessor, variableManager);
    }

    @Test
    public void testProcess_string() {
        String statement = "'asdf' <= 'asdf'";
        assertEquals(String.format(STATEMENT_FORMAT, statement, "TRUE"), responder.process(statement));
    }

    @Test
    public void testProcess_spacingNumbers() {
        String statement = "   3+   5  ==8   ";
        assertEquals(String.format(STATEMENT_FORMAT, statement, "TRUE"), responder.process(statement));
    }

    @Test
    public void testProcess_complexStatement() {
        String statement = "((5-3)/4<2)==(3+5<6)";
        assertEquals(String.format(STATEMENT_FORMAT, statement, "FALSE"), responder.process(statement));
    }

    @Test
    public void testProcess_complexStatement2() {
        String statement = "1+3<5==2+4<=6==3+5>=2";
        assertEquals(String.format(STATEMENT_FORMAT, statement, "TRUE"), responder.process(statement));
    }

    @Test
    public void testProcess_spacingStrings() {
        String statement = "   'asdf'+   'asdf'  <=    'asdf'   ";
        assertEquals(String.format(STATEMENT_FORMAT, statement, "FALSE"), responder.process(statement));
    }

    @Test
    public void testProcess_spacingArrays() {
        String statement = "{1, 2,3}+{10}=={11,2,3}";
        assertEquals(String.format(STATEMENT_FORMAT, statement, "TRUE"), responder.process(statement));
    }

    @Test
    public void testProcess_spacingBooleans() {
        String statement = "(true&&!  true)==    !false";
        assertEquals(String.format(STATEMENT_FORMAT, statement, "FALSE"), responder.process(statement));
    }

    @Test
    public void testProcess_noComparisonOperation() {
        String statement = "true || false && false";
        assertEquals(String.format(STATEMENT_FORMAT, statement, "TRUE"), responder.process(statement));
    }

    @Test
    public void testProcess_noComparisonOperation2() {
        String statement = "false";
        assertEquals(String.format(STATEMENT_FORMAT, statement, "FALSE"), responder.process(statement));
    }

    @Test
    public void testProcess_spacingInStrings() {
        String statement = "'a b c' == 'abc'";
        assertEquals(String.format(STATEMENT_FORMAT, statement, "FALSE"), responder.process(statement));
    }

    @Test
    public void testProcess_updateVariable() {
        variableManager.addVariable(new EvaluatedVariable("a", "5", VariableType.NUMBER));
        String statement = "a = 6";

        assertEquals(MessageConstants.EXISTING_VARIABLE_MESSAGE, responder.process(statement));
    }

    @Test
    public void testProcess_addVariableNumberComplexDefinition() {
        assertEquals(NEW_VARIABLE_MESSAGE, responder.process("5*a*3 + 8 - 3*a - a - 3 = 16"));
        assertEquals("1", variableManager.getVariable("a").getValue());
        assertEquals(NEW_VARIABLE_MESSAGE, responder.process("b*2 = 3 * a + 3 - a"));
        assertEquals("2", variableManager.getVariable("b").getValue());
    }

    @Test
    public void testProcess_addVariableBooleanComplexDefinition() {
        String value = "true";

        assertEquals(NEW_VARIABLE_MESSAGE, responder.process("var&&!false&&false || true&&var&&true = true || false"));
        assertEquals(value, variableManager.getVariable("var").getValue());
    }

    @Test(expected = RuntimeException.class)
    public void testProcess_addVariableBooleanImpossibleDefinition() {
        try {
            responder.process("(!a && false) = true");
        }
        catch(RuntimeException re) {
            assertEquals(IMPOSSIBLE_OPERATION_MESSAGE, re.getMessage());
            throw re;
        }
        fail("Exception did not throw!");
    }

    @Test
    public void testProcess_addVariableStringComplexDefinition() {
        String value = "'a'";

        assertEquals(NEW_VARIABLE_MESSAGE, responder.process("a = ('aaaaaab' - 'b') / 6"));
        assertEquals(NEW_VARIABLE_MESSAGE, responder.process("2 * b * 3 + 'b' = 'aaaaaab'"));
        assertEquals(value, variableManager.getVariable("a").getValue());
        assertEquals(value, variableManager.getVariable("b").getValue());
    }

    @Test
    public void testProcess_addVariableArrayComplexDefinition() {
        String value = "{1,1,1}";

        assertEquals(NEW_VARIABLE_MESSAGE, responder.process("b + {1,1,1}= ({7,8,9} - {1,2,3} ) / 3"));
        assertEquals(value, variableManager.getVariable("b").getValue());
    }

    @Test
    public void testProcess_addVariableStringWithSpaces() {
        String value = "'a b c'";
        String input = "a = " + value;

        assertEquals(NEW_VARIABLE_MESSAGE, responder.process(input));
        assertEquals(value, variableManager.getVariable("a").getValue());
    }

    @Test(expected = RuntimeException.class)
    public void testProcess_invalidInput() {
        try {
            responder.process("(5&& 3) ==4");
        }
        catch(RuntimeException re) {
            assertEquals(INVALID_INPUT_MESSAGE, re.getMessage());
            throw re;
        }
        fail("Exception did not throw!");
    }

    @Test(expected = RuntimeException.class)
    public void testProcess_quadraticDefinition() {
        try {
            responder.process("5 * a * a = 125");
        }
        catch(RuntimeException re) {
            assertEquals(LINEAR_DEFINITION_MESSAGE, re.getMessage());
            throw re;
        }
        fail("Exception did not throw!");
    }

    @Test(expected = RuntimeException.class)
    public void testProcess_changeType() {
        responder.process("a = 5");
        responder.process("a = 'asdf'");
    }
}
