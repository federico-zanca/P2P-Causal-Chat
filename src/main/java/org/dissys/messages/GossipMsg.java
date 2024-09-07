package org.dissys.messages;

import org.dissys.Protocols.Username.Username;
import org.dissys.network.Client;

import java.util.Map;
import java.util.UUID;

import static org.dissys.Protocols.GossipProtocol.receiveGossip;

public class GossipMsg extends Message{
    private final Map<UUID, Username> usernameRegistry;
    public GossipMsg(UUID senderId, Map<UUID, Username> usernameRegistry) {
        super(senderId);
        this.usernameRegistry = usernameRegistry;
    }

    @Override
    public void onMessage(Client client) {
        System.out.println("received gossip from " + getSenderId());
        receiveGossip(usernameRegistry, client.getApp());
    }

    @Override
    public String toString() {
        return "gossip from " + getSenderId();
    }
}
