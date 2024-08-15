package org.dissys.utils;

import org.dissys.Room;
import org.dissys.VectorClock;
import org.dissys.messages.ChatMessage;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SerializableRoom {
    private UUID roomId;
    private String roomName;
    private UUID localPeerId;
    private Set<String> participants;
    private VectorClock localClock;
    private List<ChatMessage> deliveredMessages;
    private String multicastIP;

    SerializableRoom(Room room) {
        this.roomId = room.getRoomId();
        this.roomName = room.getRoomName();
        this.localPeerId = room.getLocalPeerId();
        this.participants = room.getParticipants();
        this.localClock = room.getLocalClock();
        this.deliveredMessages = room.getDeliveredMessages();
        this.multicastIP = room.getMulticastIP();
    }

    Room toRoom() {
        Room room = new Room(roomId, roomName, localPeerId, participants, multicastIP);
        room.setLocalClock(localClock);
        room.setDeliveredMessages(deliveredMessages);
        return room;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public UUID getLocalPeerId() {
        return localPeerId;
    }

    public Set<String> getParticipants() {
        return participants;
    }

    public VectorClock getLocalClock() {
        return localClock;
    }

    public List<ChatMessage> getDeliveredMessages() {
        return deliveredMessages;
    }

    public String getMulticastIP() {
        return multicastIP;
    }


}
