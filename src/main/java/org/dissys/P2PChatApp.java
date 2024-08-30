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
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.dissys.Protocols.ReconnectionProtocol.retrieveLostMessages;
import static org.dissys.Protocols.Username.UsernameProposal.proposeUsername;
import static org.dissys.Protocols.Username.UsernameProposal.usernameProtocol;

public class P2PChatApp {
    private Client client;
    private CLI cli;
    private Map<UUID, Room> rooms;
    private final Map<UUID, Room> deletedRooms;
    private Map<UUID, String> usernameRegistry;
    private static Username username = null;
    public P2PChatApp(){
        this.rooms = new ConcurrentHashMap<>();
        this.usernameRegistry = new ConcurrentHashMap<>();
        this.deletedRooms = new ConcurrentHashMap<>();
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
        System.out.println("1");
        app.initialize();
        CLI cli = new CLI(app);

        app.setCLI(cli);

        cli.printAsciiArtTitle();

        usernameProtocol(app);

        retrieveLostMessages(app);
        System.out.println("3 " + P2PChatApp.username);
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
            P2PChatApp.username = new Username(state.getUsername(), state.getCode()) ;
            System.out.println("2 :" + P2PChatApp.username);
            synchronized (deletedRooms) {
                //deletedRooms.addAll(state.getDeletedRooms());
                for (Room delRoom : state.getDeletedRooms()){
                    this.deletedRooms.put(delRoom.getRoomId(), delRoom);
                    // Reconnect the room's MulticastSocket
                    try {
                        InetAddress group = InetAddress.getByName(delRoom.getMulticastIP());

                        System.out.println("Reconnecting to room " + delRoom.getRoomName() + " with IP " + delRoom.getMulticastIP() + " (room is deleted but nobody knows yet)");
                        System.out.println("Inet address group = " + group);

                        MulticastSocket socket = client.connectToGroup(group, client.getPort(), delRoom.getMulticastIP());
                        System.out.println("Opened Socket = " + socket);
                        delRoom.reconnect(socket, group);
                    } catch (IOException e) {
                        System.out.println("Failed to reconnect to room " + delRoom.getRoomName() + ": " + e.getMessage());
                    }
                    // The client UUID will be set when the Client is created
                }
            }
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
    public String getStringUsername(){
        if(username == null){
            return null;
        }else {
            return username.toString();
        }
    }
    public Map<UUID, String> getUsernameRegistry(){
        return usernameRegistry;
    }
    public boolean proposeUsernameToPeers(Username username) {
        return proposeUsername(username, client, this);
    }

    public void setUsername(Username username) {
        P2PChatApp.username = username;
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
        participants.add(getStringUsername());

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
        client.sendMulticastMessage(new RoomCreationMessage(client.getUUID(),getStringUsername(), roomId, roomName, participants, roomMulticastIP, false));

        cli.handleInput(new RoomCreated());

    }

    public void processRoomCreationMessage(RoomCreationMessage message) {
        if(!message.getParticipants().contains(username.toString())){
            //logger.info("Room creation message not for me: " + message);
            System.out.println("Room creation message not for me: " + message);
            return;
        }
        UUID roomId = message.getRoomId();
        if(deletedRooms.get(roomId)!=null){
            Room deletedRoom = deletedRooms.get(roomId);
            ChatMessage leaveRoomReminder = new ChatMessage(client.getUUID(), username.toString(), deletedRoom.getRoomId(), deletedRoom.getLocalClock(), true);
            client.sendMulticastMessage(leaveRoomReminder, deletedRoom.getRoomMulticastSocket(), deletedRoom.getRoomMulticastGroup());
            return;
        }
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
            ReconnectionRequestMessage reconnectionRequestMessage = new ReconnectionRequestMessage(client.getUUID(), getStringUsername(), oneRoom, deletedRooms);
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


        room.sendChatMessage(client, getStringUsername(), content);
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
        } else if (deletedRooms.get(roomId)!=null){
            System.out.println("Received message in deleted room: " + roomId + room.getRoomName());
            return;
        }

        room.receiveMessage(message);
        if(message.isFarewell()){
            LeaveRoomACK ack = new LeaveRoomACK(client.getUUID(), room.getRoomId(), message.getSender());
            client.sendMulticastMessage(ack, room.getRoomMulticastSocket(), room.getRoomMulticastGroup());
        }
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

    public CLI getCli() {
        return cli;
    }

    public void printSockets(){
        Map<String, MulticastSocket> sockets = client.getSockets();
        for (Map.Entry<String, MulticastSocket> entry : sockets.entrySet()) {
            System.out.println(entry.getKey() + " " + entry.getValue() + " " + entry.getValue().getInetAddress());
        }
    }

    public void leaveRoom(String roomName) {
        ChatMessage message;
        List<Room> candidates = new ArrayList<>();
        Scanner scanner = new Scanner(System.in);
        int choice;
        Room room;
        int i;

        for (Map.Entry<UUID, Room> r : rooms.entrySet()) {
            if(r.getValue().getRoomName().equals(roomName)){
                candidates.add(r.getValue());
            }
        }

        if(candidates.isEmpty()){
            System.out.println("Room not found: " + roomName);
            return;
        } else if(candidates.size() >= 2){
            System.out.println("Multiple rooms found with name " + roomName);
            System.out.println("Which one do you want to leave?");
            i = 0;
            for (Room r : candidates) {
                System.out.println(i + ". " + r.getRoomName() + " (" + r.getRoomId() + ")");
                i += 1;
            }

            // parse integer from string
            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch(NumberFormatException e){
                System.out.println("Invalid choice");
                return;
            }

            room = candidates.get(choice);
            if(room == null) {
                System.out.println("Invalid choice");
                return;
            }
        } else {
            room = candidates.getFirst();
        }

        room.getLocalClock().incrementClock(username.toString());
        message = new ChatMessage(client.getUUID(), username.toString(), room.getRoomId(), room.getLocalClock(), true);
        client.sendMulticastMessage(message, room.getRoomMulticastSocket(), room.getRoomMulticastGroup());
        try{
            Thread.sleep(1000);
        } catch(InterruptedException e){
            e.printStackTrace();
        }
        rooms.remove(room.getRoomId());
        synchronized (deletedRooms) {
            deletedRooms.put(room.getRoomId(), room);
        }
        System.out.println("You left room " + room.getRoomName() + " (" + room.getRoomId() + ")");
    }

    public Map<UUID, Room> getDeletedRooms() {
        return deletedRooms;
    }

    public List<Room> getDeletedRoomsAsList(){
        return new ArrayList<>(deletedRooms.values());
    }

    public void processLeaveRoomACK(LeaveRoomACK message) {
        Room deletedRoom = deletedRooms.get(message.getRoomId());
        if(!message.getLeavingUser().equals(username.toString()) || deletedRoom == null) {
            return;
        }
        deletedRooms.remove(message.getRoomId());
        client.closeRoomSocketIfUnused(deletedRoom);
        //deletedRooms.remove(message.getRoomId());
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
