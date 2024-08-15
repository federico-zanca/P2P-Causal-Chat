package org.dissys.utils;

import org.dissys.Room;
import org.dissys.VectorClock;

import java.io.Serializable;
import java.util.*;

public class AppState implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private UUID clientUUID;
    private List<SerializableRoom> serializedRooms;
    private transient List<Room> rooms; // don't serialize this field
    private Map<UUID, String> usernameRegistry;
    private Map<UUID, Long> connectedPeers;
    private Map<UUID, VectorClock> roomClocks;
    private LinkedHashMap<UUID, Boolean> processedMessages;

    public AppState(String username, UUID clientUUID, List<SerializableRoom> rooms) {
        this.username = username;
        this.clientUUID = clientUUID;
        this.serializedRooms = rooms;
    }


    public String getUsername() {
        return username;
    }

    public UUID getClientUUID() {
        return clientUUID;
    }

    public Map<UUID, String> getUsernameRegistry() {
        return usernameRegistry;
    }

    public Map<UUID, Long> getConnectedPeers() {
        return connectedPeers;
    }

    public Map<UUID, VectorClock> getRoomClocks() {
        return roomClocks;
    }

    public void setRooms(List<Room> rooms) {
        this.rooms = rooms;
    }

    public List<Room> getRooms() {
        return rooms;
    }


    public List<SerializableRoom> getSerializedRooms() {
        return serializedRooms;
    }

}
