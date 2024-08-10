package org.dissys;



import org.dissys.messages.Message1;

import java.util.*;

public class UglyRoom {
    private final String roomName;

    private List<User> participants;
    private User creator;
    private final List<Message1> roomChat;
    //private Map<String, VectorClock> vectorClocks; // Per-user vector clocks
    private final List<Message1> bufferedMessages; // Track messages that have not been delivered
    private Map<User, VectorClock> participantsClocks; // Per-user vector clocks

    private VectorClock currentRoomClock;

    //private Set<Message> deliveredMessages; // Track delivered messages by their IDs

    public UglyRoom(User creator, String roomName, List<User> participants) {
        this.roomName = roomName;
        this.participants = participants; // Initialize list of users
        participants.add(creator); // Add creator to participants

        this.roomChat = new ArrayList<>();
        this.bufferedMessages = new ArrayList<>();

        this.creator = creator;

        this.participantsClocks = new HashMap<>();
        VectorClock defaultclock = new VectorClock(participants.size());
        for (User u : participants) {
            participantsClocks.put(u, new VectorClock(defaultclock));
        }

        this.currentRoomClock = defaultclock;
    }

    public void leaveRoom(User user) {
        participants.remove(user);
        participantsClocks.remove(user);
        System.out.println(user.getUsername() + " left the room.");
    }

    /*
    public synchronized void postMessage(User user, String content) {
        VectorClock userClock = participantsClocks.get(user);
        userClock.incrementClock(user.getUserId());
        Message message = new Message(user.getUserId(), content, new VectorClock(userClock));
        processMessage(message);
    }
    */
    public synchronized void postMessage(Message1 message) {
        processMessage(message);
    }

    /**
     * Checks if the message can be delivered immediately using the canDeliver method. If not, the message is buffered.
     * @param message
     */
    private synchronized void processMessage(Message1 message) {
        if (canDeliver(message)) {
            deliverMessage(message);
            checkBuffer();
        } else {
            bufferedMessages.add(message);
        }
    }

        /**
     * Determines if a message can be delivered based on the vector clock values. It checks if the message's clock for the sender is exactly one more than the room's clock for the sender and that the message's clock does not exceed the room's clock for any other participant.
     * @param message
     * @return
     */
    private boolean canDeliver(Message1 message) {
        VectorClock messageClock = message.getVectorClock();
        String senderId = message.getSender().getUserId();
        VectorClock senderClock = participantsClocks.get(message.getSender());

        // Check if message's vector clock for the sender is exactly one more than the room's clock for the sender
        if (messageClock.getClock().get(senderId) != senderClock.getClock().get(senderId) + 1) {
            return false;
        }

        // Check if message's vector clock does not exceed room's clock for any other participant
        for (User participant : participants) {
            if (!participant.getUserId().equals(senderId) &&
                    messageClock.getClock().get(participant.getUserId()) > senderClock.getClock().get(participant.getUserId())) {
                return false;
            }
        }
        return true;
    }

    /**
         Adds the message to the chat log, updates the sender's vector clock, and prints the message.
    * @param message
    */
    private synchronized void deliverMessage(Message1 message) {
        roomChat.add(message);
        VectorClock senderClock = participantsClocks.get(message.getSender());
        senderClock.incrementClock(message.getSender().getUserId());
        System.out.println("Message delivered from " + message.getSender().getUsername() + ": " + message.getContent());
    }


    private synchronized void checkBuffer() {
        Iterator<Message1> iterator = bufferedMessages.iterator();
        while (iterator.hasNext()) {
            Message1 bufferedMessage = iterator.next();
            if (canDeliver(bufferedMessage)) {
                deliverMessage(bufferedMessage);
                iterator.remove();
            }
        }
    }



    /*
    public void addMessage(Message msg) {
        synchronized (roomChat) {
            roomChat.add(msg);
        }
        // incremento il clock della stanza passando l'id del mittente
        //TODO forse va in synchronized
        getRoomClock().incrementClock(msg.getSender().getUserId());

        System.out.println(msg.toString());


        checkMessages();
    }
    */

    public void addToBuffer(Message1 msg){
        synchronized (bufferedMessages) {
            bufferedMessages.add(msg);
        }
    }

    public VectorClock getRoomClock() {
        return currentRoomClock;
    }

    public String getRoomName() {
        return roomName;
    }

    public List<User> getParticipants() {
        return participants;
    }

    public List<Message1> getRoomChat() {
        return roomChat;
    }

    public User getCreator() {
        return creator;
    }

            /*
    public void joinRoom(User user) {
        participants.add(user);
        vectorClocks.put(user.getUserId(), new VectorClock(participants)); // Initialize vector clock for user


        vectorClocks.put(user.getUserId(), new VectorClock()); // Initialize vector clock for user
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
        VectorClock senderClock = vectorClocks.get(sender.getUserId());
        senderClock.incrementLocalClock(sender.getUserId());

        Message message = new Message(sender.getUserId(), content, new VectorClock(senderClock));
        roomChat.add(message);
        deliverMessages();
    }

    private void deliverMessages() {
        // Sort messages by causal order (simple implementation)
        roomChat.sort(Comparator.comparing(m -> m.getVectorClock().getClock().get(m.getSenderId())));

        for (Message message : roomChat) {
            if (!deliveredMessages.contains(message)) {
                System.out.println("Message from " + message.getSenderId() + ": " + message.getContent());
                deliveredMessages.add(message);
            }
        }
    }

    private void receiveMessage(Message message){
        Map<String, Integer> clock;
        if (!roomChat.contains(message)){
            // TODO handle vector clock
            roomChat.add(message);
        }
    }
    */
}
