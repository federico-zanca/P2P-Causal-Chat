package org.dissys.messages;

import org.dissys.Room;
import org.dissys.UglyRoom;
import org.dissys.User;
import org.dissys.VectorClock;
import org.dissys.network.Client;

import java.io.Serializable;
import java.util.UUID;

public abstract class Message implements Serializable {
    private final UUID senderId;
    public Message(UUID senderId) {
        this.senderId = senderId;
    }
    public abstract void onMessage(Client client);
    public UUID getSenderId() {
        return senderId;
    }
    @Override
    public abstract String toString();
}
