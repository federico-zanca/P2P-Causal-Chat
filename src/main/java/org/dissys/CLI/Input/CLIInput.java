package org.dissys.CLI.Input;

public abstract class CLIInput {
    private final CLIInputTypes type;

    public CLIInput(CLIInputTypes type){
        this.type = type;
    }

    public CLIInputTypes getType() {
        return type;
    }
}
