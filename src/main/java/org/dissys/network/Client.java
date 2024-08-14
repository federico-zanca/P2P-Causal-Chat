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
    private static final int MAX_MSG_CACHE_SIZE = 100;
    private final UUID uuid;
    private final InetAddress group;
    private final P2PChatApp app;
    private Map<UUID, Long> connectedPeers;
    private MulticastSocket multicastSocket;
    private NetworkInterface networkInterface;
    private static final Logger logger = LoggerConfig.getLogger();
    private Map<UUID, Boolean> processedMessages;

    public Client(P2PChatApp app){
        this.app = app;
        uuid = UUID.randomUUID();
        this.connectedPeers = new ConcurrentHashMap<>();
        try {
            group = InetAddress.getByName(MULTICAST_ADDRESS);
            connectToGroup(group, PORT);
        } catch (IOException e) {
            throw new RuntimeException("unable to connect to group");
        }

        processedMessages = new LinkedHashMap<UUID, Boolean>(MAX_MSG_CACHE_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<UUID, Boolean> eldest) {
                return size() > MAX_MSG_CACHE_SIZE;
            }
        };
    }
    public void start(){
        // Start listening for peer messages
        new Thread(this::receiveMessages).start();

        // Start sending periodic heartbeat
        new Thread(this::sendPeriodicHeartbeat).start();

        // Start a thread to remove stale peers
        new Thread(this::removeInactivePeers).start();

        // Send initial discovery message
        sendDiscoveryMessage();

        // Ask if reconnecting, may remove it later when persistence is added
        //askIfReconnecting();


    }
/*
    private void askIfReconnecting() {
        System.out.println("Are you reconnecting to the chat? (y/N)");
        Scanner scanner = new Scanner(System.in);
        String answer = scanner.nextLine();
        if (answer.trim().equalsIgnoreCase("y")) {



            // begin of fake part for simulation purposes
            System.out.print("Metti l'id della room per cui stai simulando la riconnessione -> ");
            String roomId = scanner.nextLine().trim();
            UUID uuid = UUID.fromString(roomId);
            Set<String> participants = new HashSet<>();
            participants.add(username);
            participants.add("amuro");
            Room fakeroom = new Room(uuid, "pizza", uuid, participants);
            rooms.put(uuid, fakeroom);
            // end of fake part



            ReconnectionRequestMessage message = new ReconnectionRequestMessage(uuid, username, new ArrayList<>(rooms.values()));
            sendMessage(message);
        }
    }*/

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
        DiscoveryMsg discoveryMsg = new DiscoveryMsg(uuid, app.getUsername());
        sendMessage(discoveryMsg);
    }
    private void sendPeriodicHeartbeat() {
        while (!Thread.currentThread().isInterrupted()) {
            HeartbeatMsg heartbeatMsg = new HeartbeatMsg(uuid, app.getUsername());
            sendMessage(heartbeatMsg);
            try {
                Thread.sleep(HEARTBEAT_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }


    public void sendMessage(Message message) {
        if(!(message instanceof HeartbeatMsg)){
            //System.out.println("Sending " + message);
            logger.info("Sending " + message);
        }
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
        while (!Thread.currentThread().isInterrupted()) {
            byte[] buffer = new byte[8192]; // Increased buffer size for serialized objects
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            try {
                multicastSocket.receive(packet);
                ByteArrayInputStream bais = new  ByteArrayInputStream(packet.getData());
                ObjectInputStream ois = new ObjectInputStream(bais);

                Message message = (Message) ois.readObject();

                // Check if the message has already been processed
                if (!processedMessages.containsKey(message.getMessageUUID())) {
                    processedMessages.put(message.getMessageUUID(), true);
                    processMessage(message);
                }

                ois.close();
                bais.close();

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                break;
            }
        }
    }
    public void processMessage(Message message) {
        if(!message.getSenderId().equals(uuid)){
            if( !(message instanceof HeartbeatMsg)){
                //System.out.println("Processing " + message);
                logger.info("Processing " + message);
            }
            message.onMessage(this);
        }
    }

    public void updatePeerList(UUID peerId) {
        connectedPeers.put(peerId, System.currentTimeMillis());
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

/*
    public void processReconnectionRequestMessage(ReconnectionRequestMessage message) {
        Map<UUID, VectorClock> requestedRoomsByMessageClocks = message.getRoomsClocks();
        boolean needsUpdate = false;
        List<Message> bundleOfMessagesOtherNeeds = null;
        List<Room> roomsToUpdate = null;
        VectorClock localRoomClock = null;

        //TODO check if I am in a room in which the requester is but he doesn't know -> he lost RoomCreationMessage

        for ( UUID reqRoomId : requestedRoomsByMessageClocks.keySet()){
            System.out.println("Retrieving messages for room " + reqRoomId);
            bundleOfMessagesOtherNeeds = new ArrayList<>();
            roomsToUpdate = new ArrayList<>();

            needsUpdate = false;
            Room reqRoom = rooms.get(reqRoomId);

            if(reqRoom == null) {
                continue;
            }

            localRoomClock = reqRoom.getLocalClock();

            for(ChatMessage deliveredMessage : reqRoom.getDeliveredMessages()) {
                if (deliveredMessage.getVectorClock().isAheadOf(requestedRoomsByMessageClocks.get(reqRoomId))) {
                    bundleOfMessagesOtherNeeds.add(deliveredMessage);
                }
                if (localRoomClock.isBehindOf(requestedRoomsByMessageClocks.get(reqRoomId))) {
                    // flags this room to be added to list of rooms that need to be updated (need to ask other peers to send missing messages)
                    needsUpdate = true;
                }
            }

            if(needsUpdate) {
                roomsToUpdate.add(reqRoom);
            }

            // send missing messages to requesting peer
            // TODO : do it after random amount of time and check if anyone else already sent it before sending
            if(!bundleOfMessagesOtherNeeds.isEmpty()) {
                ReconnectionReplyMessage replyMessage = new ReconnectionReplyMessage(uuid, username, bundleOfMessagesOtherNeeds);
                sendMessage(replyMessage);
            }

            //TODO not fully tested yet
            if(!roomsToUpdate.isEmpty()) {
                ReconnectionRequestMessage askForUpdateMessage = new ReconnectionRequestMessage(uuid, username, roomsToUpdate);
                sendMessage(askForUpdateMessage);
            }
        }

        // craft ReconnectionRequestMessage for rooms that need to be updated


    }*/
/*
    public void processReconnectionReplyMessage(ReconnectionReplyMessage reconnectionReplyMessage) {
        List<Message> messages = reconnectionReplyMessage.getLostMessages();
        for(Message message : messages){
            processMessage(message);
        }
    }*/

    public P2PChatApp getApp(){
        return app;
    }

    public Logger getLogger() {
        return logger;
    }
}


