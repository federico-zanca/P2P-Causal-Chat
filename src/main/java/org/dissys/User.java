package org.dissys;

import java.util.UUID;

public class User {
    private final UUID id;
    private String name;

    /**
     * constructor makes a User object with a randomly generated UUID and sets its name to name
     * @param name
     */
    public User(String name) {
        this.id = UUID.randomUUID();
        this.name = name;
    }

    /**
     * @return the UUID object of this User
     */
    public UUID getUUId() {
        return id;
    }

    /**
     *
     * @return the UUID of this User as a String
     */
    public String getUserId() {
        return id.toString();
    }

    /**
     *
     * @return the name of this User
     */
    public String getUsername() {
        return name;
    }

    /**
     * sets the User's username to the parameter name
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

}