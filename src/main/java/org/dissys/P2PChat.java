package org.dissys;

import org.dissys.network.P2PNode;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

public class P2PChat {
    private P2PNode localNode;
    private String currentRoom;

    public P2PChat(InetAddress address, int port) {
        this.localNode = new P2PNode(address, port);
    }

    public boolean setUsername(String username) {
        return localNode.proposeUsername(username);
    }
    //might not need, depends on peer discovery implementation
    public void connectToPeer(InetAddress address, int port) {
        localNode.connectToPeer(address, port);
    }

    public void createRoom(String roomName) {
        //TODO
        // Implement room creation logic
        // For now, let's just store it locally
    }

    public void joinRoom(String roomName) {
        //TODO
        // Implement room joining logic
    }

    public void sendMessage(String roomName, String message) {
        //TODO
        // Implement message sending to a room
    }

    public List<String> listRooms() {
        //TODO
        // Implement listing of the rooms where the user is a participant
        return null;
    }
}
