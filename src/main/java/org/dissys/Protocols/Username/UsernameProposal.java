package org.dissys.Protocols.Username;

import org.dissys.P2PChatApp;
import org.dissys.messages.UsernameObjectionMsg;
import org.dissys.messages.UsernameProposalMsg;
import org.dissys.messages.UsernameUpdateMsg;
import org.dissys.network.Client;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.UUID;

public class UsernameProposal {
    public static Username myProposedUsername = null;
    public static void usernameProtocol(P2PChatApp app){
        if(app.getUsername() == null){
            myProposedUsername = app.getCli().askForUsername();
            try {
                app.getClient().start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }else {
            myProposedUsername = app.getUsername();
            try {
                app.getClient().start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if(!app.proposeUsernameToPeers(myProposedUsername)){
            handleUsernameTaken(app);
        }
    }
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
        }else {
            handleUsernameTaken(app);
        }
        return !usernameObjectionReceived;
    }

    private static void handleUsernameTaken(P2PChatApp app) {
        app.getCli().printWarning(myProposedUsername +" is taken, retry with a different username");
        myProposedUsername = app.getCli().askWhenUsernameTaken(myProposedUsername);
        proposeUsername(myProposedUsername, app.getClient(), app);
    }

    public static void handleUsernameProposal(UsernameProposalMsg proposal, P2PChatApp app) {
        if (conflictFound(app, proposal.getProposedUsername(), proposal.getSenderId())) {
            // Send an objection if the proposed username conflicts with our own or another registered username
            UsernameObjectionMsg objection = new UsernameObjectionMsg(app.getClient().getUUID(), proposal.getSenderId());

            InetAddress receiverAddress = app.getClient().getConnectedPeers().get(proposal.getSenderId()).getAddress();
            int receiverPort = app.getClient().getConnectedPeers().get(proposal.getSenderId()).getPort();

            app.getClient().sendUnicastMessage(objection, receiverAddress, receiverPort);
        }
    }
    private static boolean conflictWithMyUsername(P2PChatApp app, Username proposedUsername){
        boolean conflict = false;
        Username myUsername = app.getUsername();

        if(myUsername != null && myUsername.toString().equals(proposedUsername.toString())){
            System.out.println("conflict found " + myUsername + " and " + proposedUsername);
            conflict = true;
        }else if(myProposedUsername.toString().equals(proposedUsername.toString())){
            System.out.println("conflict found " + myProposedUsername + " and " + proposedUsername);
            conflict = true;
        }
        return conflict;
    }
    private static boolean conflictFound(P2PChatApp app, Username proposedUsername, UUID senderID){
        return otherHomonymsUUIDsFound(app, senderID, proposedUsername.toString()) ||
                conflictWithMyUsername(app, proposedUsername);
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
