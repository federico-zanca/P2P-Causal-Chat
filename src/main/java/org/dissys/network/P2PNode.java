package org.dissys.network;
import org.dissys.User;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class P2PNode {
    private User localUser;
    private Map<String, UUID> usernameRegistry;
    private Map<UUID, PeerSkeleton> connectedPeers;
    private ExecutorService executor;
    private InetAddress address;
    private int port;

    /**
     * the constructor sets the local p2pNode's ip address and port, initializes the usernameRegistry, the connected peers, and the executorService
     * @param address ip address of local user
     * @param port port utilized for the p2pChat application
     */
    public P2PNode(InetAddress address, int port) {
        this.address = address;
        this.port = port;
        this.usernameRegistry = new ConcurrentHashMap<>();
        this.connectedPeers = new ConcurrentHashMap<>();
        this.executor = Executors.newCachedThreadPool();
    }

    /**
     * the method proposes a username chosen by the user to the connected peers, and returns a boolean representing the voting result of the peers on the matter
     * @param username username of the local user to be proposed to the other connected peers
     * @return true if the name was accepted, false if the name was rejected by other peers
     */
    public boolean proposeUsername(String username) {
        if (usernameRegistry.containsKey(username)) {
            return false;
        }

        User proposedUser = new User(username);
        boolean accepted = broadcastUsernameProposal(proposedUser);

        if (accepted) {
            this.localUser = proposedUser;
            usernameRegistry.put(username, proposedUser.getId());
            broadcastUsernameAccepted(proposedUser);
            return true;
        }

        return false;
    }

    /**
     * the method broadcasts a username proposal from a peer to all connected peers
     * @param proposedUser User proposal received by a peer
     * @return true if all peers voted yes to the User proposal
     */
    private boolean broadcastUsernameProposal(User proposedUser) {
        List<Future<Boolean>> futures = new ArrayList<>();
        // might need to exclude the sender or maybe not TODO
        for (PeerSkeleton peer : connectedPeers.values()) {
            futures.add(executor.submit(() -> peer.voteOnUsername(proposedUser)));
        }

        try {
            for (Future<Boolean> future : futures) {
                if (!future.get(5, TimeUnit.SECONDS)) {
                    return false;
                }
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return false;
        }

        return true;
    }

    /**
     * method used to vote on a username proposal, it checks in the P2PNode's username registry if the User already exists
     * @param proposedUser User proposal received by another peer
     * @return true if the proposed User is not found in the usernameRegistry
     */
    public boolean voteOnUsername(User proposedUser) {
        return !usernameRegistry.containsKey(proposedUser.getUsername());
    }

    /**
     * broadcasts the accepted User from the proposing peer to all the connected peers
     * @param acceptedUser User proposal that has been accepted by the proposing peer
     */
    private void broadcastUsernameAccepted(User acceptedUser) {
        for (PeerSkeleton peer : connectedPeers.values()) {
            executor.submit(() -> peer.addToUsernameRegistry(acceptedUser));
        }
    }

    /**
     * adds a user to the username registry
     * @param user
     */
    public void addToUsernameRegistry(User user) {
        usernameRegistry.put(user.getUsername(), user.getId());
    }
    //TODO
    /**
     *
     * @param peerAddress
     * @param peerPort
     */
    public void connectToPeer(InetAddress peerAddress, int peerPort) {
        PeerSkeleton newPeer = new PeerSkeleton(peerAddress, peerPort);
        UUID peerId = newPeer.getId();
        connectedPeers.put(peerId, newPeer);
        syncUsernameRegistry(newPeer);
    }
    //TODO
    /**
     *
     * @param peer
     */
    private void syncUsernameRegistry(PeerSkeleton peer) {
        peer.syncUsernameRegistry(new HashMap<>(usernameRegistry));
    }

    /**
     *
     * @return the username registry
     */
    public Map<String, UUID> getUsernameRegistry() {
        return new HashMap<>(usernameRegistry);
    }

    /**
     * adds a Peer to the connectedPeers map
     * @param peer
     */
    public void addPeer(PeerSkeleton peer) {
        connectedPeers.put(peer.getId(), peer);
    }

    /**
     * removes a peer from the connectedPeers map
     * @param peer
     */
    public void removePeer(PeerSkeleton peer) {
        connectedPeers.remove(peer.getId());
    }
}


