package org.dissys.utils;

import com.google.gson.*;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.dissys.P2PChatApp;
import org.dissys.Room;

public class PersistenceManager {
    private static boolean willReset = false;
    private static final String SAVE_FILE = "chat_app_state.json";
    private static final Logger logger = LoggerConfig.getLogger();
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
            .setPrettyPrinting()
            .create();

    public static void saveState(P2PChatApp app) {
        try (FileWriter writer = new FileWriter(SAVE_FILE)) {
            AppState state = new AppState(
                    app.getUsername().getName(),
                    app.getUsername().getCode(),
                    app.getClient().getUUID(),
                    //app.getRoomsAsList()
                    //TODO guarda se bisogna ripristinare altro
                    serializeRooms(app.getRoomsValuesAsArrayList()),
                    app.getUsernameRegistry(),
                    app.getDeletedRooms()
                    /*
                    app.getClient().getConnectedPeers(),
                    app.getClient().getProcessedMessages()
                    */
            );
            gson.toJson(state, writer);
            logger.info("Application state saved successfully.");
        } catch (IOException e) {
            logger.severe("Error saving application state: " + e.getMessage());
        }
    }

    public static AppState loadState() {
        File file = new File(SAVE_FILE);
        if (!file.exists()) {
            logger.info("No saved state found. Starting fresh.");
            return null;
        }

        try (FileReader reader = new FileReader(SAVE_FILE)) {
            AppState state = gson.fromJson(reader, AppState.class);
            state.setRooms(deserializeRooms(state.getSerializedRooms()));
            logger.info("Application state loaded successfully.");
            return state;
        } catch (IOException e) {
            logger.severe("Error loading application state: " + e.getMessage());
            return null;
        }
    }

    private static List<SerializableRoom> serializeRooms(List<Room> rooms) {
        List<SerializableRoom> serializableRooms = new ArrayList<>();
        for (Room room : rooms) {
            serializableRooms.add(new SerializableRoom(room));
        }
        return serializableRooms;
    }

    private static List<Room> deserializeRooms(List<SerializableRoom> serializableRooms) {
        List<Room> rooms = new ArrayList<>();
        for (SerializableRoom sRoom : serializableRooms) {
            rooms.add(sRoom.toRoom());
        }
        return rooms;
    }


    public static boolean reset() {
        setReset();
        File file = new File(SAVE_FILE);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                logger.info("Application state file deleted successfully.");
            } else {
                logger.severe("Failed to delete application state file.");
            }
            return deleted;
        } else {
            logger.info("No saved state file found to delete.");
            return false;
        }
    }

    public static boolean getWillReset() {
        return willReset;
    }

    private static void setReset() {
        PersistenceManager.willReset = true;
    }


    private static class UUIDTypeAdapter extends TypeAdapter<UUID> {
        @Override
        public void write(JsonWriter out, UUID value) throws IOException {
            out.value(value.toString());
        }

        @Override
        public UUID read(JsonReader in) throws IOException {
            return UUID.fromString(in.nextString());
        }
    }
}
