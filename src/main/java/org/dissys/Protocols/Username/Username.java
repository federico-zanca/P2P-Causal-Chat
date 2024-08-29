package org.dissys.Protocols.Username;

import java.io.Serializable;
import java.time.Instant;
import java.util.Random;

public class Username implements Serializable {
    private static final int CODE_DIGITS = 4;
    private final String name;
    private String code;

    public Username(String name){
        this.name = name;
        code = generateRandomCode(CODE_DIGITS);
    }
    public Username(String name, String code){
        this.name = name;
        this.code = code;
    }

    private String generateRandomCode(int digits){
        Random random = new Random();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i<digits; i++){
            result.append(random.nextInt(10));
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

    @Override
    public String toString(){
        return name + "#" + code;
    }
}
