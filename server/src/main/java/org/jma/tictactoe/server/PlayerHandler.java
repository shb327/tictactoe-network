package org.jma.tictactoe.server;

import java.io.IOException;

class PlayerHandler {
    private final Player player;
    private final Server server;

    PlayerHandler(Player player, Server server) {
        this.player = player;
        this.server = server;
    }

    void start() {
        server.getExecutor().execute(() -> {
            while (!player.getSocket().isClosed()) {
                try {
                    Protocol.Command command = Protocol.readCommand(player.getInputFromUser());
                    System.out.printf("%s: %s\n", player.toString(), command.name());
                    switch (command.name()) {
                        case "PLAY":
                            Player poll = server.getGameQueue().poll();
                            if (poll != null) {
                                startGame(poll, player);
                            } else {
                                server.getGameQueue().add(player);
                            }
                            break;
                        case "LOGOUT":
                            logout();
                            break;
                        case "LIST":
                            player.getOutputToUser().writeUTF("LIST");
                            player.getOutputToUser().writeInt(server.getPlayers().size());
                            for (Player p : server.getPlayers().values()) {
                                player.getOutputToUser().writeInt(p.getId());
                                player.getOutputToUser().writeUTF(p.getSocket().getInetAddress().toString());
                            }
                            player.getOutputToUser().flush();
                            break;
                        case "SET":
                            setMark(player, command.x(), command.y(), command.gameId());
                            break;
                        default:
                            System.err.printf("%s has entered invalid command '%s'\n", player.toString(), command.name());
                    }
                } catch (IOException e) {
                    logout();
                }
            }
        });
    }


    private void logout() {
        System.out.printf("%s has disconnected\n", player.toString());
        try {
            if (!player.getSocket().isClosed()) {
                player.getSocket().close();
            }
            server.getGameQueue().remove(player);
            server.getPlayers().remove(player.getId());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    private void startGame(Player p1, Player p2) throws IOException {
        Game game = new Game(server.getGameIdCounter().getAndIncrement(), p1, p2);
        server.getGames().put(game.getId(), game);
        sendNewGame(game);
    }

    private void setMark(Player player, byte x, byte y, int gameId) throws IOException {
        Game game = server.getGames().get(gameId);
        if (game != null) {
            game.setState("PLAYING");
            if (game.move(player.getId(), x, y)) {
                if (game.isOver()) {
                    server.getGames().remove(gameId);
                }
                sendPlayersUpdate(game);
            }
        } else {
            System.err.println("game not found");
        }
    }

    private void sendPlayersUpdate(Game game) throws IOException {
        byte[] data = Protocol.update(game);
        sendUpdate(game.getPlayer1(), data);
        sendUpdate(game.getPlayer2(), data);
        server.getBroadcaster().send(data);
    }

    private void sendUpdate(Player player, byte[] data) throws IOException {
        player.getOutputToUser().write(data);
        player.getOutputToUser().flush();
    }

    private void sendNewGame(Game game) throws IOException {
        game.getPlayer1().getOutputToUser().writeUTF("NEWGAME");
        game.getPlayer1().getOutputToUser().writeInt(game.getId());
        game.getPlayer1().getOutputToUser().writeInt(game.getPlayer1().getId());
        game.getPlayer1().getOutputToUser().writeInt(game.getPlayer2().getId());
        game.getPlayer1().getOutputToUser().writeInt(game.getTurnId());
        game.getPlayer1().getOutputToUser().flush();

        game.getPlayer2().getOutputToUser().writeUTF("NEWGAME");
        game.getPlayer2().getOutputToUser().writeInt(game.getId());
        game.getPlayer2().getOutputToUser().writeInt(game.getPlayer2().getId());
        game.getPlayer2().getOutputToUser().writeInt(game.getPlayer1().getId());
        game.getPlayer2().getOutputToUser().writeInt(game.getTurnId());
        game.getPlayer2().getOutputToUser().flush();
    }

    private void sendStrings(Player player, String... messages) throws IOException {
        for (String message : messages) {
            player.getOutputToUser().writeUTF(message);
        }
        player.getOutputToUser().flush();
    }
}

