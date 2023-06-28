 package connectx.ismaele;

 import connectx.CXPlayer;
 import connectx.CXBoard;
 import connectx.CXGameState;
 import connectx.CXCell;
 import java.util.TreeSet;
 import java.util.Random;
 import java.util.Arrays;
 import java.util.concurrent.TimeoutException;
 

 public class ismaele implements CXPlayer {
   private Random rand;
   private boolean first;
   private int  TIMEOUT;
   private long START;
   private int M;
   private int N;
   private int X;
   private int savedSolution = -1;   // soluzione ritornata in caso di superamento del timeout

 
   /* Default empty constructor */
   public ismaele() {
   }
 
   public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
     // New random seed for each game
     rand = new Random(System.currentTimeMillis());
     this.first = first;  // first == 1 -> meFirst, first == 0 -> advFirst 
     this.M = M;
     this.N = N;
     this.X = K;

     TIMEOUT = timeout_in_secs;
   }

   public int selectColumn(CXBoard B) {
     START = System.currentTimeMillis(); // Save starting time

     try {
      int depthMax = 6;
      return MiniMax(B, first, depthMax);
     } catch (TimeoutException e) {
      System.err.println("Timeout!!! Random column selected");
      return savedSolution;
     }
   }

   private int MiniMax(CXBoard B, boolean first, int depth) throws TimeoutException {
    if(first == true)   // me first moving
      
    //else    // adversary first moving

    return -1;
   }

   private void checktime() throws TimeoutException {
    if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
      throw new TimeoutException();
  }
 
/*
   private int AlphaBeta(CXBoard B, CXGameState state, int alpha, int beta) throws TimeoutException {
     
     return -1;
   }
*/
   /**
    * Check if we can block adversary's victory 
    *
    * Returns a blocking column if there is one, otherwise a random one
    */
   private int singleMoveBlock(CXBoard B, Integer[] L) throws TimeoutException {
     TreeSet<Integer> T = new TreeSet<Integer>(); // We collect here safe column indexes
 
     for(int i : L) {
       checktime();
       T.add(i); // We consider column i as a possible move
       B.markColumn(i);
 
       int j;
       boolean stop;
 
       for(j = 0, stop=false; j < L.length && !stop; j++) {
         //try {Thread.sleep((int)(0.2*1000*TIMEOUT));} catch (Exception e) {} // Uncomment to test timeout
         checktime();
         if(!B.fullColumn(L[j])) {
           CXGameState state = B.markColumn(L[j]);
           if (state == yourWin) {
             T.remove(i); // We ignore the i-th column as a possible move
             stop = true; // We don't need to check more
           }
           B.unmarkColumn(); // 
         }
       }
       B.unmarkColumn();
     }
 
     if (T.size() > 0) {
       Integer[] X = T.toArray(new Integer[T.size()]);
        return X[rand.nextInt(X.length)];
     } else {
       return L[rand.nextInt(L.length)];
     }
   }
 
   public String playerName() {
     return "ismaele";
   }
 }
 