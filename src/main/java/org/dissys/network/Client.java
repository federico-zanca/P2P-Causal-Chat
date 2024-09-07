package org.dissys.network;
import org.dissys.P2PChatApp;
import org.dissys.Room;
import org.dissys.messages.*;
import org.dissys.utils.AppState;
import org.dissys.utils.LoggerConfig;
import org.dissys.utils.PersistenceManager;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

import static org.dissys.Protocols.GossipProtocol.gossip;

//might need to make observable
public class Client {
    private final Random random = new Random();
    private static final String MULTICAST_ADDRESS = "239.1.1.1";
    private static final int MULTICAST_PORT = 5000;
    private static final int UNICAST_PORT = 5001;
    private static final long HEARTBEAT_INTERVAL = 5000; // 5 seconds
    private static final long GOSSIP_INTERVAL = 10000; // 5 seconds
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
    private ScheduledExecutorService executor;

    public MulticastSocket getMulticastSocket(){
        return this.multicastSocket;
    }

    public Client(P2PChatApp app) throws UnknownHostException {
        this.app = app;
        this.sockets = new ConcurrentHashMap<>();
        this.localAddress = InetAddress.getLocalHost();
        //this.unicastPort = MULTICAST_PORT + random.nextInt(1,500);

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

        // Initialize the ScheduledExecutorService with a fixed thread pool
        this.executor = Executors.newScheduledThreadPool(5); // Adjust the pool size based on your needs
    }
    public void start() throws IOException {

        multicastSocket = connectToGroup(group, MULTICAST_PORT, MULTICAST_ADDRESS);
        unicastSocket = new DatagramSocket(UNICAST_PORT);
    /*
        new Thread(this::receiveUnicastMessages).start();

        //sockets.put(MULTICAST_ADDRESS, multicastSocket);

        // Start listening for peer messages
        new Thread(this::receiveMessages).start();

        // Start sending periodic heartbeat
        new Thread(this::sendPeriodicHeartbeat).start();

        // Start a thread to remove stale peers
        new Thread(this::removeInactivePeers).start();*/
        executor.execute(this::receiveUnicastMessages);
        executor.execute(this::receiveMessages);
        executor.scheduleAtFixedRate(this::sendPeriodicHeartbeat, 0, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(this::removeInactivePeers, 0, PEER_TIMEOUT / 2, TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(() -> gossip(getConnectedPeers(), this), 0, GOSSIP_INTERVAL, TimeUnit.MILLISECONDS);

        // Send initial discovery message
        sendDiscoveryMessage();
    }


    public MulticastSocket connectToGroup(InetAddress groupAddress, int port, String ip) throws IOException {

        MulticastSocket multicastSocket = new MulticastSocket(port);
        networkInterface = findNetworkInterface();
        if (networkInterface == null) {
            throw new IOException("no suitable network interface found");
        } else {
            multicastSocket.setNetworkInterface(this.networkInterface);
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
        //System.out.println("sending discovery");
        DiscoveryMsg discoveryMsg = new DiscoveryMsg(uuid);
        sendMulticastMessage(discoveryMsg);
    }
    private void sendPeriodicHeartbeat() {/*
        while (!Thread.currentThread().isInterrupted()) {
            HeartbeatMsg heartbeatMsg = new HeartbeatMsg(uuid);
            sendMulticastMessage(heartbeatMsg);
            try {
                Thread.sleep(HEARTBEAT_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }*/
        //System.out.println("sending heartbeat");
        HeartbeatMsg heartbeatMsg = new HeartbeatMsg(uuid);
        sendMulticastMessage(heartbeatMsg);
    }

    public void sendMulticastMessage(Message message){
        sendMulticastMessage(message, this.multicastSocket, this.group);
    }

    public void sendMulticastMessage(Message message, MulticastSocket socket, InetAddress group) {
        //System.out.println("sending multicastMsg");
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

    public void sendUnicastMessage(Message message, InetAddress receiverAddress) {
        //System.out.println("sending unicast msg");
        try {
            // Serialize the Message object
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(message);
            oos.flush();

            // Get the byte array of the serialized object
            byte[] buffer = baos.toByteArray();

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, receiverAddress, UNICAST_PORT);
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

                //add to peers when receiving any message;
                if(connectedPeers.containsKey(message.getSenderId())){
                    connectedPeers.get(message.getSenderId()).setConnectionTimer(System.currentTimeMillis());
                }else if(!message.getSenderId().equals(uuid)){
                    connectedPeers.put(message.getSenderId(), new PeerInfo(System.currentTimeMillis(), packet.getAddress()));
                }

                //System.out.println("receive unicast " + message);
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

                        //add to peers when receiving any message;
                        if(connectedPeers.containsKey(message.getSenderId())){
                            connectedPeers.get(message.getSenderId()).setConnectionTimer(System.currentTimeMillis());
                        }else if(!message.getSenderId().equals(uuid)){
                            connectedPeers.put(message.getSenderId(), new PeerInfo(System.currentTimeMillis(), packet.getAddress()));
                        }

                        //System.out.println("receive multicast " + message);
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


    }
    public void processMessage(Message message) {
        if(!message.getSenderId().equals(uuid)){
            if( !(message instanceof HeartbeatMsg)){
                //System.out.println("Processing " + message);
                logger.info("Processing " + message);
            }
            //System.out.println("on message + " + message);
            message.onMessage(this);
            //System.out.println("finished on message " + message);
        }
    }

    private void removeInactivePeers() {
        long now = System.currentTimeMillis();
        connectedPeers.entrySet().removeIf(entry ->
                now - entry.getValue().getConnectionTimer() > PEER_TIMEOUT);
    }
    public void stop() {
        try {
            multicastSocket.leaveGroup(new InetSocketAddress(group, MULTICAST_PORT), networkInterface);
            multicastSocket.close();
            unicastSocket.close();

            // Shutdown the executor service gracefully
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    /*
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
    */
    private NetworkInterface findNetworkInterface() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            if (networkInterface.isUp() && !networkInterface.isLoopback() && networkInterface.supportsMulticast() /*&&
                    networkInterface.getDisplayName().contains("wlan")*/) { // Assicurati che il nome contenga "wlan" o simile
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
    public int getUNICAST_PORT(){
        return UNICAST_PORT;
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

    public boolean isIPUnusedByOtherRooms(String ip, Room deletedRoom){
        //List<Room> targetRooms = new ArrayList<>();
        for (Room room : app.getRoomsAsList()){
            if(room.getMulticastIP().equals(ip)){
                return false;
            }
        }
        for (Room room : app.getDeletedRoomsAsList()){
            if(room.getMulticastIP().equals(ip) && !room.getRoomId().equals(deletedRoom.getRoomId()) && !room.isAcknowledgedDeleted()){
                return false;
            }
        }
        return true;
    }

    public void closeRoomSocketIfUnused(Room deletedRoom) {
        Iterator<Map.Entry<String, MulticastSocket>> iterator = sockets.entrySet().iterator();
        MulticastSocket toBeClosed = null;
        while (iterator.hasNext()) {
            Map.Entry<String, MulticastSocket> entry = iterator.next();
            if(!entry.getKey().equals(MULTICAST_ADDRESS) && isIPUnusedByOtherRooms(entry.getKey(), deletedRoom)){
                toBeClosed = entry.getValue();
                iterator.remove();
                toBeClosed.close();
            }
        }
    }
}


