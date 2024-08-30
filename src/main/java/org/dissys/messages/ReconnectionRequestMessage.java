package org.dissys.messages;

import org.dissys.Room;
import org.dissys.VectorClock;
import org.dissys.network.Client;

import java.util.*;

import static org.dissys.Protocols.ReconnectionProtocol.processReconnectionRequestMessage;

public class ReconnectionRequestMessage extends Message {
    private final Map<UUID, VectorClock> roomsClocks;
    private final String sender;
    private final Set<UUID> deletedRooms;

    public ReconnectionRequestMessage(UUID senderId, String sender, List<Room> rooms, Set<UUID> deletedRooms) {
        super(senderId);
        this.sender = sender;
        Map<UUID, VectorClock> roomsClocks = new HashMap<>();
        for(Room room : rooms){
            roomsClocks.put(room.getRoomId(), room.getLocalClock());
        }
        this.roomsClocks = roomsClocks;
        this.deletedRooms = deletedRooms;
    }

    public Map<UUID, VectorClock> getRoomsClocks() {
        return roomsClocks;
    }

    @Override
    public void onMessage(Client client) {
        processReconnectionRequestMessage(this, client.getApp());
    }

    @Override
    public String toString() {
        return "sender = "+ sender +"  ReconnectionRequestMessage{roomsClocks=" + roomsClocks + '}';
    }

    public String getSender() {
        return sender;
    }

    public Set<UUID> getDeletedRooms() {
        return deletedRooms;
    }
}
