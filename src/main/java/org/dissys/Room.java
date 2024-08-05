package org.dissys;



import org.dissys.messages.Message;

import java.util.*;

public class Room {
    private final String roomId;

    private List<User> participants;
    private User creator;
    private List<Message> messageLog;
    private Map<String, Map<String, Integer>> vectorClocks; // Per-user vector clocks

    private Set<Message> deliveredMessages; // Track delivered messages by their IDs

    public Room(User creator) {
        this.roomId = UUID.randomUUID().toString();
        this.participants = new ArrayList<>(); // Initialize list of users
        this.messageLog = new ArrayList<>();
        this.vectorClocks = new HashMap<>();
        this.creator = creator;
        joinRoom(creator);
    }

    public String getRoomId() {
        return roomId;
    }

    public List<User> getParticipants() {
        return participants;
    }

    public List<Message> getMessageLog() {
        return messageLog;
    }

    public void joinRoom(User user) {
        participants.add(user);
        vectorClocks.put(user.getUserId(), new HashMap<>()); // Initialize vector clock for user
        for (User u : participants) {
            vectorClocks.get(user.getUserId()).put(u.getUserId(), 0); // Initialize clock for each user
        }

        // will need to send vectors to new user
        System.out.println(user.getUsername() + " joined the room.");
    }

    public void leaveRoom(User user) {
        participants.remove(user);
        vectorClocks.remove(user.getUserId());      // Remove user's vector clock
        System.out.println(user.getUsername() + " left the room.");
    }


    public void sendMessage(User sender, String content) {
        Map<String, Integer> senderClock = vectorClocks.get(sender.getUserId());
        senderClock.put(sender.getUserId(), senderClock.get(sender.getUserId()) + 1); // Increment sender's clock

        Message message = new Message(sender.getUserId(), content, new HashMap<>(senderClock));
        messageLog.add(message);
        deliverMessages();
    }

    private void deliverMessages() {
        // Sort messages by causal order (simple implementation)
        messageLog.sort(Comparator.comparing(m -> m.getVectorClock().getClock().get(m.getSenderId())));

        for (Message message : messageLog) {
            if (!deliveredMessages.contains(message)) {
                System.out.println("Message from " + message.getSenderId() + ": " + message.getContent());
                deliveredMessages.add(message);
            }
        }
    }

    private void receiveMessage(Message message){
        Map<String, Integer> clock;
        if (!messageLog.contains(message)){
            // TODO handle vector clock
            messageLog.add(message);
        }
    }

    public User getCreator() {
        return creator;
    }
}
