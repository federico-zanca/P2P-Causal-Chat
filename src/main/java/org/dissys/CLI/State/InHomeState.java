package org.dissys.CLI.State;

import org.dissys.CLI.CLI;
import org.dissys.CLI.Input.CLIInput;

public class InHomeState extends CLIState{
    public InHomeState() {
        super(StateType.IN_HOME);
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
