package org.dissys.messages;

import org.dissys.network.Client;

import java.io.Serializable;
import java.util.UUID;

public abstract class Message implements Serializable {
    private final UUID messageUUID;
    private final UUID senderId;
    public Message(UUID senderId) {
        this.senderId = senderId;
        messageUUID = UUID.randomUUID();
    }
    public abstract void onMessage(Client client);
    public UUID getSenderId() {
        return senderId;
    }

    public UUID getMessageUUID() {
        return messageUUID;
    }

    @Override
    public abstract String toString();
}
