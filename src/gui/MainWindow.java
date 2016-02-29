package gui;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
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
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
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
  public static final int DISPLAY_TEXT_MILLIS = 1000;

  // Preference keys for this package
  private static final String FRAME_DIR = "frame_dir";
  private static final String SOURCE_DIR = "source_dir";
  private static final String DATABASE_DIR = "database_dir";
  private static final String LOG_FILE = "log_file";
  private static final String PERCENT_TO_CHANGE_ON_FRAME = "percent_to_change_on_frame";
  private static final String MB_TO_LEAVE_FREE = "mb_to_leave_free";
  private static final String APPEND_TO_LOG_FILE = "append_to_log_file";
  private static final String LIST_FILES_ONLY = "list_files_only";
  private static final String QUIET_MODE = "quiet_mode";
  private static final String DEBUG_MODE = "debug_mode";

  protected Shell shlMoveImagesToFrameGui;
  private Menu menu;
  private StyledText styledText;
  // private String lineDelimiter;
  private StyleRange[] selectedRanges;
  private int newCharCount, start;
  private Font textFont;
  private boolean displayTextAppend = false;
  private Runnable timer;
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
      window.open ();
    } catch (Exception e) {
      System.out.println (e.toString ());
      e.printStackTrace ();
    }
  }

  /**
   * Open the window.
   */
  public void open () {
    readConfiguration ();
    Display display = Display.getDefault ();
    createContents ();
    shlMoveImagesToFrameGui.open ();
    shlMoveImagesToFrameGui.layout ();
    displaySettings ();
    while (!shlMoveImagesToFrameGui.isDisposed ()) {
      if (!display.readAndDispatch ()) {
        display.sleep ();
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
    String header = null;
    try {
      header = MoveImagesToFrame.getVersion () + " GUI";
    } catch (Throwable e) {
      displayText (String.format ("%s%n", e.toString ()), true);
    }
    shlMoveImagesToFrameGui.setText (header);
    shlMoveImagesToFrameGui.setLayout (null);

    styledText = new StyledText (shlMoveImagesToFrameGui, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
    styledText.getLineDelimiter ();
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
        writeToLogIfSet ();
      }
    });
    
    timer = new Runnable () {
      @Override
      public void run () {
        if (shlMoveImagesToFrameGui.isDisposed ())
          return;
        if (displayTextAppend) {
          // there has been a displayText append since the last time we ran, force a scroll to bottom of text
          displayTextAppend = false;
          int numChars = styledText.getCharCount ();
          styledText.setSelection (numChars);
          styledText.setHorizontalIndex (0);
        }
        shlMoveImagesToFrameGui.getDisplay ().timerExec (DISPLAY_TEXT_MILLIS, this);
      }
    };
    // start the timer running
    shlMoveImagesToFrameGui.getDisplay ().timerExec (DISPLAY_TEXT_MILLIS, timer);
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
    // styledText.addPaintObjectListener(new PaintObjectListener() {
    // @Override
    // public void paintObject(PaintObjectEvent event) {
    // handlePaintObject(event);
    // }
    // });
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
    // Point cSize = menu.computeSize(rect.width, SWT.DEFAULT);
    // Point sSize = statusBar.computeSize(SWT.DEFAULT, SWT.DEFAULT);
    // int statusMargin = 2;
    // coolBar.setBounds(rect.x, rect.y, cSize.x, cSize.y);
    styledText.setBounds (rect.x, rect.y, rect.width, rect.height);
    // statusBar.setBounds(rect.x + statusMargin, rect.y + rect.height - sSize.y - statusMargin, rect.width - (2 *
    // statusMargin), sSize.y);
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
        // if (boldControl.getSelection()) style.fontStyle |= SWT.BOLD;
        // if (italicControl.getSelection()) style.fontStyle |= SWT.ITALIC;
      }
      // if ((styleState & FOREGROUND) != 0) {
      // style.foreground = textForeground;
      // }
      // if ((styleState & BACKGROUND) != 0) {
      // style.background = textBackground;
      // }
      // int underlineStyle = styleState & UNDERLINE;
      // if (underlineStyle != 0) {
      // style.underline = true;
      // style.underlineColor = underlineColor;
      // switch (underlineStyle) {
      // case UNDERLINE_SINGLE: style.underlineStyle = SWT.UNDERLINE_SINGLE; break;
      // case UNDERLINE_DOUBLE: style.underlineStyle = SWT.UNDERLINE_DOUBLE; break;
      // case UNDERLINE_SQUIGGLE: style.underlineStyle = SWT.UNDERLINE_SQUIGGLE; break;
      // case UNDERLINE_ERROR: style.underlineStyle = SWT.UNDERLINE_ERROR; break;
      // case UNDERLINE_LINK: {
      // style.underlineColor = null;
      // if (link != null && link.length() > 0) {
      // style.underlineStyle = SWT.UNDERLINE_LINK;
      // style.data = link;
      // } else {
      // style.underline = false;
      // }
      // break;
      // }
      // }
      // }
      // if ((styleState & STRIKEOUT) != 0) {
      // style.strikeout = true;
      // style.strikeoutColor = strikeoutColor;
      // }
      // int borderStyle = styleState & BORDER;
      // if (borderStyle != 0) {
      // style.borderColor = borderColor;
      // switch (borderStyle) {
      // case BORDER_DASH: style.borderStyle = SWT.BORDER_DASH; break;
      // case BORDER_DOT: style.borderStyle = SWT.BORDER_DOT; break;
      // case BORDER_SOLID: style.borderStyle = SWT.BORDER_SOLID; break;
      // }
      // }
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
      // if (disposeFg && style.foreground != textForeground && style.foreground != null) style.foreground.dispose();
      // if (disposeBg && style.background != textBackground && style.background != null) style.background.dispose();
      // if (disposeStrike && style.strikeoutColor != strikeoutColor && style.strikeoutColor != null)
      // style.strikeoutColor.dispose();
      // if (disposeUnder && style.underlineColor != underlineColor && style.underlineColor != null)
      // style.underlineColor.dispose();
      // if (disposeBorder && style.borderColor != borderColor && style.borderColor != null)
      // style.borderColor.dispose();

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
    // Guard against superfluous mouse move events -- defer action until later
    Display display = styledText.getDisplay ();
    display.asyncExec (new Runnable () {
      @Override
      public void run () {
        if (appendText) {
          styledText.append (textString);
          displayTextAppend = true; // the free running timerTask will scroll to the bottom
        }
        else {
          styledText.setText (textString);
        }
      }
    });
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
    prefs.putBoolean (APPEND_TO_LOG_FILE, optionsResult.appendToLogFile);
    prefs.putBoolean (DEBUG_MODE, optionsResult.debugMode);
    prefs.putBoolean (LIST_FILES_ONLY, optionsResult.listFilesOnly);
    prefs.putInt (MB_TO_LEAVE_FREE, optionsResult.mbToLeaveFree);
    prefs.putInt (PERCENT_TO_CHANGE_ON_FRAME, optionsResult.percentToChangeOnFrame);
    prefs.putBoolean (QUIET_MODE, optionsResult.quietMode);
  }

  private void displayErrorDialog (String errorMsg, String dialogTitle) {
    displayText (errorMsg, true);
    MessageBox errorDialog = new MessageBox (shlMoveImagesToFrameGui, SWT.ICON_ERROR | SWT.OK);
    errorDialog.setText (dialogTitle);
    errorDialog.setMessage (errorMsg);
    errorDialog.open ();
  }

  protected void writeToLogIfSet () {
    // TODO Auto-generated method stub
    
  }

  private void runMoveImagesToFrame () {
    saveConfiguration ();

    Throwable caughtException = null;
    char[] charArray = new char[1024];
    int charsRead;
    QueueOutputStream queueOS = new QueueOutputStream ();
    QueueInputStream queueIS = null;
    PrintStream outPS = null;
    try {
      queueIS = new QueueInputStream (queueOS);
    } catch (IOException e) {
      String errorMsg = String.format ("IOException opening QueueInputStream: %s", e.toString ());
      displayErrorDialog (errorMsg, "Error opening QueueInputStream");
      return;
    }
    outPS = new PrintStream (queueOS);
    InputStreamReader isReader = new InputStreamReader (queueIS);

    displayText (String.format ("running MoveImagesToFrame%n"), true);

    // create a thread to move the images to the frame
    MoveImagesToFrameThread moveImagesToFrameThread =
        new MoveImagesToFrameThread ("mitfThread", Thread.currentThread (), outPS, frameDir, sourceDir, databaseDir,
            ((float) optionsResult.percentToChangeOnFrame / 100.0f),
            optionsResult.mbToLeaveFree * MoveImagesToFrame.BINARY_MB, !optionsResult.quietMode,
            optionsResult.debugMode, optionsResult.listFilesOnly);
    moveImagesToFrameThread.start ();
    
    String moveImageToFrameStr = "";
    try {
      while ((charsRead = isReader.read (charArray)) != -1) {
        moveImageToFrameStr = new String (charArray, 0, charsRead);
        displayText (moveImageToFrameStr, true);
      }
    } catch (IOException e) {
      displayText (String.format ("----------------------------------%nLast data read was %d chars long%n%s%n"
          + "IOException on Queue: %s%n", moveImageToFrameStr.length (), moveImageToFrameStr, e.toString ()), true);
      // close the Queue
      try {
        isReader.close ();
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
    // all done, close the pipe
    try {
      isReader.close ();
    } catch (IOException e) {}
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
