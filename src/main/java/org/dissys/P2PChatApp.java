package org.dissys;

import org.dissys.network.Client;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class P2PChatApp {

    public static void main(String[] args){
        P2PChatApp app = new P2PChatApp();
        CLI cli = new CLI(app);
        Client client = new Client(app);

        app.setCLI(cli);
        app.setClient(client);

        client.start();
        cli.start();

    }
    private Client client;
    private CLI cli;
    private Map<UUID, Room> rooms;
    public P2PChatApp(){
        this.rooms = new ConcurrentHashMap<>();
    }

    /*public boolean proposeUsername(String username){
        return client.
    }*/
    /*
    public boolean setUsername(String username) {
        boolean success = client.proposeUsername(username);
        if (success) {
            this.localUser = new User(username);
        }
        return success;
    }

    //might not need, depends on peer discovery implementation

    public void connectToPeer(InetAddress address, int port) {
        localNode.connectToPeer(address, port);
    }

    //probably wrong
    public Room createRoom(String roomName, Set<String> participantUsernames) {
        Set<UUID> participantIds = localNode.getUserIds(participantUsernames);
        participantIds.add(localUser.getId());  // Ensure local user is in the room
        //save UUID or usernames?
        Room newRoom = new Room(UUID.randomUUID().toString(), localUser.getUserId().toString(), );
        rooms.put(newRoom.getId(), newRoom);

        // Notify other participants
        localNode.notifyRoomCreation(newRoom);

        return newRoom;
    }
    public void handleRoomCreationNotification(Room room) {
        rooms.put(room.getId(), room);
        System.out.println("Added to new room: " + room.getName());
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
    */
    private void setClient(Client client) {
        this.client = client;
    }

    private void setCLI(CLI cli) {
        this.cli = cli;
    }

    public Client getClient() {
        return client;
    }
}
