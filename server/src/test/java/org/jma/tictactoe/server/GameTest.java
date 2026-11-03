package org.jma.tictactoe.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTest {
    private static final int P1 = 7;
    private static final int P2 = 9;

    private Game game;

    @BeforeEach
    void setUp() throws IOException {
        game = new Game(1, TestPlayers.player(P1), TestPlayers.player(P2));
    }

    /** Plays moves {x, y} alternately, starting with player 1, and checks each one is accepted. */
    private void play(int[]... moves) {
        for (int i = 0; i < moves.length; i++) {
            int playerId = i % 2 == 0 ? P1 : P2;
            assertTrue(game.move(playerId, (byte) moves[i][0], (byte) moves[i][1]),
                    "move " + i + " should be accepted");
        }
    }

    @Test
    void newGameStartsWithPlayerOneAndEmptyBoard() {
        assertEquals(P1, game.getTurnId());
        assertEquals("STARTED", game.getState());
        assertFalse(game.isOver());
        assertArrayEquals(new byte[3][3], game.getMap());
    }

    @Test
    void playerOnePlacesCrossAndPassesTurn() {
        assertTrue(game.move(P1, (byte) 2, (byte) 0));
        assertEquals(1, game.getMap()[0][2]);
        assertEquals(P2, game.getTurnId());
    }

    @Test
    void playerTwoPlacesCircleAndPassesTurnBack() {
        play(new int[]{0, 0}, new int[]{1, 2});
        assertEquals(2, game.getMap()[2][1]);
        assertEquals(P1, game.getTurnId());
    }

    @Test
    void moveOutOfTurnIsRejected() {
        assertFalse(game.move(P2, (byte) 0, (byte) 0));
        assertEquals(0, game.getMap()[0][0]);
        assertEquals(P1, game.getTurnId());
    }

    @Test
    void playerCannotMoveTwiceInARow() {
        play(new int[]{1, 1});
        assertFalse(game.move(P1, (byte) 0, (byte) 0));
        assertEquals(P2, game.getTurnId());
    }

    @Test
    void moveOnOccupiedCellIsRejected() {
        play(new int[]{1, 1});
        assertFalse(game.move(P2, (byte) 1, (byte) 1));
        assertEquals(1, game.getMap()[1][1]);
        assertEquals(P2, game.getTurnId());
    }

    @Test
    void moveByUnknownPlayerIsRejected() {
        assertFalse(game.move(42, (byte) 0, (byte) 0));
        assertEquals(P1, game.getTurnId());
    }

    static Stream<Arguments> lines() {
        return Stream.of(
                Arguments.of("top row", new int[][]{{0, 0}, {1, 0}, {2, 0}}),
                Arguments.of("middle row", new int[][]{{0, 1}, {1, 1}, {2, 1}}),
                Arguments.of("bottom row", new int[][]{{0, 2}, {1, 2}, {2, 2}}),
                Arguments.of("left column", new int[][]{{0, 0}, {0, 1}, {0, 2}}),
                Arguments.of("middle column", new int[][]{{1, 0}, {1, 1}, {1, 2}}),
                Arguments.of("right column", new int[][]{{2, 0}, {2, 1}, {2, 2}}),
                Arguments.of("main diagonal", new int[][]{{0, 0}, {1, 1}, {2, 2}}),
                Arguments.of("anti-diagonal", new int[][]{{2, 0}, {1, 1}, {0, 2}}));
    }

    /** Cells {x, y} that are not on the given line. */
    private static int[][] cellsOff(int[][] line) {
        return Stream.of(0, 1, 2)
                .flatMap(x -> Stream.of(0, 1, 2).map(y -> new int[]{x, y}))
                .filter(cell -> Stream.of(line).noneMatch(l -> l[0] == cell[0] && l[1] == cell[1]))
                .toArray(int[][]::new);
    }

    @ParameterizedTest(name = "win on the {0}")
    @MethodSource("lines")
    void lineOfThreeWins(String name, int[][] line) {
        int[][] other = cellsOff(line);
        play(line[0], other[0], line[1], other[1], line[2]);
        assertEquals("END " + P1, game.getState());
        assertTrue(game.isOver());
    }

    @Test
    void playerTwoCanWin() {
        // X O X
        // X O .
        // . O .
        play(new int[]{0, 0}, new int[]{1, 0}, new int[]{2, 0}, new int[]{1, 1}, new int[]{0, 1}, new int[]{1, 2});
        assertEquals("END " + P2, game.getState());
        assertTrue(game.isOver());
    }

    @Test
    void fullBoardWithoutLineIsDraw() {
        // X O X
        // X O O
        // O X X
        play(new int[]{0, 0}, new int[]{1, 0}, new int[]{2, 0}, new int[]{1, 1}, new int[]{0, 1},
                new int[]{2, 1}, new int[]{1, 2}, new int[]{0, 2}, new int[]{2, 2});
        assertEquals("END 0", game.getState());
        assertTrue(game.isOver());
    }

    @Test
    void winOnLastCellIsWinNotDraw() {
        // X O X
        // O X O
        // O X X
        play(new int[]{0, 0}, new int[]{1, 0}, new int[]{2, 0}, new int[]{0, 1}, new int[]{1, 1},
                new int[]{2, 1}, new int[]{1, 2}, new int[]{0, 2}, new int[]{2, 2});
        assertEquals("END " + P1, game.getState());
    }

    @Test
    void gameIsNotOverBeforeLineOrFullBoard() {
        play(new int[]{0, 0}, new int[]{1, 1}, new int[]{2, 2}, new int[]{0, 2});
        assertFalse(game.isOver());
        assertEquals("STARTED", game.getState());
    }
}
