package org.dissys.messages;

import org.dissys.network.Client;

import java.util.List;
import java.util.UUID;

public class ReconnectionReplyMessage extends Message {
    private final String sender;
    private final List<Message> lostMessages;

    public ReconnectionReplyMessage(UUID senderId, String sender, List<Message> lostMessages) {
        super(senderId);
        this.sender = sender;
        this.lostMessages = lostMessages;
    }

    public List<Message> getLostMessages() {
        return lostMessages;
    }

    @Override
    public void onMessage(Client client) {
        client.processReconnectionReplyMessage(this);
    }

    @Override
    public String toString() {
        return "ReconnectionReplyMessage";
    }

    public String getSender() {
        return sender;
    }


}
