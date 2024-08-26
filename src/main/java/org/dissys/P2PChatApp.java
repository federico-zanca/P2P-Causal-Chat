package org.dissys;

import org.dissys.CLI.CLI;
import org.dissys.CLI.Input.RoomCreated;
import org.dissys.CLI.Input.RoomInvitation;
import org.dissys.CLI.Input.RoomMessage;
import org.dissys.CLI.State.InRoomState;
import org.dissys.Protocols.Username.Username;
import org.dissys.Protocols.Username.UsernameProposal;
import org.dissys.messages.*;
import org.dissys.network.Client;
import org.dissys.utils.AppState;
import org.dissys.utils.PersistenceManager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.dissys.Protocols.ReconnectionProtocol.retrieveLostMessages;
import static org.dissys.Protocols.Username.UsernameProposal.proposeUsername;

public class P2PChatApp {
    private Client client;
    private CLI cli;
    private Map<UUID, Room> rooms;
    private Map<UUID, String> usernameRegistry;
    private static Username username = null;
    private static Username proposedUsername = null;


    public P2PChatApp(){
        this.rooms = new ConcurrentHashMap<>();
        this.usernameRegistry = new ConcurrentHashMap<>();
    }

    public static void main(String[] args){
        P2PChatApp app = new P2PChatApp();
        Client client = null;
        try {
            client = new Client(app);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        app.setClient(client);

        app.initialize();
        CLI cli = new CLI(app);

        app.setCLI(cli);

        cli.printAsciiArtTitle();

        //if a username was never set ask the user to choose one
        if(username == null){
            proposedUsername = cli.askForUsername();
            try {
                client.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            while (!app.proposeUsernameToPeers(proposedUsername)){
                cli.printWarning("Username taken, retry with a different username");
                proposedUsername = cli.askForUsername();
            }
        }else {
            try {
                client.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        retrieveLostMessages(app);

        cli.start();

        // Add shutdown hook to save state when the application exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            PersistenceManager.saveState(app);
            System.out.println("SALVANDO");
        }));
    }

    private void initialize() {
        AppState state = PersistenceManager.loadState();

        if (state != null) {
            this.username = state.getUsername();
            this.rooms = new ConcurrentHashMap<>();
            for (Room room : state.getRooms()) {
                this.rooms.put(room.getRoomId(), room);
                // Reconnect the room's MulticastSocket
                try {
                    InetAddress group = InetAddress.getByName(room.getMulticastIP());

                    System.out.println("Reconnecting to room " + room.getRoomName() + " with IP " + room.getMulticastIP());
                    System.out.println("Inet address group = " + group);

                    MulticastSocket socket = client.connectToGroup(group, client.getPort(), room.getMulticastIP());
                    System.out.println("Opened Socket = " + socket);
                    room.reconnect(socket, group);
                } catch (IOException e) {
                    System.out.println("Failed to reconnect to room " + room.getRoomName() + ": " + e.getMessage());
                }
                // The client UUID will be set when the Client is created
            }
            this.usernameRegistry = new ConcurrentHashMap<>(state.getUsernameRegistry());



        } else {
            this.rooms = new ConcurrentHashMap<>();
            this.usernameRegistry = new ConcurrentHashMap<>();
        }
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
    public Username getUsername(){
        return username;
    }
    public Map<UUID, String> getUsernameRegistry(){
        return usernameRegistry;
    }
    public boolean proposeUsernameToPeers(Username username) {
        return proposeUsername(username, client, this);
    }

    public void setUsername(Username username) {
        this.username = username;
    }

    public void updateUsernameRegistry(String updatedUsername, UUID senderId) {
        // Optionally, you can update any UI components or notify the user
        // about the username change
        if(usernameRegistry.get(senderId) == null){
            client.getLogger().info("username " + updatedUsername + " from " + senderId + " was put in the usernameRegistry of " + usernameRegistry.get(senderId));
        }
        //client.getLogger().info("username " + updatedUsername + " from " + senderId + " was put in the usernameRegistry of " + username);
        usernameRegistry.put(senderId, updatedUsername);
    }

    public void createRoom(String roomName, Set<String> participants) {
        //System.out.println("Now in client.createRoom - Creating room: " + roomName);
        UUID roomId = UUID.randomUUID();
        participants.add(username.toString());

        if(participants.size() == 1){
            System.out.println("Cannot create a room with only one participant!");
            return;
        }

        System.out.println("Participants: " + participants);
        String roomMulticastIP = generateMulticastIP();
        Room room = new Room(roomId, roomName, client.getUUID(), participants, roomMulticastIP);

        try {
            InetAddress roomMulticastGroup = InetAddress.getByName(room.getMulticastIP());
            MulticastSocket roomSocket = client.connectToGroup(roomMulticastGroup, client.getPort(), roomMulticastIP);
            room.setRoomMulticastSocket(roomSocket);
            room.setRoomMulticastGroup(roomMulticastGroup);

            //client.getSockets().put(roomMulticastIP, roomSocket);
            //System.out.println("Sockets " + client.getSockets());
        } catch (IOException e){
            throw new RuntimeException("unable to connect to group for room " + room.getRoomId() + room.getRoomName() );
        }

        System.out.println("Created room " + room.getRoomName() + " with IP " + room.getMulticastIP());
        rooms.put(roomId, room);
        client.sendMulticastMessage(new RoomCreationMessage(client.getUUID(), username.toString(), roomId, roomName, participants, roomMulticastIP, false));

        cli.handleInput(new RoomCreated());

    }

    public void processRoomCreationMessage(RoomCreationMessage message) {
        if(!message.getParticipants().contains(username)){
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
            MulticastSocket roomSocket = client.connectToGroup(roomMulticastGroup, client.getPort(), message.getMulticastIP());
            room.setRoomMulticastSocket(roomSocket);
            room.setRoomMulticastGroup(roomMulticastGroup);

            //client.getSockets().put(message.getMulticastIP(), roomSocket);
        } catch (IOException e){
            throw new RuntimeException("unable to connect to group for room " + message.getRoomId() + message.getRoomName() );
        }
        System.out.println("Created room " + room.getRoomName() + " with IP " + room.getMulticastIP());
        rooms.put(roomId, room);
        System.out.println("Added new room: " + room.getRoomName() + " with id " + roomId + " and multicast IP " + room.getMulticastIP());

        if (message.isLost()) {
            // Send a ReconnectionRequestMessage to the room creator
            ArrayList<Room> oneRoom = new ArrayList<>();
            oneRoom.add(room);
            ReconnectionRequestMessage reconnectionRequestMessage = new ReconnectionRequestMessage(client.getUUID(), username.toString(), oneRoom);
            client.sendMulticastMessage(reconnectionRequestMessage, room.getRoomMulticastSocket(), room.getRoomMulticastGroup());
        }

        //System.out.println("Sockets " + client.getSockets());
        cli.handleInput(new RoomInvitation());

    }

    public void sendMessageInChat(String roomName, String content) {
        Room room = null;
        List<Room> candidateRooms = new ArrayList<>();
        for(Room r: rooms.values()){
            if(r.getRoomName().equals(roomName)){
                candidateRooms.add(r);
            }
        }

        //if the room is not found, print "Room not found: " + roomName
        if (candidateRooms.isEmpty()) {
            System.out.println("Room not found: " + roomName);
            return;
        } else if (candidateRooms.size() == 1){
            room = candidateRooms.get(0); //cambiato da .getFirst() a get(0)
        } else {
            System.out.println("Multiple rooms found with name " + roomName + ". Please select one of the following rooms:");
            for (int i = 0; i < candidateRooms.size(); i++) {
                System.out.println(i + ". " + candidateRooms.get(i).getRoomName() + "  (" + candidateRooms.get(i).getRoomId() + ")");
            }
            int choice = cli.askForRoomChoice(candidateRooms.size());
            room = candidateRooms.get(choice);
        }


        room.sendChatMessage(client, username.toString(), content);
        System.out.println("Message sent to " + roomName);

        //VectorClock clock = room.getLocalClock();
        //clock.incrementClock(username); //ATTENZIONE!! Se l'invio del messaggio non va a buon fine il clock rimarrà incrementato e sarebbe un bordello
        //ChatMessage message = new ChatMessage(uuid, username, room.getRoomId(), content, clock);


        //sendMessage(message);

        cli.handleInput(new RoomMessage(room.getRoomId()));

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
        cli.handleInput(new RoomMessage(roomId));

    }
    public void openRoom(String roomName) {
        List<Room> candidateRooms = new ArrayList<>();

        for (Room r : rooms.values()) {
            if (r.getRoomName().equals(roomName)) {
                candidateRooms.add(r);
            }
        }

        if(candidateRooms.isEmpty()){
            System.out.println("Room not found: " + roomName);
        } else if (candidateRooms.size() == 1){
            cli.setCliState(new InRoomState(candidateRooms.get(0).getRoomId())); // cambiato da getFirst a get(0)
        } else {
            System.out.println("Multiple rooms found with name " + roomName + ". Please select one of the following rooms:");
            for (int i = 0; i < candidateRooms.size(); i++) {
                System.out.println(i + ". " + candidateRooms.get(i).getRoomName() + "  (" + candidateRooms.get(i).getRoomId() + ")");
            }
            int choice = cli.askForRoomChoice(candidateRooms.size());
            cli.setCliState(new InRoomState(candidateRooms.get(choice).getRoomId()));
        }
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
    public List<UUID> getUUIDsMatchedToUsername(String username){
        List<UUID> keys = new ArrayList<>();

        // Iterate over the entries of the hashtable
        for (Map.Entry<UUID, String> entry : usernameRegistry.entrySet()) {
            // Check if the value matches the desired value
            if (entry.getValue().equals(username)) {
                // If it matches, add the key to the list
                keys.add(entry.getKey());
            }
        }

        return keys;
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
        client.sendMulticastMessage(message);
    }

    public Map<UUID, Room> getRooms() {
        return rooms;
    }

    public List<Room> getRoomsAsList() {
        return new ArrayList<>(rooms.values());
    }

    public Username getProposedUsername() {
        return proposedUsername;
    }

    public void setProposedUsername(Username proposedUsername) {
        this.proposedUsername = proposedUsername;
    }

    public CLI getCli() {
        return cli;
    }

    public void printSockets(){
        Map<String, MulticastSocket> sockets = client.getSockets();
        for (Map.Entry<String, MulticastSocket> entry : sockets.entrySet()) {
            System.out.println(entry.getKey() + " " + entry.getValue() + " " + entry.getValue().getInetAddress());
        }
    }
}


/*
    Things to test:
    Username protocol
    Message delivery
    Persistence
    Reconnection protocol
    Network partition

    - corretta gestione riconnessioni
    - ottimizzazioni sul traffico della rete -> magari unicast per recupero messaggi
    - corretta gestione pacchetti persi
    - corretta gestione della delete e create (gestione caso in cui il comando non sia ricevuto da tutti gli interessati)
    TODO
        conflict resolution for usernames
        gossip protocol for updating and converging usernames

        dht for rooms
        delete rooms

        DONE
        home command
        random 4 digit code for usernames

 */
