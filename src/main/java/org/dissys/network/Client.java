package org.dissys.network;
import org.dissys.P2PChatApp;
import org.dissys.messages.DiscoveryMsg;
import org.dissys.messages.HeartbeatMsg;
import org.dissys.messages.Message;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

//might need to make observable
public class Client {
    private static final String MULTICAST_ADDRESS = "230.0.0.0";
    private static final int PORT = 4446;
    private static final long HEARTBEAT_INTERVAL = 5000; // 5 seconds
    private static final long PEER_TIMEOUT = 15000; // 15 seconds
    private final UUID uuid;
    private P2PChatApp app;
    private String username;
    private Map<String, UUID> usernameRegistry;
    private Map<UUID, Long> connectedPeers;
    private MulticastSocket multicastSocket;
    private NetworkInterface networkInterface;


    public Client(P2PChatApp app) throws IOException {
        this.app = app;
        uuid = UUID.randomUUID();
        this.usernameRegistry = new ConcurrentHashMap<>();
        this.connectedPeers = new ConcurrentHashMap<>();
        connectToGroup(InetAddress.getByName(MULTICAST_ADDRESS), PORT);
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
    }

    private void connectToGroup(InetAddress groupAddress, int port) throws IOException {

        multicastSocket = new MulticastSocket(port);
        networkInterface = findNetworkInterface();
        if (networkInterface == null) {
            throw new IOException("no suitable network interface found");
        }
        multicastSocket.joinGroup(new InetSocketAddress(groupAddress, port), networkInterface);

    }
    private void leaveGroup() throws IOException {
        multicastSocket.leaveGroup(new InetSocketAddress(multicastSocket.getInetAddress(), multicastSocket.getPort()), networkInterface);
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
        try {
            // Serialize the Message object
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(message);
            oos.flush();

            // Get the byte array of the serialized object
            byte[] buffer = baos.toByteArray();

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, multicastSocket.getInetAddress(), multicastSocket.getPort());
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
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        ObjectInputStream ois = null;

        while (!Thread.currentThread().isInterrupted()) {
            try {
                multicastSocket.receive(packet);

                bais.reset();
                bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());

                if (ois == null) {
                    ois = new ObjectInputStream(bais);
                } else {
                    ois = new ObjectInputStream(bais) {
                        @Override
                        protected void readStreamHeader() throws IOException {}
                    };
                }

                Message message = (Message) ois.readObject();
                processMessage(message); //tolto packet.getAddress()
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                break;
            }
        }

        // Close streams when done
        if (ois != null) {
            try {
                ois.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            bais.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                multicastSocket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                processMessage(message, packet.getAddress());
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }*/
    }
    private void processMessage(Message message) {
        if(!message.getSenderId().equals(uuid)){
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
        System.out.println("Peer discovered/updated: " + peerId);
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
            multicastSocket.leaveGroup(new InetSocketAddress(multicastSocket.getInetAddress(), multicastSocket.getPort()), networkInterface);
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


