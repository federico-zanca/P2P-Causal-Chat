package org.dissys.network;

import java.net.InetAddress;
import java.net.Socket;

public class PeerInfo {
    private Long connectionTimer;
    //private final InetAddress address;
    //private final int port;
    private final Socket socket;
    public PeerInfo(Socket socket){
        this.connectionTimer = System.currentTimeMillis();
        //this.address = address;
        //this.port = port;
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
