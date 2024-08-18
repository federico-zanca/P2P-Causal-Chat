package org.dissys.CLI;

public enum ForegroundColorANSI {
    RED ("\u001B[31m"),
    GREEN ("\u001B[32m"),
    YELLOW ("\u001B[33m"),
    BLUE ("\u001B[34m"),
    PURPLE ("\u001B[35m"),
    CYAN ("\u001B[36m"),
    WHITE ("\u001B[37m"),
    LIGHT_GRAY("\u001B[37m"),
    DARK_GRAY("\u001B[90m"),
    BLACK("\u001B[30m"),
    RESET ("\u001B[0m");

    private final String color;
    ForegroundColorANSI(String color){
        this.color = color;
    }
    public static final int USABLE_COLORS_FOR_NAMES = 6;
    public static String colorString(String s, ForegroundColorANSI color){
        return color + s + RESET;
    }
    @Override
    public String toString() {
        return color;
    }
}
