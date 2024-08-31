                    package org.dissys.Protocols;

                    import org.dissys.P2PChatApp;
                    import org.dissys.Room;
                    import org.dissys.VectorClock;
                    import org.dissys.messages.*;
                    import org.dissys.network.Client;

                    import java.util.*;
                    import java.util.concurrent.ConcurrentHashMap;

                    public class ReconnectionProtocol {
                        private static final Map<String, List<ReconnectionReplyMessage>> sensedReplies = new ConcurrentHashMap<>();

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
                                if(room == null && entry.getValue().getClock().containsKey(app.getStringUsername())) {// TODO when unicast  && !app.getDeletedRooms().containsKey(entry.getKey())) {
                                    needsUpdate = true;
                                    // send ReconnectionRequestMessage
                                    /*
                                    /*
                                    ReconnectionRequestMessage reconnectionRequestMessage = new ReconnectionRequestMessage(uuid, username, app.getRoomsValuesAsArrayList(), app.getDeletedRooms());
                                    app.sendMessage(reconnectionRequestMessage);

                                     */
                                    requestedUpdate = true;
                                }
                            }

                            // map of vector clocks of rooms requested by reconnecting peer. It is used later to know if someone has already replied to the requester with the messages I would send
                            Map<UUID, VectorClock> clocksOfRoomsOtherNeeds = new HashMap<>();
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
                                        clocksOfRoomsOtherNeeds.put(reqRoomId, deliveredMessage.getVectorClock());
                                    }
                                    if (localRoomClock.isBehindOf(requestedRoomsByMessageClocks.get(reqRoomId))) {
                                        // flags this room to be added to list of rooms that need to be updated (need to ask other peers to send missing messages)
                                        needsUpdate = true;
                                    }
                                }

                                /*
                                /*
                                if(needsUpdate) {
                                    roomsToUpdate.add(reqRoom);
                                }

                                 */

                                // send missing messages to requesting peer
                                // TODO : do it after random amount of time and check if anyone else already sent it before sending
                                // I'm sending it in the room's multicast and not in the general multicast, because only participants to the room are interested in these messages
                                // so more messages, but they are smaller and they are received only by people who might need them
                                if(!bundleOfMessagesOtherNeeds.isEmpty()) {
                                    ReconnectionReplyMessage replyMessage = new ReconnectionReplyMessage(uuid, username, bundleOfMessagesOtherNeeds, message.getSender());
                                    app.getClient().sendMulticastMessage(replyMessage, reqRoom.getRoomMulticastSocket(), reqRoom.getRoomMulticastGroup());
                                }

                                // craft ReconnectionRequestMessage for my rooms that need to be updated
                                /*
                                /*
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
                    }
