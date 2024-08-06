package org.dissys.Commands;

import org.dissys.P2PChat;

import java.util.Arrays;
import java.util.List;

public enum ChatCommand implements Command {
    CREATE {
        @Override
        public void execute(P2PChat chat, String[] args) {
            if (args.length < 1) {
                System.out.println(getUsage());
                return;
            }
            try {
                chat.createRoom(args[0]);
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
    },

    JOIN {
        @Override
        public void execute(P2PChat chat, String[] args) {
            if (args.length < 1) {
                System.out.println(getUsage());
                return;
            }
            try {
                chat.joinRoom(args[0]);
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
    },

    SEND {
        @Override
        public void execute(P2PChat chat, String[] args) {
            if (args.length < 2) {
                System.out.println(getUsage());
                return;
            }
            try {
                chat.sendMessage(args[0], String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
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
    },

    LIST {
        @Override
        public void execute(P2PChat chat, String[] args) {
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
    },

    HELP {
        @Override
        public void execute(P2PChat chat, String[] args) {
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
    SET_USERNAME {
        @Override
        public void execute(P2PChat chat, String[] args) {
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
    },

    EXIT {
        @Override
        public void execute(P2PChat chat, String[] args) {
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
    };


    /**
     * executes the command sent by calling the functions in chat
     * @param chat
     * @param args
     */
    public abstract void execute(P2PChat chat, String[] args);
}
