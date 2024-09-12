package org.dissys.Protocols;

import org.dissys.P2PChatApp;
import org.dissys.Room;
import org.dissys.VectorClock;
import org.dissys.messages.*;
import org.dissys.network.Client;

import java.util.*;
import java.util.concurrent.*;

public class ReconnectionProtocol {
    private static final Map<String, List<ReconnectionReplyMessage>> sensedReplies = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void processReconnectionRequestMessage(ReconnectionRequestMessage message, P2PChatApp app) {
        boolean requestedUpdate = false;
        String username = app.getStringUsername();
        UUID uuid = app.getUUID();
        Map<UUID, VectorClock> requestedRoomsByMessageClocks = message.getRoomsClocks();
        boolean needsUpdate = false;
        List<Message> bundleOfMessagesOtherNeeds = null;
        List<Room> roomsToUpdate = null;
        VectorClock localRoomClock = null;

        // check if the sender left a room but I don't know
        // TODO wait random amount of time before sending
        for (UUID delRoomId : message.getDeletedRooms().keySet()){
            Room myVersionOfRoom = app.getRooms().get(delRoomId);
            if(myVersionOfRoom != null && myVersionOfRoom.getParticipants().contains(message.getSender())){
                ChatMessage leftRoomReplica = new ChatMessage(message.getSenderId(), message.getSender(), delRoomId, message.getDeletedRooms().get(delRoomId), true);
                app.getClient().sendMulticastMessage(leftRoomReplica, myVersionOfRoom.getRoomMulticastSocket(), myVersionOfRoom.getRoomMulticastGroup());
                LeaveRoomACK ack = new LeaveRoomACK(app.getClient().getUUID(), delRoomId, message.getSender());
                app.getClient().sendMulticastMessage(ack, myVersionOfRoom.getRoomMulticastSocket(), myVersionOfRoom.getRoomMulticastGroup());
            }
        }

        // TODO check if the sender doesn't know I left a room -- not really necessary, could send him a unicast message

        // check if I am in a room in which the requester is, but he doesn't know -> he lost RoomCreationMessage
        for(Room room : app.getRoomsValuesAsArrayList()) {
            if(room.getParticipants().contains(message.getSender()) && !requestedRoomsByMessageClocks.containsKey(room.getRoomId()) && !message.getDeletedRooms().containsKey(room.getRoomId())) {
                // send RoomCreationMessage
                RoomCreationMessage roomCreationMessage = new RoomCreationMessage(uuid, username, room.getRoomId(), room.getRoomName(), room.getParticipants(), room.getMulticastIP(), true);
                // TODO may do it after random amount of time
                app.sendMessage(roomCreationMessage);
            }
        }

        //  check if the requester is in a room in which I am, but I don't know -> I lost RoomCreationMessage
        for(Map.Entry<UUID, VectorClock> entry : requestedRoomsByMessageClocks.entrySet()) {
            Room room = app.getRoomByID(entry.getKey());

            // if I left the room, resend leave room message
            if(room == null && entry.getValue().getClock().containsKey(app.getStringUsername())) {// TODO when unicast  && !app.getDeletedRooms().containsKey(entry.getKey())) {
                needsUpdate = true;
            }
        }

        // map of vector clocks of rooms requested by reconnecting peer. It is used later to know if someone has already replied to the requester with the messages I would send
        Map<UUID, VectorClock> clocksOfRoomsOtherNeeds = new HashMap<>();
        for ( UUID reqRoomId : requestedRoomsByMessageClocks.keySet()){
            //System.out.println("Retrieving messages for room " + reqRoomId);
            bundleOfMessagesOtherNeeds = new ArrayList<>();

            Room reqRoom = app.getRoomByID(reqRoomId);

            if(reqRoom == null) {
                continue;
            }

            localRoomClock = reqRoom.getLocalClock();

            for(ChatMessage deliveredMessage : reqRoom.getDeliveredMessages()) {
                if (deliveredMessage.getVectorClock().isAheadOf(requestedRoomsByMessageClocks.get(reqRoomId))) {
                    bundleOfMessagesOtherNeeds.add(deliveredMessage);
                    clocksOfRoomsOtherNeeds.put(reqRoomId, deliveredMessage.getVectorClock());
                }
                if (localRoomClock.isBehindOf(requestedRoomsByMessageClocks.get(reqRoomId))) {
                    // flags this room to be added to list of rooms that need to be updated (need to ask other peers to send missing messages)
                    needsUpdate = true;
                }
            }

            // send missing messages to requesting peer
            // I'm sending it in the room's multicast and not in the general multicast, because only participants to the room are interested in these messages
            // so more messages, but they are smaller and they are received only by people who might need them
            /*

            //LEGACY WAY THAT WORKS
            if(!bundleOfMessagesOtherNeeds.isEmpty()) {
                ReconnectionReplyMessage replyMessage = new ReconnectionReplyMessage(uuid, username, bundleOfMessagesOtherNeeds, message.getSender());
                app.getClient().sendMulticastMessage(replyMessage, reqRoom.getRoomMulticastSocket(), reqRoom.getRoomMulticastGroup());
            }
            */


            // Schedule the reply with a random delay
            // EXPERIMENTAL FEATURE!!!! TEST A LOT
            if (!bundleOfMessagesOtherNeeds.isEmpty()) {
                long delay = ThreadLocalRandom.current().nextLong(1, 1500); // Random delay between 1-5 seconds
                List<Message> finalBundleOfMessagesOtherNeeds = bundleOfMessagesOtherNeeds;
                scheduler.schedule(() -> sendDelayedReply(message, finalBundleOfMessagesOtherNeeds, app), delay, TimeUnit.MILLISECONDS);
            }
        }



        if(needsUpdate){
            ReconnectionRequestMessage askForUpdateMessage = new ReconnectionRequestMessage(uuid, username, app.getRoomsAsList(), app.getDeletedRooms());
            app.sendMessage(askForUpdateMessage);
        }



    }

