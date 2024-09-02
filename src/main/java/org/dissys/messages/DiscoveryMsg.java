package org.dissys.messages;

import org.dissys.network.Client;
import org.dissys.network.PeerInfo;

import java.net.InetAddress;
import java.util.UUID;

public class DiscoveryMsg extends Message{
    private final InetAddress senderAddress;
    private final int senderPort;
    public DiscoveryMsg(UUID senderId, InetAddress senderAddress, int senderPort) {
        super(senderId);
        this.senderAddress = senderAddress;
        this.senderPort = senderPort;
    }

    @Override
    public void onMessage(Client client) {
        client.getConnectedPeers().put(getSenderId(), new PeerInfo(System.currentTimeMillis(), senderAddress, senderPort));
        //System.out.println("put peer in connected peers " + getSenderId());
        client.sendUnicastMessage(new DiscoveryAckMsg(client.getUUID(), client.getLocalAddress(), client.getUnicastPort()), senderAddress, senderPort);
    }

    public InetAddress getSenderAddress() {
        return senderAddress;
    }

    @Override
    public String toString() {
        return "DiscoveryMsg: senderID{" + getSenderId().toString() + "}" ;
    }
}
