package org.dissys.messages;

import org.dissys.network.Client;

import java.util.UUID;

public class HeartbeatMsg extends Message{
    private final String username;
    public HeartbeatMsg(UUID senderId, String username) {
        super(senderId);
        this.username = username;
    }

    @Override
    public void onMessage(Client client) {
        if(client.getConnectedPeers().containsKey(getSenderId())){
            client.getConnectedPeers().get(getSenderId()).setConnectionTimer(System.currentTimeMillis());
            if(username != null){
                client.getApp().updateUsernameRegistry(username, getSenderId());
            }
        }
    }

    @Override
    public String toString() {
        return "HeartbeatMsg: senderID{"  + getSenderId().toString() + "}";
    }
}
