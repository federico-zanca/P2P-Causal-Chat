package org.dissys.CLI;

import org.dissys.CLI.Input.CLIInput;
import org.dissys.CLI.State.CLIState;
import org.dissys.CLI.State.InHomeState;
import org.dissys.CLI.State.InRoomState;
import org.dissys.Commands.ChatCommand;

import java.net.InetAddress;
import java.util.*;
import java.util.logging.Logger;

import org.dissys.P2PChatApp;
import org.dissys.Protocols.Username.Username;
import org.dissys.Room;
import org.dissys.messages.ChatMessage;
import org.dissys.network.PeerInfo;
import org.dissys.utils.LoggerConfig;
import static org.dissys.CLI.ForegroundColorANSI.*;
import static org.dissys.Protocols.Username.UsernameProposal.isValidUsername;

public class CLI {
    private CLIState cliState;
    private final P2PChatApp app;
    private final Scanner scanner;
    private static final Logger logger = LoggerConfig.getLogger();
    private static final int APP_INTERFACE_LENGHT = 40;
    private static final int DEFAULT_ROOM_MESSAGES_SHOWN = 15;
    private static final String TITLE = "P2P GroupChat CLI";
    //private Room currentRoom = null;

    public CLI(P2PChatApp app) {
        this.app = app;
        this.scanner = new Scanner(System.in);
        cliState = new InHomeState();
    }

