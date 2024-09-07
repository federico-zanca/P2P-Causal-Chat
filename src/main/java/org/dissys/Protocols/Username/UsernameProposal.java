package org.dissys.Protocols.Username;

import org.dissys.CLI.State.ChangingUsernameState;
import org.dissys.Commands.ChatCommand;
import org.dissys.P2PChatApp;
import org.dissys.messages.UsernameChangeRequestMsg;
import org.dissys.messages.UsernameObjectionMsg;
import org.dissys.messages.UsernameProposalMsg;
import org.dissys.messages.UsernameUpdateMsg;
import org.dissys.network.Client;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.dissys.Protocols.Username.Username.CODE_DIGITS;

public class UsernameProposal {
    public static Username myProposedUsername = null;
    private static AtomicInteger usernameObjectionsReceived = new AtomicInteger(0);
    public static void usernameProtocol(P2PChatApp app){
        if(app.getUsername() == null){
            myProposedUsername = app.getCli().askForUsername();
            try {
                app.getClient().start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if(!app.proposeUsernameToPeers(myProposedUsername)){
                handleUsernameTaken(app);
            }

        }else {
            myProposedUsername = app.getUsername();
            app.getCli().printNotification("checking if your username conflicts with others");
            try {
                app.getClient().start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            //check username still holds
            if(!app.proposeUsernameToPeers(myProposedUsername)){
                app.getCli().printWarning("an older peer with your username has been found, please change your username");

                app.setUsername(null);
                //ask for username
                //propose new username
                usernameProtocol(app);
            }
        }

    }
    public static boolean proposeUsername(Username proposedUsername, Client client, P2PChatApp app) {
        UsernameProposalMsg proposal = new UsernameProposalMsg(client.getUUID(), proposedUsername);
        client.sendMulticastMessage(proposal);
        // Start a timer to wait for objections
        usernameObjectionsReceived = new AtomicInteger(0);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(usernameObjectionsReceived.get() <= 0){
            app.setUsername(proposedUsername);
            app.updateUsernameRegistry(proposedUsername, app.getUUID());
            //System.out.println("set app username to " + app.getUsername());
            sendUsernameUpdate(client, proposedUsername);
        }else {
            handleUsernameTaken(app);
        }
        return usernameObjectionsReceived.get() <= 0;
    }


    private static void handleUsernameTaken(P2PChatApp app) {
        app.getCli().printWarning(myProposedUsername +" is taken, retry with a different username");
        myProposedUsername = app.getCli().askWhenUsernameTaken(myProposedUsername);
        proposeUsername(myProposedUsername, app.getClient(), app);
    }

    public static void handleUsernameProposal(UsernameProposalMsg proposal, P2PChatApp app) {
        InetAddress proposalAddress = app.getClient().getConnectedPeers().get(proposal.getSenderId()).getAddress();
        int proposalPort = app.getClient().getConnectedPeers().get(proposal.getSenderId()).getPort();

        List<UUID> conflicts = conflictsFound(app, proposal.getProposedUsername(), proposal.getSenderId());

        for (UUID id: conflicts){
            //if username is older, send conflict to proposal
            //System.out.println("conflict found " + id.toString());
            //System.out.println("timestamps " + app.getUsernamesCreationTimeStamps().get(id) + " and " + proposal.getProposedUsername().getTimestamp());

            if(id != app.getUUID()){
                if(app.getUsernameRegistry().get(id).getTimestamp() < proposal.getProposedUsername().getTimestamp()){

                    // Send an objection if the proposed username conflicts with our own or another registered username
                    UsernameObjectionMsg objection = new UsernameObjectionMsg(app.getClient().getUUID(), proposal.getSenderId());
                    //InetAddress receiverAddress = app.getClient().getConnectedPeers().get(proposal.getSenderId()).getAddress();
                    //int receiverPort = app.getClient().getConnectedPeers().get(proposal.getSenderId()).getPort();

                    app.getClient().sendUnicastMessage(objection, proposalAddress, proposalPort);

                } else{
                    //send usernameChangeRequest to other
                    InetAddress nameChangeAddress = app.getClient().getConnectedPeers().get(id).getAddress();
                    int nameChangePort = app.getClient().getConnectedPeers().get(id).getPort();

                    UsernameChangeRequestMsg changeRequest = new UsernameChangeRequestMsg(app.getUUID());

                    app.getClient().sendUnicastMessage(changeRequest, nameChangeAddress, nameChangePort);
                }
            }else {
                Username comparison;
                if(app.getUsername() == null){
                    comparison = myProposedUsername;
                }else {
                    comparison = app.getUsername();
                }
                //System.out.println("timestamps " + comparison.getTimestamp() + " and " + proposal.getProposedUsername().getTimestamp());
                if(comparison.getTimestamp() < proposal.getProposedUsername().getTimestamp()){

                    // Send an objection if the proposed username conflicts with our own or another registered username
                    UsernameObjectionMsg objection = new UsernameObjectionMsg(app.getClient().getUUID(), proposal.getSenderId());
                    app.getClient().sendUnicastMessage(objection, proposalAddress, proposalPort);

                } else{
                    //send usernameChangeRequest to other
                    handleUsernameChangeRequest(app);
                }
            }

        }
    }
    private static UUID ifConflictRetMyUUID(P2PChatApp app, Username proposedUsername) throws NoSuchElementException{
        boolean conflict = false;
        Username myUsername = app.getUsername();

        if(myUsername != null && myUsername.toString().equals(proposedUsername.toString())){
            //System.out.println("conflict found " + myUsername + " and " + proposedUsername);
            conflict = true;
        }else if(myProposedUsername.toString().equals(proposedUsername.toString())){
            //System.out.println("conflict found " + myProposedUsername + " and " + proposedUsername);
            conflict = true;
        }
        if(conflict){
            return app.getUUID();
        }else {
            throw new NoSuchElementException("no name conflict with self found");
        }
    }
    private static List<UUID> conflictsFound(P2PChatApp app, Username proposedUsername, UUID senderID){
        List<UUID> result = otherHomonymsUUIDsFound(app, senderID, proposedUsername.toString());

        try {
            if(!result.contains(ifConflictRetMyUUID(app, proposedUsername))){
                result.add(ifConflictRetMyUUID(app, proposedUsername));
            }
        }catch (NoSuchElementException ignored){}

        return result;
    }
    private static List<UUID> otherHomonymsUUIDsFound(P2PChatApp app, UUID senderID, String username){
        List<UUID> result = app.getUUIDsMatchedToUsername(username);
        result.remove(senderID);
        return result;
    }


    public static void handleUsernameObjection(UsernameObjectionMsg objection, UUID myUUID) {
        if (objection.getObjectionTarget().equals(myUUID)) {
            usernameObjectionsReceived.incrementAndGet();
        }
    }
    public static void handleUsernameChangeRequest(P2PChatApp app){
        app.getCli().setCliState(new ChangingUsernameState());
        app.setUsername(null);
        ChatCommand.EXIT.execute(app, new String[]{"exit"});
    }
    private static void sendUsernameUpdate(Client client, Username proposedUsername) {
        client.sendMulticastMessage(new UsernameUpdateMsg(client.getUUID(), proposedUsername));
    }
    public static void handleUsernameUpdate(UsernameUpdateMsg usernameUpdateMsg, Client client){
        client.getApp().updateUsernameRegistry(usernameUpdateMsg.getUpdatedUsername(), usernameUpdateMsg.getSenderId());
    }

    public static boolean isValidUsername(String username) {
        return username != null && !username.trim().isEmpty() && username.matches("^[a-zA-Z0-9]+$");
    }
    public static boolean isValidFormatUsernameCode(String username) {
        if(username == null || username.trim().isEmpty()){
            return false;
        }
        String[] parts = username.split("#");
        if(parts.length == 2 && CODE_DIGITS != 0){
            return parts[0].matches("^[a-zA-Z0-9]+$") && parts[1].matches("^[A-Z0-9]+$");
        }else if(parts.length == 1 && username.charAt(username.length()-1) == '#' && CODE_DIGITS == 0){
            return isValidUsername(parts[0]);
        }else {
            return false;
        }
    }
}
