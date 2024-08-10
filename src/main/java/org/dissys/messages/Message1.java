package org.dissys.messages;

import org.dissys.Room;
import org.dissys.User;
import org.dissys.UglyRoom;
import org.dissys.VectorClock;

public class Message1 {
    private String senderId;
    private User sender;
    private UglyRoom rcvUglyRoom;
    private Room room;
    private String content;
    private VectorClock vectorClock;
    //private Map<User, Integer> vectorClock;

    public Message1(String senderId, User sender, UglyRoom room, String content, VectorClock vectorClock) {
        this.senderId = senderId;
        this.sender = sender;
        this.rcvUglyRoom = room;
        this.content = content;
        this.vectorClock = vectorClock;
    }

    public Message1(String senderId, User sender, Room room, String content, VectorClock vectorClock) {
        this.senderId = senderId;
        this.sender = sender;
        this.room = room;
        this.content = content;
        this.vectorClock = vectorClock;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getContent() {
        return content;
    }

    public VectorClock getVectorClock() {
        return vectorClock;
    }

    public User getSender() {
        return sender;
    }

    public UglyRoom getReceiver() {
        return rcvUglyRoom;
    }

    @Override
    public String toString() {
        return "src.main.java.messsages.Message{" +
                "senderId='" + senderId + '\'' +
                ", content='" + content + '\'' +
                ", vectorClock=" + vectorClock +
                '}';
    }
}
