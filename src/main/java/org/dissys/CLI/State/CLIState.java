package org.dissys.CLI.State;

import org.dissys.CLI.CLI;
import org.dissys.CLI.Input.CLIInput;

public abstract class CLIState {
    public enum StateType {
        IN_HOME, IN_ROOM
    }
    private final StateType type;

    public CLIState(StateType type) {
        this.type = type;
    }

    public StateType getType() {
        return type;
    }

    public abstract void handleInput(CLI cli, CLIInput input);
}
