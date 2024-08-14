package org.dissys;

import org.dissys.Commands.ChatCommand;
import java.util.Scanner;
import java.util.logging.Logger;
import org.dissys.utils.LoggerConfig;

import static org.dissys.Protocols.UsernameProposal.isValidUsername;

public class CLI {
    private final P2PChatApp app;
    private final Scanner scanner;
    private static final Logger logger = LoggerConfig.getLogger();

    public CLI(P2PChatApp app) {
        this.app = app;
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        System.out.println("CLI started");
        System.out.println("Welcome to P2P Chat!");

        if(!app.isUsernameSet()){
            askForUsername();
        }

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            String[] parts = input.split("\\s+", 2);
            String commandStr = parts[0].toUpperCase();

            try {
                ChatCommand command = ChatCommand.valueOf(commandStr);
                String[] args = parts.length > 1 ? parts[1].split("\\s+") : new String[0];
                logger.info("Executing command: " + commandStr);
                command.execute(app, args);
            } catch (IllegalArgumentException e) {
                logger.warning("Unknown command entered: " + commandStr);
                System.out.println("Unknown command. Type 'help' for available commands.");
            }
        }
    }


    private void askForUsername() {
        String username = "";

        while (true) {
            System.out.print("Please enter your username: ");
            username = scanner.nextLine();

            // Check if username is not empty and only contains alphanumeric characters
            if (isValidUsername(username)) {
                System.out.println("Checking if the username is already taken...");
                if(app.proposeUsernameToPeers(username)){
                    System.out.println("Welcome " + username + "!");
                    break;
                }else {
                    System.out.println("Username taken, retry with a different username");
                }
            } else {
                System.out.println("Invalid username. It should be non-empty and only contain letters and numbers.");
            }
        }
    }


    public void notifyRoomInvite(Room room) {
        System.out.println("[!] You have been added to the room " + room.getRoomName());
    }
}