package org.dissys;

import java.io.IOException;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        try {
            P2PChat chat = new P2PChat(8000); // Choose an appropriate port
            AppCLI cli = new AppCLI(chat);
            cli.start();
        } catch (IOException e) {
            System.out.println("Failed to start chat: " + e.getMessage());
        }
    }
}