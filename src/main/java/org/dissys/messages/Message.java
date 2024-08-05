package org.dissys.messages;

import org.dissys.User;
import org.dissys.Room;
import java.util.HashMap;
import java.util.Map;

public class Message {
    private String senderId;
    private User sender;
    private Room rcvRoom;
    private String content;
    private Map<String, Integer> vectorClock;
    //private Map<User, Integer> vectorClock;

    public Message(String senderId, User sender, Room receiver, String content, Map<String, Integer> vectorClock) {
        this.senderId = senderId;
        this.sender = sender;
        this.rcvRoom = receiver;
        this.content = content;
        this.vectorClock = vectorClock;
    }
    //sender is not set here
    public Message(String userId, String content, HashMap<String, Integer> stringIntegerHashMap) {
    }

    public String getSenderId() {
        return senderId;
    }



    public String getContent() {
        return content;
    }

    public Map<String, Integer> getVectorClock() {
        return vectorClock;
    }

    public User getSender() {
        return sender;
    }

    public Room getReceiver() {
        return rcvRoom;
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