    public void start() {
        //System.out.println("CLI started");
        //System.out.println("Welcome to P2P Chat!");
        printWelcome();
        refreshHome();

        while (true) {
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

    private void printWelcome() {
        System.out.println("Welcome " + colorString(app.getStringUsername(), assignColorToName(app.getStringUsername())));
    }


    public Username askForUsername() {
        String username = "";

        while (true) {
            System.out.print("Please enter your username: ");
            username = scanner.nextLine();

            // Check if username is not empty and only contains alphanumeric characters
            if (isValidUsername(username)) {
                System.out.println("Checking if the username is already taken...");
                break;
            } else {
                System.out.println("Invalid username. It should be non-empty and only contain letters and numbers.");
            }
        }
        return new Username(username);
    }
    public void setCliState(CLIState state){
        this.cliState = state;
        switch (state.getType()){
            case IN_HOME -> {
                refreshHome();
            }
            case IN_ROOM -> {
                refreshCurrentRoomMessages();
            }
            default -> {throw new IllegalArgumentException("not a valid state type!");
            }
        }
    }

    public void handleInput(CLIInput input){
        cliState.handleInput(this, input);
    }


    public void refreshHome() {
        //refresh
        clearConsole();
        //print top row and app name
        printHeader(TITLE, APP_INTERFACE_LENGHT);
        System.out.println("Username: " + colorString(app.getUsername().toString(), assignColorToName(app.getUsername().toString())));
        System.out.println("Number of Rooms: " + app.getRoomsAsList().size());
        //print room list and each room has it's first message abbreviated
        printRoomList();
        //print bottom row
        printSeparator(APP_INTERFACE_LENGHT);
        //print >
        System.out.print(">");
    }
    private void printHeader(String header, int originalLength){
        int headerLength = header.length();

        String fstHalfSeparator = "";
        String sndHalfSeparator = "";

        if(headerLength%2 == 0){
            fstHalfSeparator = "+" + "-".repeat((originalLength- header.length())/2 - 1 );
            sndHalfSeparator = "-".repeat((originalLength- header.length())/2 - 1 ) + "+";
        }else {
            fstHalfSeparator = "+" + "-".repeat((originalLength- header.length())/2 - 1 );
            sndHalfSeparator = "-".repeat((originalLength- header.length())/2) + "+";
        }


        System.out.println(colorString(fstHalfSeparator + header + sndHalfSeparator, GREEN));
    }

    private void printSeparator(int length) {
        String separator = "+" + "-".repeat(length - 2) + "+";
        System.out.println(colorString(separator, GREEN));
    }
    private void printBlankRow(int n){
        if(n<1){
            n = 1;
        }
        String blank = "\n".repeat(n);
    }
    private String nSpaces(int n){
        return " ".repeat(n);
    }
    private void printRoomList(){
        List<Room> roomList = app.getRoomsAsList();
        for (Room room : roomList){

            String lastSender = " ";
            String lastContent = "[No Messages Received Yet]";

            if(room.getDeliveredMessages().size() != 0){
                lastSender = room.getDeliveredMessages().get(room.getDeliveredMessages().size()-1).getSender();
                lastContent = room.getDeliveredMessages().get(room.getDeliveredMessages().size()-1).getContent();;
            }


            printBlankRow(1);

            lastSender = colorString(lastSender, assignColorToName(lastSender));
            lastContent = colorString(lastContent, DARK_GRAY);

            System.out.println("[" + room.getRoomName() + "] " + lastSender + ": " + lastContent);

        }
    }

    private void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }


    public void refreshCurrentRoomMessages() {
        //refresh
        clearConsole();
        //print header
        printHeader("Room: " + getCurrentRoom().getRoomName(), APP_INTERFACE_LENGHT);
        //print last default room messages shown
        printCurrentRoomMessages();
        //print separator
        printSeparator(APP_INTERFACE_LENGHT);
        //print >
        System.out.print(">");
    }

    private void printCurrentRoomMessages() {
        int nStrdMessages = getCurrentRoom().getDeliveredMessages().size();
        String noMessages = "No messages";
        if(nStrdMessages == 0){
            System.out.println(colorString(nSpaces((APP_INTERFACE_LENGHT - noMessages.length())/2) +
                    noMessages +
                    nSpaces((APP_INTERFACE_LENGHT-noMessages.length())/2), DARK_GRAY));
        }
        int i=0;
        for (ChatMessage message : getCurrentRoom().getDeliveredMessages()) {
            if(i>= DEFAULT_ROOM_MESSAGES_SHOWN || i >= nStrdMessages){
                break;
            }

            if(message.isFarewell()){  // User left the room message, it has no content
                System.out.println(assignColorToName(message.getSender()) + message.getSender() + RESET + " left the room");
            } else {
                System.out.println(assignColorToName(message.getSender()) +
                        message.getSender() +
                        RESET +
                        ": " +
                        message.getContent());
            }
            i++;
        }
        i=0;
        if(getCurrentRoom().getBufferedMessages().size() != 0){
            printHeader("Buffered messages", APP_INTERFACE_LENGHT);
            for (ChatMessage message: getCurrentRoom().getBufferedMessages()){
                if(i >= nStrdMessages){
                    break;
                }

                System.out.println(assignColorToName(message.getSender()) +
                        message.getSender() +
                        RESET +
                        ": " +
                        message.getContent());

                i++;
            }
        }

    }

    private ForegroundColorANSI assignColorToName(String name) {
        int moduleHash = name.charAt(0)% USABLE_COLORS_FOR_NAMES;
        return ForegroundColorANSI.values()[moduleHash];
    }

    public Room getCurrentRoom() {
        if(cliState instanceof InRoomState){
            InRoomState roomState = (InRoomState) cliState;
            return app.getRoomByID(roomState.getCurrentRoomID());
        }else {
            throw new NullPointerException("currently not in a room!");
        }
    }
    public void printNotification(String s){
        System.out.println(colorString("[i]" + s, LIGHT_GRAY));
    }
    public void printWarning(String s) {
        System.out.println(  colorString("[!]" + s, YELLOW));
    }
    public void printError(String s){
        System.out.println( colorString("[!!!]" + s, RED));
    }


    public void printAsciiArtTitle() {
        System.out.println(colorString("\n" +
                "  _____ ___  _____     _____                          _____ _           _   \n" +
                " |  __ \\__ \\|  __ \\   / ____|                        / ____| |         | |  \n" +
                " | |__) | ) | |__) | | |  __ _ __ ___  _   _ _ __   | |    | |__   __ _| |_ \n" +
                " |  ___/ / /|  ___/  | | |_ | '__/ _ \\| | | | '_ \\  | |    | '_ \\ / _` | __|\n" +
                " | |    / /_| |      | |__| | | | (_) | |_| | |_) | | |____| | | | (_| | |_ \n" +
                " |_|   |____|_|       \\_____|_|  \\___/ \\__,_| .__/   \\_____|_| |_|\\__,_|\\__|\n" +
                "                                            | |                             \n" +
                "                                            |_|                             \n", GREEN));
    }

    public int askForRoomChoice(int size) {
        Scanner scanner = new Scanner(System.in);
        int choice = -1;
        while (true) {
            System.out.print("Enter the number of the room you want to join: ");
            try {
                choice = Integer.parseInt(scanner.nextLine());
                if (choice >= 0 && choice < size) {
                    break;
                } else {
                    System.out.println("Invalid room number. Please enter a number between 0 and " + (size - 1) + ".");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }

        return choice;
    }

    public void printPeers(Map<UUID, String> usernameRegistry, Map<UUID, PeerInfo> connectedPeers) {
        if(connectedPeers.size() == 0){
            printWarning("no known peers");
        }else {
            System.out.println(colorString("Connected peers: ", BLUE));
            for (UUID uuid : connectedPeers.keySet()){
                System.out.println(colorString(" - UUID: ", YELLOW)  + uuid.toString() +
                        colorString(" IP: ", CYAN)  + connectedPeers.get(uuid).getAddress().toString() +
                        colorString(" Username: ", GREEN)  + usernameRegistry.get(uuid));
            }
        }
    }

    public void printSystemInfo(String username, InetAddress localAddress, int unicastPort, UUID uuid) {
        System.out.println(colorString("--System info--", BLUE));
        System.out.println(colorString(" - Username: " , YELLOW) + username);
        System.out.println(colorString(" - IP Address: " , YELLOW) + localAddress.toString());
        System.out.println(colorString(" - Port: " , YELLOW) + unicastPort);
        System.out.println(colorString(" - UUID: " , YELLOW) + uuid.toString());
    }

    public Username askWhenUsernameTaken(Username usernameTaken) {
        Username newUsername = usernameTaken;

        System.out.println("Change username [1] or change code [2] ?");
        String input = scanner.nextLine().trim();
        String[] parts = input.split("\\s+", 2);

        while (!parts[0].equals("1") && !parts[0].equals("2")){
            printWarning("Wrong input...");
            System.out.println("Change username [1] or change code [2] ?");
            input = scanner.nextLine().trim();
            parts = input.split("\\s+", 2);

            if(parts[0].equals("1")){
                newUsername = askForUsername();
            }else if(parts[0].equals("2")){
                newUsername.changeCode();
            }
        }

        return newUsername;
    }
}