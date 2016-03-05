package gui;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.prefs.Preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wb.swt.SWTResourceManager;

import moveImagesToFrame.MoveImagesToFrame;
import moveImagesToFrame.MoveImagesToFrameThread;

public class MainWindow {
  public static final long DISPLAY_TEXT_MILLIS = 500L;
  public static final String TEXT_LOG_FILE = " to Log File";
  public static final String TEXT_APPEND = "Append";
  public static final String TEXT_WRITE = "Write";
  public static final int TEXT_AMOUNT_FOR_ONE_WRITE = 1024;

  // Preference keys for this package
  private static final String FRAME_DIR = "frame_dir";
  private static final String SOURCE_DIR = "source_dir";
  private static final String DATABASE_DIR = "database_dir";
  private static final String LOG_FILE = "log_file";
  private static final String PERCENT_TO_CHANGE_ON_FRAME = "percent_to_change_on_frame";
  private static final String MB_TO_LEAVE_FREE = "mb_to_leave_free";
  private static final String AUTO_WRITE_LOG_FILE_AFTER_RUN = "auto_write_log_file_after_run";
  private static final String APPEND_TO_LOG_FILE = "append_to_log_file";
  private static final String LIST_FILES_ONLY = "list_files_only";
  private static final String QUIET_MODE = "quiet_mode";
  private static final String DEBUG_MODE = "debug_mode";

  protected QueueOutputStream qOS;
  protected QueueInputStream qIS;
  private Display display;
  private Cursor oldCursor = null;
  protected Shell shlMoveImagesToFrameGui;
  private MainWindow thisMainWindow;
  private Menu menu;
  private StyledText styledText;
  private MenuItem mntmWriteLogFile;
  private StyleRange[] selectedRanges;
  private int newCharCount, start;
  private Font textFont;
  private MoveImagesToFrameThread moveImagesToFrameReadThread = null;
  public MoveImagesToFrameThread moveImagesToFrameThread = null;
  private boolean afterRun = false;
  private StringBuilder displaySB = null;
  private int lastCharRead = -1;
  private boolean readerEOF = false;
  private String lineDelimiter = null;
  private DirectoryDialog frameDirDialog = null;
  private DirectoryDialog sourceDirDialog = null;
  private DirectoryDialog databaseDirDialog = null;
  private FileDialog logFileDialog = null;
  private File frameDir = null;
  private File sourceDir = null;
  private File databaseDir = null;
  private File logFile = null;
  private OptionsResult optionsResult = new OptionsResult ();

  /**
   * Launch the application.
   * 
   * @param args
   */
  public static void main (String[] args) {
    try {
      MainWindow window = new MainWindow ();
      window.open (window);
    } catch (Exception e) {
      System.out.println (e.toString ());
      e.printStackTrace ();
    }
  }

  /**
   * Open the window.
   */
  public void open (MainWindow window) {
    thisMainWindow = window;
    readConfiguration ();
    Display display = Display.getDefault ();
    this.display = display;
    createContents ();
    shlMoveImagesToFrameGui.open ();
    shlMoveImagesToFrameGui.layout ();
    displaySettings ();
    while (!shlMoveImagesToFrameGui.isDisposed ()) {
      if (!display.readAndDispatch ()) {
        if (!processDataFromMoveImages ()) {
          // nothing to display, go to sleep
          display.sleep ();
        }
      }
    }
    saveConfiguration ();
  }

