package connectx.Hope;

import java.util.TreeSet;
import java.util.Random;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import connectx.CXBoard;
import connectx.CXCellState;
import connectx.CXGame;
import connectx.CXCell;
import connectx.CXGameState;
import connectx.CXPlayer;

public class Hope implements CXPlayer {
  private long[][][] zobristKeys;

  class Board extends CXBoard {
    public Board(int M, int N, int X) throws IllegalArgumentException {
      super(M, N, X);
      key = 0;
    }

    public Board(CXBoard b) throws IllegalArgumentException {
      super(b.M, b.N, b.X);
      key = 0;
      CXCell mkc[] = b.getMarkedCells();
      for (int i = 0; i < mkc.length; i++) {
        markColumn(mkc[i].j);
      }
    }

    private long key = 0;

    public long getHash() { return key; }

    public CXGameState markColumn(int col) throws IndexOutOfBoundsException, IllegalStateException {
      // System.out.println("Marking " + col + ", game state " + gameState);
			int row = super.RP[col];
      CXGameState res = super.markColumn(col);
      key ^= zobristKeys[row][col][currentPlayer % 2];    // xor di tutte le mosse fatte nella configurazione corrente
      return res;
    }

    public void unmarkColumn() throws IllegalStateException {
      CXCell oldc = MC.getLast();
      // System.out.println("Unmarking " + oldc.j + ", game state " + gameState);
      key ^= zobristKeys[oldc.i][oldc.j][currentPlayer % 2];
      super.unmarkColumn();
    }
  }

  private CXGameState myWin;
  private CXGameState yourWin;
  private int TIMEOUT;
  private long START;
  private int bestMove;
  private int evalAlphaBeta = Integer.MIN_VALUE;
  private CXCellState player;
  private CXCellState free;
  private Board localB;

  // cache
  private final int NOTFOUND = Integer.MIN_VALUE;
  private final int CACHE_SIZE = 256 * 1024 * 1024;
  private int nBits = (int)(Math.log(CACHE_SIZE)/Math.log(2));
  private int hashMask = Integer.parseUnsignedInt("1".repeat(nBits), 2);
  private int[] cache;     // hash map
  // TODO: remove after debug
  private int hashConflicts, hashWrite, hashAccess, hashMiss;


  // evaluating system
  private final int WIN = Integer.MAX_VALUE;
  private final int DRAW = 0;
  private final int LOSE = Integer.MIN_VALUE+1;


  /*
   * Default empty constructor
   */
  public Hope() {
  }

  public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
      System.out.println("ini");
    START = System.currentTimeMillis(); // Salva il tempo di inizio
    myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
    yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;

    player = first ? CXCellState.P1 : CXCellState.P2;
    free = CXCellState.FREE;

    TIMEOUT = timeout_in_secs;
    localB = new Board(M,N,X);

    Random random = new Random();
    zobristKeys = new long[M][N][2];
    cache = new int[CACHE_SIZE];
    hashConflicts = hashAccess = hashMiss = hashWrite = 0;

