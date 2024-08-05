package org.dissys;

import java.util.UUID;

public class User {
    private final UUID id;
    private String name;

    public User(String name) {
        this.id = UUID.randomUUID();
        this.name = name;
    }

    public UUID getUUId() {
        return id;
    }
    public String getUserId() {
        return id.toString();
    }

    public String getUsername() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}