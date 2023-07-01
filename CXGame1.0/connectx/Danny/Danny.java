package connectx.Danny;

import connectx.*;
import java.util.concurrent.TimeoutException;

public class Danny implements CXPlayer {
	public int localM;
	public int localN;
	public int localK;
	public int timeoutTarget;// timeout related
	private AlphaBeta testEngine;

	public Danny() {

	}

	public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
		// New random seed for each game
		localM = M;
		localK = K;
		localN = N;
		testEngine = new AlphaBeta(first);
		timeoutTarget = timeout_in_secs;

  
	}

	/**
	 * Selects a random cell in <code>FC</code>
	 */
	public int selectColumn(CXBoard B){
		long thisTimeout = System.currentTimeMillis() + timeoutTarget * 1000 - 100;
		CXBoard matchBoard = new CXBoard(localM, localN, localK);// initialize board
    Integer[] FC = B.getAvailableColumns();
		for (int i = 0; i < B.numOfMarkedCells(); i++) {
			matchBoard.markColumn(i);
		}
		double check=testEngine.eval(matchBoard);
		//System.out.println("eval first print");
		//System.out.println(check);
		double maxAlpha = Integer.MIN_VALUE;
		int maxCell=0;
		//System.out.println("approfondimento iterativo");
		try {
			for (int depth = 0; depth <= FC.length; depth++) {
				for (int CXCell : FC) {
					double actualAlpha;
					// min and max value in the engine call are -1 and +1 to elevate the number of
					// cuts from the decision tree
					actualAlpha = testEngine.engine(AlphaBeta.birthBoard(matchBoard, CXCell), false, -1, +1, depth,thisTimeout);
					if (actualAlpha > maxAlpha) {
						maxAlpha = actualAlpha;
						maxCell = CXCell;
						if (maxAlpha == 1)
							break;
						// if i have found a value 1 move i can already stop cycling through the tree
						// and wasting resources
					}

				}
				Double d = maxAlpha;
				//System.out.println(d.toString());
			}
		} catch (TimeoutException e) {
		}

		return maxCell;
	}

	public String playerName() {
		return "Danny";
	}
}