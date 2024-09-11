package org.dissys.messages;

import org.dissys.network.Client;
import org.dissys.network.PeerInfo;

import java.net.InetAddress;
import java.util.UUID;

public class HeartbeatMsg extends Message{
    private final InetAddress senderAddress;
    private final int senderPort;
    public HeartbeatMsg(UUID senderId, InetAddress senderAddress, int senderPort) {
        super(senderId);
        this.senderAddress = senderAddress;
        this.senderPort = senderPort;
    }

    @Override
    public void onMessage(Client client) {
        if (!client.getConnectedPeers().containsKey(getSenderId())) {
            client.connectToPeer(senderAddress, senderPort, getSenderId());
        }else {
            client.getConnectedPeers().get(getSenderId()).setConnectionTimer(System.currentTimeMillis());
        }
    }

    @Override
    public String toString() {
        return "HeartbeatMsg: senderID{"  + getSenderId().toString() + "}";
    }
}
