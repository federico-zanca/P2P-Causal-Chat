package org.dissys.messages;

import org.dissys.network.Client;
import org.dissys.network.PeerInfo;

import java.net.InetAddress;
import java.util.UUID;

public class DiscoveryAckMsg extends Message{
    private final InetAddress senderAddress;
    private final int senderPort;
    public DiscoveryAckMsg(UUID senderId, InetAddress senderAddress, int senderPort) {
        super(senderId);
        this.senderAddress = senderAddress;
        this.senderPort = senderPort;
    }

    @Override
    public void onMessage(Client client) {
        //System.out.println("received discovery Ack from " + senderAddress.toString() + " port " + senderPort);
        //client.getConnectedPeers().put(getSenderId(), new PeerInfo(System.currentTimeMillis(), senderAddress, senderPort));
        /*
        if (!client.getConnectedPeers().containsKey(getSenderId())) {
            client.connectToPeer(senderAddress, senderPort, getSenderId());
        }*/
    }

    public InetAddress getSenderAddress() {
        return senderAddress;
    }

    public int getSenderPort() {
        return senderPort;
    }

    @Override
    public String toString() {
        return "Discovery Ack from " + getSenderId().toString();
    }
}
