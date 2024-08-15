package org.dissys;

import org.dissys.messages.ChatMessage;
import org.dissys.messages.Message;
import org.dissys.messages.RoomCreationMessage;
import org.dissys.network.Client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.dissys.Protocols.UsernameProposal.proposeUsername;

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
    private Map<UUID, String> usernameRegistry;
    private String username = null;
    public P2PChatApp(){
        this.rooms = new ConcurrentHashMap<>();
        this.usernameRegistry = new ConcurrentHashMap<>();
    }


    private void setClient(Client client) {
        this.client = client;
    }

    private void setCLI(CLI cli) {
        this.cli = cli;
    }

    public Client getClient() {
        return client;
    }
    public boolean isUsernameSet(){
        return !(username == null);
    }
    public String getUsername(){
        return username;
    }
    public Map<UUID, String> getUsernameRegistry(){
        return usernameRegistry;
    }
    public boolean proposeUsernameToPeers(String username) {
        return proposeUsername(username, client, this);
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void updateUsernameRegistry(String updatedUsername, UUID senderId) {
        usernameRegistry.put(senderId, updatedUsername);
        // Optionally, you can update any UI components or notify the user
        // about the username change
        //TODO questo sta intasando il log
        client.getLogger().info("username " + updatedUsername + " from " + senderId + " was put in the usernameRegistry of " + username);
    }

    public void createRoom(String roomName, Set<String> participants) {
        //System.out.println("Now in client.createRoom - Creating room: " + roomName);
        UUID roomId = UUID.randomUUID();
        participants.add(username);
        String roomMulticastIP = generateMulticastIP();
        Room room = new Room(roomId, roomName, client.getUUID(), participants, roomMulticastIP);

        try {
            InetAddress roomMulticastGroup = InetAddress.getByName(room.getMulticastIP());
            MulticastSocket roomSocket = client.connectToGroup(roomMulticastGroup, client.getPort());
            room.setRoomMulticastSocket(roomSocket);
            room.setRoomMulticastGroup(roomMulticastGroup);

            client.getSockets().put(roomMulticastIP, roomSocket);
            //System.out.println("Sockets " + client.getSockets());
        } catch (IOException e){
            throw new RuntimeException("unable to connect to group for room " + room.getRoomId() + room.getRoomName() );
        }

        System.out.println("Created room " + room.getRoomName() + " with IP " + room.getMulticastIP());
        rooms.put(roomId, room);
        client.sendMessage(new RoomCreationMessage(client.getUUID(), username, roomId, roomName, participants, roomMulticastIP));
    }

    public void processRoomCreationMessage(RoomCreationMessage message) {
        if(!message.getParticipants().contains(username)){

            //System.out.println("My username: " + username);
            //System.out.println("Participants:");

            //for (String participant : message.getParticipants()) {

            //    System.out.println(participant + "->" +(participant.equals(username)));
            //}

            //logger.info("Room creation message not for me: " + message);
            System.out.println("Room creation message not for me: " + message);
            return;
        }
        UUID roomId = message.getRoomId();

        if(rooms.containsKey(roomId)){
            System.out.println("Room already exists: " + roomId);
            return;
        }
        Room room = new Room(roomId, message.getRoomName(), client.getUUID(), message.getParticipants(), message.getMulticastIP());
        try {
            InetAddress roomMulticastGroup = InetAddress.getByName(message.getMulticastIP());
            MulticastSocket roomSocket = client.connectToGroup(roomMulticastGroup, client.getPort());
            room.setRoomMulticastSocket(roomSocket);
            room.setRoomMulticastGroup(roomMulticastGroup);

            client.getSockets().put(message.getMulticastIP(), roomSocket);
        } catch (IOException e){
            throw new RuntimeException("unable to connect to group for room " + message.getRoomId() + message.getRoomName() );
        }
        System.out.println("Created room " + room.getRoomName() + " with IP " + room.getMulticastIP());
        rooms.put(roomId, room);
        System.out.println("Added new room: " + room.getRoomName() + " with id " + roomId + " and multicast IP " + room.getMulticastIP());
        //System.out.println("Sockets " + client.getSockets());

        cli.notifyRoomInvite(room);
        //TODO acknowledge participants that you are in the room to track who knows about the room
    }

    public void sendMessageInChat(String roomName, String content) {
        Room room = null;
        for(Room r: rooms.values()){
            if(r.getRoomName().equals(roomName)){
                room = r;
                break;
            }
        }
        //if the room is not found, print "Room not found: " + roomName
        if (room == null) {
            System.out.println("Room not found: " + roomName);
            return;
        }


        room.sendChatMessage(client, username, content);
        System.out.println("Message sent to " + roomName);

        //VectorClock clock = room.getLocalClock();
        //clock.incrementClock(username); //ATTENZIONE!! Se l'invio del messaggio non va a buon fine il clock rimarrà incrementato e sarebbe un bordello
        //ChatMessage message = new ChatMessage(uuid, username, room.getRoomId(), content, clock);


        //sendMessage(message);



    }
    public void processChatMessage(UUID roomId, ChatMessage message) {
        Room room;
        // Process the received chat message (e.g., update UI, maintain causal order)
        client.getLogger().info("Received message in room " + roomId + ": " + message.getContent());

        room = rooms.get(roomId);
        if (room == null) {
            //logger.info("Discarding message, room not found: " + roomId);
            System.out.println("Discarding message, room not found: " + roomId);
            return;
        }

        room.receiveMessage(message);

    }
    public void openRoom(String roomName) {
        Room room;
        // find room with name roomName in the HashTable<UUID, Room> rooms
        //iterate over the rooms and check if the roomName is equal to the roomName of the room using getRoomName()
        //if it is equal, assign the room to the room variable
        for (Room r : rooms.values()) {
            if (r.getRoomName().equals(roomName)) {
                room = r;
                room.viewRoomChat();
                return;
            }
        }
        //if the room is not found, print "Room not found: " + roomName
        System.out.println("Room not found: " + roomName);
    }

    private String generateMulticastIP() {
        // Generate a random multicast IP address in the range 224.0.0.0 to 239.255.255.255
        String generatedIP = "239.1.1.1";
        Random random = new Random();

        while (generatedIP.equals("239.1.1.1")){
            generatedIP = String.format("239.%d.%d.%d",
                    random.nextInt(256),
                    random.nextInt(256),
                    random.nextInt(256));
        }
        return generatedIP;
    }

    public Room getRoomByID(UUID uuid){
        return rooms.get(uuid);
    }
    public Room getRoomByName(String name){
        Room room = null;
        for(Room r: rooms.values()){
            if(r.getRoomName().equals(name)){
                room = r;
                break;
            }
        }
        return room;
    }
    public Set<String> getRoomsNames() {
        return rooms.values().stream().map(Room::getRoomName).collect(Collectors.toSet());
    }
    public Set<UUID> getRoomsIds() {
        return rooms.keySet();
        //return rooms.keySet().stream().map(UUID::toString).collect(Collectors.toSet());
    }
    public Set<String> getRoomsIdsAndNames() {
        return rooms.entrySet().stream().map(e -> e.getValue().getRoomName() + "  (" + e.getKey().toString() + ")").collect(Collectors.toSet());
    }

    public void addRoom(UUID uuid, Room room) {
        rooms.put(uuid, room);
    }
    public ArrayList<Room> getRoomsValuesAsArrayList(){
        return new ArrayList<>(rooms.values());
    }

    public UUID getUUID() {
        return client.getUUID();
    }

    public void sendMessage(Message message) {
        client.sendMessage(message);
    }
}
