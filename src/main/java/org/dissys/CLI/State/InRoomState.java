package org.dissys.CLI.State;

import org.dissys.CLI.CLI;
import org.dissys.CLI.Input.CLIInput;
import org.dissys.CLI.Input.RoomMessage;

import java.util.UUID;

public class InRoomState extends CLIState{
    private final UUID currentRoom;
    public InRoomState(UUID currentRoom){
        super(StateType.IN_ROOM);
        this.currentRoom = currentRoom;
    }
    @Override
    public void handleInput(CLI cli, CLIInput input) {
        switch (input.getType()){
            case ROOM_CREATED -> {
            }
            case ROOM_INVITATION -> {

            }
            case ROOM_MESSAGE -> {
                RoomMessage roomMessage = (RoomMessage) input;
                if(cli.getCurrentRoom().getRoomId().equals(roomMessage.getFromRoom())){
                    cli.refreshCurrentRoomMessages();
                }
            }
            default -> throw new IllegalArgumentException("unexpected value: " + input);
        }
    }

    public UUID getCurrentRoomID() {
        return currentRoom;
    }
}
