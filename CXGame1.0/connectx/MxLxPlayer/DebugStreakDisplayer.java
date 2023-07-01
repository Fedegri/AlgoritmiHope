package connectx.MxLxPlayer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import connectx.MxLxPlayer.Streak;
import connectx.CXBoardPanel;


/*
 * USAGE:
 * Call DebugStreakDisplayer.clear at the beginning of the move
 * Add all the streaks you want to visualize wherever in the code
 * At the end of the move call DebugStreakDisplayer.updateMainDisplay with the debugDrawPanel
 * debugDrawPanel gets loaded at init into MxLxPlayer
 */
public class DebugStreakDisplayer {
    private List<Streak> streaksToDisplay = new ArrayList<Streak>();
    

    public void addStreaks(List<Streak> streaks){
        for(Streak s : streaks){
            addStreak(s);
        }
    }

    public void addStreak(Streak streak){
        streaksToDisplay.add(streak);
    }

    public void clear(){
        streaksToDisplay.clear();
    }

    public void updateMainDisplay(CXBoardPanel debugDrawPanel){
        //System.out.println(streaksToDisplay);
        try{
          // CAREFUL WHEN DOING THIS TYPE OF ACCESS
          // DO NOT ACCESS DIRECTLY, WILL NOT COMPILE 
          Field slf = debugDrawPanel.getClass().getField("streakList");
          slf.set(debugDrawPanel, streaksToDisplay);

        } catch (Exception ex){

        }
    }
}
