package org.dissys.messages;

import org.dissys.network.Client;

import java.util.UUID;

public class HeartbeatMsg extends Message{
    private String username = null;
    public HeartbeatMsg(UUID senderId, String username) {
        super(senderId);
        this.username = username;
    }

    @Override
    public void onMessage(Client client) {
        client.updatePeerList(getSenderId());
        if(username != null){
            client.getApp().updateUsernameRegistry(username, getSenderId());
        }
    }

    public String getUsername() {
        return username;
    }

    @Override
    public String toString() {
        return "HeartbeatMsg: senderID{"  + getSenderId().toString() + "}";
    }
}
