package moveImagesToFrame;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Collection;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import gui.MainWindow;

public class MoveImagesToFrame
{
  private static final String MOVE_IMAGE_TO_FRAME_VERSION = "v0.11.0";
  private static final String MOVE_IMAGE_TO_FRAME_NAME = "MoveImagesToFrame";
  private static final String SOFTWARE_TYPE = MOVE_IMAGE_TO_FRAME_NAME + " " + MOVE_IMAGE_TO_FRAME_VERSION;
  private static final String[] extensionsToProcess = { ".jpg", ".jpeg", ".jpe" };
  private static final GregorianCalendar Year2000 = new GregorianCalendar (2000, Calendar.JANUARY , 1, 0, 0, 0);
  private static final long BINARY_MB = 1024L * 1024L;
  
  PrintStream outPS;
  File frameDir;
  File sourceDir;
  File dataBaseDir;
  float percentageToReplace;
  long numberBytestoLeaveFree;
  boolean verboseMode;
  boolean debugMode;
  /**
   * @param args
   */
  public static void main (String[] args)
  {
    File frameDir = null;
    File sourceDir = null;
    File dataBaseDir = null;
    float percentageToReplace = 0.0f;
    PrintStream outPS = System.out;
    Boolean listFilesOnly = Boolean.FALSE;
    boolean debugMode = false;
    boolean verboseMode = true;
    CommandLine cmdLineParms = null;
    long frameDirTotalSpace = 0L;
    long numberBytestoLeaveFree = 0L;
    SimpleDateFormat dateFormat = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss.SSS z");

    if ((args.length > 0) && (!args[0].startsWith("-"))) {
      // there is at least one command line parameter and it does NOT start with a "-",
      // use backward compatible positional command line parameters
      if (args.length < 5) {
        printUsage(outPS, "5 arguments are required, only " + args.length + " were specified");
      }
      frameDir = new File(args[0]);
      if (!frameDir.exists()) {
        printUsage(outPS, "1st parameter, the <Picture Frame Dir>, \"" + args[0] + "\" does not exist");
      }
      if (!frameDir.isDirectory()) {
        printUsage(outPS, "1st parameter, the <Picture Frame Dir>, \"" + args[0] + "\" is not a directory");
      }
      sourceDir = new File(args[1]);
      if (!sourceDir.exists()) {
        printUsage(outPS, "2nd parameter, the <Source Dir>, \"" + args[1] + "\" does not exist");
      }
      if (!sourceDir.isDirectory()) {
        printUsage(outPS, "2nd parameter, the <Source Dir>, \"" + args[1] + "\" is not a directory");
      }
      try {
        Integer rawPercent = Integer.valueOf(args[2]);
        if (rawPercent < 1 || rawPercent > 100) {
          printUsage(outPS, "3rd parameter, the <% to Change>, \"" + args[2] + "\" is not within range of 1 to 100");
        }
        percentageToReplace = (float) (rawPercent / 100.0);
      } catch (NumberFormatException e) {
        printUsage(outPS, "3rd parameter, the <% to Change>, \"" + args[2] + "\" is not a valid number");
      }
      dataBaseDir = new File(args[3]);
      if (!dataBaseDir.exists()) {
        printUsage(outPS, "4th parameter, the <Database Dir>, \"" + args[3] + "\" does not exist");
      }
      if (!dataBaseDir.isDirectory()) {
        printUsage(outPS, "4th parameter, the <Database Dir>, \"" + args[3] + "\" is not a directory");
      }
      frameDirTotalSpace = frameDir.getTotalSpace ();
      numberBytestoLeaveFree = 0L;
      try {
        numberBytestoLeaveFree = Long.valueOf (args[4]);
        numberBytestoLeaveFree *= BINARY_MB;
        if (frameDirTotalSpace < numberBytestoLeaveFree) {
          printUsage (outPS, String.format ("5th parameter, the <Megabytes to leave free>, \"%,d\" is larger than "
              + "the frame's total space of %,d bytes.", args[4], frameDirTotalSpace));
        }
      } catch (NumberFormatException e) {
        printUsage (outPS, "5th parameter, the <Megabytes to leave free> \"" + args[4] + "\" is not a valid number");
      }
      if (args.length >= 6) {
        // a log file has been entered, open it for append
        try {
          outPS = new PrintStream(new FileOutputStream(args[5], true), true);
        } catch (FileNotFoundException e) {
          printUsage(outPS, String.format("Could not open Log File \"%s\": %s%n", args[5], e.toString()));
        }
      }
      if (args.length >= 7) {
        // listFilesOnly has been entered, read it
        listFilesOnly = Boolean.valueOf(args[6]);
      } 
    }
    else {
      // use Apache Commons CLI to parse keyword command line parameters
      if (args.length == 0) {
        // use the GUI
        try {
          MainWindow window = new MainWindow ();
          window.open ();
        } catch (Exception e) {
          System.out.println (e.toString ());
          e.printStackTrace ();
        }
        System.exit (0); // done
      }
      
      String cmdArg = null;
      cmdLineParms = processKeywordParms (args);
      // did user ask for version?
      if (cmdLineParms.hasOption ('v')) {
        // print version and exit
        outPS.println (SOFTWARE_TYPE);
        System.exit (18);
      }
      if (cmdLineParms.hasOption ('?') || cmdLineParms.hasOption ("help")) {
        printHelpAndExit (outPS);
      }
      if (cmdLineParms.hasOption ("log")) {
        // a log file has been entered, open it for append
        cmdArg = cmdLineParms.getOptionValue ("log");
        try {
          outPS = new PrintStream(new FileOutputStream(cmdArg, true), true);
        } catch (FileNotFoundException e) {
          parmInvalid (outPS, "Optional", "-log, Log File", cmdArg, e.toString ());
        }
      }
      if (cmdLineParms.hasOption ('D')) {
        debugMode = true;
      }
      if (cmdLineParms.hasOption ('d')) {
        // Required Parameter, -d, Database directory
        cmdArg = cmdLineParms.getOptionValue ('d');
        dataBaseDir = new File (cmdArg);
        if (!dataBaseDir.exists()) {
          parmInvalid (outPS, "Required", "-d, Database directory", cmdArg, "directory doesn't exist");
        }
        if (!dataBaseDir.isDirectory()) {
          parmInvalid (outPS, "Required", "-d, Database directory", cmdArg, "isn't a directory");
        }
      }
      else {
        // required Parameter missing
        requiredParmMissing (outPS, "-d, Database directory");
      }
      if (cmdLineParms.hasOption ('f')) {
        // Required Parameter, -f, Frame directory
        cmdArg = cmdLineParms.getOptionValue ('f');
        frameDir = new File (cmdArg);
        if (!frameDir.exists()) {
          parmInvalid (outPS, "Required", "-f, Frame directory", cmdArg, "directory doesn't exist");
        }
        if (!frameDir.isDirectory()) {
          parmInvalid (outPS, "Required", "-f, Frame directory", cmdArg, "isn't a directory");
        }
      }
      else {
        // required Parameter missing
        requiredParmMissing (outPS, "-f, Frame directory");
      }
      if (cmdLineParms.hasOption ('m')) {
        // Required Parameter, -m, number of Megabytes to leave free on frame
        cmdArg = cmdLineParms.getOptionValue ('m');
        frameDirTotalSpace = frameDir.getTotalSpace ();
        numberBytestoLeaveFree = 0L;
        try {
          numberBytestoLeaveFree = Long.valueOf (cmdArg);
          numberBytestoLeaveFree *= BINARY_MB;
          if (frameDirTotalSpace < numberBytestoLeaveFree) {
            parmInvalid (outPS, "Required", "-m, number of Megabytes to leave free on frame", cmdArg, 
                String.format ("is larger than the frame's total space of %,d bytes.", frameDirTotalSpace));
          }
        } catch (NumberFormatException e) {
          parmInvalid (outPS, "Required", "-m, number of Megabytes to leave free on frame", cmdArg,
              "is not a valid number");
        }
      }
      else {
        // required Parameter missing
        requiredParmMissing (outPS, "-m, number of Megabytes to leave free on frame");
      }
      if (cmdLineParms.hasOption ('p')) {
        // Required Parameter, -p, % of frame images to change
        cmdArg = cmdLineParms.getOptionValue ('p');
        try {
          Integer rawPercent = Integer.valueOf(cmdArg);
          if (rawPercent < 1 || rawPercent > 100) {
            parmInvalid (outPS, "Required", "-p, % of frame images to change", cmdArg,
                "is not within range of 1 to 100");
          }
          percentageToReplace = ((float) rawPercent) / 100.0f;
        } catch (NumberFormatException e) {
          parmInvalid (outPS, "Required", "-p, % of frame images to change", cmdArg,
              "is not a valid number");
        }
      }
      if (cmdLineParms.hasOption ('q')) {
        verboseMode = false;
      }
      if (cmdLineParms.hasOption ('l')) {
        listFilesOnly = Boolean.TRUE;
        // if user has asked for list files only make sure verbose mode is true
        verboseMode = true;
      }
      if (cmdLineParms.hasOption ('s')) {
        // Required Parameter, -s, image Source Directory
        cmdArg = cmdLineParms.getOptionValue ('s');
        sourceDir = new File (cmdArg);
        if (!sourceDir.exists()) {
          parmInvalid (outPS, "Required", "-s, image Source Directory", cmdArg, "directory doesn't exist");
        }
        if (!sourceDir.isDirectory()) {
          parmInvalid (outPS, "Required", "-s, image Source Directory", cmdArg, "isn't a directory");
        }
      }
      else {
        // required Parameter missing
        requiredParmMissing (outPS, "-s, image Source Directory");
      }
    }
    
    if (verboseMode) {
      String currDate = dateFormat.format (new Date (System.currentTimeMillis ()));
      outPS.printf (
          "%s %s%n<Picture Frame Dir> is \"%s\", <Source Dir> is \"%s\", <%% to Change> is %d%% "
              + "%n<Database Dir> is \"%s\"%n<Megabytes to leave free> is %,d, Frames's total space that can be used "
              + "is %,d%nList Files Only = %s, debugMode = %s%n",
          SOFTWARE_TYPE, currDate, frameDir.toPath ().toString (), sourceDir.toPath ().toString (),
          ((int) (percentageToReplace * 100.0f)), dataBaseDir.toPath ().toString (),
          (numberBytestoLeaveFree / BINARY_MB), frameDirTotalSpace, listFilesOnly, debugMode);
    }
    MoveImagesToFrame moveImagesToFrame = new MoveImagesToFrame (outPS, frameDir, sourceDir, dataBaseDir,
      percentageToReplace, numberBytestoLeaveFree, verboseMode, debugMode);
    
    moveImagesToFrame.rotateFiles (listFilesOnly);
    outPS.println ("----------------------------------------------------------------");
  }

