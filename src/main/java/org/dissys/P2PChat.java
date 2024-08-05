package org.dissys;

import java.io.IOException;
import java.util.List;

public class P2PChat {
    private P2PNode node;
    private String currentRoom;

    public P2PChat(int port) throws IOException {
        this.node = new P2PNode(port);
        node.start();
    }

    public void createRoom(String roomName) {
        // Implement room creation logic
        // For now, let's just store it locally
        node.createRoom(roomName);
    }

    public void joinRoom(String roomName) {
        // Implement room joining logic
        if (node.roomExists(roomName)) {
            currentRoom = roomName;
        } else {
            throw new IllegalArgumentException("Room does not exist: " + roomName);
        }
    }

    public void sendMessage(String roomName, String message) {
        if (currentRoom == null || !currentRoom.equals(roomName)) {
            throw new IllegalStateException("You must join the room before sending a message.");
        }
        node.sendMessage(roomName, message);
    }

    public List<String> listRooms() {
        return node.listRooms();
    }
}
