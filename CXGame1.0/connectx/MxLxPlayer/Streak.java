package connectx.MxLxPlayer;

import connectx.CXCell;
import connectx.CXCellState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Streak {
    List<CellCoord> cells;

    @Override
    public String toString() {
        return "Streak(state = " + state + ", valid = " + valid + ", cells = \n" + cells.toString() + ")\n";
    }

    public List<CellCoord> getCells() {
        return cells;
    }

    public final CXCellState state;

    private CXCellState getOpponentState() {
        if (state == CXCellState.P1) {
            return CXCellState.P2;
        } else if (state == CXCellState.P2) {
            return CXCellState.P1;
        } else {
            throw new IllegalStateException("Invalid streak player.");
        }
    }

    public final int rowStart;
    public final int colStart;

    public final int rowEnd;
    public final int colEnd;

    private boolean valid;

    public boolean isValid() {
        return valid;
    }

    public int getNumberOfCells() {
        int count = 0;
        for (CellCoord cell : this.getCells()) {
            if (cell.getState() == this.state) {
                count++;
            }
        }
        return count;
    }

    public void checkValidity() {
        // Prima cerchiamo se c'è un avversario in questa streak
        for (CellCoord cell : cells) {
            if (cell.getState() == getOpponentState()) {
                this.valid = false;
                return;
            }
        }

        boolean valid = false;

        int count = 0;

        // Ora controlliamo che non sia vuota (nel qual caso, sarebbe invalida)
        for (CellCoord cell : cells) {
            if (cell.getState() == this.state) {

                count++;
                valid = count >= 1;

            }
        }

        this.valid = valid;
    }

    public Streak(StreakBoard board, CXCellState state, int rowStart, int colStart, int rowEnd, int colEnd,
            List<CXCell> cells) {
        this.state = state;
        this.rowStart = rowStart;
        this.colStart = colStart;
        this.rowEnd = rowEnd;
        this.colEnd = colEnd;

        this.cells = new ArrayList<>(cells.size());
        this.cells.addAll(cells.stream().map(cxCell -> new CellCoord(board, cxCell.i, cxCell.j)).toList());

        this.checkValidity();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Streak))
            return false;
        Streak streak = (Streak) o;
        return rowStart == streak.rowStart && colStart == streak.colStart && rowEnd == streak.rowEnd
                && colEnd == streak.colEnd && state == streak.state;
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, rowStart, colStart, rowEnd, colEnd);
    }

    /**
     * Fornisce una scia potenziale uniforme secondo le convenzioni, ossia
     *
     * - Il punto di partenza è quello dela cella più in basso. In caso di celle di
     * stessa altezza,
     * della cella più a sinistra.
     *
     * - Viceversa, il punto di fine è quello della cella più in alto. In caso di
     * celle di stessa altezza,
     * della cella più bassa.
     * 
     * @param streak
     * @return
     */
    public static Streak getUniformStreak(StreakBoard board, List<CXCell> streak, CXCellState state) {
        List<CXCell> listCopy = new ArrayList<>(streak);

        final int SWAP = 1;
        final int KEEP = -1;
        final int EQUAL = 0;

        listCopy.sort((o1, o2) -> {

            final int row1 = o1.i;
            final int row2 = o2.i;

            if (row1 < row2) {
                // o2 sta più in basso

                return SWAP;
            } else if (row2 < row1) {
                // o1 sta più in basso

                return KEEP;
            } else {
                // stessa altezza

                final int col1 = o1.j;
                final int col2 = o2.j;

                if (col1 < col2) {
                    return KEEP;
                } else if (col1 > col2) {
                    return SWAP;
                } else {
                    return EQUAL;
                }
            }
        });

        CXCell first = listCopy.get(0);
        CXCell last = listCopy.get(listCopy.size() - 1);

        return new Streak(board, state, first.i, first.j, last.i, last.j, listCopy);

        /*
         * Collections.sort(listCopy, (o1, o2) -> {
         * return -1;
         * });
         */
        /*
         * int minRow = Integer.MAX_VALUE, maxRow = -1, minCol = Integer.MAX_VALUE,
         * maxCol = -1;
         * 
         * for (CXCell cell : streak) {
         * if (minRow >= cell.i) {
         * minRow = cell.i;
         * 
         * if (minCol >= cell.j) {
         * minCol = cell.j;
         * }
         * }
         * 
         * if (maxRow <= cell.i) {
         * maxRow = cell.i;
         * 
         * if (maxCol <= cell.j) {
         * maxCol = cell.j;
         * }
         * }
         * }
         */
    }

    // Given three collinear points p, q, r, the function checks if
    // point q lies on line segment 'pr'
    static boolean onSegment(CellCoord p, CellCoord q, CellCoord r) {
        if (q.j <= Math.max(p.j, r.j) && q.j >= Math.min(p.j, r.j) &&
                q.i <= Math.max(p.i, r.i) && q.i >= Math.min(p.i, r.i))
            return true;

        return false;
    }

    // To find orientation of ordered triplet (p, q, r).
    // The function returns following values
    // 0 --> p, q and r are collinear
    // 1 --> Clockwise
    // 2 --> Counterclockwise
    static int orientation(CellCoord p, CellCoord q, CellCoord r) {
        // See https://www.geeksforgeeks.org/orientation-3-ordered-points/
        // for details of below formula.
        int val = (q.i - p.i) * (r.j - q.j) -
                (q.j - p.j) * (r.i - q.i);

        if (val == 0)
            return 0; // collinear

        return (val > 0) ? 1 : 2; // clock or counterclock wise
    }

    public static int numberOfValidStreaks(List<Streak> streaks) {
        int count = 0;
        for (Streak streak : streaks) {
            if (streak.isValid()) {
                count++;
            }
        }
        return count;
    }

    public static boolean doIntersect(Streak s1, Streak s2) {
        return doIntersect(
                s1.cells.get(0),
                s1.cells.get(s1.cells.size() - 1),
                s2.cells.get(0),
                s2.cells.get(s2.cells.size() - 1));
    }

    // The main function that returns true if line segment 'p1q1'
    // and 'p2q2' intersect.
    static boolean doIntersect(CellCoord p1, CellCoord q1, CellCoord p2, CellCoord q2) {
        // Find the four orientations needed for general and
        // special cases
        int o1 = orientation(p1, q1, p2);
        int o2 = orientation(p1, q1, q2);
        int o3 = orientation(p2, q2, p1);
        int o4 = orientation(p2, q2, q1);

        // General case
        if (o1 != o2 && o3 != o4)
            return true;

        // Special Cases
        // p1, q1 and p2 are collinear and p2 lies on segment p1q1
        if (o1 == 0 && onSegment(p1, p2, q1))
            return true;

        // p1, q1 and q2 are collinear and q2 lies on segment p1q1
        if (o2 == 0 && onSegment(p1, q2, q1))
            return true;

        // p2, q2 and p1 are collinear and p1 lies on segment p2q2
        if (o3 == 0 && onSegment(p2, p1, q2))
            return true;

        // p2, q2 and q1 are collinear and q1 lies on segment p2q2
        if (o4 == 0 && onSegment(p2, q1, q2))
            return true;

        return false; // Doesn't fall in any of the above cases
    }
}
