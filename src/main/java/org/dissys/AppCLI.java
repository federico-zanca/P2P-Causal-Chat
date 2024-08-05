package org.dissys;
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
        boolean running = true;
        while (running) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            String[] parts = input.split("\\s+", 2);
            String command = parts[0].toLowerCase();

            switch (command) {
                case "exit":
                    running = false;
                    break;
                case "create":
                    if (parts.length > 1) {
                        createRoom(parts[1]);
                    } else {
                        System.out.println("Usage: create <room_name>");
                    }
                    break;
                case "join":
                    if (parts.length > 1) {
                        joinRoom(parts[1]);
                    } else {
                        System.out.println("Usage: join <room_name>");
                    }
                    break;
                case "send":
                    if (parts.length > 1) {
                        String[] messageParts = parts[1].split("\\s+", 2);
                        if (messageParts.length == 2) {
                            sendMessage(messageParts[0], messageParts[1]);
                        } else {
                            System.out.println("Usage: send <room_name> <message>");
                        }
                    } else {
                        System.out.println("Usage: send <room_name> <message>");
                    }
                    break;
                case "list":
                    listRooms();
                    break;
                case "help":
                    printHelp();
                    break;
                default:
                    System.out.println("Unknown command. Type 'help' for available commands.");
            }
        }
        System.out.println("Goodbye!");
    }

    private void createRoom(String roomName) {
        try {
            chat.createRoom(roomName);
            System.out.println("Room created: " + roomName);
        } catch (Exception e) {
            System.out.println("Failed to create room: " + e.getMessage());
        }
    }

    private void joinRoom(String roomName) {
        try {
            chat.joinRoom(roomName);
            System.out.println("Joined room: " + roomName);
        } catch (Exception e) {
            System.out.println("Failed to join room: " + e.getMessage());
        }
    }

    private void sendMessage(String roomName, String message) {
        try {
            chat.sendMessage(roomName, message);
            System.out.println("Message sent to " + roomName);
        } catch (Exception e) {
            System.out.println("Failed to send message: " + e.getMessage());
        }
    }

    private void listRooms() {
        List<String> rooms = chat.listRooms();
        if (rooms.isEmpty()) {
            System.out.println("No rooms available.");
        } else {
            System.out.println("Available rooms:");
            for (String room : rooms) {
                System.out.println("- " + room);
            }
        }
    }

    private void printHelp() {
        System.out.println("Available commands:");
        System.out.println("  create <room_name> - Create a new chat room");
        System.out.println("  join <room_name> - Join an existing chat room");
        System.out.println("  send <room_name> <message> - Send a message to a room");
        System.out.println("  list - List available rooms");
        System.out.println("  help - Print this help message");
        System.out.println("  exit - Exit the application");
    }
}