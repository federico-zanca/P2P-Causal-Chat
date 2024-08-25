package org.dissys.messages;

import org.dissys.Protocols.Username.Username;
import org.dissys.Protocols.Username.UsernameProposal;
import org.dissys.network.Client;

import java.util.UUID;

import static org.dissys.Protocols.Username.UsernameProposal.handleUsernameUpdate;

public class UsernameUpdateMsg extends Message{
    private final Username updatedUsername;
    public UsernameUpdateMsg(UUID senderId, Username updatedUsername) {
        super(senderId);
        this.updatedUsername = updatedUsername;
    }

    @Override
    public void onMessage(Client client) {
        handleUsernameUpdate(this, client);
    }

    public Username getUpdatedUsername() {
        return updatedUsername;
    }

    @Override
    public String toString() {
        return "UsernameUpdateMsg updating other peers about " + updatedUsername;
    }
}
