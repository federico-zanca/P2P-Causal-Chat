package org.dissys.network;
import org.dissys.P2PChatApp;
import org.dissys.messages.*;
import org.dissys.utils.AppState;
import org.dissys.utils.LoggerConfig;
import org.dissys.utils.PersistenceManager;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

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
    private final Map<String, MulticastSocket> sockets;

    public MulticastSocket getMulticastSocket(){
        return this.multicastSocket;
    }

    public Client(P2PChatApp app){
        this.app = app;
        this.sockets = new ConcurrentHashMap<>();

        AppState state = PersistenceManager.loadState();
        if (state != null) {
            this.uuid = state.getClientUUID();
        } else {
            this.uuid = UUID.randomUUID();
        }

        this.connectedPeers = new ConcurrentHashMap<>();
        try {
            group = InetAddress.getByName(MULTICAST_ADDRESS);
            multicastSocket = connectToGroup(group, PORT);
        } catch (IOException e) {
            throw new RuntimeException("unable to connect to group");
        }

        sockets.put(MULTICAST_ADDRESS, multicastSocket);

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


    public MulticastSocket connectToGroup(InetAddress groupAddress, int port) throws IOException {

        MulticastSocket multicastSocket = new MulticastSocket(port);
        networkInterface = findNetworkInterface();
        if (networkInterface == null) {
            throw new IOException("no suitable network interface found");
        }
        multicastSocket.joinGroup(new InetSocketAddress(groupAddress, port), networkInterface);
        System.out.println("connected to multicast socket " + multicastSocket.getLocalSocketAddress() + " with port " + multicastSocket.getLocalPort());
        return multicastSocket;
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

    public void sendMessage(Message message){
        sendMessage(message, this.multicastSocket, this.group);
    }

    public void sendMessage(Message message, MulticastSocket socket, InetAddress group) {
        if(!(message instanceof HeartbeatMsg)){
            //System.out.println("Sending " + message);
            logger.info("Sending " + message + "\nROOMSOCKET= " + socket + "\n");
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
            socket.send(packet);

            // Close the streams
            oos.close();
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveMessages() {

        while (!Thread.currentThread().isInterrupted()) {
            for(Map.Entry<String, MulticastSocket> entry : sockets.entrySet()){
                try {
                    MulticastSocket socket = entry.getValue();
                    byte[] buffer = new byte[8192]; // Increased buffer size for serialized objects
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    socket.setSoTimeout(500);

                    try {
                        socket.receive(packet);
                        ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData());
                        ObjectInputStream ois = new ObjectInputStream(bais);

                        Message message = (Message) ois.readObject();
                        //if (!(message instanceof HeartbeatMsg))
                        //    System.out.println("Received message on " + entry.getKey() + message);
                        // Check if the message has already been processed
                        // TODO: may become really expensive, should cache only the last N messages
                        if (!processedMessages.containsKey(message.getMessageUUID())) {
                            processedMessages.put(message.getMessageUUID(), true);
                            processMessage(message);
                        }

                        ois.close();
                        bais.close();

                    } catch (SocketTimeoutException ignored) {
                        // Ignore timeout and continue to the next socket
                    }
                }catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                        break;
                }
            }
        }
        /*

        while (!Thread.currentThread().isInterrupted()) {
            byte[] buffer = new byte[8192]; // Increased buffer size for serialized objects
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            try {
                multicastSocket.receive(packet);
                ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData());
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

                          */

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
        //logger.info("Peer discovered/updated: " + peerId);
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

    public int getPort(){
        return PORT;
    }

    public P2PChatApp getApp(){
        return app;
    }

    public Logger getLogger() {
        return logger;
    }

    public Map<String, MulticastSocket> getSockets() {
        return sockets;
    }

    public Map<UUID, Long> getConnectedPeers() {
        return connectedPeers;
    }

    public Map<UUID, Boolean> getProcessedMessages() {
        return processedMessages;
    }

    public InetAddress getGroup() {
        return group;
    }

    public NetworkInterface getNetworkInterface() {
        return networkInterface;
    }

    public static String getMulticastAddress() {
        return MULTICAST_ADDRESS;
    }

}


