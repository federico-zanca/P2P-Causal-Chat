package org.dissys.Protocols;

import org.dissys.P2PChatApp;
import org.dissys.Room;
import org.dissys.VectorClock;
import org.dissys.messages.*;
import org.dissys.network.Client;

import java.util.*;

public class ReconnectionProtocol {

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
            /*
            if(app.getDeletedRooms().get(entry.getKey())!=null) {
                Room deletedRoom = app.getDeletedRooms().get(entry.getKey());
                ChatMessage lostLeaveRoomMessage = new ChatMessage(uuid, username, deletedRoom.getRoomId(), deletedRoom.getLocalClock(), true);
                List<Message> lostMessageAsList = new ArrayList<>();
                lostMessageAsList.add(lostLeaveRoomMessage);
                ReconnectionReplyMessage leaveRoomReminder = new ReconnectionReplyMessage(uuid, username, lostMessageAsList);
                app.getClient().sendMulticastMessage(leaveRoomReminder, deletedRoom.getRoomMulticastSocket(), deletedRoom.getRoomMulticastGroup());
                continue;
            }


            else
            */
            if(room == null && entry.getValue().getClock().containsKey(app.getStringUsername())){
                needsUpdate = true;
                // send ReconnectionRequestMessage
                // TODO maybe optimize, sending ReconnectionMessage in a loop. May want to send just one after the loop
                /*
                ReconnectionRequestMessage reconnectionRequestMessage = new ReconnectionRequestMessage(uuid, username, app.getRoomsValuesAsArrayList(), app.getDeletedRooms());
                app.sendMessage(reconnectionRequestMessage);

                 */
                requestedUpdate = true;
            }
        }

        for ( UUID reqRoomId : requestedRoomsByMessageClocks.keySet()){
            //System.out.println("Retrieving messages for room " + reqRoomId);
            bundleOfMessagesOtherNeeds = new ArrayList<>();

            /*
            roomsToUpdate = new ArrayList<>();

            needsUpdate = false;

             */
            Room reqRoom = app.getRoomByID(reqRoomId);

            if(reqRoom == null) {
                continue;
            }

            localRoomClock = reqRoom.getLocalClock();

            for(ChatMessage deliveredMessage : reqRoom.getDeliveredMessages()) {
                if (deliveredMessage.getVectorClock().isAheadOf(requestedRoomsByMessageClocks.get(reqRoomId))) {
                    bundleOfMessagesOtherNeeds.add(deliveredMessage);
                }
                if (localRoomClock.isBehindOf(requestedRoomsByMessageClocks.get(reqRoomId))) {
                    // flags this room to be added to list of rooms that need to be updated (need to ask other peers to send missing messages)
                    needsUpdate = true;
                }
            }

            //TODO may add check if I already requested an update because I found out that I am missing a room
            /*
            if(needsUpdate) {
                roomsToUpdate.add(reqRoom);
            }

             */

            // send missing messages to requesting peer
            // TODO : do it after random amount of time and check if anyone else already sent it before sending
            if(!bundleOfMessagesOtherNeeds.isEmpty()) {
                ReconnectionReplyMessage replyMessage = new ReconnectionReplyMessage(uuid, username, bundleOfMessagesOtherNeeds);
                app.getClient().sendMulticastMessage(replyMessage, reqRoom.getRoomMulticastSocket(), reqRoom.getRoomMulticastGroup());
            }

            // craft ReconnectionRequestMessage for my rooms that need to be updated
            /*
            //TODO may add check if I already requested an update because I found out that I am missing a room
            if(!roomsToUpdate.isEmpty()) {
                ReconnectionRequestMessage askForUpdateMessage = new ReconnectionRequestMessage(uuid, username, roomsToUpdate, app.getDeletedRooms());
                app.sendMessage(askForUpdateMessage);
            }

             */
        }

        if(needsUpdate){
            ReconnectionRequestMessage askForUpdateMessage = new ReconnectionRequestMessage(uuid, username, app.getRoomsAsList(), app.getDeletedRooms());
            app.sendMessage(askForUpdateMessage);
        }



    }

    public static void processReconnectionReplyMessage(ReconnectionReplyMessage reconnectionReplyMessage, Client client) {
        List<Message> messages = reconnectionReplyMessage.getLostMessages();
        for(Message message : messages){
            client.processMessage(message);
        }
    }

    public static void retrieveLostMessages(P2PChatApp app){
        ReconnectionRequestMessage message = new ReconnectionRequestMessage(app.getClient().getUUID(), app.getStringUsername(), new ArrayList<>(app.getRooms().values()), app.getDeletedRooms());
        app.sendMessage(message);
    }
}