  /**
   * Create contents of the window.
   */
  protected void createContents () {
    shlMoveImagesToFrameGui = new Shell ();
    shlMoveImagesToFrameGui.setMinimumSize (new Point (740, 600));
    shlMoveImagesToFrameGui.setSize (800, 600);
    shlMoveImagesToFrameGui.addShellListener (new ShellListener() {
      @Override
      public void shellIconified (ShellEvent arg0) {}
      @Override
      public void shellDeiconified (ShellEvent arg0) {}
      @Override
      public void shellDeactivated (ShellEvent arg0) {}
      @Override
      public void shellClosed (ShellEvent arg0) {
        saveConfiguration ();
        cleanUpForExit ();
      }
      @Override
      public void shellActivated (ShellEvent arg0) {}
    });
    
    String header = null;
    try {
      header = MoveImagesToFrame.getVersion () + " GUI";
    } catch (Throwable e) {
      displayText (String.format ("%s%n", e.toString ()), true);
    }
    shlMoveImagesToFrameGui.setText (header);
    shlMoveImagesToFrameGui.setLayout (null);

    styledText = new StyledText (shlMoveImagesToFrameGui, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
    this.lineDelimiter = styledText.getLineDelimiter ();
    installStyledTextListeners ();

    menu = new Menu (shlMoveImagesToFrameGui, SWT.BAR);
    shlMoveImagesToFrameGui.setMenuBar (menu);

    MenuItem mntmFileSubmenu = new MenuItem (menu, SWT.CASCADE);
    mntmFileSubmenu.setText (" File");

    Menu menuFile = new Menu (mntmFileSubmenu);
    mntmFileSubmenu.setMenu (menuFile);

    MenuItem mntmSetFrameDir = new MenuItem (menuFile, SWT.NONE);
    mntmSetFrameDir.setImage (
        SWTResourceManager.getImage (MainWindow.class, "/com/sun/java/swing/plaf/windows/icons/TreeOpen.gif"));
    mntmSetFrameDir.setText ("Set Frame Dir");
    mntmSetFrameDir.addSelectionListener (new SelectionAdapter () {
      @Override
      public void widgetSelected (SelectionEvent event) {
        openFrameDir ();
      }
    });

    MenuItem mntmSetSourceDir = new MenuItem (menuFile, SWT.NONE);
    mntmSetSourceDir.setImage (
        SWTResourceManager.getImage (MainWindow.class, "/com/sun/java/swing/plaf/windows/icons/TreeOpen.gif"));
    mntmSetSourceDir.setText ("Set Source Dir");
    mntmSetSourceDir.addSelectionListener (new SelectionAdapter () {
      @Override
      public void widgetSelected (SelectionEvent event) {
        openSourceDir ();
      }
    });

    MenuItem mntmSetDatabaseDir = new MenuItem (menuFile, SWT.NONE);
    mntmSetDatabaseDir.setImage (
        SWTResourceManager.getImage (MainWindow.class, "/com/sun/java/swing/plaf/windows/icons/TreeOpen.gif"));
    mntmSetDatabaseDir.setText ("Set Database Dir");
    mntmSetDatabaseDir.addSelectionListener (new SelectionAdapter () {
      @Override
      public void widgetSelected (SelectionEvent event) {
        openDatabaseDir ();
      }
    });

    MenuItem mntmSetLogFile = new MenuItem (menuFile, SWT.NONE);
    mntmSetLogFile.setImage (
        SWTResourceManager.getImage (MainWindow.class, "/com/sun/java/swing/plaf/windows/icons/TreeOpen.gif"));
    mntmSetLogFile.setText ("Set Log File");
    mntmSetLogFile.addSelectionListener (new SelectionAdapter () {
      @Override
      public void widgetSelected (SelectionEvent event) {
        openLogFile ();
      }
    });

    MenuItem mntmSetOptions = new MenuItem (menuFile, SWT.NONE);
    mntmSetOptions.setImage (
        SWTResourceManager.getImage (MainWindow.class, "/com/sun/java/swing/plaf/windows/icons/DetailsView.gif"));
    mntmSetOptions.setText ("Options");
    mntmSetOptions.addSelectionListener (new SelectionAdapter () {
      @Override
      public void widgetSelected (SelectionEvent event) {
        OptionsDialog optionsDialog = new OptionsDialog (shlMoveImagesToFrameGui, SWT.DIALOG_TRIM, optionsResult);
        optionsResult = (OptionsResult) optionsDialog.open ();
        displaySettings ();
      }
    });

    MenuItem mntmExit = new MenuItem (menuFile, SWT.NONE);
    mntmExit.setText ("Exit");
    mntmExit.addSelectionListener (new SelectionAdapter () {
      @Override
      public void widgetSelected (SelectionEvent event) {
        saveConfiguration ();
        shlMoveImagesToFrameGui.close ();
      }
    });

    MenuItem mntmRun = new MenuItem (menu, SWT.NONE);
    mntmRun.setText ("     Run     ");
    mntmRun.addSelectionListener (new SelectionAdapter () {
      @Override
      public void widgetSelected (SelectionEvent event) {
        runMoveImagesToFrame ();
      }
    });
    
    MenuItem mntmWriteLogFile = new MenuItem(menu, SWT.NONE);
    this.mntmWriteLogFile = mntmWriteLogFile;
    mntmWriteLogFile.addSelectionListener (new SelectionAdapter () {
      @Override
      public void widgetSelected (SelectionEvent event) {
        writeToLogIfSet ();
      }
    });
    enableDisableLogFileMenuItem ();
  }
    
  private boolean cleanUpForExit ()
  {
    boolean cleanupSucceeded = true;
    
    if (moveImagesToFrameThread != null || moveImagesToFrameReadThread != null) {
      displayText ("It may take a minute or so to close the Move Images to Frame process", false);
    }
    
    if (moveImagesToFrameThread != null) {
      // we are moving images to the frame, interrupt it
      moveImagesToFrameThread.interrupt ();
      // wait for it to exit
      try {
        moveImagesToFrameThread.join (5000L);
      } catch (InterruptedException e) {
        // re-throw InterruptedException
        Thread.currentThread ().interrupt ();
      }
      if (moveImagesToFrameThread.isAlive ()) {
        displayErrorDialog ("Move Images To Frame Process is still executing after Exit signal", "Termination Failure");
        cleanupSucceeded = false;
      }
      else {
        // thread exited successfully
        moveImagesToFrameThread = null;
      }
    }
    if (moveImagesToFrameReadThread != null) {
      // we are displaying data from moving images to the frame, interrupt it
      moveImagesToFrameReadThread.interrupt ();
      // wait for it to exit
      try {
        moveImagesToFrameReadThread.join (1000L);
      } catch (InterruptedException e) {
        // re-throw InterruptedException
        Thread.currentThread ().interrupt ();
      }
      if (moveImagesToFrameReadThread.isAlive ()) {
        displayErrorDialog ("Move Images To Frame (Display Data) Process is still executing after Exit signal",
            "Termination Failure");
        cleanupSucceeded = false;
      }
      else {
        // thread exited successfully
        moveImagesToFrameReadThread = null;
      }
    }
    return cleanupSucceeded;
  }

  private void enableDisableLogFileMenuItem () {
    mntmWriteLogFile
        .setText ((optionsResult != null && optionsResult.appendToLogFile ? TEXT_APPEND : TEXT_WRITE) + TEXT_LOG_FILE);
    if (logFile == null || optionsResult.autoWriteLogFile || (!optionsResult.autoWriteLogFile && !afterRun)) {
      mntmWriteLogFile.setEnabled(false);
    }
    else {
      mntmWriteLogFile.setEnabled(true);
    }
  }

  void installStyledTextListeners () {
    styledText.addVerifyListener (new VerifyListener () {
      @Override
      public void verifyText (VerifyEvent event) {
        handleVerifyText (event);
      }
    });
    styledText.addModifyListener (new ModifyListener () {
      @Override
      public void modifyText (ModifyEvent event) {
        handleModify (event);
      }
    });
    styledText.addListener (SWT.Dispose, new Listener () {
      @Override
      public void handleEvent (Event event) {
        StyleRange[] styles = styledText.getStyleRanges (0, styledText.getCharCount (), false);
        for (int i = 0; i < styles.length; i++) {
          Object data = styles[i].data;
          if (data != null) {
            if (data instanceof Image)
              ((Image) data).dispose ();
            if (data instanceof Control)
              ((Control) data).dispose ();
          }
        }
      }
    });
    shlMoveImagesToFrameGui.addControlListener (new ControlAdapter () {
      @Override
      public void controlResized (ControlEvent event) {
        handleResize (event);
      }
    });
  }

  void handleResize (ControlEvent event) {
    Rectangle rect = shlMoveImagesToFrameGui.getClientArea ();
    styledText.setBounds (rect.x, rect.y, rect.width, rect.height);
  }

  void handleVerifyText (VerifyEvent event) {
    start = event.start;
    newCharCount = event.text.length ();
    int replaceCharCount = event.end - start;

    // mark styles to be disposed
    selectedRanges = styledText.getStyleRanges (start, replaceCharCount, false);
  }

  void handleModify (ModifyEvent event) {
    if (newCharCount > 0 && start >= 0) {
      StyleRange style = new StyleRange ();
      if (textFont != null && !textFont.equals (styledText.getFont ())) {
        style.font = textFont;
      }
      else {
        style.fontStyle = SWT.NONE;
      }

      int[] ranges = {
          start, newCharCount
      };
      StyleRange[] styles = {
          style
      };
      styledText.setStyleRanges (start, newCharCount, ranges, styles);
    }
    disposeRanges (selectedRanges);
  }

  void disposeRanges (StyleRange[] ranges) {
    StyleRange[] allRanges = styledText.getStyleRanges (0, styledText.getCharCount (), false);
    for (int i = 0; i < ranges.length; i++) {
      StyleRange style = ranges[i];
      boolean disposeFg = true, disposeBg = true, disposeStrike = true, disposeUnder = true, disposeBorder = true,
          disposeFont = true;

      for (int j = 0; j < allRanges.length; j++) {
        StyleRange s = allRanges[j];
        if (disposeFont && style.font == s.font)
          disposeFont = false;
        if (disposeFg && style.foreground == s.foreground)
          disposeFg = false;
        if (disposeBg && style.background == s.background)
          disposeBg = false;
        if (disposeStrike && style.strikeoutColor == s.strikeoutColor)
          disposeStrike = false;
        if (disposeUnder && style.underlineColor == s.underlineColor)
          disposeUnder = false;
        if (disposeBorder && style.borderColor == s.borderColor)
          disposeBorder = false;
      }
      if (disposeFont && style.font != textFont && style.font != null)
        style.font.dispose ();

      Object data = style.data;
      if (data != null) {
        if (data instanceof Image)
          ((Image) data).dispose ();
        if (data instanceof Control)
          ((Control) data).dispose ();
      }
    }
  }

  protected void openFrameDir () {
    if (frameDirDialog == null) {
      frameDirDialog = new DirectoryDialog (shlMoveImagesToFrameGui, SWT.OPEN);
      frameDirDialog.setText ("Select Frame Directory");
    }
    String dirName = frameDirDialog.open ();
    if (dirName != null) {
      frameDir = openDir (dirName);
    }
    else {
      frameDir = null;
    }
    displaySettings ();
  }

  protected void openSourceDir () {
    if (sourceDirDialog == null) {
      sourceDirDialog = new DirectoryDialog (shlMoveImagesToFrameGui, SWT.OPEN);
      sourceDirDialog.setText ("Select Source Directory");
    }
    String dirName = sourceDirDialog.open ();
    if (dirName != null) {
      sourceDir = openDir (dirName);
    }
    else {
      sourceDir = null;
    }
    displaySettings ();
  }

  protected void openDatabaseDir () {
    if (databaseDirDialog == null) {
      databaseDirDialog = new DirectoryDialog (shlMoveImagesToFrameGui, SWT.OPEN);
      databaseDirDialog.setText ("Select Database Directory");
    }
    String dirName = databaseDirDialog.open ();
    if (dirName != null) {
      databaseDir = openDir (dirName);
    }
    else {
      databaseDir = null;
    }
    displaySettings ();
  }

  private File openDir (String dirName) {
    File dirOpened = null;
    dirOpened = new File (dirName);

    if (!dirOpened.exists ()) {
      MessageBox errorDialog = new MessageBox (shlMoveImagesToFrameGui, SWT.ICON_ERROR | SWT.OK);
      errorDialog.setText ("Error opening Directory");
      errorDialog.setMessage ("Error, the specified directory does not exist.");
      errorDialog.open ();
      dirOpened = null;
    }
    else if (!dirOpened.isDirectory ()) {
      MessageBox errorDialog = new MessageBox (shlMoveImagesToFrameGui, SWT.ICON_ERROR | SWT.OK);
      errorDialog.setText ("Error opening Directory");
      errorDialog.setMessage ("Error, the specified directory is actually a file.");
      errorDialog.open ();
      dirOpened = null;
    }
    return dirOpened;
  }

  protected void openLogFile () {
    if (logFileDialog == null) {
      logFileDialog = new FileDialog (shlMoveImagesToFrameGui, SWT.OPEN);
      logFileDialog.setText ("Select Log File");
    }
    String fileName = logFileDialog.open ();
    if (fileName != null) {
      logFile = openFile (fileName);
      if (logFile != null) {
        MessageBox questionDialog = new MessageBox (shlMoveImagesToFrameGui, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
        questionDialog.setText ("Append to Log File");
        questionDialog.setMessage ("Do you want to append to the end of the Log File?");
        int result = questionDialog.open ();
        if (result == SWT.YES) {
          optionsResult.appendToLogFile = true;
        }
        else {
          optionsResult.appendToLogFile = false;
        } 
        questionDialog.setText ("Append to Log File");
        questionDialog.setMessage ("Do you want to write / append to the Log File after Run?");
         result = questionDialog.open ();
        if (result == SWT.YES) {
          optionsResult.autoWriteLogFile = true;
        }
        else {
          optionsResult.autoWriteLogFile = false;
        } 
      }
    }
    else {
      logFile = null;
    }
    displaySettings ();
  }

  private File openFile (String fileName) {
    File fileOpened = null;
    fileOpened = new File (fileName);

    if (fileOpened.exists () && !fileOpened.isFile ()) {
      MessageBox errorDialog = new MessageBox (shlMoveImagesToFrameGui, SWT.ICON_ERROR | SWT.OK);
      errorDialog.setText ("Error opening file");
      errorDialog.setMessage ("Error, the opened file is actually a directory.");
      errorDialog.open ();
      fileOpened = null;
    }
    return fileOpened;
  }

  private void displaySettings () {
    enableDisableLogFileMenuItem ();
    StringBuilder ds = new StringBuilder (1000);
    ds.append ("Frame Dir: ");
    if (frameDir != null) {
      ds.append (frameDir.toPath ().toString ());
    }
    else {
      ds.append ("<not set> <Required>");
    }
    ds.append ("\nSource Dir: ");
    if (sourceDir != null) {
      ds.append (sourceDir.toPath ().toString ());
    }
    else {
      ds.append ("<not set> <Required>");
    }
    ds.append ("\nDatabase Dir: ");
    if (databaseDir != null) {
      ds.append (databaseDir.toPath ().toString ());
    }
    else {
      ds.append ("<not set> <Required>");
    }
    ds.append ("\nLog File: ");
    if (logFile != null) {
      ds.append (logFile.toPath ().toString ());
    }
    else {
      ds.append ("<not set> <Optional>");
    }
    ds.append (", Auto-write to Log File after Run: ");
    ds.append (optionsResult.autoWriteLogFile ? "true" : "false");
    ds.append (", Append to Log File: ");
    ds.append (optionsResult.appendToLogFile ? "true" : "false");
    ds.append ("\nOptions:\n");
    ds.append ("% to change on Frame: ");
    ds.append (optionsResult.percentToChangeOnFrame);
    ds.append ("\nMegabytes to leave free: ");
    ds.append (optionsResult.mbToLeaveFree);
    ds.append ("\nList Files Only: ");
    ds.append (optionsResult.listFilesOnly);
    ds.append ("\nQuiet Mode: ");
    ds.append (optionsResult.quietMode);
    ds.append ("\nDebug Mode: ");
    ds.append (optionsResult.debugMode);
    ds.append ('\n');
    displayText (ds.toString (), false);
  }

  protected void displayText (final String textString, final boolean appendText) {
    if (appendText) {
      if (textString == null) {
        // this is a forced scroll request
        // doForcedScroll ();
      }
      else {
        // System.out.println ("displayText");
        styledText.append (textString);
        // chkForForcedScroll (textString);
      }
      // displayTextAppend = true;
      if (!display.isDisposed ())
        display.wake ();
    }
    else {
      styledText.setText (textString);
    }
  }
  
  private void readConfiguration () {
    Preferences prefs = Preferences.userNodeForPackage (MainWindow.class);

    this.frameDir = null;
    this.sourceDir = null;
    this.databaseDir = null;
    this.logFile = null;

    String frameDir = prefs.get (FRAME_DIR, null);
    if (frameDir != null && !frameDir.equals ("")) {
      this.frameDir = new File (frameDir);
    }
    String sourceDir = prefs.get (SOURCE_DIR, null);
    if (sourceDir != null && !sourceDir.equals ("")) {
      this.sourceDir = new File (sourceDir);
    }
    String databaseDir = prefs.get (DATABASE_DIR, null);
    if (databaseDir != null && !databaseDir.equals ("")) {
      this.databaseDir = new File (databaseDir);
    }
    String logFile = prefs.get (LOG_FILE, null);
    if (logFile != null && !logFile.equals ("")) {
      this.logFile = new File (logFile);
    }
    optionsResult.autoWriteLogFile = prefs.getBoolean (AUTO_WRITE_LOG_FILE_AFTER_RUN, false);
    optionsResult.appendToLogFile = prefs.getBoolean (APPEND_TO_LOG_FILE, true);
    optionsResult.debugMode = prefs.getBoolean (DEBUG_MODE, false);
    optionsResult.listFilesOnly = prefs.getBoolean (LIST_FILES_ONLY, false);
    optionsResult.mbToLeaveFree = prefs.getInt (MB_TO_LEAVE_FREE, 100);
    optionsResult.percentToChangeOnFrame = prefs.getInt (PERCENT_TO_CHANGE_ON_FRAME, 10);
    optionsResult.quietMode = prefs.getBoolean (QUIET_MODE, false);
  }

  private void saveConfiguration () {
    Preferences prefs = Preferences.userNodeForPackage (MainWindow.class);

    prefs.put (FRAME_DIR, (this.frameDir != null ? this.frameDir.toPath ().toString () : ""));
    prefs.put (SOURCE_DIR, (this.sourceDir != null ? this.sourceDir.toPath ().toString () : ""));
    prefs.put (DATABASE_DIR, (this.databaseDir != null ? this.databaseDir.toPath ().toString () : ""));
    prefs.put (LOG_FILE, (this.logFile != null ? this.logFile.toPath ().toString () : ""));
    prefs.putBoolean (AUTO_WRITE_LOG_FILE_AFTER_RUN, optionsResult.autoWriteLogFile);
    prefs.putBoolean (APPEND_TO_LOG_FILE, optionsResult.appendToLogFile);
    prefs.putBoolean (DEBUG_MODE, optionsResult.debugMode);
    prefs.putBoolean (LIST_FILES_ONLY, optionsResult.listFilesOnly);
    prefs.putInt (MB_TO_LEAVE_FREE, optionsResult.mbToLeaveFree);
    prefs.putInt (PERCENT_TO_CHANGE_ON_FRAME, optionsResult.percentToChangeOnFrame);
    prefs.putBoolean (QUIET_MODE, optionsResult.quietMode);
  }

  private void displayErrorDialog (String errorMsg, String dialogTitle) {
    displayText (errorMsg + "\n", true);
    MessageBox errorDialog = new MessageBox (shlMoveImagesToFrameGui, SWT.ICON_ERROR | SWT.OK);
    errorDialog.setText (dialogTitle);
    errorDialog.setMessage (errorMsg);
    errorDialog.open ();
  }

  protected void writeToLogIfSet () {
    if (logFile != null && !optionsResult.autoWriteLogFile) {
      // the log file is defined, was not written via auto-write, use optionsResult.appendToLogFile 
      //   to append or overwrite
      writeStyledTextToLogFile ();
    }
  }

  private void writeStyledTextToLogFile () {
    FileWriter fw;
    try {
      fw = new FileWriter (logFile, optionsResult.appendToLogFile);
    } catch (IOException e) {
      displayErrorDialog (String.format ("Log File not Found: %s", e.toString ()), "Log File not found");
      return;
    }
    int textEndOffset = styledText.getCharCount ();
    int textStart = 0;
    int amtLeftToReadWrite;
    int amtToReadWrite;
    String textToWriteToLog;
    displayText (String.format ("Writing %,d bytes to Log File%n", textEndOffset), true);
    Date date = new Date (System.currentTimeMillis ());
    SimpleDateFormat dateTime = new SimpleDateFormat ();
    if (writeStringToFile (fw,
        String.format ("========================================%s%n", dateTime.format (date)))) {
      return; // error on write
    }
    do {
      amtLeftToReadWrite = textEndOffset - textStart;
      amtToReadWrite = TEXT_AMOUNT_FOR_ONE_WRITE < amtLeftToReadWrite ? TEXT_AMOUNT_FOR_ONE_WRITE : amtLeftToReadWrite;
      textToWriteToLog = styledText.getTextRange (textStart, amtToReadWrite);
      if (writeStringToFile (fw, textToWriteToLog)) {
        return; // error on write
      }
      textStart += amtToReadWrite;
    } while (textStart < textEndOffset);
    if (writeStringToFile (fw, "========================================")) {
      return; // error on write
    }
    try {
      fw.close ();
    } catch (IOException e) {
    }
    afterRun = false;
    displaySettings ();
    displayText ("Finished writing Log File\n", true);
    return;
  }

  private boolean writeStringToFile (FileWriter fw, String textToWriteToLog) {
    try {
      fw.write (textToWriteToLog);
    } catch (IOException e) {
      displayErrorDialog (String.format ("Error writing to Log File: %s", e.toString ()),
          "Error writing to Log File");
      try {
        fw.close ();
      } catch (IOException e1) {}
      return true;
    }
    return false;
  }

  private void runMoveImagesToFrame () {
    saveConfiguration ();

    // System.out.printf ("Run, This Thread: %s, Display: %s%n", Thread.currentThread ().toString (),
    // Display.getCurrent ().toString ());

    displaySB = new StringBuilder (1000);
    QueueOutputStream queueOS = new QueueOutputStream ();
    queueOS.setDisplay (display);
    PrintStream outPS = null;
    outPS = new PrintStream (queueOS, true);
    try {
      qIS = new QueueInputStream (queueOS, this);
    } catch (IOException e) {
      String errorMsg = String.format ("IOException opening QueueInputStream: %s", e.toString ());
      displayErrorDialog (errorMsg, "Error opening QueueInputStream");
      return;
    }

    displayText (String.format ("running MoveImagesToFrame%n"), true);
    oldCursor = styledText.getCursor ();
    styledText.setCursor (display.getSystemCursor(SWT.CURSOR_WAIT));
    
    moveImagesToFrameReadThread = new MoveImagesToFrameThread ("mitfReadThread", Thread.currentThread (), outPS,
        frameDir, sourceDir, databaseDir, ((float) optionsResult.percentToChangeOnFrame / 100.0f),
        optionsResult.mbToLeaveFree * MoveImagesToFrame.BINARY_MB, !optionsResult.quietMode, optionsResult.debugMode,
        optionsResult.listFilesOnly, true, thisMainWindow);
    moveImagesToFrameReadThread.start ();
  }

  public void handleMoveToFrameDisplay (PrintStream outPS) {
    // create a thread to move the images to the frame
    MoveImagesToFrameThread moveImagesToFrameThread =
        new MoveImagesToFrameThread ("mitfThread", Thread.currentThread (), outPS, frameDir, sourceDir, databaseDir,
            ((float) optionsResult.percentToChangeOnFrame / 100.0f),
            optionsResult.mbToLeaveFree * MoveImagesToFrame.BINARY_MB, !optionsResult.quietMode,
            optionsResult.debugMode, optionsResult.listFilesOnly, false, thisMainWindow);
    moveImagesToFrameThread.start ();
    
    // get the data from moveImagesToFrameThread
    getDataFromMoveImages (); // reads until end of stream
    
    // now wait for the move images thread to end
    try {
      moveImagesToFrameThread.join ();
    } catch (InterruptedException e) {
      // re-throw InterruptedException
      Thread.currentThread ().interrupt ();
    }
  }

  private void getDataFromMoveImages () {
    Throwable caughtException;
    String moveImageToFrameStr = "";
    
    try {
      // DEBUG
      // qOS.debugInOutIndicator ("R");
      // end DEBUG
      while ((moveImageToFrameStr = readLine ()) != null) {
        // DEBUG
        // lastLineFromMoveImageToFrame = moveImageToFrameStr;
        // qOS.debugInOutIndicator ("L");
        // end DEBUG
        addToDisplaySB (moveImageToFrameStr, lineDelimiter);
      }
      // all done, close the pipe
      try {
        qIS.close ();
      } catch (IOException e) {}

      // indicate that move images to frame is done
      qIS = null;
    } catch (IOException e) {
      displayText (String.format ("----------------------------------%nLast data read was %d chars long%n%s%n"
          + "IOException on Queue: %s%n", moveImageToFrameStr.length (), moveImageToFrameStr, e.toString ()), true);
      // close the Queue
      try {
        qIS.close ();
      } catch (IOException e2) {
        displayText (String.format ("IOException on closing Queue: %s%n", e2.toString ()), true);
      }
      // ask the execution thread to stop
      moveImagesToFrameThread.interrupt ();
      try {
        // wait for it to stop
        moveImagesToFrameThread.join ();
      } catch (InterruptedException e1) {
        // did we get an InterruptedException because of a problem in MoveImagesToFrame.rotate?
        caughtException = moveImagesToFrameThread.getCaughtException ();
        if (caughtException != null) {
          formatDisplayThrowable (caughtException);
        }
        // re-throw InterruptedException
        Thread.currentThread ().interrupt ();
      }
    }
  }
  
  private String readLine () throws IOException { 
    StringBuilder sb = new StringBuilder (1000);
    String readLineStr;
    int intRead;

    if (readerEOF)
      return null;
    
    while ((intRead = qIS.read ()) != -1) {
      if (intRead == '\r') {
        // got a <CR> line termination character
        lastCharRead = intRead;
        break;
      }
      if (intRead == '\n') {
        // got a <NL> line termination character
        if (lastCharRead == '\r') {
          // this is <CR><NL>, ignore the <NL>
          continue;
        }
        // got a <NL> line termination character
        lastCharRead = intRead;
        break;
      }
      lastCharRead = intRead;
      sb.append ((char) intRead);
    }
    
    if (intRead == -1) {
      // reader reached end of stream, any data in sb?
      readerEOF = true;
      if (sb.length () == 0) {
        // nothing in sb, return null
        return null;
      }
    }
    
    readLineStr = sb.toString ();
    sb.setLength (0);
    return readLineStr;
  }

  private void addToDisplaySB (String...displayData)
  {
    synchronized(displaySB) {
      for (int i = 0; i < displayData.length; ++i) {
        displaySB.append (displayData[i]);
      }
    }
  }

  private boolean processDataFromMoveImages () {
    boolean dataProcessedToDisplay = false;
    String displaySBStr = null;
    
    while (moveImagesToFrameReadThread != null && !dataProcessedToDisplay) {
      // get data from thread while it is alive
      synchronized (displaySB) {
        if (displaySB.length () > 0) {
          dataProcessedToDisplay = true;
          displaySBStr = displaySB.toString ();
          displaySB.setLength (0);
        }
      }
      if (dataProcessedToDisplay) {
        styledText.append (displaySBStr);
        int numChars = styledText.getCharCount ();
        styledText.setSelection (numChars);
        styledText.setHorizontalIndex (0);
      }
      else {
        // there was no data available, wait for read thread to exit but only for DISPLAY_TEXT_MILLIS milliseconds
        try {
          moveImagesToFrameReadThread.join (DISPLAY_TEXT_MILLIS);
        } catch (InterruptedException e) {
          // re-throw InterruptedException
          Thread.currentThread ().interrupt ();
        }
        if (!moveImagesToFrameReadThread.isAlive ()) {
          // the thread isn't alive so we have all of the data, get the last few lines
          if (oldCursor != null) {
            // restore the original cursor
            styledText.setCursor (oldCursor);
          }
          synchronized (displaySB) {
            if (displaySB.length () > 0) {
              dataProcessedToDisplay = true;
              displaySBStr = displaySB.toString ();
              displaySB.setLength (0);
            }
          }
          if (dataProcessedToDisplay) {
            styledText.append (displaySBStr);
            int numChars = styledText.getCharCount ();
            styledText.setSelection (numChars);
            styledText.setHorizontalIndex (0);
          }

          if (optionsResult.autoWriteLogFile && logFile != null) {
            // auto-write the results to the log file
            writeStyledTextToLogFile ();
          }
          else {
            afterRun = true; // enable the write log button
            enableDisableLogFileMenuItem ();
          }
          // indicate we have processed all the data to the display
          moveImagesToFrameReadThread = null;
          displaySB = null;
        }
        else {
          // thread is still alive, did we get data while waiting?
          synchronized (displaySB) {
            if (displaySB.length () > 0) {
              dataProcessedToDisplay = true;
              displaySBStr = displaySB.toString ();
              displaySB.setLength (0);
            }
          }
          if (dataProcessedToDisplay) {
            styledText.append (displaySBStr);
            int numChars = styledText.getCharCount ();
            styledText.setSelection (numChars);
            styledText.setHorizontalIndex (0);
          }
          break; // need to return to dispatch loop
        }
      }
    }
    return dataProcessedToDisplay;
  }

  private void formatDisplayThrowable (Throwable caughtException) {
    String indent = "";
    StringBuffer sb = new StringBuffer (2000);
    
    while (caughtException != null) {
      sb.append (indent);
      sb.append (caughtException.toString ());
      if (caughtException.getCause () != null) {
        indent = indent + "  ";
        sb.append (indent);
        sb.append ("Caused By:\n");
        indent = indent + "  ";
        caughtException = caughtException.getCause ();
      }
    }
    displayText (sb.toString (), true);
  }
}
