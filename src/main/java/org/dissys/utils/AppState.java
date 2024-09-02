package org.dissys.utils;

import org.dissys.Protocols.Username.Username;
import org.dissys.Room;
import org.dissys.VectorClock;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

public class AppState implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Username username;
    //private final String code;
    //private final Long timestamp;
    private final UUID clientUUID;
    private final List<SerializableRoom> serializedRooms;
    private transient List<Room> rooms; // don't serialize this field
    private final Map<UUID, Username> usernameRegistry;
    private Map<UUID, Long> connectedPeers;
    private Map<UUID, VectorClock> roomClocks;
    private LinkedHashMap<UUID, Boolean> processedMessages;
    private List<SerializableRoom> serializedDeletedRooms;
    private transient List<Room> deletedRooms;
    public AppState(Username username,UUID clientUUID, List<SerializableRoom> rooms, Map<UUID, Username> usernameRegistry, List<SerializableRoom> deletedRooms) {
        this.username = username;
        //this.code = code;
        //this.timestamp = timestamp;
        this.clientUUID = clientUUID;
        this.serializedRooms = rooms;
        this.usernameRegistry = usernameRegistry;

        this.serializedDeletedRooms = deletedRooms;
    }
/*
    public Long getTimestamp() {
        return timestamp;
    }*/

    public Username getUsername() {
        return username;
    }/*
    public String getCode(){
        return code;
    }*/

    public UUID getClientUUID() {
        return clientUUID;
    }

    public Map<UUID, Username> getUsernameRegistry() {
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


    public List<Room> getDeletedRooms() {
        return deletedRooms;
    }

    public void setDeletedRooms(List<Room> deletedRooms) {
        this.deletedRooms = deletedRooms;
    }

    public List<SerializableRoom> getSerializedDeletedRooms() {
        return serializedDeletedRooms;
    }


}
