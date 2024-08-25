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
    private final Random random = new Random();
    private static final String MULTICAST_ADDRESS = "239.1.1.1";
    private static final int MULTICAST_PORT = 5000;
    private final int unicastPort;
    private static final long HEARTBEAT_INTERVAL = 5000; // 5 seconds
    private static final long PEER_TIMEOUT = 15000; // 15 seconds
    private static final int MAX_MSG_CACHE_SIZE = 100;
    private final UUID uuid;
    private final InetAddress group;
    private final P2PChatApp app;
    private Map<UUID, PeerInfo> connectedPeers;
    private InetAddress localAddress = null;
    private MulticastSocket multicastSocket = null;
    private DatagramSocket unicastSocket = null;
    private NetworkInterface networkInterface = null;
    private static final Logger logger = LoggerConfig.getLogger();
    private Map<UUID, Boolean> processedMessages;
    private final Map<String, MulticastSocket> sockets;

    public MulticastSocket getMulticastSocket(){
        return this.multicastSocket;
    }

    public Client(P2PChatApp app) throws UnknownHostException {
        this.app = app;
        this.sockets = new ConcurrentHashMap<>();
        this.localAddress = InetAddress.getLocalHost();
        this.unicastPort = MULTICAST_PORT + random.nextInt(1,100);

        AppState state = PersistenceManager.loadState();
        if (state != null) {
            this.uuid = state.getClientUUID();
        } else {
            this.uuid = UUID.randomUUID();
        }

        this.connectedPeers = new ConcurrentHashMap<>();

        group = InetAddress.getByName(MULTICAST_ADDRESS);
        //multicastSocket = connectToGroup(group, PORT);

        processedMessages = new LinkedHashMap<UUID, Boolean>(MAX_MSG_CACHE_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<UUID, Boolean> eldest) {
                return size() > MAX_MSG_CACHE_SIZE;
            }
        };
    }
    public void start() throws IOException {

        multicastSocket = connectToGroup(group, MULTICAST_PORT, MULTICAST_ADDRESS);
        unicastSocket = new DatagramSocket(unicastPort);

        new Thread(this::receiveUnicastMessages).start();

        //sockets.put(MULTICAST_ADDRESS, multicastSocket);

        // Start listening for peer messages
        new Thread(this::receiveMessages).start();

        // Start sending periodic heartbeat
        new Thread(this::sendPeriodicHeartbeat).start();

        // Start a thread to remove stale peers
        new Thread(this::removeInactivePeers).start();

        // Send initial discovery message
        sendDiscoveryMessage();

        app.retrieveLostMessages();
        // Ask if reconnecting, may remove it later when persistence is added
        //askIfReconnecting();


    }


    public MulticastSocket connectToGroup(InetAddress groupAddress, int port, String ip) throws IOException {

        MulticastSocket multicastSocket = new MulticastSocket(port);
        networkInterface = findNetworkInterface();
        if (networkInterface == null) {
            throw new IOException("no suitable network interface found");
        }
        multicastSocket.joinGroup(new InetSocketAddress(groupAddress, port), networkInterface);
        sockets.put(ip, multicastSocket);
        //System.out.println("connected to multicast socket " + MULTICAST_ADDRESS + " with port " + PORT);
        return multicastSocket;
    }
    private void leaveGroup() throws IOException {
        multicastSocket.leaveGroup(new InetSocketAddress(group, MULTICAST_PORT), networkInterface);
        multicastSocket.close();
    }

    private void sendDiscoveryMessage() {
        DiscoveryMsg discoveryMsg = new DiscoveryMsg(uuid, localAddress, unicastPort);
        sendMulticastMessage(discoveryMsg);
    }
    private void sendPeriodicHeartbeat() {
        while (!Thread.currentThread().isInterrupted()) {
            HeartbeatMsg heartbeatMsg = new HeartbeatMsg(uuid);
            sendMulticastMessage(heartbeatMsg);
            try {
                Thread.sleep(HEARTBEAT_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void sendMulticastMessage(Message message){
        sendMulticastMessage(message, this.multicastSocket, this.group);
    }

    public void sendMulticastMessage(Message message, MulticastSocket socket, InetAddress group) {
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

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, MULTICAST_PORT);
            socket.send(packet);

            // Close the streams
            oos.close();
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendUnicastMessage(Message message, InetAddress receiverAddress, int receiverPort) {
        try {
            // Serialize the Message object
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(message);
            oos.flush();

            // Get the byte array of the serialized object
            byte[] buffer = baos.toByteArray();

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, receiverAddress, receiverPort);
            unicastSocket.send(packet); // Send the packet via unicast

            // Close the streams
            oos.close();
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void receiveUnicastMessages() {
        while (!Thread.currentThread().isInterrupted()) {
            byte[] buffer = new byte[8192]; // Buffer for receiving data
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            try {
                unicastSocket.receive(packet);
                ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData());
                ObjectInputStream ois = new ObjectInputStream(bais);

                Message message = (Message) ois.readObject();
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

    private void removeInactivePeers() {
        while (!Thread.currentThread().isInterrupted()) {
            long now = System.currentTimeMillis();
            connectedPeers.entrySet().removeIf(entry ->
            now - entry.getValue().getConnectionTimer() > PEER_TIMEOUT);
            try {
                Thread.sleep(PEER_TIMEOUT / 2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    public void stop() {
        try {
            multicastSocket.leaveGroup(new InetSocketAddress(group, MULTICAST_PORT), networkInterface);
            multicastSocket.close();
            unicastSocket.close(); // Close the unicast socket when stopping
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
        return MULTICAST_PORT;
    }
    public int getUnicastPort(){
        return unicastPort;
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

    public Map<UUID, PeerInfo> getConnectedPeers() {
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

    public InetAddress getLocalAddress() {
        return localAddress;
    }
}


