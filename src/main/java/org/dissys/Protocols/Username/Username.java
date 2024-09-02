package org.dissys.Protocols.Username;

import java.io.Serializable;
import java.time.Instant;
import java.util.Random;

public class Username implements Serializable {
    public static final int CODE_DIGITS = 0;
    private final String name;
    private String code;
    private final Long timestamp;

    public Username(String name){
        this.name = name;
        code = generateRandomCode(CODE_DIGITS);
        this.timestamp = System.currentTimeMillis();
    }
    public Username(String name, String code){
        this.name = name;
        this.code = code;
        this.timestamp = System.currentTimeMillis();
    }

    public Username(String username, String code, Long timestamp) {
        this.name = username;
        this.code = code;
        this.timestamp = timestamp;
    }

    private String generateRandomCode(int length) {
        Random random = new Random();
        StringBuilder result = new StringBuilder();
        String characters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            result.append(characters.charAt(index));
        }

        return result.toString();
    }

    public void changeCode(){
        code = generateRandomCode(CODE_DIGITS);
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString(){
        return name + "#" + code;
    }
}