  private static void parmInvalid (PrintStream outPS, String typeParm, String cmdStr, String arg, String error) {
    outPS.printf ("%s parameter \"%s\", argument \"%s\" is invalid, \"%s\", can't continue.%n%n", typeParm, cmdStr, arg,
        error);
    printHelpAndExit (outPS);
  }

  private static void requiredParmMissing (PrintStream outPS, String cmdStr) {
    outPS.printf ("Required parameter \"%s\", not specified, can't continue.%n%n", cmdStr);
    printHelpAndExit (outPS);
  }

  private static void printHelpAndExit (PrintStream outPS) {
    // print help and exit
    HelpFormatter formatter = new HelpFormatter();
    Options options = getCmdLineOptions ();
    formatter.printHelp( MOVE_IMAGE_TO_FRAME_NAME, options );
    System.exit (8);
  }
  
  private static CommandLine processKeywordParms (String[] args) {
    CommandLineParser parser = new DefaultParser ();
    CommandLine cmdLine = null;
    Options options = getCmdLineOptions ();

    try {
      // parse the command line arguments
      cmdLine = parser.parse (options, args);
    } catch (ParseException e) {
      // oops, something went wrong
      System.out.printf ("Command Line Parsing failed.  Reason: %s%n", e.toString ());
      System.exit (20);
    }

    return cmdLine;
  }

