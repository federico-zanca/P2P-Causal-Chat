package org.dissys.messages;

import org.dissys.network.Client;
import org.dissys.network.PeerInfo;

import java.net.InetAddress;
import java.util.UUID;

public class DiscoveryMsg extends Message{
    public DiscoveryMsg(UUID senderId) {
        super(senderId);
    }

    @Override
    public void onMessage(Client client) {
        //client.getConnectedPeers().put(getSenderId(), new PeerInfo(System.currentTimeMillis()));
        //System.out.println("put peer in connected peers " + getSenderId());
        InetAddress receiverAddress = client.getConnectedPeers().get(getSenderId()).getAddress();

        client.sendUnicastMessage(new DiscoveryAckMsg(client.getUUID()), receiverAddress);
    }

    @Override
    public String toString() {
        return "DiscoveryMsg: senderID{" + getSenderId().toString() + "}" ;
    }
}
