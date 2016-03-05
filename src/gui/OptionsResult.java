package gui;

public class OptionsResult {
  int percentToChangeOnFrame;
  int mbToLeaveFree;
  boolean autoWriteLogFile;
  boolean appendToLogFile;
  boolean listFilesOnly;
  boolean quietMode;
  boolean debugMode;
  
  public OptionsResult ()
  {
    percentToChangeOnFrame = 1;
    mbToLeaveFree = 0;
    autoWriteLogFile = false;
    appendToLogFile = false;
    listFilesOnly = false;
    quietMode = false;
    debugMode = false;
  }

  @Override
  public String toString () {
    return "OptionsResult [percentToChangeOnFrame=" + percentToChangeOnFrame + ", mbToLeaveFree=" + mbToLeaveFree
        + ", autoWriteLogFile=" + autoWriteLogFile + ", appendToLogFile=" + appendToLogFile + ", listFilesOnly="
        + listFilesOnly + ", quietMode=" + quietMode + ", debugMode=" + debugMode + "]";
  }
}
