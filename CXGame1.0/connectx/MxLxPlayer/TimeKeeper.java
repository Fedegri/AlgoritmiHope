package connectx.MxLxPlayer;

import java.util.concurrent.TimeoutException;

/*
  Utility class to keeep the time and throw TimeoutExeption before being sigkilled by overlord process
*/
public class TimeKeeper {
    private long keeperStartTime;
    private int keeperTimeout;

    public TimeKeeper(int timeout){
        keeperTimeout = timeout;
    }

    public int getTimeout(){
      return keeperTimeout;
    }

    public void setStartTime(long startTime){
        keeperStartTime = startTime;
    }

    public void checktime() throws TimeoutException {
      if ((System.currentTimeMillis() - keeperStartTime) / 1000.0 >= keeperTimeout * (99.0 / 100.0))
        throw new TimeoutException();
    }

    public boolean ranOutOfTime(){
        try {
          checktime();
          return false;
        } catch (TimeoutException e) {
          System.err.println("[!] Timeout!");
          return true;
        }
    }
    
}
    