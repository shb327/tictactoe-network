package org.jma.tictactoe.server;

class Game {
    private int id;
    private Player player1;
    private Player player2;
    private int turnId;
    private String state;
    private byte[][] map = new byte[3][3];

    Game(int id, Player player1, Player player2) {
        this.id = id;
        this.player1 = player1;
        this.player2 = player2;
        this.state = "STARTED";
        this.turnId = player1.getId();
    }

    int getId() {
        return id;
    }

    Player getPlayer1() {
        return player1;
    }

    Player getPlayer2() {
        return player2;
    }

    int getTurnId() {
        return turnId;
    }

    String getState() {
        return state;
    }

    byte[][] getMap() {
        return map;
    }

    void setTurnId(int turnId) {
        this.turnId = turnId;
    }

    void setState(String state) {
        this.state = state;
    }

    boolean isOver() {
        return state.startsWith("END");
    }

    boolean move(int playerId, byte x, byte y) {
        if (isOver() || x < 0 || x > 2 || y < 0 || y > 2) {
            return false;
        }
        if (turnId != playerId || map[y][x] != 0) {
            return false;
        }
        if (turnId == player1.getId()) {
            map[y][x] = 1;
            turnId = player2.getId();
        } else {
            map[y][x] = 2;
            turnId = player1.getId();
        }
        if (checkCondition((byte) 1)) {
            state = "END " + player1.getId();
        } else if (checkCondition((byte) 2)) {
            state = "END " + player2.getId();
        } else if (isFull()) {
            state = "END 0";
        }
        return true;
    }

    private boolean checkCondition(byte player) {
        for (int i = 0; i < 3; i++) {
            if (map[i][0] == player && map[i][1] == player && map[i][2] == player) {
                return true;
            }
            if (map[0][i] == player && map[1][i] == player && map[2][i] == player) {
                return true;
            }
        }
        return (map[0][0] == player && map[1][1] == player && map[2][2] == player) ||
                (map[2][0] == player && map[1][1] == player && map[0][2] == player);
    }

    private boolean isFull() {
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                if (map[x][y] == 0) return false;
            }
        }
        return true;
    }
}
