package org.jma.tictactoe.viewer;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Main {
    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) throws IOException {
        int port=25566;
        if(args.length == 0 || args[0].trim().isEmpty()){
            System.out.println("using default port 25566");
        }else{
            port = Integer.parseInt(args[0]);
        }
        DatagramSocket socket = new DatagramSocket(port);
        System.out.println("started on port: "+port);
        while (true) {
            byte[] buf = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);
            byte[] data = packet.getData();
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(data));
            input.readUTF();
            System.out.println("Game id: " + input.readInt());
            String state = input.readUTF();
            int p1 = input.readInt();
            int p2 = input.readInt();
            if (state.equals("END 0")) {
                System.out.println("STATE: DRAW");
            } else if (state.startsWith("END ")) {
                int winId = Integer.parseInt(state.split(" ")[1]);
                System.out.println("STATE: WIN Player " + (winId == p1 ? "1" : "2"));
            } else {
                System.out.println("STATE: " + state);
            }
            System.out.println("Player1 id: " + p1);
            System.out.println("Player2 id: " + p2);
            System.out.println("Turn player: " + input.readInt());
            System.out.println("=====");
            for (int x = 0; x < 9; x++) {
                int value = input.readByte();
                if (x > 0 && x % 3 == 0) {
                    System.out.println();
                }
                if (value == 1) {
                    System.out.print("X ");
                } else if (value == 2) {
                    System.out.print("O ");
                } else {
                    System.out.print("  ");
                }
            }
            System.out.println("\n=====");
        }
    }
}
