/** VERSIONE 2 - iterative deepening */

package connectx.Dorina;

import java.util.TreeSet;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import connectx.CXBoard;
import connectx.CXCellState;
import connectx.CXGame;
import connectx.CXGameState;
import connectx.CXPlayer;

public class Dorina implements CXPlayer {

  private CXGameState myWin;
  private CXGameState yourWin;
  private int TIMEOUT;
  private long START;
  private int bestMove;
  private int evalAlphaBeta = Integer.MIN_VALUE;
  private CXCellState player;
  private CXCellState free;

  /*
   * Sistema di valutazione
   */ 
  private final int WIN = Integer.MAX_VALUE;
  private final int DRAW = 0;
  private final int LOSE = Integer.MIN_VALUE;

  /*
   * Default empty constructor
   */
  public Dorina() {
  }

  /*
   * INIZIALIZZAZIONE  
   */
  public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
    myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
    yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;

    player = first ? CXCellState.P1 : CXCellState.P2;
    free = CXCellState.FREE;

    TIMEOUT = timeout_in_secs;
  }

  /*
   * SELEZIONE COLONNA
   */
  public int selectColumn(CXBoard B) {
    START = System.currentTimeMillis(); // Save starting

    Integer[] FC = B.getAvailableColumns();
    bestMove = FC[0];

    int eval = LOSE;
    int alpha = LOSE;
    int beta = WIN;

    if (B.numOfFreeCells() == B.M * B.N)              // prima mossa
      return B.N / 2; // java arrotonda automaticamente per difetto
    else if (B.numOfFreeCells() == ((B.M * B.N) - 1)) // seconda mossa
      return secondMove(B);
    else {
      if (FC.length == 1)      // una sola cella libera, ultima mossa
        return FC[0];

      try {
        for (int currentColumn : FC) {    // per ogni colonna libera
          checktime();
          B.markColumn(currentColumn);      // supponiamo di mettere il gettone nella colonna corrente
          if (B.gameState() == myWin)         // se fa vincere, la si seleziona
            return currentColumn;

          if (!enemyIsWinning(B)) {
            int currentEval = eval;
            int depth = 1;
            // iterative deepening
            while (depth <= B.numOfFreeCells()) {
              System.out.println("depth = " + depth + ",  eval = " + eval + ",  current eval = " + currentEval);
              checktime();
              currentEval = alphaBeta(B, false, alpha, beta, depth); /** PERCHÈ MYTURN È SETTATO A FALSE A PRESCINDERE????? */
              if (currentEval > eval) {
                eval = currentEval;
                bestMove = currentColumn;
                System.out.println("  best move = " + bestMove + ",  current column = " + currentColumn);
              }

              depth++;
            }
          }
        }

        B.unmarkColumn();
      } catch (Exception e) {
        System.out.println("Timeout!");
        return bestMove;
      }
    }

    return bestMove;
  }

  private int secondMove(CXBoard B) {
    if (B.N % 2 == 0) { // even columns
      if (B.cellState(B.M - 1, B.N / 2) == CXCellState.FREE) {
        return B.N / 2;
      } else {
        return (B.N / 2) - 1;
      }
    } else { // odd columns
      if (B.cellState(B.M - 1, (B.N - 1) / 2) == CXCellState.FREE) {
        return (B.N - 1) / 2;
      } else {
        return ((B.N - 1) / 2) - 1;
      }
    }
  }

  private Boolean enemyIsWinning(CXBoard B) {
    Boolean isWinning = false;
    for (int currentColumn : B.getAvailableColumns()) {
      B.markColumn(currentColumn);
      if (B.gameState() == yourWin) {
        isWinning = true;
        B.unmarkColumn();
        break;
      }
      B.unmarkColumn();
    }
    return isWinning;
  }

  /* ALPHABETA */
  private int alphaBeta(CXBoard B, boolean myTurn, int alpha, int beta, int depth) throws TimeoutException {
    checktime();
    if (B.gameState() != CXGameState.OPEN || depth <= 0) // se il gioco non è più aperto OPPURE depth <= 0 => termina
      evalAlphaBeta = evaluate(B);
    else if (myTurn) { // massimizzare
      evalAlphaBeta = LOSE; // Integer.MIN_VALUE
      for (int currentColumn : B.getAvailableColumns()) {
        B.markColumn(currentColumn);
        evalAlphaBeta = Math.max(evalAlphaBeta, alphaBeta(B, !myTurn, alpha, beta, depth - 1)); // si prende il massimo tra i valori generati minimizzando i sottoalberi (supponendo quale sia la mossa che farà l'avversario)
        B.unmarkColumn();
        alpha = Math.max(evalAlphaBeta, alpha);
        if (beta <= alpha)
          break;
      }
    } else { // minimizzare
      evalAlphaBeta = WIN; // Integer.MAX_VALUE
      for (int currentColumn : B.getAvailableColumns()) {
        B.markColumn(currentColumn);
        evalAlphaBeta = Math.min(evalAlphaBeta, alphaBeta(B, !myTurn, alpha, beta, depth - 1)); // si prende il minimo tra i valori generati massimizzando i sottoalberi
        B.unmarkColumn();
        beta = Math.min(evalAlphaBeta, beta);
        if (beta <= alpha)
          break;
      }
    }
    return evalAlphaBeta;
  }

  // funzione chiamata a fine partita OPPURE quando la depth utilizzata diventa 0
  private int evaluate(CXBoard B) throws TimeoutException {

    if (B.gameState() == myWin)
      return WIN;
    else if (B.gameState() == yourWin)
      return LOSE;
    else if (B.gameState() == CXGameState.DRAW)
      return DRAW;
    else // SOLO SE depth <= 0
      return evalNonePosition(B);
  }

  private int evalNonePosition(CXBoard B) throws TimeoutException {
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

          if (cellState == player)
            countPlayer++;
          else if (cellState == free)
            countFree++;
          else
            countOpponent++;
        }

        // Evaluate the row based on the counts
        if (countPlayer > 0 && countOpponent == 0)
          evalRow += countPlayer * countPlayer * countPlayer;
        else if (countOpponent > 0 && countPlayer == 0)
          evalRow -= countOpponent * countOpponent * countOpponent;

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

          if (cellState == player)
            countPlayer++;
          else if (cellState == free)
            countFree++;
          else
            countOpponent++;
        }

        // Evaluate the column based on the counts
        if (countPlayer > 0 && countOpponent == 0)
          evalCol += countPlayer * countPlayer * countPlayer;
        else if (countOpponent > 0 && countPlayer == 0)
          evalCol -= countOpponent * countOpponent * countOpponent;

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
          if (cellState == player)
            countPlayer++;
          else if (cellState == free)
            countFree++;
          else
            countOpponent++;
        }

        // Evaluate the diagonal based on the counts
        if (countPlayer > 0 && countOpponent == 0)
          evalDiag += countPlayer * countPlayer * countPlayer;
        else if (countOpponent > 0 && countPlayer == 0)
          evalDiag -= countOpponent * countOpponent * countOpponent;

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

          if (cellState == player)
            countPlayer++;
          else if (cellState == free)
            countFree++;
          else
            countOpponent++;
        }

        // Evaluate the reverse diagonal based on the counts
        if (countPlayer > 0 && countOpponent == 0)
          evalDiag += countPlayer * countPlayer * countPlayer;
        else if (countOpponent > 0 && countPlayer == 0)
          evalDiag -= countOpponent * countOpponent * countOpponent;

        // Bonus for having more free cells in the reverse diagonal
        evalDiag += countFree;
      }
    }

    // Combine the evaluations for rows, columns, and diagonals
    // System.out.println("evalRow = " + evalRow + " evalCol = " + evalCol + "
    // evalDiag = " + evalDiag);
    int eval = evalRow + evalCol + evalDiag;

    return eval;
  }

  private void checktime() throws TimeoutException {
    if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (90.0 / 100.0)) // tempo margine per evitare di
                                                                                   // andare in timeout
      throw new TimeoutException();
  }

  public String playerName() {
    return "Dorina";
  }

}