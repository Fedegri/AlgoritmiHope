package connectx.MxLxPlayer;

import connectx.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


public class StreakBoard extends CXBoard {

    // protected HashMap<CXCell, List<List<CXCell>>> streaksMap;

    protected List<Streak> streaksP1;

    protected List<Streak> streaksP2;
    CXCellState[][] packageBoard; //This exposes the protected board

    public List<Streak> getStreaksP1() {
        return streaksP1;
    }

    public List<Streak> getStreaksP2() {
        return streaksP2;
    }

    /**
     * Create a board of size MxN and initialize the game parameters
     *
     * @param M Board rows
     * @param N Board columns
     * @param X Number of symbols to be aligned (horizontally, vertically, diagonally) for a win
     * @throws IllegalArgumentException If M,N are smaller than 1
     */
    public StreakBoard(int M, int N, int X) throws IllegalArgumentException {
        super(M, N, X);
        this.packageBoard = super.B;
        streaksP1 = new ArrayList<>();
        streaksP2 = new ArrayList<>();
    }

    public StreakBoard(CXBoard cxBoard) {
        super(cxBoard.M, cxBoard.N, cxBoard.X);
        this.packageBoard = super.B;
        streaksP1 = new ArrayList<>();
        streaksP2 = new ArrayList<>();

        for (CXCell c : cxBoard.getMarkedCells()) {
            this.markColumn(c.j);
            markCount--; // to balance out from copies.
        }

    }

    public static long markCount = 0;
    public static long unmarkCount = 0;
    @Override
    public CXGameState markColumn(int col) throws IndexOutOfBoundsException, IllegalStateException {
        markCount++;
        // Prendiamo qui il currentPlayer perché cambierà dopo il metodo super::markColumn
        final int currentPlayer = super.currentPlayer;

        final CXGameState gameState = super.markColumn(col);

        final CXCell lastCell = getLastMove();
        insertStreaksIntoCell(lastCell, currentPlayer);
        updateStreaks(lastCell, streaksP1);
        updateStreaks(lastCell, streaksP2);

        return gameState;
    }

    @Override
    public void unmarkColumn() throws IllegalStateException {
        unmarkCount++;
        final CXCell lastCellBeforeUndo = getLastMove();
        final int currentPlayer = this.currentPlayer;

        super.unmarkColumn(); // This will undo last move

        updateStreaksAfterUndoMove(lastCellBeforeUndo, currentPlayer);

    }

    /**
     * Da usare per inserire per la prima volta le scie potenziali
     * (in altre parole, dopo che abbiamo messo la pedina lì)
     *
     * @param cell cella di cui inserire le scie potenziali
     */
    private void insertStreaksIntoCell(CXCell cell, int currentPlayer) {
        final List<Streak> streaksList = calcStreaksForNewCell(cell);

        // streaksMap.put(cell, streaksList);

        for (Streak streak : streaksList) {
            if (currentPlayer == 0) {
                substituteOrAddStreak(streaksP1, streak);
            } else {
                substituteOrAddStreak(streaksP2, streak);
            }
        }
    }

    private void updateStreaks(CXCell lastCell, List<Streak> streaks) {
        for (Streak streak : streaks) {
            streak.checkValidity();
        }
        /*
        Iterator<Streak> streakIterator = streaks.iterator();

        while (streakIterator.hasNext()) {
            Streak streak = streakIterator.next();

            if (streak.cells.contains(lastCell)) {
                if (streak.state != lastCell.state && lastCell.state != CXCellState.FREE) {
                    // streakIterator.remove();
                    streak.checkValidity();
                } else {
                    int index = streak.cells.indexOf(lastCell);
                    streak.cells.set(index, lastCell);
                }
            }
        }*/
    }

    private void substituteOrAddStreak(List<Streak> streaks, Streak newStreak) {
        if (streaks.isEmpty()) {
            streaks.add(newStreak);
        } else {
            int index = streaks.indexOf(newStreak);
            if (index == -1) {
                streaks.add(newStreak);
            } else {
                // Update
                streaks.set(index, newStreak);
            }
        }
    }

    /**
     * Da usare per ripristinare l'integrità delle scie potenziali dopo un undo.
     *
     * @param cellMove mossa che è stata annullata
     */
    private void updateStreaksAfterUndoMove(CXCell cellMove, int currentPlayer) {

        if (currentPlayer == 0) {
            updateStreaksAfterUndoMove(streaksP1, cellMove);
        } else {
            updateStreaksAfterUndoMove(streaksP2, cellMove);
        }
    }

    private void updateStreaksAfterUndoMove(List<Streak> streaks, CXCell cellMove) {
        /*
        for (Streak streak : streaks) {
            // TODO: Optimize cycle
            for (int i = 0; i < streak.cells.size(); i++) {
                CXCell cell = streak.cells.get(i);
                if (cell.i == cellMove.i && cell.j == cellMove.j) {
                    streak.cells.set(i, new CXCell(cell.i, cell.j, CXCellState.FREE));
                    streak.checkValidity();
                }
            }
        }*/
    }

