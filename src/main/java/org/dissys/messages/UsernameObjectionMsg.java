package org.dissys.messages;

import org.dissys.network.Client;

import java.util.UUID;

import static org.dissys.Protocols.Username.UsernameProposal.handleUsernameObjection;

public class UsernameObjectionMsg extends Message{
    private final UUID objectionTarget;

    public UsernameObjectionMsg(UUID senderId, UUID objectionTarget) {
        super(senderId);
        this.objectionTarget = objectionTarget;
    }

    public UUID getObjectionTarget() {
        return objectionTarget;
    }

    @Override
    public void onMessage(Client client) {
        handleUsernameObjection(this, client.getUUID());
    }

    @Override
    public String toString() {
        return "UsernameObjectionMsg objecting username proposed by " + objectionTarget;
    }
}