  private static Options getCmdLineOptions () {
    // define the option we support
    Options options = new Options ();
    options.addOption ("?", false, "print this message");
    options.addOption ("help", false, "print this message");
    options.addOption ("v", "version", false, "print the version information and exit");
    options.addOption ("q", "quiet", false, "quiet mode, most output not displayed");
    options.addOption ("D", "Debug", false, "print debugging information");
    options.addOption ("f", "frame-dir", true, "Picture Frame Directory (Required)");
    options.addOption ("s", "source-dir", true, "image Source Directory (Required)");
    options.addOption ("p", "percent", true, "% of frame images to change (1-100) (Required)");
    options.addOption ("d", "database-dir", true, "Database directory (Required)");
    options.addOption ("m", "megabytes-free", true, "number of Megabytes to leave free on frame (Required)");
    options.addOption ("l", "list-files-only", false, "list files only (frame & source), don't rotate files");
    options.addOption ("log", true, "log file to append all output to");
    return options;
  }

  /**
   * @param outPS
   * @param frameDir
   * @param sourceDir
   * @param dataBaseDir
   * @param percentageToReplace
   * @param numberBytestoLeaveFree
   * @param verboseMode
   * @param debugMode
   */
  public MoveImagesToFrame (PrintStream outPS, File frameDir, File sourceDir, File dataBaseDir,
    float percentageToReplace, long numberBytestoLeaveFree, boolean verboseMode, boolean debugMode)
  {
    this.outPS = outPS;
    this.frameDir = frameDir;
    this.sourceDir = sourceDir;
    this.dataBaseDir = dataBaseDir;
    this.percentageToReplace = percentageToReplace;
    this.numberBytestoLeaveFree = numberBytestoLeaveFree;
    this.verboseMode = verboseMode;
    this.debugMode = debugMode;
  }

