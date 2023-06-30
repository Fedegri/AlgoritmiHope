package connectx.Morales;

import java.util.TreeSet;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import connectx.CXBoard;
import connectx.CXCellState;
import connectx.CXGame;
import connectx.CXGameState;
import connectx.CXPlayer;

public class Morales implements CXPlayer {

  private CXGameState myWin;
  private CXGameState yourWin;
  private int TIMEOUT;
  private long START;
  private int bestMove;
  private int evalAlphaBeta = Integer.MIN_VALUE;
  private CXCellState player;
  private CXCellState free;
  private int depth;

  // evaluating system
  private final int WIN = Integer.MAX_VALUE;
  private final int DRAW = 0;
  private final int LOSE = Integer.MIN_VALUE;

  /*
   * Default empty constructor
   */
  public Morales() {
  }

  public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
    myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
    yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;

    player = first ? CXCellState.P1 : CXCellState.P2;
    free = CXCellState.FREE;

    initDepth(M, N);

    TIMEOUT = timeout_in_secs;
  }

  private void initDepth(int row, int cols) {
    if (cols == 4) {
      if (row == 4)
        depth = 14; // 4 X 4
      else if (row == 5)
        depth = 18; // 5 X 4
      else if (row == 6)
        depth = 22; // 6 X 4
      else
        depth = 13; // 7 X 4
    } else if (cols == 5) {
      if (row == 4)
        depth = 18; // 4 X 5
      else
        depth = 11; // 5 X 5, 6 X 5, 7 X 5
    } else if (cols == 6) { // 4 X 6, 5 X 6, 6 X 6, 7 X 6
      depth = 10;
    } else if (cols == 7) { // 4 X 7, 5 X 7, 6 X 7, 7 X 7
      depth = 8;
    } else if (cols == 25) { // 25 X 25
      depth = 3;
    } else if (cols == 50) { // 50 X 50
      depth = 2;
    } else { // 100 X 100
      depth = 1;
    }
  }

  public int selectColumn(CXBoard B) {
    START = System.currentTimeMillis(); // Save starting

    Integer[] FC = B.getAvailableColumns();
    bestMove = FC[0];

    int eval = LOSE;
    int alpha = LOSE;
    int beta = WIN;

    if (B.numOfFreeCells() == B.M * B.N)              // prima mossa
      return B.N / 2;    // java arrotonda automaticamente per difetto
    else
      if (B.numOfFreeCells() == ((B.M * B.N) - 1))    // seconda mossa
        return secondMove(B);
      else{
        if (FC.length == 1)    // una sola cella libera, ultima mossa
          return FC[0];

        try {
          depth = evaluateDepth(B.numOfFreeCells());

          for (int currentColumn : FC) {
            checktime();
            B.markColumn(currentColumn);
            if (B.gameState() == myWin)
              return currentColumn;

            if (!enemyIsWinning(B)) {
              int evaltmp = alphaBeta(B, false, alpha, beta, depth);
              if (evaltmp > eval) {
                eval = evaltmp;
                bestMove = currentColumn;
              }
            }

            B.unmarkColumn();
          }

        } catch (Exception e) {
          System.out.println("Timeout!");
          return bestMove;
        }
      }
    return bestMove;
  }

  private int secondMove(CXBoard B) {
    if (B.N % 2 == 0) // colonne pari
      if (B.cellState(B.M - 1, B.N / 2) == CXCellState.FREE)
        return B.N / 2;
      else
        return (B.N / 2) - 1;
    else              // collonne dispari
      if (B.cellState(B.M - 1, (B.N - 1) / 2) == CXCellState.FREE)
        return (B.N - 1) / 2;
      else
        return ((B.N - 1) / 2) - 1;
  }

  private boolean enemyIsWinning(CXBoard B) {
    boolean isWinning = false;
    for (int currentColumn : B.getAvailableColumns()) {   // per ogni colonna disponibile:
      B.markColumn(currentColumn);
      if (B.gameState() == yourWin) {                         // se scegliendo quella colonna, l'avversario è in vantaggio
        isWinning = true;
        B.unmarkColumn();
        break;
      }
      B.unmarkColumn();
    }
    return isWinning;
  }

  /**
   * ALPHABETA
   */
  private int alphaBeta(CXBoard B, boolean myTurn, int alpha, int beta, int depth) throws TimeoutException {
    if (B.gameState() != CXGameState.OPEN || depth == 0) {
      evalAlphaBeta = evaluate(B);
    } else if (myTurn) {
      evalAlphaBeta = LOSE;
      for (int j : B.getAvailableColumns()) {
        B.markColumn(j);
        evalAlphaBeta = Math.max(evalAlphaBeta, alphaBeta(B, false, alpha, beta, depth - 1));
        B.unmarkColumn();
        alpha = Math.max(evalAlphaBeta, alpha);
        if (beta <= alpha)
          break;
      }
    } else {
      evalAlphaBeta = WIN;
      for (int k : B.getAvailableColumns()) {
        B.markColumn(k);
        evalAlphaBeta = Math.min(evalAlphaBeta, alphaBeta(B, true, alpha, beta, depth - 1));
        B.unmarkColumn();
        beta = Math.min(evalAlphaBeta, beta);
        if (beta <= alpha)
          break;
      }
    }
    return evalAlphaBeta;
  }

  // funzione chiamata a fine partita O quando la depth utilizzata diventa 0
  private int evaluate(CXBoard B) throws TimeoutException {

    if (B.gameState() == myWin)
      return WIN;
    else if (B.gameState() == yourWin)
      return LOSE;
    else if (B.gameState() == CXGameState.DRAW)
      return DRAW;
    else { // qui entriamo solo se il gioco è ancora aperto e nessun giocatore ha vinto
      return evaluateNonePosition(B);
    }
  }

  private int evaluateNonePosition(CXBoard B) throws TimeoutException {
    CXCellState[][] board = B.getBoard();

    int evalRow = 0;
    int evalCol = 0;
    int evalDiag = 0;

    // Evaluate rows
    for (int i = 0; i < B.M; i++) {
      for (int j = 0; j <= B.N - B.X; j++) {
        int countPlayer = 0; // Count of player's cells
        int countOpponent = 0; // Count of opponent's cells
        int countFree = 0; // Count of free cells

        for (int k = 0; k < B.X; k++) {
          CXCellState cellState = board[i][j + k];

          if (cellState == player) {
            countPlayer++;
          } else if (cellState == free) {
            countFree++;
          } else {
            countOpponent++;
          }
        }

        // Evaluate the row based on the counts
        if (countPlayer > 0 && countOpponent == 0) {
          evalRow += countPlayer * countPlayer * countPlayer;
        } else if (countOpponent > 0 && countPlayer == 0) {
          evalRow -= countOpponent * countOpponent * countOpponent;
        }

        // Bonus for having more free cells in the row
        evalRow += countFree;
      }
    }

    // Evaluate columns
    for (int i = 0; i <= B.M - B.X; i++) {
      for (int j = 0; j < B.N; j++) {
        int countPlayer = 0; // Count of player's cells
        int countOpponent = 0; // Count of opponent's cells
        int countFree = 0; // Count of free cells

        for (int k = 0; k < B.X; k++) {
          CXCellState cellState = board[i + k][j];

          if (cellState == player) {
            countPlayer++;
          } else if (cellState == free) {
            countFree++;
          } else {
            countOpponent++;
          }
        }

        // Evaluate the column based on the counts
        if (countPlayer > 0 && countOpponent == 0) {
          evalCol += countPlayer * countPlayer * countPlayer;
        } else if (countOpponent > 0 && countPlayer == 0) {
          evalCol -= countOpponent * countOpponent * countOpponent;
        }

        // Bonus for having more free cells in the column
        evalCol += countFree;
      }
    }

    // Evaluate diagonals
    for (int i = 0; i <= B.M - B.X; i++) {
      for (int j = 0; j <= B.N - B.X; j++) {
        int countPlayer = 0; // Count of player's cells
        int countOpponent = 0; // Count of opponent's cells
        int countFree = 0; // Count of free cells

        for (int k = 0; k < B.X; k++) {
          CXCellState cellState = board[i + k][j + k];
          if (cellState == player) {
            countPlayer++;
          } else if (cellState == free) {
            countFree++;
          } else {
            countOpponent++;
          }
        }

        // Evaluate the diagonal based on the counts
        if (countPlayer > 0 && countOpponent == 0) {
          evalDiag += countPlayer * countPlayer * countPlayer;
        } else if (countOpponent > 0 && countPlayer == 0) {
          evalDiag -= countOpponent * countOpponent * countOpponent;
        }

        // Bonus for having more free cells in the diagonal
        evalDiag += countFree;
      }
    }

    // Evaluate reverse diagonals
    for (int i = B.X - 1; i < B.M; i++) {
      for (int j = 0; j <= B.N - B.X; j++) {
        int countPlayer = 0; // Count of player's cells
        int countOpponent = 0; // Count of opponent's cells
        int countFree = 0; // Count of free cells

        for (int k = 0; k < B.X; k++) {
          CXCellState cellState = board[i - k][j + k];

          if (cellState == player) {
            countPlayer++;
          } else if (cellState == free) {
            countFree++;
          } else {
            countOpponent++;
          }
        }

        // Evaluate the reverse diagonal based on the counts
        if (countPlayer > 0 && countOpponent == 0) {
          evalDiag += countPlayer * countPlayer * countPlayer;
        } else if (countOpponent > 0 && countPlayer == 0) {
          evalDiag -= countOpponent * countOpponent * countOpponent;
        }

        // Bonus for having more free cells in the reverse diagonal
        evalDiag += countFree;
      }
    }

    // Combine the evaluations for rows, columns, and diagonals
    int eval = evalRow + evalCol + evalDiag;

    return eval;
  }

  // evaluating depth
  private int evaluateDepth(int nFC) {
    if (depth == nFC) {
      return nFC - 1;
    } else
      return depth;
  }

  private void checktime() throws TimeoutException {
    if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0)) {
      throw new TimeoutException();
    }
  }

  public String playerName() {
    return "Morales";
  }

}
