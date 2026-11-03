package org.jma.tictactoe.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProtocolTest {

    private interface Writer {
        void write(DataOutputStream out) throws IOException;
    }

    private static DataInputStream stream(Writer writer) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        writer.write(new DataOutputStream(bytes));
        return new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PLAY", "LIST", "LOGOUT"})
    void readsCommandWithoutArguments(String name) throws IOException {
        DataInputStream in = stream(out -> out.writeUTF(name));
        assertEquals(new Protocol.Command(name, (byte) 0, (byte) 0, 0), Protocol.readCommand(in));
    }

    @Test
    void readsSetWithCoordinatesAndGameId() throws IOException {
        DataInputStream in = stream(out -> {
            out.writeUTF("SET");
            out.writeByte(2);
            out.writeByte(1);
            out.writeInt(42);
        });
        assertEquals(new Protocol.Command("SET", (byte) 2, (byte) 1, 42), Protocol.readCommand(in));
    }

    @Test
    void readsConsecutiveCommandsFromOneStream() throws IOException {
        DataInputStream in = stream(out -> {
            out.writeUTF("PLAY");
            out.writeUTF("SET");
            out.writeByte(0);
            out.writeByte(2);
            out.writeInt(5);
            out.writeUTF("LIST");
        });
        assertEquals("PLAY", Protocol.readCommand(in).name());
        assertEquals(new Protocol.Command("SET", (byte) 0, (byte) 2, 5), Protocol.readCommand(in));
        assertEquals("LIST", Protocol.readCommand(in).name());
    }

    @Test
    void unknownCommandIsReturnedByName() throws IOException {
        DataInputStream in = stream(out -> out.writeUTF("DISCONNECT"));
        assertEquals("DISCONNECT", Protocol.readCommand(in).name());
    }

    @Test
    void commandNamesAreCaseSensitive() throws IOException {
        DataInputStream in = stream(out -> out.writeUTF("play"));
        assertEquals("play", Protocol.readCommand(in).name());
    }

    @Test
    void truncatedSetFails() throws IOException {
        DataInputStream in = stream(out -> {
            out.writeUTF("SET");
            out.writeByte(1);
        });
        assertThrows(EOFException.class, () -> Protocol.readCommand(in));
    }

    @Test
    void closedStreamFails() throws IOException {
        DataInputStream in = stream(out -> {
        });
        assertThrows(EOFException.class, () -> Protocol.readCommand(in));
    }

    @Test
    void updateHasGameStatePlayersTurnAndBoard() throws IOException {
        Game game = new Game(3, TestPlayers.player(7), TestPlayers.player(9));
        game.move(7, (byte) 2, (byte) 0);
        game.setState("PLAYING");

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(Protocol.update(game)));
        assertEquals("UPDATE", in.readUTF());
        assertEquals(3, in.readInt());
        assertEquals("PLAYING", in.readUTF());
        assertEquals(7, in.readInt());
        assertEquals(9, in.readInt());
        assertEquals(9, in.readInt());
        byte[] board = new byte[9];
        in.readFully(board);
        assertEquals("[0, 0, 1, 0, 0, 0, 0, 0, 0]", Arrays.toString(board));
        assertEquals(-1, in.read());
    }
}
