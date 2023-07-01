package connectx.MxLxPlayer;

import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCellState;
import java.util.concurrent.TimeoutException;
import java.util.LinkedList;
import java.util.List;
import connectx.MxLxPlayer.IllegalyEfficientBoard;
import connectx.MxLxPlayer.Streak;
import connectx.MxLxPlayer.StreakBoard;

public class CriticalMoves {
  /*
   * Returns winning move column (> 0) if can win in a single move
   * Otherwise returns -1
   * 
   * REMOVED: checktime.
   * Note:
   * For loops are fast, no reason to time, wasted computation for a function
   * that is called a lot.
   * 
   * There is an argument to make that it is useful to have something that is
   * always
   * called throw TimeoutExceptions when it is time to return but if there is a
   * bug
   * somewhere with timing good luck finding it!
   * 
   * 
   * B: Board
   * L: List of available columns
   * myWin: In what gameState is it considered my win
   * timeKeeper: TimeKeeper Instance to check the time
   */

  static public int singleMoveWin(CXBoard B, Integer[] L, CXGameState myWin) {
    for (Integer i : L) {
      CXGameState state = B.markColumn(i);
      B.unmarkColumn();
      if (state == myWin)
        return i; // Winning column found: return immediately
    }
    return -1;
  }


  /*
   * Single move block implementation was O(L.size()^{2}), which is bad.
   * We can make it O(L.size())
   * Returns -1 if no need to block
   * If no way to block will still return the first in case opposite AI is dumb
   * and we can block
   * both in time :)
   * 
   * B: board
   * L: available moves
   * yourWin: GameState in which the opponent wins
   */

  static public int singleMoveBlock(CXBoard B, Integer[] L, CXGameState yourWin) {

    int ret = -1;
    IllegalyEfficientBoard.swapCurrentPlayer(B);
    for (int i : L) {
      CXGameState marked = B.markColumn(i);
      B.unmarkColumn();
      if (marked == yourWin) {
        ret = i;
        break;
      }
    }
    IllegalyEfficientBoard.swapCurrentPlayer(B);
    return ret;
  }

  static public int localizedSingleMoveBlock(CXBoard B, Integer localColumn, CXGameState yourWin){
    int ret = -1;
    IllegalyEfficientBoard.swapCurrentPlayer(B);

    int nCols = B.N;
    int nConnect = B.X;

    int startIndex = Math.max(0, localColumn - nConnect);
    int endIndex = Math.min(nCols, localColumn + nConnect);
    
    for (int i=startIndex; i< endIndex; i++) {
      try{
        CXGameState marked = B.markColumn(i);
        B.unmarkColumn();
        if (marked == yourWin) {
          ret = i;
          break;
        }
      } catch (IllegalStateException ex){
        // No harm done
      }
    }

    IllegalyEfficientBoard.swapCurrentPlayer(B);
    return ret;
  }

  /*
   * Returns a list of moves that do not allow the opponent to win next move.
   */
  static public Integer[] notOpponentWinsNext(Integer[] L, CXBoard B, CXGameState yourWin) {
    LinkedList<Integer> newL = new LinkedList<Integer>();
    for (Integer i : L) {
      CXGameState state = CXGameState.OPEN;
      B.markColumn(i);
      // Puo succedere che c'e solo un posto rimanente nella colonna
      // e mettere un secondo sarebbe illegale
      try {
        state = B.markColumn(i);
        B.unmarkColumn();
      } catch (IllegalStateException e) {
        // If we try to play in a full column it fails
        // But for this use case we don't actually care
      }

      B.unmarkColumn();
      if (state != yourWin) {
        newL.add(i);
      } else {
        System.err.printf("[+] Avoiding bad move : %s\n", i);
      }
    }

    return newL.toArray(new Integer[newL.size()]);
  }

  /*
   * Double Block
   */
  public static boolean checkDoubleAttack(StreakBoard sb,
      List<Streak> playerStreaks,
      String playerName) {

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
      if (count == sb.X) {
        System.err.println("PLAYER " + playerName + " HAS WON");
      }
      if (count == sb.X - 1) {
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
    return streaksCount >= 2;
  }

  public static boolean isThereDoubleAttack_v1(CXBoard B, Integer[] L, CXGameState winningSide, CXGameState yourWin) throws TimeoutException {
    int doSingleMoveWin = singleMoveWin(B, L, winningSide);

    // Caso A
    if (doSingleMoveWin != -1) {
      return true;
    }

    // Caso B; c'è almeno una mossa tale che, per **qualsiasi** mossa
    // dell'avversario, noi vinciamo
    // alla mossa successiva

    int count;

    for (Integer col : L) {
      B.markColumn(col);

      if (B.gameState() != CXGameState.OPEN) {
        B.unmarkColumn();
        continue;

        // Skip
      }

      count = 0;
      for (Integer col2 : B.getAvailableColumns()) {
        CXGameState state = B.markColumn(col2);

        if (state == yourWin) {
          // Interrompiamo prima se c'è una vittoria per l'avversario
          B.unmarkColumn();
          break;
        }

        if (B.gameState() != CXGameState.OPEN) {
          B.unmarkColumn();
          continue;
        }

        if (singleMoveWin(B, B.getAvailableColumns(), winningSide) != -1) {
          count++;
        }

        B.unmarkColumn();
      }
      B.unmarkColumn();

      if (count == L.length) {
        // Almeno un match trovato in cui c'è un doppio attacco.
        return true;
      }
    }
    return false;
  }

}