    private static void sendDelayedReply(ReconnectionRequestMessage originalRequest, List<Message> bundleOfMessagesOtherNeeds, P2PChatApp app) {
        // Filter out messages that have already been sent by others
        List<Message> uniqueMessages = new ArrayList<>();
        Set<UUID> coveredMessageIds = new HashSet<>();

        synchronized (sensedReplies) {
            List<ReconnectionReplyMessage> repliesForUser = sensedReplies.getOrDefault(originalRequest.getSender(), Collections.emptyList());
            for (ReconnectionReplyMessage reply : repliesForUser) {
                for (Message message : reply.getLostMessages()) {
                    coveredMessageIds.add(message.getMessageUUID());
                }
            }

            for (Message message : bundleOfMessagesOtherNeeds) {
                if (!coveredMessageIds.contains(message.getMessageUUID())) {
                    uniqueMessages.add(message);
                    coveredMessageIds.add(message.getMessageUUID());
                }
            }
        }

        if (!uniqueMessages.isEmpty()) {
            ReconnectionReplyMessage replyMessage = new ReconnectionReplyMessage(app.getUUID(), app.getStringUsername(), uniqueMessages, originalRequest.getSender());
            for (Room room : app.getRoomsValuesAsArrayList()) {
                if (room.getParticipants().contains(originalRequest.getSender())) {
                    app.getClient().sendMulticastMessage(replyMessage, room.getRoomMulticastSocket(), room.getRoomMulticastGroup());
                    break; // Send to the first applicable room
                }
            }
        }
    }

    public static void processReconnectionReplyMessage(ReconnectionReplyMessage reconnectionReplyMessage, Client client) {
        if (!reconnectionReplyMessage.getInterestedUser().equals(client.getApp().getStringUsername())) {
            client.getLogger().info("ReconnectionReplyMessage not for me " + reconnectionReplyMessage);
            synchronized (sensedReplies) {
                List<ReconnectionReplyMessage> repliesForUser = sensedReplies.computeIfAbsent(reconnectionReplyMessage.getInterestedUser(), k -> new ArrayList<>());
                repliesForUser.add(reconnectionReplyMessage);
            }
            return;
        }
        List<Message> messages = reconnectionReplyMessage.getLostMessages();
        for(Message message : messages){
            client.processMessage(message);
        }
    }

    public static void retrieveLostMessages(P2PChatApp app){
        ReconnectionRequestMessage message = new ReconnectionRequestMessage(app.getClient().getUUID(), app.getStringUsername(), new ArrayList<>(app.getRooms().values()), app.getDeletedRooms());
        app.sendMessage(message);
    }

    // Method to clean up old entries in sensedReplies
    public static void cleanupSensedReplies() {
        // TODO
        return;
    }
}
