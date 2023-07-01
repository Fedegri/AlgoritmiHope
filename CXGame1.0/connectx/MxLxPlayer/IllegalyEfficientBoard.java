package connectx.MxLxPlayer;

import connectx.CXBoard;
import connectx.CXCell;
import connectx.CXCellState;
import connectx.CXGameState;
import connectx.CXPlayer;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;

// Utility class for efficient board manipulation
@SuppressWarnings({"unchecked"})
public class IllegalyEfficientBoard {
  public static void __dangerous__efficientMarkColumn(int col, CXBoard b) {
    // THIS INTRODUCES TEDIOUS BUGS.
    // No clue why and do not have the time to debug
    // (Probably the illegal access stuff has to do with it :))))))

    try {
      Class<?> cls = Class.forName("connectx.CXBoard");

      // You have nice fields, I'd like to see them
      Field rp_field = cls.getDeclaredField("RP");
      Field player_field = cls.getDeclaredField("Player");
      Field curr_player_field = cls.getDeclaredField("currentPlayer");
      Field board_field = cls.getDeclaredField("B");
      Field ac_field = cls.getDeclaredField("AC");
      Field mc_field = cls.getDeclaredField("MC");
      Field m_field = cls.getDeclaredField("M");

      // Don't really care that they are private
      rp_field.setAccessible(true);
      ac_field.setAccessible(true);
      player_field.setAccessible(true);
      curr_player_field.setAccessible(true);
      board_field.setAccessible(true);
      mc_field.setAccessible(true);

      // Let me access them from these variables
      CXCellState[][] B = (CXCellState[][]) board_field.get(b);
      TreeSet<Integer> AC = (TreeSet<Integer>) ac_field.get(b);
      CXCellState[] Player = (CXCellState[]) player_field.get(b);
      int RP[] = (int[]) rp_field.get(b);
      int currentPlayer = (int) curr_player_field.get(b);
      LinkedList<CXCell> MC = (LinkedList<CXCell>) mc_field.get(b);

      // Doing stuff with private fields :))))
      // Basically Mark column without the winning condition check.
      // Why no winning condition check?
      // 1. It's an overhead on all marking operations that I do not care for because really
      // unlikeley and we are not going THAT far into the Tree
      // 2. I would rather the connectedness heuristic replace the check.
      //    Since it will be ran anyways and the winning move would hane n connected so it would
      //    return INFTY as the value for connectedness
      //
      // THIS METHOD WILL NOT BE USED

      int row = RP[col]--;
      if (RP[col] == -1)
        AC.remove(col);

      B[row][col] = Player[currentPlayer];
      CXCell newc = new CXCell(row, col, Player[currentPlayer]);
      MC.add(newc); // Add move to the history
      currentPlayer = (currentPlayer + 1) % 2;
      

      rp_field.set(b,RP);
      ac_field.set(b,AC);
      player_field.set(b,Player);
      curr_player_field.set(b,currentPlayer);
      board_field.set(b,B);
      mc_field.set(b,MC);
    } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException
        | SecurityException e) {
      System.err.println(e.toString());
    };
  }

  // Makes the position open so that you can still play a move even if game ended for a player
  public static void openPosition(CXBoard b) {
    try {
      Class<?> cls = Class.forName("connectx.CXBoard");
      Field gameStateField = cls.getDeclaredField("gameState");
      gameStateField.setAccessible(true);

      CXGameState currentGameState = (CXGameState) gameStateField.get(b);
      currentGameState = CXGameState.OPEN;

      gameStateField.set(b, currentGameState);
    } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException e) {
      System.err.println("Could not Illegaly swap :(");
      System.err.println(e);
    }
  }

  public static void swapCurrentPlayer(CXBoard b) {
    try {
      /* TODO
      Maybe this should be abstracted if heavily used.
      If not a little duplication isn't a big deal 
      */
      Class<?> cls = Class.forName("connectx.CXBoard");
      Field curr_player_field = cls.getDeclaredField("currentPlayer");
      curr_player_field.setAccessible(true);

      int currentPlayer = (int) curr_player_field.get(b);
			currentPlayer = (currentPlayer + 1) % 2;

      curr_player_field.setInt(b, currentPlayer);
    } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException e) {
      System.err.println("Could not Illegaly swap :(");
      System.err.println(e);
    }
  }
}
