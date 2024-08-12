package org.dissys.network;
import org.dissys.P2PChatApp;
import org.dissys.Room;
import org.dissys.VectorClock;
import org.dissys.messages.*;
import org.dissys.utils.LoggerConfig;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

//might need to make observable
public class Client {
    private static final String MULTICAST_ADDRESS = "239.1.1.1";
    private static final int PORT = 5000;
    private static final long HEARTBEAT_INTERVAL = 5000; // 5 seconds
    private static final long PEER_TIMEOUT = 15000; // 15 seconds
    private final UUID uuid;
    private final InetAddress group;
    private P2PChatApp app;
    private String username;
    private Map<String, UUID> usernameRegistry;
    private Map<UUID, Long> connectedPeers;
    private MulticastSocket multicastSocket;
    private NetworkInterface networkInterface;
    private static final Logger logger = LoggerConfig.getLogger();
    private Map<UUID, Room> rooms;

    public Client(P2PChatApp app){
        this.app = app;
        uuid = UUID.randomUUID();
        this.usernameRegistry = new ConcurrentHashMap<>();
        this.connectedPeers = new ConcurrentHashMap<>();
        this.rooms = new ConcurrentHashMap<>();
        try {
            group = InetAddress.getByName(MULTICAST_ADDRESS);
            connectToGroup(group, PORT);
        } catch (IOException e) {
            throw new RuntimeException("unable to connect to group");
        }



        /*
        // dummy room for testing
        Set<String> dummyparts = new HashSet<>();
        dummyparts.add("dummy");
        dummyparts.add("dummier");

        Room dummy = new Room(UUID.randomUUID(), "dummy", UUID.randomUUID(), dummyparts);
        rooms.put(dummy.getRoomId(), dummy);
        */
    }
    public void start(){
        username = askForUsername();

        // Start listening for peer messages
        new Thread(this::receiveMessages).start();

        // Start sending periodic heartbeat
        new Thread(this::sendPeriodicHeartbeat).start();

        // Start a thread to remove stale peers
        new Thread(this::removeInactivePeers).start();

        // Send initial discovery message
        sendDiscoveryMessage();
    }

    private String askForUsername() {
        Scanner scanner = new Scanner(System.in);
        String username = "";

        while (true) {
            System.out.print("Please enter your username: ");
            username = scanner.nextLine();

            // Check if username is not empty and only contains alphanumeric characters
            if (isValidUsername(username)) {
                break;
            } else {
                System.out.println("Invalid username. It should be non-empty and only contain letters and numbers.");
            }
        }

        // Parse and print the username in a decent format
        username = parseUsername(username);

        return username;
    }

    // Method to validate the username
    private static boolean isValidUsername(String username) {
        return username != null && !username.trim().isEmpty() && username.matches("^[a-zA-Z0-9]+$");
    }

    // Method to parse the username (e.g., capitalize the first letter)
    private static String parseUsername(String username) {
        if (username.length() > 1) {
            return username.substring(0, 1).toUpperCase() + username.substring(1).toLowerCase();
        } else {
            return username.toUpperCase();
        }
    }

    private void connectToGroup(InetAddress groupAddress, int port) throws IOException {

        multicastSocket = new MulticastSocket(port);
        networkInterface = findNetworkInterface();
        if (networkInterface == null) {
            throw new IOException("no suitable network interface found");
        }
        multicastSocket.joinGroup(new InetSocketAddress(groupAddress, port), networkInterface);
        System.out.println("connected to multicast socket " + multicastSocket.getLocalSocketAddress() + " with port " + multicastSocket.getLocalPort());

    }
    private void leaveGroup() throws IOException {
        multicastSocket.leaveGroup(new InetSocketAddress(group, PORT), networkInterface);
        multicastSocket.close();
    }

