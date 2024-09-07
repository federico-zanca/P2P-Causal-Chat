package org.dissys.Protocols;

import org.dissys.P2PChatApp;
import org.dissys.Protocols.Username.Username;
import org.dissys.messages.GossipMsg;
import org.dissys.network.Client;
import org.dissys.network.PeerInfo;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class GossipProtocol {
    public static void gossip(Map<UUID, PeerInfo> connectedPeers, Client client) {
        if (connectedPeers.isEmpty())
            return;
        // Select a random peer to gossip with
        List<UUID> peersList = connectedPeers.keySet().stream().toList();
        UUID randomPeer = peersList.get(new Random().nextInt(peersList.size()));

        if (randomPeer == client.getUUID()) return;

        // Send the local username registry to the selected peer
        client.sendUnicastMessage(new GossipMsg(client.getUUID(), client.getApp().getUsernameRegistry()),
                connectedPeers.get(randomPeer).getAddress(),
                connectedPeers.get(randomPeer).getPort());
    }

    public static void receiveGossip(Map<UUID, Username> receivedRegistry, P2PChatApp app) {
        // Merge received username registry with local registry
        /*
        for (Map.Entry<UUID, Username> entry : receivedRegistry.entrySet()) {
            this.usernameRegistry.putIfAbsent(entry.getKey(), entry.getValue());

        //System.out.println("Peer " + peerId + " updated registry from " + senderId);*/
        app.mergeUsernameRegistries(receivedRegistry);
    }

}
