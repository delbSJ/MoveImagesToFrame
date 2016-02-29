/**
 * 
 */
package moveImagesToFrame;

import java.io.File;
import java.io.PrintStream;

/**
 * @author Del
 *
 */
public class MoveImagesToFrameThread extends Thread {
  
  Thread callingThread;
  PrintStream outPS;
  File frameDir;
  File sourceDir;
  File dataBaseDir;
  float percentageToReplace;
  long numberBytestoLeaveFree;
  boolean verboseMode;
  boolean debugMode;
  boolean listFilesOnly;
  Throwable caughtException;

  public MoveImagesToFrameThread () {
    // TODO Auto-generated constructor stub
  }
  
  public MoveImagesToFrameThread (String threadName, Thread callingThread, PrintStream outPS, File frameDir,
      File sourceDir, File dataBaseDir, float percentageToReplace, long numberBytestoLeaveFree, boolean verboseMode,
      boolean debugMode, boolean listFilesOnly)  {
    super (threadName);
    this.callingThread = callingThread;
    this.outPS = outPS;
    this.frameDir = frameDir;
    this.sourceDir = sourceDir;
    this.dataBaseDir = dataBaseDir;
    this.percentageToReplace = percentageToReplace;
    this.numberBytestoLeaveFree = numberBytestoLeaveFree;
    this.verboseMode = verboseMode;
    this.debugMode = debugMode;
    this.listFilesOnly = listFilesOnly;
    this.caughtException = null;
  }

  @Override
  public void run () {
    try {
      super.run ();
      MoveImagesToFrame moveImagesToFrame = new MoveImagesToFrame (outPS, frameDir, sourceDir, dataBaseDir,
          percentageToReplace, numberBytestoLeaveFree, verboseMode, debugMode);
        
        moveImagesToFrame.rotateFiles (listFilesOnly);
    } catch (Throwable e) {
      caughtException = e;
      callingThread.interrupt ();
    }
    outPS.close ();
  }

  public Throwable getCaughtException () {
    return caughtException;
  }
}
