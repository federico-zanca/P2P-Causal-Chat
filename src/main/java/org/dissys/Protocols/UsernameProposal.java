package org.dissys.Protocols;

import org.dissys.P2PChatApp;
import org.dissys.messages.UsernameObjectionMsg;
import org.dissys.messages.UsernameProposalMsg;
import org.dissys.messages.UsernameUpdateMsg;
import org.dissys.network.Client;

import java.util.UUID;

public class UsernameProposal {
    public static boolean proposeUsername(String proposedUsername, Client client, P2PChatApp app) {
        UsernameProposalMsg proposal = new UsernameProposalMsg(client.getUUID(), proposedUsername);
        client.sendMessage(proposal);
        // Start a timer to wait for objections
        usernameObjectionReceived = false;

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(!usernameObjectionReceived){
            app.setUsername(proposedUsername);
            //System.out.println("set app username to " + app.getUsername());
            sendUsernameUpdate(client, proposedUsername);
        }
        return !usernameObjectionReceived;
    }
    public static void handleUsernameProposal(UsernameProposalMsg proposal, P2PChatApp app) {
        if (proposal.getProposedUsername().equals(app.getUsername()) ||
                proposal.getProposedUsername().equals(app.getProposedUsername())||
                app.getUsernameRegistry().containsValue(proposal.getProposedUsername())) {

            // Send an objection if the proposed username conflicts with our own or another registered username
            UsernameObjectionMsg objection = new UsernameObjectionMsg(app.getClient().getUUID(), proposal.getSenderId());
            app.getClient().sendMessage(objection);
        }
    }
    private static volatile boolean usernameObjectionReceived = false;

    public static void handleUsernameObjection(UsernameObjectionMsg objection, UUID myUUID) {
        if (objection.getObjectionTarget().equals(myUUID)) {
            usernameObjectionReceived = true;
        }
    }
    private static void sendUsernameUpdate(Client client, String proposedUsername) {
        client.sendMessage(new UsernameUpdateMsg(client.getUUID(), proposedUsername));
    }
    public static void handleUsernameUpdate(UsernameUpdateMsg usernameUpdateMsg, Client client){
        client.getApp().updateUsernameRegistry(usernameUpdateMsg.getUpdatedUsername(), usernameUpdateMsg.getSenderId());
    }

    public static boolean isValidUsername(String username) {
        return username != null && !username.trim().isEmpty() && username.matches("^[a-zA-Z0-9]+$");
    }
}
