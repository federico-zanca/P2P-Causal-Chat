package org.dissys.messages;

import org.dissys.VectorClock;
import org.dissys.network.Client;

import java.util.UUID;

public class ChatMessage extends Message {
    private final String roomId;
    private final String content;
    private final VectorClock vectorClock;

    public ChatMessage(UUID senderId, String roomId, String content, VectorClock vectorClock) {
        super(senderId);
        this.roomId = roomId;
        this.content = content;
        this.vectorClock = vectorClock;
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

    @Override
    public void onMessage(Client client) {
        client.processChatMessage(roomId, this);
    }

    @Override
    public String toString() {
        return "ChatMessage{roomId='" + roomId + "', content='" + content + "', vectorClock=" + vectorClock + '}';
    }
}