package moveImagesToFrame;

import java.io.EOFException;
import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.TreeMap;

public class PictureFrameFileInfo implements Externalizable, Cloneable
{
  private static final String DIRECTORY_SEPARATOR = (File.separator.equals ("\\")? "\\\\" : File.separator);
  private static final long serialVersionUID = -7284842560366517025L;
  
  private String fullyQualifiedSourceFilename;
  private String filenameOnFrame;
  private String fileNameKey;
  private GregorianCalendar dateMovedToFrame;
  private GregorianCalendar dateLastOnFrame;
  private boolean uniqueOnFrame;
  private boolean toBeDeleted;
  private boolean toBeMoved;
  private boolean onFrame;
  private PrintStream outPS;

  public PictureFrameFileInfo ()
  {
    fullyQualifiedSourceFilename = null;
    filenameOnFrame = null;
    fileNameKey = null;
    dateMovedToFrame = null;
    dateLastOnFrame = null;
    uniqueOnFrame = false;
    toBeDeleted = false;
    toBeMoved = false;
    onFrame = false;
    outPS = System.out;
  }
  
  /**
   * @param fullyQualifiedFilename
   * @param onFrame
   * @param outPS
   */
  public PictureFrameFileInfo (String fullyQualifiedFilename, boolean sourceFile, PrintStream outPS)
  {
    if (sourceFile) {
      fullyQualifiedSourceFilename = fullyQualifiedFilename;
    }
    else {
      filenameOnFrame = fullyQualifiedFilename;
    }
    fileNameKey = rationalizeKey (fullyQualifiedFilename);
    dateMovedToFrame = null;
    dateLastOnFrame = null;
    uniqueOnFrame = false;
    toBeDeleted = false;
    toBeMoved = false;
    onFrame = !sourceFile;
    this.outPS = outPS;
  }
  
  public PictureFrameFileInfo (String fullyQualifiedFilename, String filenameOnFrame,
    GregorianCalendar dateMovedToFrame, GregorianCalendar dateLastOnFrame, boolean toBeDeleted, boolean toBeMoved,
    boolean onFrame, boolean uniqueOnFrame, PrintStream outPS)
  {
    this.fullyQualifiedSourceFilename = fullyQualifiedFilename;
    this.filenameOnFrame = filenameOnFrame;
    fileNameKey = rationalizeKey ((fullyQualifiedFilename != null ? fullyQualifiedFilename : filenameOnFrame));
    this.dateMovedToFrame = dateMovedToFrame;
    this.dateLastOnFrame = dateLastOnFrame;
    this.uniqueOnFrame = uniqueOnFrame;
    this.toBeDeleted = toBeDeleted;
    this.toBeMoved = toBeMoved;
    this.onFrame = onFrame;
    this.outPS = outPS;
  }

  public static TreeMap<String, PictureFrameFileInfo> getFileInfoFromDatabase (File dataBaseDir, PrintStream outPS)
  {
    TreeMap<String, PictureFrameFileInfo> fileInfoFromDatabase = new TreeMap<String, PictureFrameFileInfo> ();
    PictureFrameFileInfo pictureFrameFileInfo = null;
    FileInputStream fi = null;
    ObjectInputStream ois = null;
    File databaseFile = new File (dataBaseDir, "fileDB.fdb");
    
    // de-serialize the PictureFrameFileInfo dataBase if it exists
    if (databaseFile.exists ()) {
      // data base exist, read it
      try {
        fi = new FileInputStream (databaseFile);
        ois = new ObjectInputStream (fi);
      }
      catch (IOException e) {
        outPS.printf ("Fatal Error while opening Database: %s%n", e);
        e.printStackTrace ();
        System.exit (1);
      }

      while (true) {
        try {
          pictureFrameFileInfo = (PictureFrameFileInfo) ois.readObject ();
        }
        catch (EOFException | InvalidClassException e) {
          break;
        }
        catch (OptionalDataException ode) {
          if (ode.eof) {
            // we have reached the end of file, break out of loop
            break;
          }
        }
        catch (ClassNotFoundException | IOException e) {
          // fatal exception
          outPS.printf ("Fatal Error while reading Database: %s%n", e);
          e.printStackTrace ();
          System.exit (1);
        }
        if (pictureFrameFileInfo != null) {
          fileInfoFromDatabase.put (pictureFrameFileInfo.getFileNameKey (), pictureFrameFileInfo);
        }
      }
      
      try {
        ois.close ();
      }
      catch (IOException e) {
        outPS.printf ("IOException while closing Database: %s%n", e);
        e.printStackTrace ();
      }
    }
    return fileInfoFromDatabase;
  }
  
