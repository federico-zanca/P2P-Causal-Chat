package org.dissys.Commands;

import org.dissys.P2PChat;

public interface Command {
    void execute(P2PChat chat, String[] args);
    String getUsage();
    String getDescription();
}
