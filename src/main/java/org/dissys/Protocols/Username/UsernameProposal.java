package org.dissys.Protocols.Username;

import org.dissys.P2PChatApp;
import org.dissys.messages.UsernameObjectionMsg;
import org.dissys.messages.UsernameProposalMsg;
import org.dissys.messages.UsernameUpdateMsg;
import org.dissys.network.Client;

import java.util.List;
import java.util.UUID;

public class UsernameProposal {
    public static boolean proposeUsername(Username proposedUsername, Client client, P2PChatApp app) {
        UsernameProposalMsg proposal = new UsernameProposalMsg(client.getUUID(), proposedUsername);
        client.sendMulticastMessage(proposal);
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
        if (conflictFound(app, proposal.getProposedUsername().toString(), proposal.getSenderId())) {
            
            // Send an objection if the proposed username conflicts with our own or another registered username
            UsernameObjectionMsg objection = new UsernameObjectionMsg(app.getClient().getUUID(), proposal.getSenderId());
            app.getClient().sendMulticastMessage(objection);
        }
    }
    private static boolean conflictFound(P2PChatApp app, String proposedUsername, UUID senderID){
        return app.getUsername().equals(proposedUsername) ||
                app.getProposedUsername().equals(proposedUsername) ||
                otherHomonymsUUIDsFound(app, senderID, proposedUsername);
    }
    private static boolean otherHomonymsUUIDsFound(P2PChatApp app, UUID senderID, String username){
        List<UUID> result = app.getUUIDsMatchedToUsername(username);
        result.remove(senderID);
        return !result.isEmpty();
    }
    private static volatile boolean usernameObjectionReceived = false;

    public static void handleUsernameObjection(UsernameObjectionMsg objection, UUID myUUID) {
        if (objection.getObjectionTarget().equals(myUUID)) {
            usernameObjectionReceived = true;
        }
    }
    private static void sendUsernameUpdate(Client client, Username proposedUsername) {
        client.sendMulticastMessage(new UsernameUpdateMsg(client.getUUID(), proposedUsername));
    }
    public static void handleUsernameUpdate(UsernameUpdateMsg usernameUpdateMsg, Client client){
        client.getApp().updateUsernameRegistry(usernameUpdateMsg.getUpdatedUsername().toString(), usernameUpdateMsg.getSenderId());
    }

    public static boolean isValidUsername(String username) {
        return username != null && !username.trim().isEmpty() && username.matches("^[a-zA-Z0-9]+$");
    }
}
