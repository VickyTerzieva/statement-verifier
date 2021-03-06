package src.org.nvl;

import src.org.nvl.core.input.type.InputTypeDeterminer;
import src.org.nvl.core.input.type.SimpleInputTypeDeterminer;
import src.org.nvl.core.responder.Responder;
import src.org.nvl.core.responder.ResponderImpl;
import src.org.nvl.core.responder.processor.RequestProcessor;
import src.org.nvl.core.responder.processor.RequestProcessorImpl;
import src.org.nvl.core.statement.RpnStatementVerifier;
import src.org.nvl.core.statement.StatementVerifier;
import src.org.nvl.core.variable.manager.MapVariableManager;
import src.org.nvl.core.variable.manager.VariableManager;
import src.org.nvl.ui.GraphicalUserInterface;
import src.org.nvl.ui.SwingGraphicalUserInterface;

import java.util.HashMap;

/**
 * Constructs the StatementVerifier and runs it on a GUI
 */
public class GuiStatementVerifier {
    private GraphicalUserInterface graphicalUserInterface;

    public GuiStatementVerifier() {
        VariableManager variableManager = new MapVariableManager(new HashMap<>());
        StatementVerifier statementVerifier = new RpnStatementVerifier(variableManager);
        RequestProcessor requestProcessor = new RequestProcessorImpl(statementVerifier, variableManager);

        InputTypeDeterminer typeDeterminer = new SimpleInputTypeDeterminer(variableManager);
        Responder responder = new ResponderImpl(typeDeterminer, requestProcessor, variableManager);

        graphicalUserInterface = new SwingGraphicalUserInterface(responder);
    }

    public void start() {
        graphicalUserInterface.start();
    }

    public static void main(String[] args) {
        GuiStatementVerifier guiStatementVerifier = new GuiStatementVerifier();
        guiStatementVerifier.start();
    }
}
