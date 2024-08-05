package org.dissys;
import org.dissys.Commands.ChatCommand;

import java.util.List;
import java.util.Scanner;

public class AppCLI {
    private final P2PChat chat;
    private final Scanner scanner;

    public AppCLI(P2PChat chat) {
        this.chat = chat;
        this.scanner = new Scanner(System.in);
    }

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