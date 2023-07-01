package connectx.MxLxPlayer;

import connectx.CXCellState;
import java.util.Objects;

public class CellCoord {
    private final StreakBoard board;

    /**
     * Cell row index
     */
    public final int i;
    /**
     * Cell column index
     */
    public final int j;

    public CellCoord(StreakBoard board, int i, int j) {
        this.board = board;
        this.i = i;
        this.j = j;
    }

    @Override
    public boolean equals(Object o) {
        //Streak streak = (Streak) o;
        if (this == o)
            return true;
        if (!(o instanceof CellCoord))
            return false;
        CellCoord cellCoord = (CellCoord) o;
        boolean ret = this.hashCode() == cellCoord.hashCode();
        return ret;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.i, this.j, this.board);
    }

    // public final CXCellState state;
    public CXCellState getState() {
        return board.packageBoard[i][j];
    }

    @Override
    public String toString() {
        return "CellCoord(i = " + i + ", j = " + j + ", state = " + getState() + ")\n";
    }
}
