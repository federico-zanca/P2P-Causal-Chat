package org.dissys.CLI.Input;

import java.util.UUID;

public class RoomMessage extends CLIInput{
    private final UUID fromRoom;
    public RoomMessage(UUID fromRoom) {
        super(CLIInputTypes.ROOM_MESSAGE);
        this.fromRoom = fromRoom;
    }

    public UUID getFromRoom() {
        return fromRoom;
    }
}
