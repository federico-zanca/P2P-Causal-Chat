package org.dissys.messages;

import org.dissys.network.Client;

import java.util.UUID;

public class HeartbeatMsg extends Message{
    public HeartbeatMsg(UUID senderId) {
        super(senderId);
    }

    @Override
    public void onMessage(Client client) {
        if(client.getConnectedPeers().containsKey(getSenderId())){
            client.getConnectedPeers().get(getSenderId()).setConnectionTimer(System.currentTimeMillis());
        }
    }

    @Override
    public String toString() {
        return "HeartbeatMsg: senderID{"  + getSenderId().toString() + "}";
    }
}
