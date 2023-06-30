package connectx.Federico;

import java.util.TreeSet;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import connectx.CXBoard;
import connectx.CXCellState;
import connectx.CXGame;
import connectx.CXGameState;
import connectx.CXPlayer;

public class Federico implements CXPlayer {

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
  private final int WIN = 10000;
  private final int DRAW = 9999;
  private final int NONE = 1;
  private final int LOSE = 0;

  /*
   * Default empty constructor
   */
  public Federico() {
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

    int eval = Integer.MIN_VALUE;
    int alpha = Integer.MIN_VALUE;
    int beta = Integer.MAX_VALUE;

    if (B.numOfFreeCells() == B.M * B.N) { // first move
      return firstMove(B.N);
    } else {
      if (B.numOfFreeCells() == ((B.M * B.N) - 1)) { // second move
        return secondMove(B);
      } else {
        if (FC.length == 1) { // last move
          return FC[0];
        }

        try {

          depth = evaluateDepth(B.numOfFreeCells());

          for (int i : FC) {
            checktime();
            B.markColumn(i);
            if (B.gameState() == myWin) {
              return i;
            }

            if (!enemyIsWinning(B)) {
              // int evaltmp = miniMax(B, false, depth);
              int evaltmp = alphaBeta(B, false, alpha, beta, depth);
              if (evaltmp > eval) {
                eval = evaltmp;
                bestMove = i;
              }
            }

            B.unmarkColumn();
          }

        } catch (Exception e) {
          System.out.println("Timeout!");
          return bestMove;
        }

      }
    }
    return bestMove;
  }

