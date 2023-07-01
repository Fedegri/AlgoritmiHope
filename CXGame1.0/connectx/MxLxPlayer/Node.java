package connectx.MxLxPlayer;
import java.util.List;
import connectx.CXBoard;
import java.util.ArrayList;

public class Node {
    // TODO:
    // Valutare non salvare tutta la scacchiera ma solo [i] che e' la casella salvata
    // In tal caso per arrivare alla posizione serve O(logn * {tempo per simulare la mossa})
    // Per resettare la posizione e tornare alla posizione precedente O(logn * {tempo per rimuovere un pezzo dalla scacchiera})
    CXBoard board;
    boolean player;
    List<Node> children;
    Node parent;
    
    public Node(CXBoard iboard, boolean iplayer){
      board = iboard;
      player = iplayer;
      children = new ArrayList<Node>();
    }

    public boolean isLeaf(){
      return children.size() == 0;
    }
}
