package org.dissys.Commands;

import org.dissys.P2PChatApp;

public interface Command {
    void execute(P2PChatApp chat, String[] args);
    String getUsage();
    String getDescription();
}