  private void rotateFiles (boolean listFilesOnly)
  {
    TreeMap<String, PictureFrameFileInfo> filesInFrameDir = new TreeMap<String, PictureFrameFileInfo> ();
    TreeMap<String, PictureFrameFileInfo> uniqueFilesInFrameDir = new TreeMap<String, PictureFrameFileInfo> ();
    TreeMap<String, PictureFrameFileInfo> filesInSourceDir = null;
    TreeMap<String, PictureFrameFileInfo> modifiedFilesInDir = null;
    LastModifiedDirs lastModifiedDirs;
    int numFrameDirFiles, numSourceDirFiles, numUniqueFrameDirFiles;
    long frameDirLastModifiedTime = frameDir.lastModified ();
    long sourceDirLastModifiedTime = sourceDir.lastModified ();
    boolean frameDirModified = false;
    boolean sourceDirModified = false;

    // try to compare last modified times for frameDir & sourceDir
    if ((lastModifiedDirs = LastModifiedDirs.getLastModifiedDirsFromDatabase (dataBaseDir, outPS)) != null) {
      // we were able to get the LastModifiedDirs from the data base, compare last run to current run
      if (debugMode) {
        outPS.printf ("Last modified times from database:    %s%n", lastModifiedDirs);
        LastModifiedDirs currDirTimes = new LastModifiedDirs (frameDirLastModifiedTime, sourceDirLastModifiedTime);
        outPS.printf ("Last modified times from directories: %s%n", currDirTimes);
      }
      if (frameDirLastModifiedTime != lastModifiedDirs.getFrameDirLastModifiedTime ()) {
        // frameDir has been modified since last run
        frameDirModified = true;
        if (listFilesOnly || debugMode) {
          outPS.println ("The Frame Dir has been modified since the last run.");
        }
      }
      if (sourceDirLastModifiedTime != lastModifiedDirs.getSourceDirLastModifiedTime ()) {
        // frameDir has been modified since last run
        sourceDirModified = true;
        if (listFilesOnly || debugMode) {
          outPS.println ("The Source Dir has been modified since the last run.");
        }
      }
    }
    
    // try to get file information from dataBase directory
    outputIfVerbose ("Read the Picture Frame File Info Database%n");
    filesInSourceDir = PictureFrameFileInfo.getFileInfoFromDatabase (dataBaseDir, outPS);
    if (filesInSourceDir.isEmpty ()) {
      // the Database does not exist, get a list of all files in the source directory
      outputIfVerbose ("Picture Frame File Info Database did not exist, scanning Source Dir%n");
      filesInSourceDir = scanDirectory (sourceDir, true);
      sourceDirModified = false; // ignore sourceDirModified since we just read all of sourceDir
    }

    if (sourceDirModified) {
      // the sourceDir was modified, read the sourceDir and merge those records to database records
      modifiedFilesInDir = scanDirectory (sourceDir, true);
      mergePictureFrameFileInfo (modifiedFilesInDir, filesInSourceDir, uniqueFilesInFrameDir, true);
    }
    if (frameDirModified) {
      // the frameDir was modified, read the frameDir and merge those records to database records
      modifiedFilesInDir = null;
      modifiedFilesInDir = scanDirectory (frameDir, false);
      mergePictureFrameFileInfo (modifiedFilesInDir, filesInSourceDir, uniqueFilesInFrameDir, false);
    }
    // we found the Database file (or created it), now extract the frame's file info
    extractFrameFileInfo (filesInSourceDir, filesInFrameDir, uniqueFilesInFrameDir);

    if (filesInFrameDir.isEmpty ()) {
      // the File Info on the Source indicates there are no known files in the Frame, extract the file info from frame
      outputIfVerbose ("File Info from the Frame doesn't exist, scanning Frame Dir%n");
      // get a list of all files in the frame directory
      filesInFrameDir = scanDirectory (frameDir, false);
      mergePictureFrameFileInfo (filesInFrameDir, filesInSourceDir, uniqueFilesInFrameDir, false);
    }
    
    numUniqueFrameDirFiles = uniqueFilesInFrameDir.size ();
    numSourceDirFiles = filesInSourceDir.size () - numUniqueFrameDirFiles;
    numFrameDirFiles = filesInFrameDir.size ();
    outputIfVerbose ("Number of sourceDir files: %d, Number of frameDir files: %d, Number of Unique frameDir "
    + "files: %d%n", numSourceDirFiles, numFrameDirFiles, numUniqueFrameDirFiles);
    
    warnOfUnknownFrameFiles (uniqueFilesInFrameDir);

    if (sourceDirModified || frameDirModified) {
      // only update database if there have been modification, Issue #1
      // now that we have a complete list of files on the frame and in the source directory save it
      PictureFrameFileInfo.saveToDataBase (filesInSourceDir, dataBaseDir, outPS);
      saveDirsLastModifiedTimeToDatabase (dataBaseDir, frameDir, sourceDir);
    }
    if (!listFilesOnly) {
      // not listing files only, rotate files
      boolean modified = false;
      if (numFrameDirFiles > 0) {
        // determine which files should be deleted from frame and then new files copied to frame
        modified = replaceFrameFilesWithSourceFiles (filesInFrameDir, filesInSourceDir, percentageToReplace,
          numberBytestoLeaveFree, numSourceDirFiles, numFrameDirFiles, numUniqueFrameDirFiles, sourceDir, frameDir);
      }
      else {
        // there are no known files in frameDir, move as many sourceDir files to the frame as will 'fit'
        modified = replaceFrameFilesWithSourceFiles (filesInFrameDir, filesInSourceDir, 100.0f, numberBytestoLeaveFree,
          numSourceDirFiles, numFrameDirFiles, numUniqueFrameDirFiles, sourceDir, frameDir);
      }
      if (modified) {
        // finished moving files, update database files
        // frame or source files changed, update database, Issue # 1
        PictureFrameFileInfo.saveToDataBase (filesInSourceDir, dataBaseDir, outPS);
        saveDirsLastModifiedTimeToDatabase (dataBaseDir, frameDir, sourceDir);
      }
    }
    else {
      // list files only, no rotate of files
      listFilesInfo ("", filesInSourceDir, true);
      outPS.println ();
      listFilesInfo ("not ", filesInSourceDir, false);
    }

    if (debugMode) {
      frameDirLastModifiedTime = frameDir.lastModified ();
      sourceDirLastModifiedTime = sourceDir.lastModified ();
      // create the frameDir & sourceDir modified time object
      LastModifiedDirs currDirTimes = new LastModifiedDirs (frameDirLastModifiedTime, sourceDirLastModifiedTime);
      outPS.printf ("Last modified times of directories: %s%n", currDirTimes);
    }
    
    outPS.println ("MoveImagesToFrame Finished");
  }
  
  private void listFilesInfo (String dirType, TreeMap<String, PictureFrameFileInfo> filesInDir, boolean listFrame)
  {
    int numFilesListed = 0;
    outPS.printf ("------------------- List of files %son Frame%n", dirType);
    for (PictureFrameFileInfo aFile : filesInDir.values ()) {
      if ((listFrame && aFile.isOnFrame ()) || (!listFrame && !aFile.isOnFrame ())) {
        outPS.println (aFile.toString ());
        ++numFilesListed;
      }
    }
    outPS.printf ("----------%,d files listed%n", numFilesListed);
  }

