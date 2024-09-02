package org.dissys.messages;

import org.dissys.network.Client;

import java.util.UUID;

import static org.dissys.Protocols.Username.UsernameProposal.handleUsernameChangeRequest;

public class UsernameChangeRequestMsg extends Message{
    public UsernameChangeRequestMsg(UUID senderId) {
        super(senderId);
    }

    @Override
    public void onMessage(Client client) {
        handleUsernameChangeRequest(client.getApp());
    }


    @Override
    public String toString() {
        return "UsernameChangeRequest " ;
    }
}