    private void sendDiscoveryMessage() {
        DiscoveryMsg discoveryMsg = new DiscoveryMsg(uuid);
        sendMessage(discoveryMsg);
    }
    private void sendPeriodicHeartbeat() {
        while (!Thread.currentThread().isInterrupted()) {
            HeartbeatMsg heartbeatMsg = new HeartbeatMsg(uuid);
            sendMessage(heartbeatMsg);
            try {
                Thread.sleep(HEARTBEAT_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }


    public void sendMessage(Message message) {
        //System.out.println("Sending " + message);
    logger.info("Sending " + message);
        try {
            // Serialize the Message object
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(message);
            oos.flush();

            // Get the byte array of the serialized object
            byte[] buffer = baos.toByteArray();

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            multicastSocket.send(packet);

            // Close the streams
            oos.close();
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveMessages() {
        byte[] buffer = new byte[8192]; // Increased buffer size for serialized objects
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);


        while (!Thread.currentThread().isInterrupted()) {
            try {
                multicastSocket.receive(packet);
                ByteArrayInputStream bais = new  ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                ObjectInputStream ois = new ObjectInputStream(bais);

                Message message = (Message) ois.readObject();
                processMessage(message);

                ois.close();
                bais.close();

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                break;
            }
        }
    }
    private void processMessage(Message message) {
        if(!message.getSenderId().equals(uuid)){
            //System.out.println("Processing " + message);
            logger.info("Processing " + message);
            message.onMessage(this);
        }
        /*
        if (parts.length == 2) {
            String messageType = parts[0];
            String remotePeerId = parts[1];

            if (!remotePeerId.equals(uuid.toString())) {
                if (messageType.equals("DISCOVER") || messageType.equals("HEARTBEAT")) {
                    updatePeerList(UUID.fromString(remotePeerId)); //should do it anyway
                }

                if (messageType.equals("DISCOVER")) {
                    // Respond to discovery with an announcement
                    sendMessage("HEARTBEAT:" + uuid);
                }
                //da cambiare in una enum onMessage
            }
        }*/
    }

    public void updatePeerList(UUID peerId) {
        connectedPeers.put(peerId, System.currentTimeMillis());
        //System.out.println("Peer discovered/updated: " + peerId);
        logger.info("Peer discovered/updated: " + peerId);
    }
    private void removeInactivePeers() {
        while (!Thread.currentThread().isInterrupted()) {
            long now = System.currentTimeMillis();
            connectedPeers.entrySet().removeIf(entry ->
                    now - entry.getValue() > PEER_TIMEOUT);
            try {
                Thread.sleep(PEER_TIMEOUT / 2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    public void stop() {
        try {
            multicastSocket.leaveGroup(new InetSocketAddress(group, PORT), networkInterface);
            multicastSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private NetworkInterface findNetworkInterface() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            if (networkInterface.isUp() && !networkInterface.isLoopback() && networkInterface.supportsMulticast()) {
                return networkInterface;
            }
        }
        return null;
    }
    public UUID getUUID(){
        return uuid;
    }

    public void processChatMessage(UUID roomId, ChatMessage message) {
        Room room;
        // Process the received chat message (e.g., update UI, maintain causal order)
        logger.info("Received message in room " + roomId + ": " + message.getContent());

        room = rooms.get(roomId);
        if (room == null) {
            logger.info("Discarding message, room not found: " + roomId);
            return;
        }

        room.receiveMessage(message);

    }
    public Set<UUID> getRoomsIds() {
        return rooms.keySet();
    }

    public Set<String> getRoomsNames() {
        return rooms.keySet().stream().map(UUID::toString).collect(Collectors.toSet());
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

    public String getUsername() {
        return username;
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

        VectorClock clock = room.getLocalClock();
        ChatMessage message = new ChatMessage(uuid, username, room.getRoomId(), content, clock);

        sendMessage(message);

    }

    public void processRoomCreationMessage(RoomCreationMessage message) {
        if(!message.getParticipants().contains(username)){
            logger.info("Room creation message not for me: " + message);
            return;
        }
        UUID roomId = message.getRoomId();
        if(rooms.containsKey(roomId)){
            logger.info("Room already exists: " + roomId);
            return;
        }
        Room room = new Room(roomId, message.getRoomName(), uuid, message.getParticipants());
        rooms.put(roomId, room);
        logger.info("Added new room: " + room.getRoomName() + " with id " + roomId);

        //TODO acknowledge participants that you are in the room to track who knows about the room
    }


    //on start get peers list from memory, if empty perform peer discovery
    /*

    public boolean proposeUsername(String username, UUID uuid) {
        if (usernameRegistry.containsKey(username)) {
            return false;
        }

        boolean accepted = broadcastUsernameProposal( );

        if (accepted) {
            usernameRegistry.put(username, uuid);
            broadcastUsernameAccepted(usernameRegistry.get(username));
            return true;
        }

        return false;
    }


    private boolean broadcastUsernameProposal(User proposedUser) {
        List<Future<Boolean>> futures = new ArrayList<>();
        // might need to exclude the sender or maybe not TODO
        for (PeerSkeleton peer : connectedPeers.values()) {
            futures.add(executor.submit(() -> peer.voteOnUsername(proposedUser)));
        }

        try {
            for (Future<Boolean> future : futures) {
                if (!future.get(5, TimeUnit.SECONDS)) {
                    return false;
                }
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return false;
        }

        return true;
    }


    public boolean voteOnUsername(User proposedUser) {
        return !usernameRegistry.containsKey(proposedUser.getUsername());
    }


    private void broadcastUsernameAccepted(User acceptedUser) {
        for (PeerSkeleton peer : connectedPeers.values()) {
            executor.submit(() -> peer.addToUsernameRegistry(acceptedUser));
        }
    }


    public void addToUsernameRegistry(User user) {
        usernameRegistry.put(user.getUsername(), user.getId());
    }


    public void notifyRoomCreation(Room room) {
        for (UUID participantId : room.getParticipants()) {
            if (!participantId.equals(localUser.getId())) {
                PeerSkeleton peer = connectedPeers.get(participantId);
                if (peer != null) {
                    executor.submit(() -> peer.notifyRoomCreation(room));
                }
            }
        }
    }

    public void handleRoomCreationNotification(Room room) {
        // Forward to P2PChat
        p2pChat.handleRoomCreationNotification(room);
    }


    public void connectToPeer(InetSocketAddress inetSocketAddress) {
        PeerSkeleton newPeer = new PeerSkeleton(peerAddress, peerPort);
        UUID peerId = newPeer.getId();
        connectedPeers.put(peerId, newPeer);
        syncUsernameRegistry(newPeer);
    }

    private void syncUsernameRegistry(PeerSkeleton peer) {
        peer.syncUsernameRegistry(new HashMap<>(usernameRegistry));
    }


    public Map<String, UUID> getUsernameRegistry() {
        return new HashMap<>(usernameRegistry);
    }


    public void addPeer(PeerSkeleton peer) {
        connectedPeers.put(peer.getId(), peer);
    }


    public void removePeer(PeerSkeleton peer) {
        connectedPeers.remove(peer.getId());
    }

    public Set<UUID> getUserIds(Set<String> usernames) {
        return usernames.stream()
                .map(usernameRegistry::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
*/
}


