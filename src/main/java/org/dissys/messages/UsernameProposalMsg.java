package org.dissys.messages;

import org.dissys.Protocols.Username.Username;
import org.dissys.network.Client;

import java.util.UUID;

import static org.dissys.Protocols.Username.UsernameProposal.handleUsernameProposal;

public class UsernameProposalMsg extends Message{
    private final Username usernameProposal;
    public UsernameProposalMsg(UUID senderId, Username usernameProposal) {
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

    public Username getProposedUsername() {
        return usernameProposal;
    }
}
