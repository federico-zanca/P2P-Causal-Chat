package org.dissys.messages;

import org.dissys.P2PChatApp;
import org.dissys.network.Client;

import java.util.UUID;

public class LeaveRoomACK extends Message{
    private final UUID roomId;
    private final String leavingUser;



    public LeaveRoomACK(UUID senderId, UUID roomId, String leavingUser) {
        super(senderId);
        this.roomId = roomId;
        this.leavingUser = leavingUser;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public String getLeavingUser() {
        return leavingUser;
    }

    public void onMessage(Client client){
        P2PChatApp app = client.getApp();
        app.processLeaveRoomACK(this);
    }

    @Override
    public String toString() {
        return "LeaveRoomACK [roomId=" + roomId + ", leavingUser=" + leavingUser + " ACK sender=" + getSenderId() + "]";
    }
}
