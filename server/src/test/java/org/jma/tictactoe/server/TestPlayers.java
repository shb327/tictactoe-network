package org.jma.tictactoe.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

final class TestPlayers {

    private TestPlayers() {
    }

    /** A player backed by an unconnected socket with no-op streams. */
    static Player player(int id) throws IOException {
        return new Player(id, new Socket() {
            @Override
            public InputStream getInputStream() {
                return InputStream.nullInputStream();
            }

            @Override
            public OutputStream getOutputStream() {
                return OutputStream.nullOutputStream();
            }
        });
    }
}
