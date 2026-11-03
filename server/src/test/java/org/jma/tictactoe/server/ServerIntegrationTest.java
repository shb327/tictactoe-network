package org.jma.tictactoe.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerIntegrationTest {

    private static final class Client implements AutoCloseable {
        final Socket socket;
        final DataInputStream in;
        final DataOutputStream out;

        Client(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
        }

        void send(String command) throws IOException {
            out.writeUTF(command);
            out.flush();
        }

        void set(int x, int y, int gameId) throws IOException {
            out.writeUTF("SET");
            out.writeByte(x);
            out.writeByte(y);
            out.writeInt(gameId);
            out.flush();
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }

    private record NewGame(int gameId, int self, int opponent, int turn) {
    }

    private record Update(int gameId, String state, int player1, int player2, int turn, String board) {
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static Client connect(int port) throws Exception {
        long deadline = System.currentTimeMillis() + 5000;
        while (true) {
            try {
                return new Client(new Socket("127.0.0.1", port));
            } catch (IOException e) {
                if (System.currentTimeMillis() > deadline) {
                    throw e;
                }
                Thread.sleep(50);
            }
        }
    }

    private static NewGame readNewGame(Client client) throws IOException {
        assertEquals("NEWGAME", client.in.readUTF());
        return new NewGame(client.in.readInt(), client.in.readInt(), client.in.readInt(), client.in.readInt());
    }

    private static Update readUpdate(Client client) throws IOException {
        assertEquals("UPDATE", client.in.readUTF());
        int gameId = client.in.readInt();
        String state = client.in.readUTF();
        int player1 = client.in.readInt();
        int player2 = client.in.readInt();
        int turn = client.in.readInt();
        byte[] board = new byte[9];
        client.in.readFully(board);
        return new Update(gameId, state, player1, player2, turn, Arrays.toString(board));
    }

    private static int readListSize(Client client) throws IOException {
        assertEquals("LIST", client.in.readUTF());
        int count = client.in.readInt();
        for (int i = 0; i < count; i++) {
            client.in.readInt();
            client.in.readUTF();
        }
        return count;
    }

    @Test
    @Timeout(20)
    void twoPlayersPlayAGameToAWin() throws Exception {
        int port = freePort();
        int broadcastPort = freePort();
        Thread serverThread = new Thread(() -> new Server(port, broadcastPort).start(), "server");
        serverThread.setDaemon(true);
        serverThread.start();

        try (Client first = connect(port); Client second = connect(port)) {
            first.send("PLAY");
            // LIST is answered after PLAY is handled, so the first player is queued before the second joins.
            first.send("LIST");
            assertEquals(2, readListSize(first));
            second.send("PLAY");

            NewGame game1 = readNewGame(first);
            NewGame game2 = readNewGame(second);
            int gameId = game1.gameId();
            int p1 = game1.self();
            int p2 = game2.self();
            assertEquals(new NewGame(gameId, p1, p2, p1), game1);
            assertEquals(new NewGame(gameId, p2, p1, p1), game2);

            // X takes the top row while O plays the middle row.
            int[][] moves = {{0, 0}, {0, 1}, {1, 0}, {1, 1}, {2, 0}};
            Update last = null;
            for (int i = 0; i < moves.length; i++) {
                Client mover = i % 2 == 0 ? first : second;
                mover.set(moves[i][0], moves[i][1], gameId);
                Update seenByFirst = readUpdate(first);
                Update seenBySecond = readUpdate(second);
                assertEquals(seenByFirst, seenBySecond);
                assertEquals(gameId, seenByFirst.gameId());
                assertEquals(p1, seenByFirst.player1());
                assertEquals(p2, seenByFirst.player2());
                if (i < moves.length - 1) {
                    assertEquals("PLAYING", seenByFirst.state());
                    assertEquals(i % 2 == 0 ? p2 : p1, seenByFirst.turn());
                }
                last = seenByFirst;
            }

            assertEquals("END " + p1, last.state());
            assertEquals("[1, 1, 1, 2, 2, 0, 0, 0, 0]", last.board());
        }
    }
}
