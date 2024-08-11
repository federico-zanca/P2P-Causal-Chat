package org.dissys.messages;

import org.dissys.VectorClock;
import org.dissys.network.Client;

import java.util.UUID;

public class ChatMessage extends Message {
    private final String roomId;
    private final String content;
    private final VectorClock vectorClock;
    private String sender;

    public ChatMessage(UUID senderId, String sender, String roomId, String content, VectorClock vectorClock) {
        super(senderId);
        this.roomId = roomId;
        this.content = content;
        this.vectorClock = vectorClock;
        this.sender = sender;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getContent() {
        return content;
    }

    public VectorClock getVectorClock() {
        return vectorClock;
    }

    public String getSender() {
        return sender;
    }

    @Override
    public void onMessage(Client client) {
        client.processChatMessage(roomId, this);
    }

    @Override
    public String toString() {
        return "ChatMessage{roomId='" + roomId + "', content='" + content + "', vectorClock=" + vectorClock + '}';
    }
}