  private int firstMove(int col) {
    if (col % 2 == 0) { // even columns
      return col / 2;
    } else { // odd columns
      return (col - 1) / 2;
    }
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
    for (int j : B.getAvailableColumns()) {
      B.markColumn(j);
      if (B.gameState() == yourWin) {
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
  private int alphaBeta(CXBoard B, Boolean ourPlayer, int alpha, int beta, int depth) throws TimeoutException {
    if (B.gameState() != CXGameState.OPEN || depth == 0) {
      evalAlphaBeta = evaluate(B);
    } else if (ourPlayer) {
      evalAlphaBeta = Integer.MIN_VALUE;
      for (int j : B.getAvailableColumns()) {
        B.markColumn(j);
        evalAlphaBeta = Math.max(evalAlphaBeta, alphaBeta(B, false, alpha, beta, depth - 1));
        B.unmarkColumn();
        alpha = Math.max(evalAlphaBeta, alpha);
        if (beta <= alpha)
          break;
      }
    } else {
      evalAlphaBeta = Integer.MAX_VALUE;
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

  private int evaluate(CXBoard B) throws TimeoutException {
    if (B.gameState() == myWin)
      return WIN;
    else if (B.gameState() == yourWin)
      return LOSE;
    else if (B.gameState() == CXGameState.DRAW)
      return DRAW;
    else {
      return evaluateNonePosition(B);
    }
  }

  private int evaluateNonePosition(CXBoard B) throws TimeoutException {
    CXCellState[][] board = B.getBoard();

    int evalRow = 0;
    int tmpRow = 0;
    int countRowCell = 0;

    for (int i = 0; i < B.M; i++) {
      tmpRow = 0;
      countRowCell = 0;
      for (int j = 0; j < B.N; j++) {
        checktime();
        if (board[i][j] == player) { // our cell
          tmpRow = tmpRow + 2;
          countRowCell++;
        } else if (board[i][j] == free) // free cell
        {
          tmpRow++;
          countRowCell++;
        } else // enemy cell
        {
          if (countRowCell > B.X) {
            tmpRow = 0;
          }
          countRowCell = 0;
        }
      }
      evalRow += tmpRow;
    }

    int evalCol = 0;
    int tmpCol = 0;
    int countColCell = 0;

    for (int i = 0; i < B.N; i++) {
      tmpCol = 0;
      countColCell = 0;
      for (int j = 0; j < B.M; j++) {
        checktime();
        if (board[j][i] == player) { // our cell
          tmpCol = tmpCol + 2;
          countColCell++;
        } else if (board[j][i] == free) // free cell
        {
          tmpCol++;
          countColCell++;
        } else // enemy cell
        {
          if (countColCell <= B.X) {
            tmpCol = 0;
          }
          countColCell = 0;
        }
      }
      evalCol += tmpCol;
    }

    int evalDiag1 = 0;
    int evalDiag2 = 0;
    int tmpDiag = 0;
    int countDiagCell = 0;

    // diagonale che parte da in alto a sinistra e scende in basso
    for (int i = 0; i < B.M - 1; i++) {
      if (i == 0) { // scorro tutta la riga 0 per trovare la altre diagonali
        for (int j = 0; j < B.N - 1; j++) {
          int x = i;
          int y = j;
          int count = 0;
          tmpDiag = 0;
          countDiagCell = 0;
          while (y < B.N && x < B.M) {
            if (board[x][y] == player) { // our cell
              tmpDiag = tmpDiag + 2;
              countDiagCell++;
            } else if (board[x][y] == free) // free cell
            {
              tmpDiag++;
              countDiagCell++;
            } else // enemy cell
            {
              if (countDiagCell <= B.X) {
                tmpDiag = 0;
              }
              countDiagCell = 0;
            }
            x++;
            y++;
            count++;
          }
          if (count >= B.X) {
            evalDiag1 += tmpDiag;
          }
        }
      } else {
        int x = i;
        int y = 0;
        int count = 0;
        tmpDiag = 0;
        countDiagCell = 0;
        while (y < B.N && x < B.M - 1) {
          if (board[x][y] == player) { // our cell
            tmpDiag = tmpDiag + 2;
            countDiagCell++;
          } else if (board[x][y] == free) // free cell
          {
            tmpDiag++;
            countDiagCell++;
          } else // enemy cell
          {
            if (countDiagCell <= B.X) {
              tmpDiag = 0;
            }
            countDiagCell = 0;
          }
          x++;
          y++;
          count++;
        }
        if (count >= B.X) {
          evalDiag1 += tmpDiag;
        }
      }
    }

    // diagonale che parte dal basso a sinistra e sale
    for (int i = 1; i < B.M; i++) {
      if (i == B.M - 1) { // scorro tutta la riga B.M per trovare la altre diagonali
        for (int j = 0; j < B.N - 1; j++) {
          int x = i;
          int y = j;
          int count = 0;
          tmpDiag = 0;
          countDiagCell = 0;
          while (x >= 0 && y < B.N) {
            if (board[x][y] == player) { // our cell
              tmpDiag = tmpDiag + 2;
              countDiagCell++;
            } else if (board[x][y] == free) // free cell
            {
              tmpDiag++;
              countDiagCell++;
            } else // enemy cell
            {
              if (countDiagCell <= B.X) {
                tmpDiag = 0;
              }
              countDiagCell = 0;
            }
            x--;
            y++;
            count++;
          }
          if (count >= B.X) {
            evalDiag2 += tmpDiag;
          }
        }
      } else {
        int x = i;
        int y = 0;
        int count = 0;
        tmpDiag = 0;
        countDiagCell = 0;
        while (x >= 0 && y < B.N) {
          if (board[x][y] == player) { // our cell
            tmpDiag = tmpDiag + 2;
            countDiagCell++;
          } else if (board[x][y] == free) // free cell
          {
            tmpDiag++;
            countDiagCell++;
          } else // enemy cell
          {
            if (countDiagCell <= B.X) {
              tmpDiag = 0;
            }
            countDiagCell = 0;
          }
          x--;
          y++;
          count++;
        }
        if (count >= B.X) {
          evalDiag2 += tmpDiag;
        }
      }
    }

    int totEvalDiag = evalDiag1 + evalDiag2;

    return NONE + evalCol + evalRow + totEvalDiag;
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
    return "Federico";
  }

}