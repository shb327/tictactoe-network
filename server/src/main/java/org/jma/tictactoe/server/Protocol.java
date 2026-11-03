package org.jma.tictactoe.server;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

final class Protocol {

    record Command(String name, byte x, byte y, int gameId) {
    }

    private Protocol() {
    }

    static Command readCommand(DataInputStream in) throws IOException {
        String name = in.readUTF();
        if (name.equals("SET")) {
            byte x = in.readByte();
            byte y = in.readByte();
            int gameId = in.readInt();
            return new Command(name, x, y, gameId);
        }
        return new Command(name, (byte) 0, (byte) 0, 0);
    }

    static byte[] update(Game game) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        out.writeUTF("UPDATE");
        out.writeInt(game.getId());
        out.writeUTF(game.getState());
        out.writeInt(game.getPlayer1().getId());
        out.writeInt(game.getPlayer2().getId());
        out.writeInt(game.getTurnId());
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                out.writeByte(game.getMap()[x][y]);
            }
        }
        return baos.toByteArray();
    }
}
