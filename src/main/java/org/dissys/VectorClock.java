package org.dissys;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class VectorClock implements Serializable {
    private Map<String, Integer> clock;

    /** Constructor initializes the vector clock for a set of participants
     *
     * @param participants
     */
    public VectorClock(Set<String> participants) {
        this.clock = new HashMap<>();
        initializeClockForParticipants(participants);
    }

    public VectorClock(VectorClock clock){
        this.clock = new HashMap<>(clock.getClock());
    }

    /*
    public VectorClock(int size){
        this.clock = new HashMap<>();
        for (int i = 0; i < size; i++) {
            this.clock.put(String.valueOf(i), 0);
        }
    }

     */

    /** Initialize the clock for all participants with a starting value of 0
     *
     * @param participants
     */
    private void initializeClockForParticipants(Set<String> participants) {
        for (String participant : participants) {
            clock.put(participant, 0);
        }
    }

    /** Increment the clock value for the specified user by 1
     *
     * @param username
     */
    public void incrementClock(String username) {
        clock.compute(username, (k, currentTimestamp) -> currentTimestamp != null ? currentTimestamp + 1 : 0);
    }

    /** Return the current state of the vector clock **/
    public Map<String, Integer> getClock() {
        return clock;
    }

    /**
     *  Update the local vector clock with the received clock values, taking the maximum for each entry
    ***/
    public void updateClock(Map<String, Integer> receivedClock, String currentUser) {
        for (Map.Entry<String, Integer> entry : receivedClock.entrySet()) {
            String userId = entry.getKey();
            Integer remoteTimestamp = entry.getValue();
            Integer localTimestamp = clock.get(userId);
            if (localTimestamp == null || remoteTimestamp >= localTimestamp) {
                clock.put(userId, remoteTimestamp);
            }
        }
    }

    // probably a better implementation of updateClock
    public synchronized void updateFromMessage(String senderId, Map<String, Integer> messageClock) {
        // Update the sender's timestamp
        clock.merge(senderId, messageClock.get(senderId), Integer::max);

        // Update other timestamps if they're ahead in the message clock
        for (Map.Entry<String, Integer> entry : messageClock.entrySet()) {
            if (!entry.getKey().equals(senderId)) {
                clock.merge(entry.getKey(), entry.getValue(), Integer::max);
            }
        }
    }

    /** Check if the local vector clock is equal to or ahead of the received clock
     *
     * @param receivedClock
     * @return true if the local clock is equal to or ahead of the received clock, false otherwise
     */
    public boolean isClockUpToDate(Map<String, Integer> receivedClock) {
        for (Map.Entry<String, Integer> entry : receivedClock.entrySet()) {
            if (entry.getValue() > clock.getOrDefault(entry.getKey(), 0)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString(){
        String ret="\n";
        for (String usr : clock.keySet()){
            ret += usr +" -> " + clock.get(usr).toString() + "\n";
        }
        return ret;
    }
}
