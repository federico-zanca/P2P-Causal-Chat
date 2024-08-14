package org.dissys.messages;

import org.dissys.network.Client;

import java.util.List;
import java.util.UUID;

import static org.dissys.Protocols.ReconnectionProtocol.processReconnectionReplyMessage;

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
        processReconnectionReplyMessage(this, client);
    }

    @Override
    public String toString() {
        return "ReconnectionReplyMessage";
    }

    public String getSender() {
        return sender;
    }


}
