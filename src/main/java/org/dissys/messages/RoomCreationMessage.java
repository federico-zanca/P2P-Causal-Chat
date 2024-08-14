package org.dissys.messages;

import org.dissys.Room;
import org.dissys.network.Client;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class RoomCreationMessage extends Message {
;   private final UUID roomId;
    private final String roomName;
    private final Set<String> participants;
    private final String sender;

    public RoomCreationMessage(UUID senderId, String sender, UUID roomId, String roomName, Set<String> participants) {
        super(senderId);

        this.sender = sender;

        this.roomId = roomId;
        this.roomName = roomName;
        this.participants = participants;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public Set<String> getParticipants() {
        return participants;
    }

    @Override
    public void onMessage(Client client) {
        client.getApp().processRoomCreationMessage(this);
    }

    @Override
    public String toString() {
        return "RoomCreationMessage{roomId='" + roomId + "', roomName='" + roomName + "', participants=" + participants + '}';
    }

    public String getSender() {
        return sender;
    }
}
