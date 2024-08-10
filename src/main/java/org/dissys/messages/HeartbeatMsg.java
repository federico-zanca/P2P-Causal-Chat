package org.dissys.messages;

import org.dissys.network.Client;

import java.util.UUID;

public class HeartbeatMsg extends Message{
    public HeartbeatMsg(UUID senderId) {
        super(senderId);
    }

    @Override
    public void onMessage(Client client) {
        client.updatePeerList(getSenderId());
    }

    @Override
    public String toString() {
        return "HeartbeatMsg: senderID{"  + getSenderId().toString() + "}";
    }
}
