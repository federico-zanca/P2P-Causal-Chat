package org.dissys.Protocols;

import org.dissys.P2PChatApp;
import org.dissys.Room;
import org.dissys.VectorClock;
import org.dissys.messages.ChatMessage;
import org.dissys.messages.Message;
import org.dissys.messages.ReconnectionReplyMessage;
import org.dissys.messages.ReconnectionRequestMessage;
import org.dissys.network.Client;

import java.util.*;

public class ReconnectionProtocol {
    private void askIfReconnecting(P2PChatApp app) {
        System.out.println("Are you reconnecting to the chat? (y/N)");
        Scanner scanner = new Scanner(System.in);
        String answer = scanner.nextLine();
        if (answer.trim().equalsIgnoreCase("y")) {
            String username = app.getUsername();


            // begin of fake part for simulation purposes
            System.out.print("Metti l'id della room per cui stai simulando la riconnessione -> ");
            String roomId = scanner.nextLine().trim();
            UUID uuid = UUID.fromString(roomId);
            Set<String> participants = new HashSet<>();
            participants.add(username);
            participants.add("amuro");
            System.out.print("Metti l'ip multicast della room per cui stai simulando la riconnessione -> ");
            String multicastIP = scanner.nextLine().trim();
            Room fakeroom = new Room(uuid, "pizza", uuid, participants, multicastIP);
            //fakeroom.setMulticastSocket(client.conn);
            app.addRoom(uuid, fakeroom);
            // end of fake part



            ReconnectionRequestMessage message = new ReconnectionRequestMessage(uuid, username, app.getRoomsValuesAsArrayList());
            app.getClient().sendMessage(message);
        }
    }

    public static void processReconnectionRequestMessage(ReconnectionRequestMessage message, P2PChatApp app) {
        String username = app.getUsername();
        UUID uuid = app.getUUID();
        Map<UUID, VectorClock> requestedRoomsByMessageClocks = message.getRoomsClocks();
        boolean needsUpdate = false;
        List<Message> bundleOfMessagesOtherNeeds = null;
        List<Room> roomsToUpdate = null;
        VectorClock localRoomClock = null;

        //TODO check if I am in a room in which the requester is but he doesn't know -> he lost RoomCreationMessage

        for ( UUID reqRoomId : requestedRoomsByMessageClocks.keySet()){
            System.out.println("Retrieving messages for room " + reqRoomId);
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

            if(needsUpdate) {
                roomsToUpdate.add(reqRoom);
            }

            // send missing messages to requesting peer
            // TODO : do it after random amount of time and check if anyone else already sent it before sending
            if(!bundleOfMessagesOtherNeeds.isEmpty()) {
                ReconnectionReplyMessage replyMessage = new ReconnectionReplyMessage(uuid, username, bundleOfMessagesOtherNeeds);
                app.sendMessage(replyMessage);
            }

            //TODO not fully tested yet
            if(!roomsToUpdate.isEmpty()) {
                ReconnectionRequestMessage askForUpdateMessage = new ReconnectionRequestMessage(uuid, username, roomsToUpdate);
                app.sendMessage(askForUpdateMessage);
            }
        }

        // craft ReconnectionRequestMessage for rooms that need to be updated


    }

    public static void processReconnectionReplyMessage(ReconnectionReplyMessage reconnectionReplyMessage, Client client) {
        List<Message> messages = reconnectionReplyMessage.getLostMessages();
        for(Message message : messages){
            client.processMessage(message);
        }
    }
}
