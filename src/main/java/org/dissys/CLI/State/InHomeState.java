package org.dissys.CLI.State;

import org.dissys.CLI.CLI;
import org.dissys.CLI.Input.CLIInput;

import java.util.ArrayList;
import java.util.List;

public class InHomeState extends CLIState{
    public InHomeState() {
        super(StateType.IN_HOME, List.of(StateType.IN_HOME, StateType.IN_ROOM, StateType.CHANGING_USERNAME));
    }

    @Override
    public void runFirstAction(CLI cli) {
        cli.refreshHome();
    }

    @Override
    public void handleInput(CLI cli, CLIInput input) {
        switch (input.getType()){
            case ROOM_CREATED, ROOM_INVITATION, ROOM_MESSAGE -> {
                cli.refreshHome();
            }
            default -> throw new IllegalArgumentException("unexpected value: " + input);
        }
    }
}
