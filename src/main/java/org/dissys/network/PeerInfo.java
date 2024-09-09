package org.dissys.network;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class PeerInfo {
    private Long connectionTimer;
    //private final InetAddress address;
    //private final int port;
    private ObjectOutputStream out;
    private final Socket socket;
    public PeerInfo(Socket socket) throws IOException {
        this.connectionTimer = System.currentTimeMillis();
        //this.address = address;
        //this.port = port;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.socket = socket;
    }

    public Long getConnectionTimer() {
        return connectionTimer;
    }
/*
    public int getPort() {
        return port;
    }*/

    public void setConnectionTimer(Long connectionTimer) {
        this.connectionTimer = connectionTimer;
    }

    public ObjectOutputStream getOutputStream() {
        return out;
    }
/*
    public InetAddress getAddress() {
        return address;
    }*/

    public Socket getSocket() {
        return socket;
    }
    /*public void setAddress(InetAddress address) {
        this.address = address;
    }*/
}
