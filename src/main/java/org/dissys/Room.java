package org.dissys;

import org.dissys.messages.Message;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Room {
    private final String roomId;
    private final String localPeerId;  // basically my id
    private final Set<String> participants; // all participants in the room (including me)
    private final VectorClock localClock; // my vector clock
    private final Queue<Message> messageBuffer; // messages that have not been delivered
    private final List<Message> deliveredMessages; // messages that have been delivered


    public Room(String roomId, String localPeerId, Set<String> participants) {
        this.roomId = roomId;
        this.localPeerId = localPeerId;
        this.participants = new HashSet<>(participants);
        this.localClock = new VectorClock(participants);
        this.messageBuffer = new ConcurrentLinkedQueue<>();
        this.deliveredMessages = new ArrayList<>();
    }


    /**
     * Creates a new message, increments the local vector clock, and includes the updated clock in the message.
     *
     * @param content The content of the message.
     * @param sender The user sending the message.
     * @return A new message with the updated vector clock.
     */
    public Message createMessage(String content, User sender) {
        localClock.incrementClock(localPeerId);
        return new Message(localPeerId, sender, this, content, new VectorClock(localClock) );
    }

    /**
     * Receives a message, adds it to the message buffer, and processes the buffer to deliver any deliverable messages.
     *
     * @param message The received message.
     */
    public void receiveMessage(Message message) {
        messageBuffer.offer(message);
        processMessages();
    }

    /**
     * Processes the message buffer, attempting to deliver any messages that can be delivered in causal order.
     */
    private void processMessages() {
        boolean delivered;
        do {
            delivered = false;
            Iterator<Message> iterator = messageBuffer.iterator();
            while (iterator.hasNext()) {
                Message message = iterator.next();
                if (canDeliver(message)) {
                    deliverMessage(message);
                    iterator.remove();
                    delivered = true;
                }
            }
        } while (delivered);
    }

    //TODO magari rendere più efficiente processMessages ordinando i messaggi in modo furbo prima di processarli

    /**
     * Checks if a message can be delivered based on the current state of the local vector clock and message dependencies.
     *
     * @param message The message to check for deliverability.
     * @return True if the message can be delivered, false otherwise.
     */
    private boolean canDeliver(Message message) {
        VectorClock messageClock = message.getVectorClock();
        String sender = message.getSender().getUserId();

        // Check if this is the next message we're expecting from the sender
        if (messageClock.getClock().get(sender) != localClock.getClock().get(sender) + 1) {
            return false;
        }

        // Check if we have received all messages that this message depends on
        for (String participant : participants) {
            if (!participant.equals(sender) &&
                    messageClock.getClock().get(participant) > localClock.getClock().get(participant)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Delivers a message, updates the local vector clock, and adds the message to the delivered messages list.
     *
     * @param message The message to deliver.
     */
    private void deliverMessage(Message message) {
        deliveredMessages.add(message);
        localClock.updateClock(message.getVectorClock().getClock(), message.getSender().getUserId());
        // Notify listeners or update UI
        System.out.println("Delivered message in room " + roomId + ": " + message.getContent() + " from " + message.getSender());
    }


    public List<Message> getDeliveredMessages() {
        return new ArrayList<>(deliveredMessages);
    }

    public String getRoomId() {
        return roomId;
    }

    public Set<String> getParticipants() {
        return new HashSet<>(participants);
    }

    public VectorClock getLocalClock() {
        return new VectorClock(localClock);
    }

    public List<Message> getBufferedMessages() {
        return new ArrayList<>(messageBuffer);
    }

    public void removeParticipant(String participant) {
        participants.remove(participant);
    }

}