  public static void saveToDataBase (TreeMap<String, PictureFrameFileInfo> totalFileInfo, File dataBaseDir, PrintStream outPS)
  {
    FileOutputStream fo = null;
    ObjectOutputStream oos = null;
    File databaseFileTmp = new File (dataBaseDir, "fileDB.fdbtmp");
    File databaseFile = new File (dataBaseDir, "fileDB.fdb");

    // serialize to the PictureFrameFileInfo dataBase
    outPS.println ("Saving Source & Frame File Info to database");
    try {
      fo = new FileOutputStream (databaseFileTmp, false);
      oos = new ObjectOutputStream (fo);
    }
    catch (IOException e) {
      outPS.printf ("Fatal Error while creating temporary Database: %s%n", e);
      e.printStackTrace ();
      System.exit (1);
    }

    for (PictureFrameFileInfo pictureFrameFileInfo : totalFileInfo.values ()) {
      try {
        oos.writeObject (pictureFrameFileInfo);
      }
      catch (IOException e) {
        // fatal exception
        outPS.printf ("Fatal Error while writing Database: %s%n", e);
        e.printStackTrace ();
        System.exit (1);
      }
    }

    try {
      oos.close ();
    }
    catch (IOException e) {
      // fatal exception
      outPS.printf ("Fatal Error while closing Database: %s%n", e);
      e.printStackTrace ();
      System.exit (1);
    }
    
    // we have successfully written the temporary DataBase file, delete the old one if it exists
    if (databaseFile.exists ()) {
      // old data base file exists, delete it
      try {
        Files.delete (databaseFile.toPath ());
      }
      catch (IOException e) {
        // unable to delete old data base file
        outPS.printf ("Fatal Error while deleting old Database: %s%n", e);
        e.printStackTrace();
        System.exit (1);
      }
    }
    try {
      // the temporary data base file is renamed to the master data base file
      Files.move (databaseFileTmp.toPath (), databaseFile.toPath (), StandardCopyOption.REPLACE_EXISTING);
    }
    catch (IOException e) {
      // unable to rename data base temp file to data base file
      outPS.printf ("Fatal Error while renaming temporary Database: %s%n", e);
      e.printStackTrace();
      System.exit (1);
    }
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#clone()
   */
  @Override
  public Object clone () throws CloneNotSupportedException
  {
    return super.clone ();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals (Object obj)
  {
    if (this == obj) { return true; }
    if (obj == null) { return false; }
    if (getClass () != obj.getClass ()) { return false; }
    PictureFrameFileInfo other = (PictureFrameFileInfo) obj;
    if (fileNameKey == null) {
      if (other.fileNameKey != null) { return false; }
    }
    else if (!fileNameKey.equals (other.fileNameKey)) { return false; }
    return true;
  }

  /**
   * @return the fileNameKey
   */
  public String getFileNameKey ()
  {
    return fileNameKey;
  }

  /**
   * @return the filenameOnFrame
   */
  public String getFilenameOnFrame ()
  {
    return filenameOnFrame;
  }

  /**
   * @return the fullyQualifiedSourceFilename
   */
  public String getFullyQualifiedSourceFilename ()
  {
    return fullyQualifiedSourceFilename;
  }

  /**
   * @return the dateMovedToFrame
   */
  public GregorianCalendar getDateMovedToFrame ()
  {
    return dateMovedToFrame;
  }

  /**
   * @return the dateLastOnFrame
   */
  public GregorianCalendar getDateLastOnFrame ()
  {
    return dateLastOnFrame;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode ()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((fileNameKey == null) ? 0 : fileNameKey.hashCode ());
    return result;
  }

  /**
   * @return the onFrame
   */
  public boolean isOnFrame ()
  {
    return onFrame;
  }

  /**
   * @return the toBeDeleted
   */
  public boolean isToBeDeleted ()
  {
    return toBeDeleted;
  }

  /**
   * @return the toBeMoved
   */
  public boolean isToBeMoved ()
  {
    return toBeMoved;
  }

  /**
   * @return the uniqueOnFrame
   */
  public boolean isUniqueOnFrame ()
  {
    return uniqueOnFrame;
  }

  public void mergeFileInfo (PictureFrameFileInfo fromFileInfo)
  {
    // take file info from the fromFile and add it to this file info
    if (this.fullyQualifiedSourceFilename == null) {
      // may also be null in the from file info
      this.fullyQualifiedSourceFilename = fromFileInfo.getFullyQualifiedSourceFilename ();
    }
    if (this.filenameOnFrame == null) {
      // may also be null in the from file info
      this.filenameOnFrame = fromFileInfo.getFilenameOnFrame ();
    }
    if (this.dateMovedToFrame == null) {
      // may also be null in the from file info
      this.dateMovedToFrame = fromFileInfo.getDateMovedToFrame ();
    }
    if (this.dateLastOnFrame == null) {
      // may also be null in the from file info
      this.dateLastOnFrame = fromFileInfo.getDateLastOnFrame ();
    }
    if (!this.onFrame) {
      // the current file info is NOT on frame, use the onFrame from the from file
      this.onFrame = fromFileInfo.isOnFrame ();
    }
    if (this.onFrame) {
      // the merged file info indicates that the file is on the frame
      if (this.fullyQualifiedSourceFilename == null) {
        // on the frame but not in source dir, it is unique on the frame
        this.uniqueOnFrame = true;
      }
      else {
        // on the frame and in the source dir, is NOT unique to the frame
        this.uniqueOnFrame = false;
        // set the dateLastOnFrame to the dateMovedToFrame
        this.dateLastOnFrame = fromFileInfo.getDateMovedToFrame ();
      }
    }
    // validate merged file info
    if ((this.fullyQualifiedSourceFilename == null) && (this.filenameOnFrame == null) && (this.fileNameKey != null)) {
      // source and frame file names are null, however, file name key is not, program logic error
      Exception ple = new Exception (">>>Program logic error, object has a key but no information: " + this.toString ());
      outPS.println (ple.toString ());
      ple.printStackTrace (System.out);
    }
    if ((this.filenameOnFrame == null) && (this.onFrame || this.uniqueOnFrame)) {
      Exception ple = new Exception (">>>Program logic error, filenameOnFrame does not match onFrame & uniqueOnFrame :"
        + this.toString ());
      outPS.println (ple.toString ());
      ple.printStackTrace (System.out);
    }
  }

  private String rationalizeKey (String qualifiedFilename)
  {
    // rationalized file name will always have a jpg extension
    int dotOffset = -1;
    String fileNameRationalized = null;
    if (qualifiedFilename != null) {
      // we have a non-null file name we need to set to lower case and change '#' to '_'
      String[] qualifiedFilenameArray = qualifiedFilename.toLowerCase ().split (DIRECTORY_SEPARATOR);
      String lcFileNameExt = qualifiedFilenameArray[qualifiedFilenameArray.length-1];
      StringBuilder workSB = new StringBuilder (lcFileNameExt);
      if ((dotOffset = workSB.lastIndexOf (".")) != -1) {
        // make sure the extension is ".jpg"
        workSB.replace (dotOffset, workSB.length (), ".jpg");
      }
      
      int hashOffset;
      int hashStartOffset = 0;

      // convert any '#' characters underscore
      while ((hashOffset = workSB.indexOf ("#", hashStartOffset)) != -1) {
        // we have found a '#' replace it with a '_'
        workSB.setCharAt (hashOffset, '_');
        // continue scanning for '#'
        hashStartOffset = hashOffset;
      }
      fileNameRationalized = workSB.toString ();
    }
    
    return fileNameRationalized;
  }

  @Override
  public void readExternal (ObjectInput in) throws IOException, ClassNotFoundException
  {
    fullyQualifiedSourceFilename = (String) in.readObject();
    filenameOnFrame = (String) in.readObject();
    fileNameKey = (String) in.readObject();
    dateMovedToFrame = (GregorianCalendar) in.readObject();
    dateLastOnFrame = (GregorianCalendar) in.readObject();
    onFrame = in.readBoolean ();
    uniqueOnFrame = in.readBoolean ();
  }

  /**
   * @param filenameOnFrame the filenameOnFrame to set
   */
  public void setFilenameOnFrame (String filenameOnFrame)
  {
    this.filenameOnFrame = filenameOnFrame;
    if (filenameOnFrame != null) {
      onFrame = true;
      uniqueOnFrame = fullyQualifiedSourceFilename == null ? true : false;
      if (fileNameKey == null) {
        fileNameKey = rationalizeKey (filenameOnFrame);
      }
    }
    else {
      // filenameOnFrame set null, clear frame flags
      onFrame = uniqueOnFrame = false;
      if (fullyQualifiedSourceFilename == null && fileNameKey != null) {
        // Program logic error, object has a key but no information, if in database this is fatal
        Exception ple = new Exception (">>>Program logic error, object has a key but no information: "
          + this.toString ());
        outPS.println (ple.toString ());
        ple.printStackTrace (System.out);
      }
    }
  }

  /**
   * @param fullyQualifiedSourceFilename the fullyQualifiedSourceFilename to set
   */
  public void setFullyQualifiedSourceFilename (String fullyQualifiedSourceFilename)
  {
    this.fullyQualifiedSourceFilename = fullyQualifiedSourceFilename;
    if (fullyQualifiedSourceFilename != null) {
      this.setOnFrame (filenameOnFrame == null ? false : true);
      this.setUniqueOnFrame (false);
      if (fileNameKey == null) {
        fileNameKey = rationalizeKey (fullyQualifiedSourceFilename);
      }
    }
    else {
      // setting fullyQualifiedSourceFilename null, validate that it is on frame
      if (filenameOnFrame != null) {
        // record is still valid
        this.setOnFrame (true);
        this.setUniqueOnFrame (true);
      }
      else {
        // both fullyQualifiedSourceFilename & filenameOnFrame are null, fileNameKey MUST be null
        if (fileNameKey != null) {
          // Program logic error, object has a key but no information, if in database this is fatal
          Exception ple = new Exception (">>>Program logic error, object has a key but no information: "
            + this.toString ());
          outPS.println (ple.toString ());
          ple.printStackTrace (System.out);
        }
        this.setOnFrame (false);
        this.setUniqueOnFrame (false);
      }
    }
  }

  /**
   * @param dateMovedToFrame the dateMovedToFrame to set
   */
  public void setDateMovedToFrame (GregorianCalendar dateMovedToFrame)
  {
    this.dateMovedToFrame = dateMovedToFrame;
  }

  /**
   * @param dateLastOnFrame the dateLastOnFrame to set
   */
  public void setDateLastOnFrame (GregorianCalendar dateLastOnFrame)
  {
    this.dateLastOnFrame = dateLastOnFrame;
  }

  /**
   * @param onFrame the onFrame to set
   */
  public void setOnFrame (boolean onFrame)
  {
    this.onFrame = onFrame;
    if ((onFrame && filenameOnFrame == null) || (!onFrame && filenameOnFrame != null) || (!onFrame && uniqueOnFrame)) {
      Exception ple = new Exception (">>>Program logic error, filenameOnFrame does not match onFrame & uniqueOnFrame :"
          + this.toString ());
        outPS.println (ple.toString ());
        ple.printStackTrace (System.out);
    }
  }

  /**
   * @param toBeDeleted the toBeDeleted to set
   */
  public void setToBeDeleted (boolean toBeDeleted)
  {
    this.toBeDeleted = toBeDeleted;
  }

  /**
   * @param toBeMoved the toBeMoved to set
   */
  public void setToBeMoved (boolean toBeMoved)
  {
    this.toBeMoved = toBeMoved;
  }

  /**
   * @param uniqueOnFrame the uniqueOnFrame to set
   */
  public void setUniqueOnFrame (boolean uniqueOnFrame)
  {
    this.uniqueOnFrame = uniqueOnFrame;
    // validate the flag
    if (uniqueOnFrame) {
      // make sure it is unique
      if ((fullyQualifiedSourceFilename != null) || (filenameOnFrame == null)) {
        // program logic error, this is not unique
        Exception ple = new Exception (">>>Program logic error, object set Unique but isn't: " + this.toString ());
        outPS.println (ple.toString ());
        ple.printStackTrace (System.out);
      }
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString ()
  {
    StringBuilder builder = new StringBuilder ();
    builder.append ("PictureFrameFileInfo [fullyQualifiedSourceFilename=");
    builder.append (fullyQualifiedSourceFilename);
    builder.append (", filenameOnFrame=");
    builder.append (filenameOnFrame);
    builder.append (", fileNameKey=");
    builder.append (fileNameKey);
    builder.append (", dateMovedToFrame=");
    formatCalendar (dateMovedToFrame, builder);
    builder.append (", dateLastOnFrame=");
    formatCalendar (dateLastOnFrame, builder);
    builder.append (", uniqueOnFrame=");
    builder.append (uniqueOnFrame);
    builder.append (", toBeDeleted=");
    builder.append (toBeDeleted);
    builder.append (", toBeMoved=");
    builder.append (toBeMoved);
    builder.append (", onFrame=");
    builder.append (onFrame);
    builder.append ("]");
    return builder.toString ();
  }

  private void formatCalendar (GregorianCalendar date, StringBuilder builder)
  {
    if (date == null) {
      builder.append ("null");
    }
    else {
      SimpleDateFormat dateFormat = new SimpleDateFormat ("yyyy.MM.dd HH:mm:ss z");
      builder.append (dateFormat.format (date.getTime ()));
    }
  }

  @Override
  public void writeExternal (ObjectOutput out) throws IOException
  {
    out.writeObject (fullyQualifiedSourceFilename);
    out.writeObject (filenameOnFrame);
    out.writeObject (fileNameKey);
    out.writeObject (dateMovedToFrame);
    out.writeObject (dateLastOnFrame);
    out.writeBoolean (onFrame);
    out.writeBoolean (uniqueOnFrame);
  }
}