    /**
     *
     * @param cell cella per cui calcolare le scie potenziali
     *
     * @return lista di scie potenziali da questa cella
     */
    private List<Streak> calcStreaksForNewCell(CXCell cell) {
        List<Streak> streaks = new ArrayList<>();

        // B is CXCellState[M][N];
        // M numero di righe
        // N numero di colonne

        // X numero necessario per vincere nelle scie.

        // Ottenere tutte le scie potenziali che provengono da questa cella.

        // Ricordiamo che:
        // - Una scia è potenziale se contiene almeno una cella del nostro player
        //   e contiene per il resto celle vuote, oppure del nostro giocatore
        //   (quindi una scia NON può avere al suo interno celle avversarie)

        // - Ogni cella ha potenzialmente scie da ben 6 direzioni differenti (su, giù, sx, dx, e diagonali)
        //   di cui vanno calcolate le scie potenziali. Escludiamo basso perché lì
        //   tutte le eventuali scie sono già concrete.

        // - Una scia è potenziale solo se è di lunghezza pari ad almeno X.
        //   Quindi tutte le scie di lunghezza <X non vanno considerate.

        // L'interesse nelle scie potenziali è rilevare dove possiamo vincere sulla scacchiera
        // Prima che tale vittoria si sia materializzata.

        final int row = cell.i;
        final int col = cell.j;
        final CXCellState state = cell.state;

        final boolean enoughBottomSpace = M-row >= X; // Per le diagonali

        final boolean enoughTopSpace = X <= row + 1;
        final boolean enoughLeftSpace = X <= col + 1;
        final boolean enoughRightSpace = N-col >= X;

        // 1. Direzione in alto
        if (enoughTopSpace) { // C'è spazio in alto
            // La scia in alto è automaticamente potenziale in un inserimento

            List<CXCell> streak = new ArrayList<>(X);
            streak.add(cell);
            for (int i = 1; i <= X-1; i++) {
                streak.add(new CXCell(row-i, col, CXCellState.FREE));
            }

            streaks.add(Streak.getUniformStreak(this, streak, cell.state));
        }

        // 2. Direzione a sx
        if (enoughLeftSpace) {
            final List<CXCell> streak = getStreakFromDiagonal(cell, 0, -1);

            if (streak != null) {
                // streaks.add(Streak.getUniformStreak(streak, cell.state));
                substituteOrAddStreak(streaks, Streak.getUniformStreak(this, streak, cell.state));
            }
        } // Se X > col + 1, non c'è abbastanza spazio a sx

        // 3. Direzione a dx
        if (enoughRightSpace) {
            final List<CXCell> streak = getStreakFromDiagonal(cell, 0, +1);

            if (streak != null) {
                // streaks.add(Streak.getUniformStreak(streak, cell.state));
                substituteOrAddStreak(streaks, Streak.getUniformStreak(this, streak, cell.state));
            }
        }

        // 4a. Diagonale sx-upper
        if (enoughTopSpace && enoughLeftSpace) {
            final List<CXCell> streak = getStreakFromDiagonal(cell, -1, -1);

            if (streak != null) {
                // streaks.add(Streak.getUniformStreak(streak, cell.state));
                substituteOrAddStreak(streaks, Streak.getUniformStreak(this, streak, cell.state));
            }
        }

        // 4b. Diagonale dx-upper
        if (enoughTopSpace && enoughRightSpace) {
            final List<CXCell> streak = getStreakFromDiagonal(cell, -1, +1);

            if (streak != null) {
                // streaks.add(Streak.getUniformStreak(streak, cell.state));
                substituteOrAddStreak(streaks, Streak.getUniformStreak(this, streak, cell.state));
            }
        }

        // 5a. Diagonale sx-bottom
        if (enoughBottomSpace && enoughLeftSpace) {
            final List<CXCell> streak = getStreakFromDiagonal(cell, +1, -1);

            if (streak != null) {
                // streaks.add(Streak.getUniformStreak(streak, cell.state));
                substituteOrAddStreak(streaks, Streak.getUniformStreak(this, streak, cell.state));
            }
        }

        // 5b. Diagonale dx-bottom
        if (enoughBottomSpace && enoughRightSpace) {
            final List<CXCell> streak = getStreakFromDiagonal(cell, +1, +1);
            if (streak != null) {
                // streaks.add(Streak.getUniformStreak(streak, cell.state));
                substituteOrAddStreak(streaks, Streak.getUniformStreak(this, streak, cell.state));
            }
        }

        streaks.forEach(Streak::checkValidity);
        return streaks;
    }

    /**
     * Non controlla i boundaries.
     *
     * @param cell
     * @param rowDirection
     * @param colDirection
     * @return null se non c'è nessuna scia potenziale, altrimenti ritorna la lista della scia.
     */
    private List<CXCell> getStreakFromDiagonal(CXCell cell, int rowDirection, int colDirection) {
        List<CXCell> streak = null;

        final int row = cell.i;
        final int col = cell.j;
        final CXCellState state = cell.state;

        boolean compatible = true;

        // System.out.println("start col: " + col);
        for (int k = 1; k <= X-1; k++) {
            // direction top => k * direction is negative,
            // direction bottom => k * direction is positive
            final int cursorRow = row + k*rowDirection;

            final int cursorCol = col + k*colDirection;

            // System.out.println("cursorCol: " + cursorCol);
            // System.out.flush();

            final CXCellState cursorCellState = B[cursorRow][cursorCol];

            if (!(cursorCellState == CXCellState.FREE || cursorCellState == state)) {
                // C'è una cella nemica nella striscia potenziale

                compatible = false;
                break;
            }
        }

        if (compatible) {
            streak = new ArrayList<>(X);
            streak.add(cell);
            for (int k = 1; k <= X-1; k++) {
                final int cursorRow = row + k*rowDirection;

                final int cursorCol = col + k*colDirection;

                final CXCellState cursorCellState = B[cursorRow][cursorCol];

                streak.add(new CXCell(cursorRow, cursorCol, cursorCellState));
            }
        }
        return streak;
    }
}
