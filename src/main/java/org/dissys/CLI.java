package org.dissys;

import org.dissys.Commands.ChatCommand;
import java.util.Scanner;
import java.util.logging.Logger;
import org.dissys.utils.LoggerConfig;

public class CLI {
    private final P2PChatApp app;
    private final Scanner scanner;
    private static final Logger logger = LoggerConfig.getLogger();

    public CLI(P2PChatApp app) {
        this.app = app;
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        logger.info("CLI started");
        System.out.println("Welcome to P2P Chat!");
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


    /*public void askChooseUsername(){
        boolean usernameIsChosen = false;
        while (!usernameIsChosen){

            System.out.println("Choose a username:");
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            String[] parts = input.split("\\s+", 2);
            String username = parts[0];

            usernameIsChosen = app.proposeUsername(username);
            if(usernameIsChosen){
                System.out.println("Welcome " + username + "!");
            }else {
                System.out.println("Username taken");
            }
        }

    }*/
}