  private void saveDirsLastModifiedTimeToDatabase (File dataBaseDir, File frameDir, File sourceDir)
  {
    long frameDirLastModifiedTime = frameDir.lastModified ();
    long sourceDirLastModifiedTime = sourceDir.lastModified ();
    // create the frameDir & sourceDir modified time object
    LastModifiedDirs lastModifiedDirs = new LastModifiedDirs (frameDirLastModifiedTime, sourceDirLastModifiedTime);
    LastModifiedDirs.saveToDataBase (lastModifiedDirs, dataBaseDir, outPS);
  }
  
  private void warnOfUnknownFrameFiles (TreeMap<String, PictureFrameFileInfo> uniqueFilesInFrameDir)
  {
    if (uniqueFilesInFrameDir.size () > 0) {
      Collection<PictureFrameFileInfo> frameFileInfo = uniqueFilesInFrameDir.values ();
      // warn that the unique files currently in the Frame will not be removed
      outputIfVerbose ("Warning: the following %d file(s) that are in the Picture Frame are unknown and will not"
        + " be deleted to make room for new files%n", uniqueFilesInFrameDir.size ());
      for (PictureFrameFileInfo aFile : frameFileInfo) {
        outputIfVerbose (aFile.getFilenameOnFrame ());
        outputIfVerbose ("%n");
      }
      // now empty the list of unique files in the Picture Frame, we can't remove them for space for new files
      uniqueFilesInFrameDir.clear ();
    }
  }
  
  private void outputIfVerbose (String formatString, Object...args) {
    if (verboseMode) {
      // verbose is true, output the format string and any arguments
      outPS.printf (formatString, args);
    }
  }
  
  private void outputIfDebug (String debugFormatString, Object...args) {
    if (debugMode) {
      // debug mode is true, output the debug Format string
      outPS.printf (debugFormatString, args);
    }
  }

  private void mergePictureFrameFileInfo (TreeMap<String, PictureFrameFileInfo> fromMap,
    TreeMap<String, PictureFrameFileInfo> toMap, TreeMap<String, PictureFrameFileInfo> uniqueFilesInFrameDir,
    boolean fromMapIsSrc)
  {
    /*
     * fromMapIsSrc - true - the fromMap is updating sourceDir Info
     *                false - the fromMap is updating frameDir Info
     * 
     * o Keep track of all toMap records updated, added and deleted
     * o Extract into a temporary TreeMap all records from toMap that have sourceDir or frameDir Info (specified by 
     *   fromMapIsSrc)
     * o If the fromMap has a record that is not in toMap, add the record to toMap
     * o If the fromMap has a record that is in toMap update sourceDir or frameDir Info (specified by fromMapIsSrc)
     *   save as much info in toMap record (Calendar object especially) as possible.
     *   o Once the data for the existing record is updated delete the equivalent record from the temporary TreeMap
     *     so that we know that record in toMap still exists and has been updated.
     * o Once all the data in fromMap has been processed we need to check the temporary TreeMap
     * o If there are records in the temporary TreeMap, check them against toMap, do the following:
     *   o fromMapIsSrc is true, this file / record no longer exists in sourceDir, check if the record has frameDir
     *     info, if it does, zero out all the SourceDir fields, if it does NOT then delete this record from toMap
     *   o fromMapIsSrc is false, this file / record no longer exists in frameDir, check if the record has sourceDir
     *     info, if it does, zero out all the frameDir fields, if it does NOT then delete this record from toMap
     */

    TreeMap<String, PictureFrameFileInfo> tempMap = null;
    int toMapUpdates = 0;
    int toMapAdded = 0;
    int toMapDeleted = 0;
    int fromMapRead = 0;
    // ArrayList<PictureFrameFileInfo> listOfFromMap = new ArrayList<PictureFrameFileInfo> (fromMap.values ());
    PictureFrameFileInfo toMapRecord = null;
    long picturesProcessed = 0;
    
    // if (updateToMap) {
    outputIfVerbose ("Starting merge of picture frame file info%n");
    tempMap = getDataAboutFromMapWithinToMap (toMap, fromMapIsSrc);
    for (PictureFrameFileInfo fromMapRecord : fromMap.values ()) {
      ++fromMapRead;
      picturesProcessed = incrPicturesProcessed (picturesProcessed);
      if ((toMapRecord = toMap.get (fromMapRecord.getFileNameKey ())) != null) {
        // the toMap already has a record for the fromMapRecord, update the toMapRecord
        ++toMapUpdates;
        // do a quick merge of the fromMapRecord to the toMapRecord, check validity later
        toMapRecord.mergeFileInfo (fromMapRecord);
        
        // delete the matching record in tempMap so we know it is still there
        tempMap.remove (fromMapRecord.getFileNameKey ());
      }
      else {
        // the toMap doesn't have a record for the fromMapRecord, add the fromMapRecord
        ++toMapAdded;
        if (!fromMapIsSrc) {
          // the fromMapRecord is from the frameDir, this file is unique to the frameDir
          fromMapRecord.setUniqueOnFrame (true);
        }
        toMap.put (fromMapRecord.getFileNameKey (), fromMapRecord);
        
        // for validity checking
        toMapRecord = fromMapRecord;
      }
      // we have now merged the the toMapRecord with the fromMapRecord, validate fields
      GregorianCalendar dateMovedToFrame = toMapRecord.getDateMovedToFrame ();
      GregorianCalendar dateLastOnFrame = toMapRecord.getDateLastOnFrame ();
      if (toMapRecord.isOnFrame ()) {
        if (!toMapRecord.isUniqueOnFrame ()) {
          // this record has both source and frame info, make sure dateLastOnFrame is valid
          if (dateLastOnFrame == null || (dateLastOnFrame.compareTo (Year2000) < 0)) {
            // dateLastOnFrame is invalid / or set to initial value, update
            if (dateMovedToFrame == null || (dateMovedToFrame.compareTo (Year2000) < 0)) {
              // dateMovedToFrame is also invalid / or set to initial value, set to current time
              dateLastOnFrame = new GregorianCalendar ();
              dateLastOnFrame.setTimeInMillis (System.currentTimeMillis ());
            }
            else {
              // dateMovedToFrame is valid, use it as dateLastOnFrame
              dateLastOnFrame = dateMovedToFrame;
            }
          }
          toMapRecord.setDateLastOnFrame (dateLastOnFrame);
        }
        if (dateMovedToFrame == null || (dateMovedToFrame.compareTo (Year2000) < 0)) {
          // dateMovedToFrame is invalid / or set to initial value, update
          if (dateLastOnFrame == null || (dateLastOnFrame.compareTo (Year2000) < 0)) {
            // dateLastOnFrame is also invalid / or set to initial value, set to current time
            dateMovedToFrame = new GregorianCalendar ();
            dateMovedToFrame.setTimeInMillis (System.currentTimeMillis ());
          }
          else {
            // dateLastOnFrame is valid, use it as dateMovedToFrame
            dateMovedToFrame = dateLastOnFrame;
          }
        }
        toMapRecord.setDateMovedToFrame (dateMovedToFrame);
      }
    }
    
    // any file records that are left in tempMap are to be deleted
    for (PictureFrameFileInfo toBeDeleted : tempMap.values ()) {
      ++toMapDeleted;
      toMap.remove (toBeDeleted.getFileNameKey ());
    }
    
    outputIfVerbose ("Completed merge of picture frame file info, %d input records read, %d records updated, " 
    + "%d records added and %d records deleted.%n", fromMapRead, toMapUpdates, toMapAdded, toMapDeleted);
  }

