package org.dissys.messages;

import org.dissys.network.Client;

import java.util.UUID;

import static org.dissys.Protocols.UsernameProposal.handleUsernameUpdate;

public class UsernameUpdateMsg extends Message{
    private final String updatedUsername;
    public UsernameUpdateMsg(UUID senderId, String updatedUsername) {
        super(senderId);
        this.updatedUsername = updatedUsername;
    }

    @Override
    public void onMessage(Client client) {
        handleUsernameUpdate(this, client);
    }

    public String getUpdatedUsername() {
        return updatedUsername;
    }

    @Override
    public String toString() {
        return "UsernameUpdateMsg updating other peers about " + updatedUsername;
    }
}
