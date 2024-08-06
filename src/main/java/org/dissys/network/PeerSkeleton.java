package org.dissys.network;

import org.dissys.User;

import java.net.InetAddress;
import java.util.Map;
import java.util.UUID;

public class PeerSkeleton {
    private UUID id;
    private InetAddress address;
    private int port;

    public PeerSkeleton(InetAddress address, int port) {
        //generated UUID might be wrong
        this.id = UUID.randomUUID();
        this.address = address;
        this.port = port;
    }

    public UUID getId() {
        return id;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public boolean voteOnUsername(User proposedUser) {
        //TODO
        // In a real implementation, this would send a network request to the actual peer
        // For now, we'll simulate it
        return true;
    }

    public void addToUsernameRegistry(User user) {
        //TODO
        // In a real implementation, this would send a network request to the actual peer
        // For now, we'll just log it
        System.out.println("Adding user " + user.getUsername() + " to peer " + id);
    }

    public void syncUsernameRegistry(Map<String, UUID> registry) {
        //TODO
        // In a real implementation, this would send the registry to the actual peer
        // For now, we'll just log it
        System.out.println("Syncing username registry with peer " + id);
    }

    // Other methods for peer-to-peer communication...
}
