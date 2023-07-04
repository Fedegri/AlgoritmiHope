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
			int row = super.RP[col];
      CXGameState res = super.markColumn(col);
      key ^= zobristKeys[row][col][currentPlayer % 2];    // xor di tutte le mosse fatte nella configurazione corrente
      return res;
    }

    public void unmarkColumn() throws IllegalStateException {
      CXCell oldc = MC.getLast();
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

  /*
   * SELECT COLUMN
   */
  public int selectColumn(CXBoard B) {
    hashConflicts = hashAccess = hashMiss = hashWrite = 0;
    try {
      localB = new Board(B);

      START = System.currentTimeMillis(); // Salva il tempo di inizio

      Integer[] FC = localB.getAvailableColumns();
      bestMove = FC[0];
    
      int eval;
      int alpha;
      int beta;
      boolean marked = false;
    
      if (FC.length == 1)       // Ultima mossa, rimane solo una colonna disponibile
        return FC[0];

      try {
        int currentDepth = 1;
        while (currentDepth <= localB.numOfFreeCells()) {  // massima profondità = numero di celle libere
          eval = LOSE;
          alpha = LOSE;
          beta = WIN;

          for (int i = 0; i < FC.length; ++i) {       // per ogni colonna libera (FreeColumn)
            int currentColumn = selectSort(FC, i);      // selectSort mette in testa a FC la colonna con valore alpha-beta maggiore
            checktime();
            localB.markColumn(currentColumn);
            marked = true;
            if (localB.gameState() == myWin){           // se fa vincere il giocatore, termina ritornando la colonna corrente
              localB.unmarkColumn();
              return currentColumn;
            }

            int evaltmp = alphaBeta(false, alpha, beta, currentDepth);
            if (evaltmp > eval) {
              eval = evaltmp;
              bestMove = currentColumn;
            }

            localB.unmarkColumn();
            marked = false;
          }

          currentDepth++;     // Incrementa la profondità massima per la prossima iterazione
        }
      } catch (TimeoutException e) {
        System.out.println("Timeout: best move returned");
        if (marked)
          localB.unmarkColumn();
          
        return bestMove;
      }
      //System.out.println("access = " + hashAccess + ", miss = " + hashMiss + ", hashConflict = " + hashConflicts + ", hashWrite = " + hashWrite);

      return bestMove;
    }
    catch(IllegalStateException e){ 
      e.printStackTrace();
      return 0;
    }
  }

  /*
   * SELECT SORT
   */
  public int selectSort(Integer[] A, int start) {
    int maxIndex = start;
    localB.markColumn(A[start]);
    int max = getCache();     // get del valore alpha-beta dalla cache, se presente
    localB.unmarkColumn();

    for(int i = start + 1; i < A.length; ++i) {
      localB.markColumn(A[i]);
      int val = getCache();
      localB.unmarkColumn();
      if(val > max) {
        maxIndex = i;
        max = val;
      }
    }

    int tmp = A[start];         // mette in testa il valore maggiore trovato
    A[start] = A[maxIndex];
    A[maxIndex] = tmp;
    return A[start];
  }

  /*
   * CACHE: addCache
   */
  private void addCache(int alphabetaEval) {
    ++hashWrite;
    long hash = localB.getHash();
    int key =(int) (hash & hashMask);   // prende i 24 bit meno significativi come chiave di indirizzamento
    if(cache[key] != 0) {
      ++hashConflicts;
    }
    cache[key] = alphabetaEval;
  }

  /*
   * CACHE: getCache
   */
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

  /**
   * ALPHABETA
   */
  private int alphaBeta(boolean myTurn, int alpha, int beta, int depth) throws TimeoutException {
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
    addCache(evalAlphaBeta);
    return evalAlphaBeta;
  }

  /*
   * EVALUATE: fine partita 
   */
  // funzione chiamata a fine partita OPPURE quando la depth utilizzata in alpha-beta è 0
  private int evaluate(){
    int ret;
    if (localB.gameState() == myWin)                  // fine partita: WIN
      ret = WIN;
    else if (localB.gameState() == yourWin)           // fine partita: LOSE
      ret = LOSE;
    else if (localB.gameState() == CXGameState.DRAW)  // fine partita: DRAW
      ret = DRAW;
    else                                              // depth = 0 e partita ancora in gioco
      ret = evaluateHeuristic();

    /* Se return è 0, allora la partita è terminata in DRAW.
       Si incrementa il valore di 1 poichè il valore 0 viene usato come indicatore di cella vuota in cache */ 
    if(ret == 0)        
      ret = 1;

    return ret;
  }

  /*
   * FUNZIONE DI VALUTAZIONE EURISTICA
   */
  private int evaluateHeuristic() {
    CXCellState[][] copyBoard = localB.getBoard();

    int evalRow = 0;
    int evalCol = 0;
    int evalDiag = 0;

    // Valutazione righe  (suppongo M=7,N=7,X=5)
    for (int i = 0; i < localB.M; i++) {                  // per ogni riga  (7 righe)
      for (int j = 0; j <= localB.N - localB.X; j++) {      // per ogni colonna da 0 a N-X    (0 - 7-5=2)
        int countPlayer = 0;    // celle del giocatore  
        int countOpponent = 0;  // celle dell'avversario
        int countFree = 0;      // celle libere

        /* analizza tutte le possibili sequenze di 5 gettoni nella riga corrente */
        for (int x = 0; x < localB.X; x++) {                  // per ogni x da 0 a X-1     (0 - 4  ->  j=0: 0,1,2,3,4 -> j=1:1,2,3,4,5 -> j=2:2,3,4,5,6)
          CXCellState cellState = copyBoard[i][j + x];          // controlla il giocatore di appartenenza di ogni cella e incrementa il contatore relativo

          if (cellState == player)
            countPlayer++;
          else if (cellState == free)
            countFree++;
          else
            countOpponent++;
        }

        // valuta la riga basandosi sul numero di celle 
        if (countPlayer > 0 && countOpponent == 0)                    // ci sono solo dei gettoni del giocatore, 0 dell'avversario
          evalRow += countPlayer * countPlayer * countPlayer;             // incrementa il valore della riga
        else if (countOpponent > 0 && countPlayer == 0)               // ci sono solo dei gettoni dell'avversario, 0 del giocatore
          evalRow -= countOpponent * countOpponent * countOpponent;       // decrementa il valore della riga

        // bonus se ci sono celle libere nella riga
        evalRow += countFree;
      }
    }

    // Valutazione colonne
    for (int i = 0; i <= localB.M - localB.X; i++) {
      for (int j = 0; j < localB.N; j++) {
        int countPlayer = 0;    // celle del giocatore  
        int countOpponent = 0;  // celle dell'avversario
        int countFree = 0;      // celle libere

        /* analizza tutte le possibili sequenze di 5 gettoni nella colonna corrente */
        for (int x = 0; x < localB.X; x++) {
          CXCellState cellState = copyBoard[i + x][j];    // controlla il giocatore di appartenenza di ogni cella e incrementa il contatore relativo

          if (cellState == player)
            countPlayer++;
          else if (cellState == free)
            countFree++;
          else
            countOpponent++;
        }

        // valuta la colonna basandosi sul numero di celle 
        if (countPlayer > 0 && countOpponent == 0)                    // ci sono solo dei gettoni del giocatore, 0 dell'avversario
          evalCol += countPlayer * countPlayer * countPlayer;             // incrementa il valore della colonna
        else if (countOpponent > 0 && countPlayer == 0)               // ci sono solo dei gettoni dell'avversario, 0 del giocatore
          evalCol -= countOpponent * countOpponent * countOpponent;       // decrementa il valore della colonna

        // bonus se ci sono celle libere nella colonna
        evalCol += countFree;
      }
    }

    // Valutazione diagonali  (suppongo M=7,N=7,X=5)
    for (int i = 0; i <= localB.M - localB.X; i++) {        // righe da 0 a M-X       (0 - 7-5=2)
      for (int j = 0; j <= localB.N - localB.X; j++) {      // colonne da 0 a N-X     (0 - 7-5=2)
        int countPlayer = 0;    // celle del giocatore  
        int countOpponent = 0;  // celle dell'avversario
        int countFree = 0;      // celle libere

        /* analizza tutte le possibili sequenze di 5 gettoni diagonali sulla matrice */
        for (int x = 0; x < localB.X; x++) {          // per ogni x da 0 a X-1     (0 - 4  ->  j=0:0,1,2,3,4 -> j=1:1,2,3,4,5 -> j=2:2,3,4,5,6
                                                      //                                       i=0:0,1,2,3,4 -> i=1:1,2,3,4,5 -> i=2:2,3,4,5,6)
          CXCellState cellState = copyBoard[i + x][j + x];

          if (cellState == player)
            countPlayer++;
          else if (cellState == free)
            countFree++;
          else
            countOpponent++;
        }

        // valuta la diagonale basandosi sul numero di celle 
        if (countPlayer > 0 && countOpponent == 0)                    // ci sono solo dei gettoni del giocatore, 0 dell'avversario
          evalDiag += countPlayer * countPlayer * countPlayer;             // incrementa il valore della diagonale
        else if (countOpponent > 0 && countPlayer == 0)               // ci sono solo dei gettoni dell'avversario, 0 del giocatore
          evalDiag -= countOpponent * countOpponent * countOpponent;       // decrementa il valore della diagonale

        // bonus se ci sono celle libere nella diagonale
        evalDiag += countFree;
      }
    }

    // Valutazione diagonali inverse  (suppongo M=7,N=7,X=5)
    for (int i = localB.X - 1; i < localB.M; i++) {         // righe da X-1 a M-1   (5-1=4 - 6)
      for (int j = 0; j <= localB.N - localB.X; j++) {      // colonne da 0 a N-X   (0 - 7-5=2)
        int countPlayer = 0;    // celle del giocatore  
        int countOpponent = 0;  // celle dell'avversario
        int countFree = 0;      // celle libere

        /* analizza tutte le possibili sequenze di 5 gettoni diagonali inverse sulla matrice */
        for (int x = 0; x < localB.X; x++) {                    // per ogni x da 0 a X-1     (0 - 4  ->  j=0:0,1,2,3,4 -> j=1:1,2,3,4,5 -> j=2:2,3,4,5,6
                                                                //                                       i=4:4,3,2,1,0 -> i=5:4,3,2,1,0 -> i=6:4,3,2,1,0)
          CXCellState cellState = copyBoard[i - x][j + x];

          if (cellState == player)
            countPlayer++;
          else if (cellState == free)
            countFree++;
          else
            countOpponent++;
        }

        // valuta la diagonale inversa basandosi sul numero di celle 
        if (countPlayer > 0 && countOpponent == 0)                    // ci sono solo dei gettoni del giocatore, 0 dell'avversario
          evalDiag += countPlayer * countPlayer * countPlayer;             // incrementa il valore della diagonale
        else if (countOpponent > 0 && countPlayer == 0)               // ci sono solo dei gettoni dell'avversario, 0 del giocatore
          evalDiag -= countOpponent * countOpponent * countOpponent;       // decrementa il valore della diagonale

        // bonus se ci sono celle libere nella diagonale
        evalDiag += countFree;
      }
    }

    // il valore totale corrisponde alla somma delle valutazioni di righe, colonne e diagonali
    int eval = evalRow + evalCol + evalDiag;

    return eval;
  }


  private void checktime() throws TimeoutException {
    if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0)) {
      throw new TimeoutException();
    }
  }

  public String playerName() {
    return "Hope";
  }

}