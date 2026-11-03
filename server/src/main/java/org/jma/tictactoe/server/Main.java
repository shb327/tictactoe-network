package org.jma.tictactoe.server;

import java.io.IOException;

public class Main {

      public static void main(String[] args) {
        int port = 25565;
        int broadcastPort = 25566;
        if (args.length != 2) {
            System.err.println("Warning: no ports specified, using default port server 25565 and broadcast 25566");
        } else {
            port = Integer.parseInt(args[0]);
            broadcastPort = Integer.parseInt(args[1]);
        }
        Server server = new Server(port, broadcastPort);
        try {
            server.start();
        }catch (Exception e){
            System.exit(0);
        }
    }
}
