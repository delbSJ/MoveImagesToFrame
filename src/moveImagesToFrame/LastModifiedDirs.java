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
import java.text.SimpleDateFormat;
import java.util.Date;

public class LastModifiedDirs implements Externalizable
{
  private static final long serialVersionUID = -7284842560366513042L;
  
  private long frameDirLastModifiedTime;
  private long sourceDirLastModifiedTime;

  /**
   * 
   */
  public LastModifiedDirs ()
  {
    this.frameDirLastModifiedTime = 0L;
    this.sourceDirLastModifiedTime = 0L;
  }

  /**
   * @param frameDirLastModifiedTime
   * @param sourceDirLastModifiedTime
   */
  public LastModifiedDirs (long frameDirLastModifiedTime, long sourceDirLastModifiedTime)
  {
    this.frameDirLastModifiedTime = frameDirLastModifiedTime;
    this.sourceDirLastModifiedTime = sourceDirLastModifiedTime;
  }

  public static LastModifiedDirs getLastModifiedDirsFromDatabase (File dataBaseDir, PrintStream outPS)
  {
    LastModifiedDirs lastModifiedDirsFromDatabase = null;
    FileInputStream fi = null;
    ObjectInputStream ois = null;
    File databaseFile = new File (dataBaseDir, "lastModDB.fdb");
    
    // de-serialize the LastModifiedDirs dataBase if it exists
    if (databaseFile.exists ()) {
      // data base exist, read it
      try {
        fi = new FileInputStream (databaseFile);
        ois = new ObjectInputStream (fi);
      }
      catch (IOException e) {
        outPS.printf ("Fatal Error while opening Database: lastModDB.fdb: %s%n", e);
        e.printStackTrace ();
        System.exit (1);
      }

      try {
        lastModifiedDirsFromDatabase = (LastModifiedDirs) ois.readObject ();
      }
      catch (EOFException | InvalidClassException e) {}
      catch (OptionalDataException ode) {}
      catch (ClassNotFoundException | IOException e) {
        // fatal exception
        outPS.printf ("Fatal Error while reading Database: lastModDB.fdb: %s%n", e);
        e.printStackTrace ();
        System.exit (1);
      }
      
      try {
        ois.close ();
      }
      catch (IOException e) {
        outPS.printf ("IOException while closing Database: lastModDB.fdb: %s%n", e);
        e.printStackTrace ();
      }
    }
    return lastModifiedDirsFromDatabase;
  }
  
  public static void saveToDataBase (LastModifiedDirs lastModifiedDirsToDatabase, File dataBaseDir, PrintStream outPS)
  {
    FileOutputStream fo = null;
    ObjectOutputStream oos = null;
    File databaseFileTmp = new File (dataBaseDir, "lastModDB.fdbtmp");
    File databaseFile = new File (dataBaseDir, "lastModDB.fdb");

    // serialize to the PictureFrameFileInfo dataBase
    outPS.println ("Saving Frame Dir & Source Dir last modified times to database");
    try {
      fo = new FileOutputStream (databaseFileTmp, false);
      oos = new ObjectOutputStream (fo);
    }
    catch (IOException e) {
      outPS.printf ("Fatal Error while creating temporary Database: lastModDB.fdbtmp: %s%n", e);
      e.printStackTrace ();
      System.exit (1);
    }

    try {
      oos.writeObject (lastModifiedDirsToDatabase);
    }
    catch (IOException e) {
      // fatal exception
      outPS.printf ("Fatal Error while writing Database: lastModDB.fdbtmp: %s%n", e);
      e.printStackTrace ();
      System.exit (1);
    }

    try {
      oos.close ();
    }
    catch (IOException e) {
      // fatal exception
      outPS.printf ("Fatal Error while closing Database: lastModDB.fdbtmp: %s%n", e);
      e.printStackTrace ();
      System.exit (1);
    }
    
    // we have successfully written the temporary DataBase file, delete the old one if it exists
    if (databaseFile.exists ()) {
      // old data base file exists, delete it
      if (!databaseFile.delete ()) {
        // unable to delete old data base file
        outPS.printf ("Fatal Error while deleteing Database: lastModDB.fdb.%n");
        System.exit (1);
      }
    }
    if (!databaseFileTmp.renameTo (databaseFile)) {
      // unable to rename data base temp file
      outPS.printf ("Fatal Error while renaming temporary Database: lastModDB.fdbtmp.%n");
      System.exit (1);
    }
  }

  /**
   * @return the frameDirLastModifiedTime
   */
  public long getFrameDirLastModifiedTime ()
  {
    return frameDirLastModifiedTime;
  }

  /**
   * @return the sourceDirLastModifiedTime
   */
  public long getSourceDirLastModifiedTime ()
  {
    return sourceDirLastModifiedTime;
  }

  /**
   * @param frameDirLastModifiedTime the frameDirLastModifiedTime to set
   */
  public void setFrameDirLastModifiedTime (long frameDirLastModifiedTime)
  {
    this.frameDirLastModifiedTime = frameDirLastModifiedTime;
  }

  /**
   * @param sourceDirLastModifiedTime the sourceDirLastModifiedTime to set
   */
  public void setSourceDirLastModifiedTime (long sourceDirLastModifiedTime)
  {
    this.sourceDirLastModifiedTime = sourceDirLastModifiedTime;
  }

  @Override
  public void readExternal (ObjectInput in) throws IOException, ClassNotFoundException
  {
    frameDirLastModifiedTime = in.readLong ();
    sourceDirLastModifiedTime = in.readLong ();
  }

  @Override
  public void writeExternal (ObjectOutput out) throws IOException
  {
    out.writeLong (frameDirLastModifiedTime);
    out.writeLong (sourceDirLastModifiedTime);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString ()
  {
    SimpleDateFormat dateFormat = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss.S z");
    StringBuilder builder = new StringBuilder ();
    builder.append ("LastModifiedDirs [frameDirLastModifiedTime=");
    builder.append (dateFormat.format (new Date (frameDirLastModifiedTime)));
    builder.append (", sourceDirLastModifiedTime=");
    builder.append (dateFormat.format (new Date (sourceDirLastModifiedTime)));
    builder.append ("]");
    return builder.toString ();
  }

}