    try{      // generazione random
      for (int i = 0; i < zobristKeys.length; i++) {
        for (int j = 0; j < zobristKeys[i].length; j++) {
          if (i % 10 == 0) // check every 10 iterations
            checktime();

          zobristKeys[i][j][0] = random.nextLong();
          zobristKeys[i][j][1] = random.nextLong();
        }
      }

    }
    catch (TimeoutException e) {
      System.out.println("Timeout!");
    }
  }

  public int selectColumn(CXBoard B) {
    hashConflicts = hashAccess = hashMiss = hashWrite = 0;
    try {
      localB = new Board(B);

      // System.out.println(Arrays.toString(Arrays.stream(B.getMarkedCells())
      //         .map(c -> c.i + " " + c.j)
      //         .toArray(String[]::new)));
      // System.out.println(Arrays.toString(Arrays.stream(localB.getMarkedCells())
      //         .map(c -> c.i + " " + c.j)
      //         .toArray(String[]::new)));

      int sel = selectColumnForReal();
      System.out.println("access = " + hashAccess + ", miss = " + hashMiss + ", hashConflict = " + hashConflicts + ", hashWrite = " + hashWrite);
      return sel;
    } catch(IllegalStateException e){ 
      e.printStackTrace();
      return 0;
    }
  }

  public int selectSort(Integer[] A, int start) {
    int maxI = start;
    localB.markColumn(A[start]);
    int max = getCache();
    localB.unmarkColumn();

    for(int i = start; i < A.length; ++i) {
      localB.markColumn(A[i]);
      int val = getCache();
      localB.unmarkColumn();
      if(val > max) {
        maxI = i;
        max = val;
      }
    }

    int tmp = A[start];
    A[start] = A[maxI];
    A[maxI] = tmp;
    return A[start];
  }

  public int selectColumnForReal() {
    START = System.currentTimeMillis(); // Salva il tempo di inizio

    Integer[] FC = localB.getAvailableColumns();
    bestMove = FC[0];
  
    int eval = LOSE;
    int alpha = LOSE;
    int beta = WIN;
  
    if (FC.length == 1)       // Ultima mossa, rimane solo una colonna disponibile
      return FC[0];

    try {
      int currentDepth = 1;
      while (currentDepth <= localB.numOfFreeCells()) {  // massima profondità = numero di celle libere
        System.out.println("detph = " + currentDepth);
        eval = LOSE;
        alpha = LOSE;
        beta = WIN;

        for (int i = 0; i < FC.length; ++i) {
          int currentColumn = selectSort(FC, i);
          checktime();
          localB.markColumn(currentColumn);
          if (localB.gameState() == myWin)
            return currentColumn;

          int evaltmp = alphaBeta(false, alpha, beta, currentDepth);
          if (evaltmp > eval) {
            eval = evaltmp;
            bestMove = currentColumn;
          }

          localB.unmarkColumn();
        }

        currentDepth++; // Incrementa la profondità massima per la prossima iterazione
      }
    } catch (TimeoutException e) {
      System.out.println("Timeout!");
      return bestMove;
    }
    return bestMove;
  }

  private void addCache(int alphabetaEval) {
    ++hashWrite;
    long hash = localB.getHash();
    int key =(int) (hash & hashMask);   // prende i 24 bit meno significativi come chiave di indirizzamento
    if(cache[key] != 0) {
      ++hashConflicts;
    }
    cache[key] = alphabetaEval;
  }

  private int getCache() {
    ++hashAccess;
    long hash = localB.getHash();
    int key =(int) (hash & hashMask);   // prende i 24 bit meno significativi come chiave di indirizzamento
    if(cache[key] == 0) {
      ++hashMiss;
      return NOTFOUND;   // valore non trovato
    }
    return cache[key];
  }

  private int alphaBeta(boolean myTurn, int alpha, int beta, int depth) throws TimeoutException {
    int res = alphaBetaForReal(myTurn, alpha, beta, depth);
    addCache(res);
    return res;
  }

  /**
   * ALPHABETA
   */
  private int alphaBetaForReal(boolean myTurn, int alpha, int beta, int depth) throws TimeoutException {
    checktime();
    if (localB.gameState() != CXGameState.OPEN || depth == 0)
      evalAlphaBeta = evaluate();
    else if (myTurn) {
      evalAlphaBeta = LOSE;
      
      Integer[] FC = localB.getAvailableColumns();
      for (int i = 0; i < FC.length; ++i) {
        int currentCol = selectSort(FC, i);
        localB.markColumn(currentCol);
        evalAlphaBeta = Math.max(evalAlphaBeta, alphaBeta(false, alpha, beta, depth - 1));
        localB.unmarkColumn();
        alpha = Math.max(evalAlphaBeta, alpha);
        if (beta <= alpha)
          break;
      }
    } else {
      evalAlphaBeta = WIN;
      for (int currentCol : localB.getAvailableColumns()) {
        localB.markColumn(currentCol);
        evalAlphaBeta = Math.min(evalAlphaBeta, alphaBeta(true, alpha, beta, depth - 1));
        localB.unmarkColumn();
        beta = Math.min(evalAlphaBeta, beta);
        if (beta <= alpha)
          break;
      }
    }
    return evalAlphaBeta;
  }

  // funzione chiamata a fine partita O quando la depth utilizzata diventa 0
  private int evaluate(){
    int ret;
    if (localB.gameState() == myWin)
      ret = WIN;
    else if (localB.gameState() == yourWin)
      ret = LOSE;
    else if (localB.gameState() == CXGameState.DRAW)
      ret = DRAW;
    else // qui entriamo solo se il gioco è ancora aperto e nessun giocatore ha vinto
      ret = evaluateNonePosition();

    if(ret == 0)
      ret = 1;

    return ret;
  }

  private int evaluateNonePosition() {
    CXCellState[][] board = localB.getBoard();

    int evalRow = 0;
    int evalCol = 0;
    int evalDiag = 0;

    // Evaluate rows
    for (int i = 0; i < localB.M; i++) {
      for (int j = 0; j <= localB.N - localB.X; j++) {
        int countPlayer = 0; // Count of player's cells
        int countOpponent = 0; // Count of opponent's cells
        int countFree = 0; // Count of free cells

        for (int k = 0; k < localB.X; k++) {
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
    for (int i = 0; i <= localB.M - localB.X; i++) {
      for (int j = 0; j < localB.N; j++) {
        int countPlayer = 0; // Count of player's cells
        int countOpponent = 0; // Count of opponent's cells
        int countFree = 0; // Count of free cells

        for (int k = 0; k < localB.X; k++) {
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
    for (int i = 0; i <= localB.M - localB.X; i++) {
      for (int j = 0; j <= localB.N - localB.X; j++) {
        int countPlayer = 0; // Count of player's cells
        int countOpponent = 0; // Count of opponent's cells
        int countFree = 0; // Count of free cells

        for (int k = 0; k < localB.X; k++) {
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
    for (int i = localB.X - 1; i < localB.M; i++) {
      for (int j = 0; j <= localB.N - localB.X; j++) {
        int countPlayer = 0; // Count of player's cells
        int countOpponent = 0; // Count of opponent's cells
        int countFree = 0; // Count of free cells

        for (int k = 0; k < localB.X; k++) {
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
    int eval = evalRow + evalCol + evalDiag;

    return eval;
  }


  private void checktime() throws TimeoutException {
    if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0)) {    // tempo margine per evitare di andare in timeout
      throw new TimeoutException();
    }
  }

  public String playerName() {
    return "Hope";
  }

}