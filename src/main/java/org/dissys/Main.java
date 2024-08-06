package org.dissys;

import java.io.IOException;
import java.net.InetAddress;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        try {
            P2PChat chat = new P2PChat(InetAddress.getLocalHost(), 8000); // Choose an appropriate port
            CLI cli = new CLI(chat);
            cli.start();
        } catch (IOException e) {
            System.out.println("Failed to start chat: " + e.getMessage());
        }
    }
}