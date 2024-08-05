package org.dissys;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class VectorClock {
    private Map<String, Integer> clock;

    /** Constructor initializes the vector clock for a set of participants
     *
     * @param participants
     */
    public VectorClock(Set<String> participants) {
        this.clock = new HashMap<>();
        initializeClockForParticipants(participants);
    }

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
     * @param userId
     */
    public void incrementLocalClock(String userId) {
        clock.compute(userId, (k, currentTimestamp) -> currentTimestamp != null ? currentTimestamp + 1 : 0);
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
}
