package org.dissys.messages;

import org.dissys.network.Client;

import java.util.UUID;

import static org.dissys.Protocols.UsernameProposal.handleUsernameProposal;

public class UsernameProposalMsg extends Message{
    private final String usernameProposal;
    public UsernameProposalMsg(UUID senderId, String usernameProposal) {
        super(senderId);
        this.usernameProposal = usernameProposal;
    }
    @Override
    public void onMessage(Client client) {
        handleUsernameProposal(this, client.getApp());
    }

    @Override
    public String toString() {
        return "Username proposal from " + getSenderId() + " proposing " + usernameProposal;
    }

    public String getProposedUsername() {
        return usernameProposal;
    }
}
