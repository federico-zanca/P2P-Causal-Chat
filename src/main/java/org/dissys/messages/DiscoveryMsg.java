package org.dissys.messages;

import org.dissys.network.Client;

import java.util.UUID;

public class DiscoveryMsg extends Message{
    public DiscoveryMsg(UUID senderId) {
        super(senderId);
    }

    @Override
    public void onMessage(Client client) {
        client.updatePeerList(getSenderId()); //should do it anyway
        client.sendMessage(new HeartbeatMsg(client.getUUID()));
    }

    @Override
    public String toString() {
        return "DiscoveryMsg: senderID{" + getSenderId().toString() + "}" ;
    }
}
