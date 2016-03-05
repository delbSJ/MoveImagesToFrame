package moveImagesToFrame;

import java.io.File;
import java.io.PrintStream;

import gui.MainWindow;

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
  boolean doDisplay;
  MainWindow mainWindow;
  Throwable caughtException;

  public MoveImagesToFrameThread () {}
  
  public MoveImagesToFrameThread (String threadName, Thread callingThread, PrintStream outPS, File frameDir,
      File sourceDir, File dataBaseDir, float percentageToReplace, long numberBytestoLeaveFree, boolean verboseMode,
      boolean debugMode, boolean listFilesOnly, boolean doDisplay, MainWindow mainWindow)  {
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
    this.doDisplay = doDisplay;
    this.mainWindow = mainWindow;
    this.caughtException = null;
    mainWindow.moveImagesToFrameThread = this;
  }

  @Override
  public void run () {
    super.run ();
    if (doDisplay) {
      mainWindow.handleMoveToFrameDisplay (outPS);
    }
    else {
      try {
        MoveImagesToFrame moveImagesToFrame = new MoveImagesToFrame (outPS, frameDir, sourceDir, dataBaseDir,
            percentageToReplace, numberBytestoLeaveFree, verboseMode, debugMode);

        moveImagesToFrame.rotateFiles (listFilesOnly);
      } catch (Throwable e) {
        caughtException = e;
        callingThread.interrupt ();
      }
    }
    outPS.close ();
    // DEBUG Start
    // System.out.printf ("Exiting Thread \"%s\"%n", Thread.currentThread ().getName ());
    // DEBUG End
  }

  public Throwable getCaughtException () {
    return caughtException;
  }
}
