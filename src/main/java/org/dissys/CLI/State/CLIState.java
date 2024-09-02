package org.dissys.CLI.State;

import org.dissys.CLI.CLI;
import org.dissys.CLI.Input.CLIInput;

import java.util.List;

public abstract class CLIState {


    public enum StateType {
        IN_HOME, IN_ROOM, CHANGING_USERNAME
    }
    private final StateType type;
    private final List<StateType> possibleNextStates;

    public CLIState(StateType type, List<StateType> possibleNextStates) {
        this.type = type;
        this.possibleNextStates = possibleNextStates;
    }

    public StateType getType() {
        return type;
    }

    public List<StateType> getPossibleNextStates() {
        return possibleNextStates;
    }
    public abstract void runFirstAction(CLI cli);
    public abstract void handleInput(CLI cli, CLIInput input);
}
