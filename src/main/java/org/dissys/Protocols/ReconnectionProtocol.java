package org.dissys.Protocols;

import org.dissys.P2PChatApp;
import org.dissys.Room;
import org.dissys.VectorClock;
import org.dissys.messages.*;
import org.dissys.network.Client;

import java.util.*;

public class ReconnectionProtocol {

    public static void processReconnectionRequestMessage(ReconnectionRequestMessage message, P2PChatApp app) {
        Boolean requestedUpdate = false;
        String username = app.getUsername().toString();
        UUID uuid = app.getUUID();
        Map<UUID, VectorClock> requestedRoomsByMessageClocks = message.getRoomsClocks();
        boolean needsUpdate = false;
        List<Message> bundleOfMessagesOtherNeeds = null;
        List<Room> roomsToUpdate = null;
        VectorClock localRoomClock = null;

        // check if I am in a room in which the requester is, but he doesn't know -> he lost RoomCreationMessage
        for(Room room : app.getRoomsValuesAsArrayList()) {
            if(room.getParticipants().contains(message.getSender()) && !requestedRoomsByMessageClocks.containsKey(room.getRoomId())) {
                // send RoomCreationMessage
                RoomCreationMessage roomCreationMessage = new RoomCreationMessage(uuid, username, room.getRoomId(), room.getRoomName(), room.getParticipants(), room.getMulticastIP(), true);
                // TODO may do it after random amount of time
                app.sendMessage(roomCreationMessage);
            }
        }

        //  check if the requester is in a room in which I am, but I don't know -> I lost RoomCreationMessage
        for(Map.Entry<UUID, VectorClock> entry : requestedRoomsByMessageClocks.entrySet()) {
            Room room = app.getRoomByID(entry.getKey());
            if(room == null && entry.getValue().getClock().containsKey(app.getUsername())){
                // send ReconnectionRequestMessage
                ReconnectionRequestMessage reconnectionRequestMessage = new ReconnectionRequestMessage(uuid, username, app.getRoomsValuesAsArrayList());
                app.sendMessage(reconnectionRequestMessage);
                requestedUpdate = true;
            }
        }

        for ( UUID reqRoomId : requestedRoomsByMessageClocks.keySet()){
            //System.out.println("Retrieving messages for room " + reqRoomId);
            bundleOfMessagesOtherNeeds = new ArrayList<>();
            roomsToUpdate = new ArrayList<>();

            needsUpdate = false;
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
            if(needsUpdate) {
                roomsToUpdate.add(reqRoom);
            }

            // send missing messages to requesting peer
            // TODO : do it after random amount of time and check if anyone else already sent it before sending
            if(!bundleOfMessagesOtherNeeds.isEmpty()) {
                ReconnectionReplyMessage replyMessage = new ReconnectionReplyMessage(uuid, username, bundleOfMessagesOtherNeeds);
                app.sendMessage(replyMessage);
            }

            // craft ReconnectionRequestMessage for my rooms that need to be updated
            //TODO may add check if I already requested an update because I found out that I am missing a room
            if(!roomsToUpdate.isEmpty()) {
                ReconnectionRequestMessage askForUpdateMessage = new ReconnectionRequestMessage(uuid, username, roomsToUpdate);
                app.sendMessage(askForUpdateMessage);
            }
        }




    }

    public static void processReconnectionReplyMessage(ReconnectionReplyMessage reconnectionReplyMessage, Client client) {
        List<Message> messages = reconnectionReplyMessage.getLostMessages();
        for(Message message : messages){
            client.processMessage(message);
        }
    }
}