  private TreeMap<String, PictureFrameFileInfo> getDataAboutFromMapWithinToMap (
    TreeMap<String, PictureFrameFileInfo> toMap, boolean fromMapIsSrc)
  {
    TreeMap<String, PictureFrameFileInfo> tempMap = new TreeMap<String, PictureFrameFileInfo> ();
    
    for (PictureFrameFileInfo toMapRecord : toMap.values ()) {
      if (fromMapIsSrc) {
        // add record to tempMap if the record has information about sourceDir files
        if (toMapRecord.getFullyQualifiedSourceFilename () != null) {
          tempMap.put (toMapRecord.getFileNameKey (), toMapRecord);
        }
      }
      else {
        // add record to tempMap if the record has information about frameDir files
        if (toMapRecord.getFilenameOnFrame () != null) {
          tempMap.put (toMapRecord.getFileNameKey (), toMapRecord);
        }
      }
    }
    return tempMap;
  }

  private long incrPicturesProcessed (long picturesProcessed)
  {
    if ((++picturesProcessed % 1000) == 0) {
      outputIfVerbose ("%d%n", picturesProcessed);
    }
    return picturesProcessed;
  }

  private void extractFrameFileInfo (TreeMap<String, PictureFrameFileInfo> filesInSourceDir,
    TreeMap<String, PictureFrameFileInfo> frameFileInfoMap, TreeMap<String, PictureFrameFileInfo> uniqueFrameFileInfoMap)
  {
    outputIfVerbose ("Extract information on images on the Picture Frame%n");
    Collection<PictureFrameFileInfo> pictureFrameFileInfoCollection = filesInSourceDir.values ();
    for (PictureFrameFileInfo pictureFrameFileInfo : pictureFrameFileInfoCollection) {
      if (pictureFrameFileInfo.isOnFrame ()) {
        // this pictureFrameFileInfo is for a file on the picture frame, add it to frameFileInfoMap
        frameFileInfoMap.put (pictureFrameFileInfo.getFileNameKey (), pictureFrameFileInfo);
        if (pictureFrameFileInfo.isUniqueOnFrame ()) {
          uniqueFrameFileInfoMap.put (pictureFrameFileInfo.getFileNameKey (), pictureFrameFileInfo);
        }
      }
    }

    // outPS.printf ("There are %d source files, %d frame files and %d unique-on-frame files%n", sourceFiles,
    // frameFiles, uniqueFrameFiles);
  }

