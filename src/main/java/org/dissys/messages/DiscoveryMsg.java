package org.dissys.messages;

import org.dissys.network.Client;

import java.util.UUID;

public class DiscoveryMsg extends Message{
    private String username = null;
    public DiscoveryMsg(UUID senderId, String username) {
        super(senderId);
        this.username = username;
    }

    @Override
    public void onMessage(Client client) {
        client.updatePeerList(getSenderId()); //should do it anyway
        if(username != null){
            client.getApp().updateUsernameRegistry(username, getSenderId());
        }
        client.sendMessage(new HeartbeatMsg(client.getUUID(), client.getApp().getUsername()));
    }

    public String getUsername() {
        return username;
    }

    @Override
    public String toString() {
        return "DiscoveryMsg: senderID{" + getSenderId().toString() + "}" ;
    }
}
