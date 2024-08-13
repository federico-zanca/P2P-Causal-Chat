package org.dissys.messages;

import org.dissys.Room;
import org.dissys.VectorClock;
import org.dissys.network.Client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ReconnectionRequestMessage extends Message {
    private final Map<UUID, VectorClock> roomsClocks;
    private final String sender;

    public ReconnectionRequestMessage(UUID senderId, String sender, List<Room> rooms) {
        super(senderId);
        this.sender = sender;
        Map<UUID, VectorClock> roomsClocks = new HashMap<>();
        for(Room room : rooms){
            roomsClocks.put(room.getRoomId(), room.getLocalClock());
        }
        this.roomsClocks = roomsClocks;
    }

    public Map<UUID, VectorClock> getRoomsClocks() {
        return roomsClocks;
    }

    @Override
    public void onMessage(Client client) {
        client.processReconnectionRequestMessage(this);
    }

    @Override
    public String toString() {
        return "sender = "+ sender +"  ReconnectionRequestMessage{roomsClocks=" + roomsClocks + '}';
    }

    public String getSender() {
        return sender;
    }
}
