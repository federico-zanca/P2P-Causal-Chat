package org.dissys;

import java.util.UUID;

class User {
    private final UUID id;
    private String name;

    public User(String name) {
        this.id = UUID.randomUUID();
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}