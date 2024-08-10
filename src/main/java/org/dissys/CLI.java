package org.dissys;
import org.dissys.Commands.ChatCommand;

import java.util.Scanner;

public class CLI {
    private final P2PChatApp app;
    private final Scanner scanner;

    /**
     * constructor creates a CLI and saves the parameter chat as the application
     * @param app
     */
    public CLI(P2PChatApp app) {
        this.app = app;
        this.scanner = new Scanner(System.in);
    }

    /**
     * starts the CLI, the command help can be used to see what commands are available
     */
    public void start() {
        System.out.println("Welcome to P2P Chat!");
        //askChooseUsername();
        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            String[] parts = input.split("\\s+", 2);
            String commandStr = parts[0].toUpperCase();

            try {
                ChatCommand command = ChatCommand.valueOf(commandStr);
                String[] args = parts.length > 1 ? parts[1].split("\\s+") : new String[0];
                command.execute(app, args);
            } catch (IllegalArgumentException e) {
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