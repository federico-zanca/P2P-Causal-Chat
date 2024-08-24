package org.dissys.network;

import java.net.InetAddress;

public class PeerInfo {
    private Long connectionTimer;
    private final InetAddress address;
    private final int port;
    public PeerInfo(Long connectionTimer, InetAddress address, int port){
        this.connectionTimer = connectionTimer;
        this.address = address;
        this.port = port;
    }

    public Long getConnectionTimer() {
        return connectionTimer;
    }

    public int getPort() {
        return port;
    }

    public void setConnectionTimer(Long connectionTimer) {
        this.connectionTimer = connectionTimer;
    }

    public InetAddress getAddress() {
        return address;
    }

    /*public void setAddress(InetAddress address) {
        this.address = address;
    }*/
}
