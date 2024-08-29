package org.dissys;

import org.dissys.messages.ChatMessage;
import org.dissys.network.Client;


import java.io.Serializable;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Room implements Serializable {
    private final UUID roomId;
    private final String roomName;
    private final UUID localPeerId;    // basically my id
    private final Set<String> participants; // all participants in the room (including me)
    private VectorClock localClock; // my vector clock
    private final Queue<ChatMessage> messageBuffer; // messages that have not been delivered
    private List<ChatMessage> deliveredMessages; // messages that have been delivered
    private final String multicastIP;
    private MulticastSocket roomMulticastSocket;
    private InetAddress roomMulticastGroup;



    public Room(UUID roomId, String roomName, UUID localPeerId, Set<String> participants, String multicastIP) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.localPeerId = localPeerId;
        this.participants = new HashSet<>(participants);
        this.localClock = new VectorClock(participants);
        this.messageBuffer = new ConcurrentLinkedQueue<>();
        this.deliveredMessages = new ArrayList<>();
        this.multicastIP = multicastIP;
    }


    /**
     * Creates a new message, increments the local vector clock, and includes the updated clock in the message.
     *
     * @param content The content of the message.
     * @param sender The user sending the message.
     * @return A new message with the updated vector clock.
     */
    public ChatMessage createMessage(String content, String sender) {
        localClock.incrementClock(sender);
        return new ChatMessage(localPeerId, sender, roomId, content, new VectorClock(localClock) );
    }

                                /**
     * Receives a message, adds it to the message buffer, and processes the buffer to deliver any deliverable messages.
     *
     * @param message The received message.
     */
    public void receiveMessage(ChatMessage message) {
        if(message.getVectorClock().isObsoleteWithRespectTo(localClock)) {
            return;
        }
        messageBuffer.offer(message);
        processMessages();
        //wait for 2 sec and then print delivered messages and buffered messages

        //This will all be removed later, for debugging
        /*
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Delivered messages in room " + roomName + ":");
        for (ChatMessage deliveredMessage : deliveredMessages) {
            System.out.println(deliveredMessage.getSender() + ": " + deliveredMessage.getContent());
        }
        System.out.println("Buffered messages in room " + roomName + ":");
        for (ChatMessage bufferedMessage : messageBuffer) {
            System.out.println(bufferedMessage);
        }
        */

    }

    /**
     * Processes the message buffer, attempting to deliver any messages that can be delivered in causal order.
     */
    private void processMessages() {
        boolean delivered;
        ChatMessage msg;
        do {
            delivered = false;
            Iterator<ChatMessage> iterator = messageBuffer.iterator();
            while (iterator.hasNext()) {
                ChatMessage message = iterator.next();
                if(isAlreadyDelivered(message)) {
                    iterator.remove();
                }
                if (canDeliver(message)) {
                    deliverMessage(message);
                    msg = message;
                    iterator.remove();
                    delivered = true;

                }
            }
        } while (delivered);
    }

    boolean isAlreadyDelivered(ChatMessage message) {
        VectorClock messageClock = message.getVectorClock();
        return messageClock.isObsoleteWithRespectTo(localClock);
    }

    //TODO magari rendere più efficiente processMessages ordinando i messaggi in modo furbo prima di processarli

    /**
     * Checks if a message can be delivered based on the current state of the local vector clock and message dependencies.
     *
     * @param message The message to check for deliverability.
     * @return True if the message can be delivered, false otherwise.
     */
    private boolean canDeliver(ChatMessage message) {
        VectorClock messageClock = message.getVectorClock();
        String sender = message.getSender();

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
    private void deliverMessage(ChatMessage message) {
        deliveredMessages.add(message);
        localClock.updateClock(message.getVectorClock().getClock(), message.getSender());
        // Notify listeners or update UI
        System.out.println("Delivered message in room " + roomName + ": " + message.getContent() + " from " + message.getSender());
    }

    public void sendChatMessage(Client client, String sender, String content){
        VectorClock messageClock = new VectorClock(localClock);
        messageClock.incrementClock(sender);
        ChatMessage message = new ChatMessage(localPeerId, sender, roomId, content, messageClock);
        client.sendMulticastMessage(message, this.roomMulticastSocket, this.roomMulticastGroup);
        receiveMessage(message);
    }

    public List<ChatMessage> getDeliveredMessages() {
        return new ArrayList<>(deliveredMessages);
    }

    public String getRoomName() {
        return roomName;
    }

    public Set<String> getParticipants() {
        return new HashSet<>(participants);
    }

    public VectorClock getLocalClock() {
        return  localClock;
    }

    public void setLocalClock(VectorClock clock) {
        this.localClock = clock;
    }

    public List<ChatMessage> getBufferedMessages() {
        return new ArrayList<>(messageBuffer);
    }

    public void removeParticipant(String participant) {
        participants.remove(participant);
    }

    public void viewRoomChat() {
        System.out.println("Room " + roomName + " chat:");
        for (ChatMessage message : deliveredMessages) {
            System.out.println(message.getSender() + ": " + message.getContent());
        }
    }

    public UUID getRoomId() {
        return roomId;
    }

    public UUID getLocalPeerId() {
        return localPeerId;
    }

    public String getMulticastIP() {
        return multicastIP;
    }

    public MulticastSocket getRoomMulticastSocket() {
        return roomMulticastSocket;
    }

    public void setRoomMulticastSocket(MulticastSocket roomMulticastSocket) {
        this.roomMulticastSocket = roomMulticastSocket;
    }

    public void setDeliveredMessages(List<ChatMessage> messages) {
        this.deliveredMessages = new ArrayList<>(messages);
    }

    public void setRoomMulticastGroup(InetAddress roomMulticastGroup) {
        this.roomMulticastGroup = roomMulticastGroup;
    }

    public InetAddress getRoomMulticastGroup() {
        return roomMulticastGroup;
    }
    // When reconnecting, you'll need to recreate the MulticastSocket
    public void reconnect(MulticastSocket socket, InetAddress group) {
        this.roomMulticastSocket = socket;
        this.roomMulticastGroup = group;
    }
}