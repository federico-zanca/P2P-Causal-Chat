package org.dissys.network;
import java.net.*;
import java.io.*;
import java.util.*;

public class P2PNode {
    private int port;
    private List<Peer> knownPeers;
    private ServerSocket serverSocket;
    private Map<String, String> localData; // Simple key-value store
    private Set<String> rooms = new HashSet<>();
    public P2PNode(int port) {
        this.port = port;
        this.knownPeers = new ArrayList<>();
        this.localData = new HashMap<>();
    }


    public void createRoom(String roomName) {
        rooms.add(roomName);
        // In a real implementation, you'd broadcast this to other peers
    }

    public boolean roomExists(String roomName) {
        return rooms.contains(roomName);
    }

    public List<String> listRooms() {
        return new ArrayList<>(rooms);
    }

    public void sendMessage(String roomName, String message) {
        // In a real implementation, you'd send this to all peers in the room
        System.out.println("Sending message to " + roomName + ": " + message);
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        new Thread(this::acceptConnections).start();
        new Thread(this::discoverPeers).start();
    }

    private void acceptConnections() {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                new Thread(() -> handleConnection(socket)).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleConnection(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String message = in.readLine();
            processMessage(message, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processMessage(String message, PrintWriter out) {
        // Implement your message processing logic here
        // For example:
        if (message.startsWith("GET:")) {
            String key = message.substring(4);
            out.println(localData.getOrDefault(key, "Not found"));
        } else if (message.startsWith("PUT:")) {
            String[] parts = message.substring(4).split("=", 2);
            localData.put(parts[0], parts[1]);
            out.println("OK");
        }
    }

    private void discoverPeers() {
        // Implement peer discovery
        // This could involve broadcasting on the local network or contacting known peers
    }

    public void sendMessage(Peer peer, String message) {
        try (
                Socket socket = new Socket(peer.getAddress(), peer.getPort());
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            out.println(message);
            String response = in.readLine();
            System.out.println("Response: " + response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


