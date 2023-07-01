package connectx.MxLxPlayer;

import connectx.CXCellState;
import connectx.MxLxPlayer.CellCoord;
import connectx.MxLxPlayer.Streak;
import connectx.MxLxPlayer.StreakBoard;
import connectx.MxLxPlayer.DebugStreakDisplayer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class Heuristics {
    public DebugStreakDisplayer debugStreakDisplayer=new DebugStreakDisplayer();

    public boolean evenPosition(StreakBoard board) {
        int c1 = estimateConnectivity(board, CXCellState.P1);
        int c2 = estimateConnectivity(board, CXCellState.P2);

        return Math.abs(c1 - c2) < 20;
    }

    /*
     * public static int estimateDominance(StreakBoard board, CXCellState player) {
     * CXCellState opponent;
     * 
     * if (player == CXCellState.P1) {
     * opponent = CXCellState.P2;
     * } else {
     * opponent = CXCellState.P1;
     * }
     * 
     * return -estimateConnectivity(board, opponent);
     * }
     */

    public int score(StreakBoard board, CXCellState player, CXCellState playingPlayer) {
        final List<Streak> myStreaks;
        final List<Streak> opponentStreaks;

        CXCellState opponent;
        if (player == CXCellState.P1) {
            opponent = CXCellState.P2;

            myStreaks = board.getStreaksP1();
            opponentStreaks = board.getStreaksP2();

        } else {
            myStreaks = board.getStreaksP2();

            opponent = CXCellState.P1;
            opponentStreaks = board.getStreaksP1();
        }

        if (playingPlayer == player) {
            if (checkDoubleAttack(board, myStreaks)) {
                return 200000000;
            }
            if (heuristicNMoveWins(board, myStreaks, 1)) {
                return 100000000;
            }
            if (checkDoubleAttack(board, opponentStreaks)) {
                return -200000000;
            }
            // return a more heuritic eval

        } else {
            if (heuristicNMoveWins(board, opponentStreaks, 1)) {
                return -100000000;
            }
            if (checkDoubleAttack(board, myStreaks)) {
                return 200000000;
            }
        }
        return estimateConnectivity(board, player) - estimateConnectivity(board, opponent);
    }

    static int count = 0;

    public int score2(StreakBoard board, CXCellState player, CXCellState playingPlayer) {
        CXCellState opponent;
        if (player == CXCellState.P1) {
            opponent = CXCellState.P2;
        } else {
            opponent = CXCellState.P1;
        }
        long scoreA = (long) estimateConnectivity(board, player); // - estimateConnectivity(board, opponent); // (int)
                                                                  // ((float) estimateConnectivity(board, opponent) *
                                                                  // 0.5f);
        long scoreB = (long) -estimateConnectivity(board, opponent);
        // TODO: Remove after debug phase is finished
        if (count > 1000 && count < 1010) {
            System.err.println(
                    "[DEBUG] connectivity player " + player.toString() + ": " + estimateConnectivity(board, player));
        }
        count++;

        if (playingPlayer == player) {

            final List<Streak> streaksOpponent;
            if (playingPlayer == CXCellState.P1) {

                if (checkDoubleAttack(board, board.getStreaksP1())) {
                    return 200000000;
                }
                if (heuristicNMoveWins(board, board.getStreaksP1(), 1)) {
                    return 100000000;
                }
                streaksOpponent = board.getStreaksP2();
            } else {
                if (checkDoubleAttack(board, board.getStreaksP2())) {
                    return 200000000;
                }
                if (heuristicNMoveWins(board, board.getStreaksP2(), 1)) {
                    return 100000000;
                }
                streaksOpponent = board.getStreaksP1();
            }

            if (checkDoubleAttack(board, streaksOpponent)) {
                // OPPONENT HAS DOUBLE ATTACK
                return -200000000;
            }
        } else {
            final List<Streak> streaks;
            if (playingPlayer == CXCellState.P1) {

                if (heuristicNMoveWins(board, board.getStreaksP2(), 1)) {
                    return -100000000;
                }
                streaks = board.getStreaksP2();
            } else {
                if (heuristicNMoveWins(board, board.getStreaksP1(), 1)) {
                    return -100000000;
                }
                streaks = board.getStreaksP1();
            }

            if (checkDoubleAttack(board, streaks)) {
                // WE HAVE DOUBLE ATTACK
                return 200000000;
            }

        }

        return (int) (scoreA + scoreB);
    }

    public boolean heuristicNMoveWinsv2(StreakBoard sb,
            List<Streak> playerStreaks,
            int n) {

        int streaksCount = 0;
        for (Streak streak : playerStreaks) {
            if (!streak.isValid()) {
                continue;
            }

            int multiplier = 0;
            int numberOfCells = streak.getNumberOfCells();

            if (numberOfCells == sb.X - 1) {
                // Don't care if not about to win
                continue;
            }

            for (CellCoord cell : streak.getCells()) {
                if (cell.getState() != CXCellState.FREE) {
                    // Only care about free cells
                    continue;
                }

                // Check if a move can be made on here.
                if (cell.i == sb.M - 1 ||
                        sb.getBoard()[cell.i + 1][cell.j] != CXCellState.FREE) {

                    multiplier = 1;
                }
            }

            streaksCount += multiplier;
        }
        // System.out.println("There is a double attack for P2");
        return streaksCount >= n;
    }

    public boolean heuristicNMoveWins(StreakBoard sb,
            List<Streak> playerStreaks,
            int n) {

        int streaksCount = 0;
        for (Streak streak : playerStreaks) {
            int count = 0;
            int multiplier = 0;
            if (!streak.isValid()) {
                continue;
            }
            for (CellCoord cell : streak.getCells()) {
                if (cell.getState() == streak.state) {
                    count++;
                }
            }
            if (count == sb.X - 1) {
                //debugStreakDisplayer.addStreak(streak);
                for (CellCoord cell : streak.getCells()) {
                    if (cell.getState() == CXCellState.FREE) {
                        // Check if a move can be made on here.
                        if (cell.i == sb.M - 1 ||
                                sb.getBoard()[cell.i + 1][cell.j] != CXCellState.FREE) {

                            multiplier = 1;
                        }
                    }
                }
                streaksCount += multiplier;
            }
        }
        // System.out.println("There is a double attack for P2");
        return streaksCount >= n;
    }

    public boolean checkDoubleAttack(StreakBoard sb,
            List<Streak> playerStreaks) {

        return heuristicNMoveWins(sb, playerStreaks, 2);
    }

    public int estimateConnectivity(StreakBoard board, CXCellState player) {
        // Euristica:
        // +1 per ogni streak valida
        // +5 per ogni pedina in una streak
        // Se due scie hanno intersezione, il loro valore vale doppio

        List<Streak> streaks;

        if (player == CXCellState.P1) {
            streaks = board.getStreaksP1();

        } else {
            streaks = board.getStreaksP2();
        }

        streaks.forEach(Streak::checkValidity);

        final int validStreaksNumber = Streak.numberOfValidStreaks(streaks);

        final List<Streak> validStreaks = streaks.stream().filter(Streak::isValid).toList();

        int[] streaksScores = new int[validStreaks.size()];

        int streakMinus2Count = 0;
        int streakMinus1Count = 0;
        int streakFull = 0;
        // System.out.println("[DEBUG] validStreaks number: " + validStreaks.size());
        for (int i = 0; i < validStreaks.size(); i++) {
            int streakScore = 1;
            Streak streak = validStreaks.get(i);

            int multiplier = 1;

            // multiplier += ((streak.colEnd - streak.colStart) / 2) * 3;
            int count = 0;
            for (CellCoord cell : streak.getCells()) {
                if (cell.getState() != CXCellState.FREE) {
                    multiplier += 0;
                    if (cell.getState() != player) {
                        // System.out.println("Evento che non dovrebe mai accadere: " +
                        // streak.isValid());
                        // multiplier += 1;
                        // continue;
                    }

                    if (cell.getState() == player) {
                        // multiplier += 2;
                        count++;
                    }
                }
            }
            if (count == board.X - 3) {
                multiplier = 5;
            }
            if (count == board.X - 2) {
                multiplier = 10; // 30
                streakMinus2Count++;
            }
            if (count == board.X - 1) {
                // System.out.println("Big multiplier");
                multiplier = 20; // 100
                streakMinus1Count++;
            }
            if (count == streakFull) {
                streakFull++;
            }
            streakScore = streakScore * multiplier;

            streaksScores[i] = streakScore;
        }

        List<Integer> skipList = new ArrayList<>();
        for (int i = 0; i < validStreaks.size(); i++) {
            if (skipList.contains(i)) {
                continue;
            }
            for (int j = 0; j < validStreaks.size() / 2; j++) {
                if (i == j) {
                    continue;
                }
                if (skipList.contains(i)) {
                    continue;
                }

                if (Streak.doIntersect(validStreaks.get(i), validStreaks.get(j))) {
                    streaksScores[i] = (int) ((float) streaksScores[i] * 1);

                    skipList.add(i);
                }
            }
        }

        int bonusScore = 0;
        if (streakMinus1Count == 1) {
            bonusScore = 100;
        } else if (streakMinus1Count == 2) {
            bonusScore = 300;
        } else if (streakMinus1Count == 3) {
            bonusScore = 500;
        }

        if (streakFull > 0) {
            return 10000000 * streakFull;
        }

        return validStreaksNumber * 4 +
                streakMinus2Count * 2
                + bonusScore;

        // return IntStream.of(streaksScores).sum(); // validStreaksNumber * 2
        // + streakMinus2Count * 2
        // + bonusScore;

        /*
         * IntStream.of(streaksScores).sum()
         * + validStreaksNumber*2
         * + streakMinus2Count * 20
         * + streakMinus1Count * 100;
         */
    }
}
