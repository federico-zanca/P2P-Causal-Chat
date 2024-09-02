package org.dissys.CLI.State;

import org.dissys.CLI.CLI;
import org.dissys.CLI.Input.CLIInput;

import java.util.List;

import static org.dissys.CLI.State.CLIState.StateType.CHANGING_USERNAME;

public class ChangingUsernameState extends CLIState{
    public ChangingUsernameState() {
        super(CHANGING_USERNAME, List.of());
    }

    @Override
    public void runFirstAction(CLI cli) {
        cli.printWarning("A username conflict has been found!");
        cli.printNotification("You will have to restart the application and change your username");
        cli.printWarning("Shutting down ...");
    }

    @Override
    public void handleInput(CLI cli, CLIInput input) {
        switch (input.getType()){
            case ROOM_CREATED, ROOM_INVITATION, ROOM_MESSAGE -> {

            }
            default -> throw new IllegalArgumentException("unexpected value: " + input);
        }
    }
}
