package org.dissys.Commands;

import org.dissys.P2PChatApp;
import org.dissys.Room;
import org.dissys.network.Client;
import org.dissys.utils.LoggerConfig;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public enum ChatCommand implements Command {
    /*CREATE {
        @Override
        public void execute(P2PChatApp chat, String[] args) {
            if (args.length < 1) {
                System.out.println(getUsage());
                return;
            }
            try {
                //chat.createRoom(args[0]);
                System.out.println("Room created: " + args[0]);
            } catch (Exception e) {
                System.out.println("Failed to create room: " + e.getMessage());
            }
        }

        @Override
        public String getUsage() {
            return "create <room_name>";
        }

        @Override
        public String getDescription() {
            return "Create a new chat room";
        }
    },*/

    /*JOIN {
        @Override
        public void execute(P2PChatApp chat, String[] args) {
            if (args.length < 1) {
                System.out.println(getUsage());
                return;
            }
            try {
                //chat.joinRoom(args[0]);
                System.out.println("Joined room: " + args[0]);
            } catch (Exception e) {
                System.out.println("Failed to join room: " + e.getMessage());
            }
        }

        @Override
        public String getUsage() {
            return "join <room_name>";
        }

        @Override
        public String getDescription() {
            return "Join an existing chat room";
        }
    },*/

    /*SEND {
        @Override
        public void execute(P2PChatApp chat, String[] args) {
            if (args.length < 2) {
                System.out.println(getUsage());
                return;
            }
            try {
                //chat.sendMessage(args[0], String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                System.out.println("Message sent to " + args[0]);
            } catch (Exception e) {
                System.out.println("Failed to send message: " + e.getMessage());
            }
        }

        @Override
        public String getUsage() {
            return "send <room_name> <message>";
        }

        @Override
        public String getDescription() {
            return "Send a message to a room";
        }
    },*/

    /*LIST {
        @Override
        public void execute(P2PChatApp chat, String[] args) {
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

        @Override
        public String getUsage() {
            return "list";
        }

        @Override
        public String getDescription() {
            return "List available rooms";
        }
    },*/

    HELP {
        @Override
        public void execute(P2PChatApp chat, String[] args) {
            System.out.println("Available commands:");
            for (ChatCommand cmd : ChatCommand.values()) {
                System.out.printf("  %-10s - %s%n", cmd.getUsage(), cmd.getDescription());
            }
        }

        @Override
        public String getUsage() {
            return "help";
        }

        @Override
        public String getDescription() {
            return "Print this help message";
        }
    },
    /*SET_USERNAME {
        @Override
        public void execute(P2PChatApp chat, String[] args) {
            if (args.length < 1) {
                System.out.println(getUsage());
                return;
            }
            boolean success = chat.setUsername(args[0]);
            if (success) {
                System.out.println("Username set successfully: " + args[0]);
            } else {
                System.out.println("Failed to set username. It may already be in use.");
            }
        }

        @Override
        public String getUsage() {
            return "set_username <username>";
        }

        @Override
        public String getDescription() {
            return "Set your username";
        }
    },*/

    EXIT {
        @Override
        public void execute(P2PChatApp chat, String[] args) {
            System.out.println("Goodbye!");
            System.exit(0);
        }

        @Override
        public String getUsage() {
            return "exit";
        }

        @Override
        public String getDescription() {
            return "Exit the application";
        }
    },

    LIST {
        @Override
        public void execute(P2PChatApp chat, String[] args) {
            //Set<String> rooms = chat.getClient().getRoomsNames();
            Set<String> rooms = chat.getClient().getRoomsIdsAndNames();
            if (rooms.isEmpty()) {
                System.out.println("No rooms available.");
            } else {
                System.out.println("Available rooms:");
                for (String room : rooms) {
                    System.out.println("- " + room);
                }
            }
        }

        @Override
        public String getUsage() {
            return "list";
        }

        @Override
        public String getDescription() {
            return "List available rooms";
        }
    },

    OPEN{
        @Override
        public void execute(P2PChatApp chat, String[] args) {
            if (args.length == 1) {
                chat.getClient().openRoom(args[0]);
            }
            else {
                System.out.println(getUsage());
                return;
            }
        }

        @Override
        public String getUsage() {
            return "open <room_name>"; //TODO handle rooms with the same name
        }

        @Override
        public String getDescription() {
            return "Open and view a chat room";
        }
    },

    SEND {
        @Override
        public void execute(P2PChatApp chat, String[] args) {
            if (args.length < 2) {
                System.out.println(getUsage());
                return;
            }
            try {
                Client client = chat.getClient();
                client.sendMessageInChat(args[0], String.join(" ", Arrays.copyOfRange(args, 1, args.length)));


            } catch (Exception e) {
                System.out.println("Failed to send message: " + e.getMessage());
            }
        }

        @Override
        public String getUsage() {
            return "send <room_name> <message>";
        }

        @Override
        public String getDescription() {
            return "Send a message to a room";
        }
    },

    CREATE {
        @Override
        public void execute(P2PChatApp chat, String[] args) {
            if (args.length < 2) {
                System.out.println(getUsage());
                return;
            }
            String roomName = args[0];
            System.out.println("Creating room: " + roomName);
            // move the rest of the args to a set
            Set<String> participants = new HashSet<>(Arrays.asList(Arrays.copyOfRange(args, 1, args.length)));
            chat.getClient().createRoom(roomName, participants);
        }

        @Override
        public String getUsage() {
            return "create <room_name> <participant1> <participant2> ...";
        }

        @Override
        public String getDescription() {
            return "Create a new chat room";
        }
    },
    CLOCK {
        @Override
        public void execute(P2PChatApp chat, String[] args) {
            Client client = chat.getClient();
            if(args.length != 1){
                System.out.println(getUsage());
                return;
            }
            String roomName = args[0];
            Room room = client.getRoomByName(roomName);
            if (room != null){
                System.out.println(room.getLocalClock());
            } else {
                System.out.println("Room not found :(");
            }
        }

        @Override
        public String getUsage() {
            return "clock <room>";
        }

        @Override
        public String getDescription() {
            return "View your VectorClock for a given room";
        }
    },

    VIEW_LOG {
        @Override
        public void execute(P2PChatApp chat, String[] args) {
            int linesToShow = 10; // Default number of lines to show
            if (args.length > 0) {
                try {
                    linesToShow = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid number of lines. Using default (10).");
                }
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(LoggerConfig.getLogFilePath()))) {
                String line;
                java.util.List<String> lastLines = new java.util.ArrayList<>();
                while ((line = reader.readLine()) != null) {
                    lastLines.add(line);
                    if (lastLines.size() > linesToShow) {
                        lastLines.remove(0);
                    }
                }
                System.out.println("Last " + linesToShow + " log entries:");
                for (String logLine : lastLines) {
                    System.out.println(logLine);
                }
            } catch (IOException e) {
                System.out.println("Error reading log file: " + e.getMessage());
            }

        }

        @Override
        public String getUsage() {
            return "view_log [number_of_lines]";
        }

        @Override
        public String getDescription() {
            return "View the last N lines of the log file (default 10)";
        }


    };
        /**
         * executes the command sent by calling the functions in chat
         * @param chat
         * @param args
         */
    public abstract void execute(P2PChatApp chat, String[] args);
}