  private boolean replaceFrameFilesWithSourceFiles (TreeMap<String, PictureFrameFileInfo> filesInFrameDir,
    TreeMap<String, PictureFrameFileInfo> filesInSourceDir, float percentageToReplace, long numberBytestoLeaveFree,
    int numberOfSourceFiles, int numberOfFrameFiles, int numUniqueFrameDirFiles, File sourceDir, File frameDir)
  {
    int numberOfFilesThatCanBeSwapped = numberOfSourceFiles - numberOfFrameFiles;
    int maxNumberOfFilesToSwap = (int) (numberOfFrameFiles * percentageToReplace);
    int numberOfFilesToSwap = Math.min (numberOfFilesThatCanBeSwapped, maxNumberOfFilesToSwap);
    int numberOfFilesThatCanBeDeleted = numberOfFrameFiles - numUniqueFrameDirFiles;
    ArrayList<PictureFrameFileInfo> filesThatCanBeSwapped = new ArrayList<PictureFrameFileInfo> (numberOfFilesThatCanBeSwapped);
    ArrayList<PictureFrameFileInfo> filesThatCanBeDeleted = new ArrayList<PictureFrameFileInfo> (numberOfFilesThatCanBeDeleted);
    int numberOfFilesDeleted = 0;
    int numberOfFilesCopied = 0;
    
    for (PictureFrameFileInfo swapCandidate : filesInSourceDir.values ()) {
      // look for files that are only in the sourceDir and files on frame but not unique
      if (!swapCandidate.isOnFrame ()) {
        // this file is only in the sourceDir, add it to the list of files that can be swapped
        filesThatCanBeSwapped.add (swapCandidate);
      }
      if (swapCandidate.isOnFrame () && !swapCandidate.isUniqueOnFrame ()) {
        // this file is on the frame and is not unique (only on frame)
        filesThatCanBeDeleted.add (swapCandidate);
      }
    }
    // now sort the list of files that can be swapped by time last on frame, oldest to newest
    Collections.sort (filesThatCanBeSwapped, new PictureFrameFileInfoCompareDateLastOnFrame ());
    
    // now sort the list of files that can be deleted by time moved to frame, oldest to newest
    Collections.sort (filesThatCanBeDeleted, new PictureFrameFileInfoCompareDateMovedToFrame ());
    
    long currentTime = System.currentTimeMillis ();
    PictureFrameFileInfo pictureInfo;
    GregorianCalendar fileActionDate;
    String fileName = null;
    boolean success;
    for (int i=0; i<numberOfFilesToSwap; ++i) {
      // first delete files on frame to get room to move new files to frame
      pictureInfo = filesThatCanBeDeleted.get (i);
      fileActionDate = new GregorianCalendar ();
      fileActionDate.setTimeInMillis (currentTime + i);
      pictureInfo.setDateLastOnFrame (fileActionDate);
      success = deleteFile (fileName = pictureInfo.getFilenameOnFrame ());
      if (success) {
        ++numberOfFilesDeleted;
        pictureInfo.setFilenameOnFrame (null);
        outputIfVerbose ("Successfully deleted Frame file \"%s\" to make room for new files%n", fileName);
      }
    }
    
    for (int i=0; i<numberOfFilesThatCanBeSwapped; ++i) {
      // now copy files to frame
      pictureInfo = filesThatCanBeSwapped.get (i);
      // 1st determine if file will 'fit'
      long frameUsableSpace = frameDir.getUsableSpace () - numberBytestoLeaveFree;
      long sizeOfFileToCopy = new File (pictureInfo.getFullyQualifiedSourceFilename ()).length ();
      if (frameUsableSpace < sizeOfFileToCopy) {
        // this file would use more space than allowed, done
        break;
      }
      
      fileActionDate = new GregorianCalendar ();
      fileActionDate.setTimeInMillis (System.currentTimeMillis ());
      pictureInfo.setDateMovedToFrame (fileActionDate);
      success = copyFileToFrame (pictureInfo, frameDir);
      if (success) {
        ++numberOfFilesCopied;
        fileName = pictureInfo.getFullyQualifiedSourceFilename ();
        outputIfVerbose ("Successfully copied Source file \"%s\" to Frame%n", fileName);
      }
    }
    int numberOfFrameFilesNow = numberOfFrameFiles - numberOfFilesDeleted + numberOfFilesCopied;
    outPS.printf ("There were originally %,d files on the Frame%n%,d of those files were deleted%n"
      + "%,d files were copied to the frame%n%,d files are now on the Frame%n", numberOfFrameFiles,
      numberOfFilesDeleted, numberOfFilesCopied, numberOfFrameFilesNow);
    
    return ((numberOfFilesDeleted > 0 || numberOfFilesCopied > 0) ? true : false);
  }

