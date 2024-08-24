package org.dissys.Commands;

import org.dissys.CLI.ForegroundColorANSI;
import org.dissys.CLI.State.InHomeState;
import org.dissys.P2PChatApp;
import org.dissys.Room;
import org.dissys.utils.LoggerConfig;
import org.dissys.utils.PersistenceManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

import static org.dissys.CLI.ForegroundColorANSI.*;

public enum ChatCommand implements Command {

    HELP {
        @Override
        public void execute(P2PChatApp chat, String[] args) {
            System.out.println("Available commands:");
            for (ChatCommand cmd : ChatCommand.values()) {
                System.out.printf( "  %-10s - %s%n" , colorString(cmd.getUsage(), GREEN), colorString(cmd.getDescription(), LIGHT_GRAY));

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
    HOME{
        @Override
        public void execute(P2PChatApp chat, String[] args) {
            chat.getCli().setCliState(new InHomeState());
        }

        @Override
        public String getUsage() {
            return "home";
        }

        @Override
        public String getDescription() {
            return "Return to the home page";
        }
    },

    EXIT {
        @Override
        public void execute(P2PChatApp chat, String[] args) {
            System.out.println("Goodbye!");
            if(!PersistenceManager.getWillReset()) {
                PersistenceManager.saveState(chat);
            }
            chat.getClient().stop();
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
            Map<UUID, Room> rooms = chat.getRooms();
            //Set<String> rooms = chat.getClient().getRoomsIdsAndNames();
            if (rooms.isEmpty()) {
                System.out.println("No rooms available.");
            } else {
                System.out.println("Available rooms:");
                for (UUID roomId : rooms.keySet()) {
                    System.out.println("- " + rooms.get(roomId).getRoomName() + " (" + roomId + ")");
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
                chat.openRoom(args[0]);
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
                chat.sendMessageInChat(args[0], String.join(" ", Arrays.copyOfRange(args, 1, args.length)));

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
            chat.createRoom(roomName, participants);
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
            if(args.length != 1){
                System.out.println(getUsage());
                return;
            }
            String roomName = args[0];
            Room room = chat.getRoomByName(roomName);
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


    },

    RESET {
        @Override
        public void execute(P2PChatApp chat, String[] args) {
            if (PersistenceManager.reset()) {
                System.out.println("Saved state has been deleted successfully.");
            } else {
                System.out.println("Failed to delete saved state or no state file exists.");
            }
        }

        @Override
        public String getUsage() {
            return "reset";
        }

        @Override
        public String getDescription() {
            return "Delete the saved application state from disk";
        }
    },

    DELETE_LOG {
        @Override
        public void execute(P2PChatApp chat, String[] args) {
            LoggerConfig.deleteLogFile();
            System.out.println("Log file deleted successfully.");
        }

        @Override
        public String getUsage() {
            return "delete_log";
        }

        @Override
        public String getDescription() {
            return "Delete the log file";
        }
    },

    SOCKETS {
        @Override
        public void execute(P2PChatApp chat, String[] args) {
            chat.printSockets();
        }

        @Override
        public String getUsage() {
            return "sockets";
        }

        @Override
        public String getDescription() {
            return "Print the sockets of the chat app";
        }
    },
    SYSTEM_INFO{
        @Override
        public void execute(P2PChatApp chat, String[] args) {
            String username = chat.getUsername();
            InetAddress localAddress = chat.getClient().getLocalAddress();
            int unicastPort = chat.getClient().getUnicastPort();
            UUID uuid = chat.getClient().getUUID();

            chat.getCli().printSystemInfo(username, localAddress, unicastPort, uuid);
        }

        @Override
        public String getUsage() {
            return "system_info";
        }

        @Override
        public String getDescription() {
            return "prints the username/address/port/uuid of the user";
        }
    },

    PEERS{
        @Override
        public void execute(P2PChatApp chat, String[] args) {
            chat.getCli().printPeers(chat.getUsernameRegistry(), chat.getClient().getConnectedPeers());
        }

        @Override
        public String getUsage() {
            return "peers";
        }

        @Override
        public String getDescription() {
            return "Prints the list of connected peers";
        }
    };


        /**
         * executes the command sent by calling the functions in chat
         * @param chat
         * @param args
         */
    public abstract void execute(P2PChatApp chat, String[] args);
}
