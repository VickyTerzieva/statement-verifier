package src.org.nvl.core.variable.manager;

import src.org.nvl.core.variable.EvaluatedVariable;

import java.util.*;

public class MapVariableManager implements VariableManager {
    private final String INVALID_INPUT_MESSAGE = "You found a bug! Invalid input. ";
    private Map<String, EvaluatedVariable> allVariables;

    public MapVariableManager(HashMap<String, EvaluatedVariable> hashMap) {
        allVariables = hashMap;
    }

    public void addVariable(EvaluatedVariable variable) {
        allVariables.put(variable.getName(), variable);
    }

    public void removeVariable(String name) {
        allVariables.remove(name);
    }

    public void updateVariable(EvaluatedVariable variable) {
        if (!allVariables.containsKey(variable.getName())) {
            throw new RuntimeException(INVALID_INPUT_MESSAGE);
        }

        EvaluatedVariable variableToUpdate = allVariables.get(variable.getName());
        variableToUpdate.setValue(variable.getValue());
        variableToUpdate.setType(variable.getType());
    }

    @Override
    public boolean containsVariable(String name) {
        return allVariables.containsKey(name);
    }

    @Override
    public EvaluatedVariable getVariable(String name) {
        return allVariables.get(name);
    }

    @Override
    public Set<EvaluatedVariable> variables() {
        return new HashSet<>(allVariables.values());
    }

    //TODO - try to think of better names
    @Override
    public String freeNameOfVariable(Set<String> variablesInInput) {
        String lettersArray = "abcdefghijklmnopqrstuvwxyz";
        Random rand = new Random();
        int index = rand.nextInt(26);
        StringBuilder name = new StringBuilder();
        name.append(lettersArray.charAt(index));
        while((allVariables != null && allVariables.containsKey(name.toString())) ||
                (variablesInInput != null && variablesInInput.contains(name.toString()))) {
            name.append(name);
        }
        return name.toString();
    }
}