  private boolean copyFileToFrame (PictureFrameFileInfo pictureInfoFileToCopy, File frameDir)
  {
    boolean success = true;
    File srcFileToCopy = new File (pictureInfoFileToCopy.getFullyQualifiedSourceFilename ());
    String srcFileName = srcFileToCopy.getName ();
    File targetFileToCopyTo = new File (frameDir, srcFileName);
    Path source = srcFileToCopy.toPath ();
    Path target = targetFileToCopyTo.toPath ();
    String nameOnFrame = target.toString ();
    pictureInfoFileToCopy.setFilenameOnFrame (nameOnFrame);
    
    
    try {
      Files.copy (source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }
    catch (IOException e) {
      success = false;
      outPS.printf ("Unable to copy file \"%s\" to \"%s\": %s%n", source.toString (), target.toString (),
        e.toString ());
    }
    
    return success;
  }

  private boolean deleteFile (String deleteFileStr)
  {
    boolean success = true;
    File fileToDelete = new File (deleteFileStr);
    try {
      Files.delete (fileToDelete.toPath ());
    }
    catch (IOException e) {
      success = false;
      outPS.printf ("Unable to delete file \"%s\": %s%n", deleteFileStr, e.toString ());
    }
    return success;
  }

  private TreeMap<String, PictureFrameFileInfo> scanDirectory (File directoryToScanForFiles, boolean sourceScan)
  {
    TreeMap<String, PictureFrameFileInfo> pictureFrameFileInfoMap = new TreeMap<String, PictureFrameFileInfo> ();
    TreeSet<File> directoriesToProcess = new TreeSet<File> ();
    directoriesToProcess.add (directoryToScanForFiles);
    
    processDirStructure (pictureFrameFileInfoMap, directoriesToProcess, sourceScan);
    
    outputIfVerbose ("Finished Scan%n");
    return pictureFrameFileInfoMap;
  }

  private void processDirStructure (TreeMap<String, PictureFrameFileInfo> pictureFrameFileInfoMap,
    TreeSet<File> directoriesToProcess, boolean sourceScan)
  {
    String canonicalPath = null;
    String extension = null;
    String name = null;
    int dotOffset = -1;
    long picturesProcessed = 0;
    long lastOnFrameInitialTime = 1L;
    long dateMovedToFrame = System.currentTimeMillis ();
    
    outputIfVerbose ("Starting scan of the %s directory%n", (sourceScan ? "Source" : "Frame"));
    // recursively process directories looking for jpg files
    while (!directoriesToProcess.isEmpty ()) {
      // get (and remove) 1st in Sorted Set (TreeSet)
      File dirToProcess = directoriesToProcess.pollFirst ();
      File[] filesInDir = dirToProcess.listFiles ();
      for (File fileOrDir : filesInDir) {
        picturesProcessed = incrPicturesProcessed (picturesProcessed);
        if (fileOrDir.isFile ()) {
          // this is a file, is it a jpg file?
          try {
            canonicalPath = fileOrDir.getCanonicalPath ();
          }
          catch (IOException e) {
            outPS.printf ("\"%s\" getCanonicalPath: %s%n", fileOrDir, e);
            continue;
          }
          name = fileOrDir.getName ();
          if ((dotOffset = name.lastIndexOf ('.')) != -1) {
            // we have an extension, check if jpg or jpeg
            boolean isCorrectExt = false;
            extension = name.substring (dotOffset);
            for (int i=0; i<extensionsToProcess.length; ++i) {
              if (extension.equalsIgnoreCase (extensionsToProcess[i])) {
                // this is an extension to process
                isCorrectExt = true;
                break;
              }
            }
            if (isCorrectExt) {
              // add the file to the map
              PictureFrameFileInfo pictureFrameFileInfo = new PictureFrameFileInfo (canonicalPath, sourceScan, outPS);
              GregorianCalendar initialDate = new GregorianCalendar ();
              if (sourceScan) {
                // scanning source, set dateLastOnFrame to an old date (January 1, 1970 shortly after midnight)
                initialDate.setTimeInMillis (lastOnFrameInitialTime + picturesProcessed);
                pictureFrameFileInfo.setDateLastOnFrame (initialDate);
              }
              else {
                // scanning frame, set dateMovedToFrame to current time + picturesProcessed (as millisecs)
                initialDate.setTimeInMillis (dateMovedToFrame + picturesProcessed);
                pictureFrameFileInfo.setDateMovedToFrame (initialDate);
              }
              pictureFrameFileInfoMap.put (pictureFrameFileInfo.getFileNameKey (), pictureFrameFileInfo);
            }
          }
        }
        else {
          // this is a directory, add it to directoriesToProcess
          directoriesToProcess.add (fileOrDir);
        }
      }
    }
  }

  private static void printUsage (PrintStream outPS, String message)
  {
    outPS.println ("Positional Command Line Parameters:");
    outPS.printf ("Usage Error (%s): %s%n", SOFTWARE_TYPE, message);
    outPS.println ("Usage:MoveImagesToFrame <Picture Frame Dir> <Source Dir> <% to Change> "
      + "<Database Dir> <Megabytes to leave free> [Log File]");
    outPS.println ("  Where:");
    outPS.println ("    \"Picture Frame Dir\" = The fully qualified path to the directory on the Picture Frame "
      + "to copy images");
    outPS.println ("    \"Source Dir\" = The fully qualified path to the directory containing the images to "
      + "rotate to the Picture Frame");
    outPS.println ("    \"% to Change\" = The percentage of the photos on the Picture Frame to replace, "
        + "a number between 1 and 100");
    outPS.println ("    \"Database Dir\" = The fully qualified path to the directory where the program "
        + "can store information about the files in <Picture Frame Dir> and <Source Dir>");
    outPS.println ("    \"Megabytes to leave free\" = The number of MegaBytes (1,048,576 bytes) to leave free "
        + "on the frame for other picture sources");
    outPS.println ("    \"Log File\" = (Optional) The Log File, (default: console (DOS box)) all output will " 
        + "be appended to this file.");
    outPS.println ("    \"List Files Only\" = (Optional, default: false) Only list the files on frame and "
      + "Source Dir");
    outPS.printf ("%nKeyword Command Line Parameters%n");
    printHelpAndExit (outPS);
  }
  
  public static String getVersion ()
  {
    return SOFTWARE_TYPE;
  }
}

class PictureFrameFileInfoCompareDateLastOnFrame implements Comparator<PictureFrameFileInfo>
{
  @Override
  public int compare (PictureFrameFileInfo obj1, PictureFrameFileInfo obj2)
  {
    return obj1.getDateLastOnFrame ().compareTo (obj2.getDateLastOnFrame ());
  }
}

class PictureFrameFileInfoCompareDateMovedToFrame implements Comparator<PictureFrameFileInfo>
{
  @Override
  public int compare (PictureFrameFileInfo obj1, PictureFrameFileInfo obj2)
  {
    return obj1.getDateMovedToFrame ().compareTo (obj2.getDateMovedToFrame ());
  }
}
