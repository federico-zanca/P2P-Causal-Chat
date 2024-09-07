package org.dissys.messages;

import org.dissys.network.Client;
import org.dissys.network.PeerInfo;

import java.net.InetAddress;
import java.util.UUID;

public class DiscoveryAckMsg extends Message{
    public DiscoveryAckMsg(UUID senderId) {
        super(senderId);
    }

    @Override
    public void onMessage(Client client) {
    }

    @Override
    public String toString() {
        return "Discovery Ack from " + getSenderId().toString();
    }
}
