package org.dissys;
import org.dissys.Commands.ChatCommand;

import java.util.List;
import java.util.Scanner;

public class AppCLI {
    private final P2PChat chat;
    private final Scanner scanner;

    /**
     * constructor creates a CLI and saves the parameter chat as the application
     * @param chat
     */
    public AppCLI(P2PChat chat) {
        this.chat = chat;
        this.scanner = new Scanner(System.in);
    }

    /**
     * starts the CLI, the command help can be used to see what commands are available
     */
    public void start() {
        System.out.println("Welcome to P2P Chat!");
        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            String[] parts = input.split("\\s+", 2);
            String commandStr = parts[0].toUpperCase();

            try {
                ChatCommand command = ChatCommand.valueOf(commandStr);
                String[] args = parts.length > 1 ? parts[1].split("\\s+") : new String[0];
                command.execute(chat, args);
            } catch (IllegalArgumentException e) {
                System.out.println("Unknown command. Type 'help' for available commands.");
            }
        }
    }
}