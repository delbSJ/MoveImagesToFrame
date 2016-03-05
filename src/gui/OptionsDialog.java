package gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

public class OptionsDialog extends Dialog {

  private OptionsResult optionsResult;
  protected Shell shlMoveimagestoframeOptions;
  Text textMBtoLeaveFree;
  Label lblMBtoLeaveFree;
  Spinner percentToChgSpinner;
  Label labelPercentToChg;
  Button btnAutoWriteLogFile;
  Label lblAutoWriteLogFile;
  Button btnAppendLogFile;
  Label lblAppendLogFile;
  Button btnListFilesOnly;
  Label lblListFilesOnly;
  Button btnQuietMode;
  Label lblQuietMode;
  Button btnDebugMode;
  Label lblDebugMode;
  
  /**
   * Create the dialog.
   * @param parent
   * @param style
   * @param optionsResult
   */
  public OptionsDialog (Shell parent, int style, OptionsResult optionsResult) {
    super (parent, style);
    setText ("MoveImagesToFrame Options");
    this.optionsResult = optionsResult;
  }

  /**
   * Open the dialog.
   * @return the result
   */
  public Object open () {
    if (optionsResult == null) {
      optionsResult = new OptionsResult ();
    }
    createContents ();
    shlMoveimagestoframeOptions.open ();
    shlMoveimagestoframeOptions.layout ();
    Display display = getParent ().getDisplay ();
    while (!shlMoveimagestoframeOptions.isDisposed ()) {
      if (!display.readAndDispatch ()) {
        display.sleep ();
      }
    }
    return optionsResult;
  }

  /**
   * Create contents of the dialog.
   */
  private void createContents () {
    shlMoveimagestoframeOptions = new Shell (getParent (), getStyle ());
    shlMoveimagestoframeOptions.setMinimumSize(new Point(286, 285));
    shlMoveimagestoframeOptions.setSize (286, 285);
    shlMoveimagestoframeOptions.setLayout(new GridLayout(2, false));
    
    final Spinner percentToChgSpinner = new Spinner(shlMoveimagestoframeOptions, SWT.BORDER);
    percentToChgSpinner.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
    this.percentToChgSpinner = percentToChgSpinner;
    percentToChgSpinner.setMinimum(1);
    percentToChgSpinner.setMaximum (100);
    percentToChgSpinner.setSelection (optionsResult.percentToChangeOnFrame);
    Label labelPercentToChg = new Label(shlMoveimagestoframeOptions, SWT.NONE);
    this.labelPercentToChg = labelPercentToChg;
    labelPercentToChg.setText("% to change on Frame      ");
    labelPercentToChg.setAlignment(SWT.RIGHT);
    
    Text textMBtoLeaveFree = new Text(shlMoveimagestoframeOptions, SWT.BORDER | SWT.RIGHT);
    this.textMBtoLeaveFree = textMBtoLeaveFree;
    textMBtoLeaveFree.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    textMBtoLeaveFree.setText (String.valueOf (optionsResult.mbToLeaveFree));;
    Label lblMBtoLeaveFree = new Label(shlMoveimagestoframeOptions, SWT.NONE);
    this.lblMBtoLeaveFree = lblMBtoLeaveFree;
    lblMBtoLeaveFree.setText("Megabytes to leave free");
    
    final Button btnAutoWriteLogFile = new Button(shlMoveimagestoframeOptions, SWT.CHECK);
    this.btnAutoWriteLogFile = btnAutoWriteLogFile;
    btnAutoWriteLogFile.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
    btnAutoWriteLogFile.setAlignment(SWT.RIGHT);
    btnAutoWriteLogFile.setSelection (optionsResult.autoWriteLogFile);
    btnAutoWriteLogFile.addSelectionListener (new SelectionAdapter () {
      @Override
      public void widgetSelected (SelectionEvent event) {
        optionsResult.autoWriteLogFile = btnAutoWriteLogFile.getSelection ();

      }
    });
    final Label lblAutoWriteLogFile = new Label(shlMoveimagestoframeOptions, SWT.NONE);
    this.lblAutoWriteLogFile = lblAutoWriteLogFile;
    lblAutoWriteLogFile.setText("Auto-write to Log File after Run");
    lblAutoWriteLogFile.addMouseListener (new MouseListener() {
      @Override
      public void mouseUp (MouseEvent arg0) {}
      
      @Override
      public void mouseDown (MouseEvent arg0) {
        //clicked on label, toggle the labels control
        optionsResult.autoWriteLogFile = !btnAutoWriteLogFile.getSelection ();
        lblAutoWriteLogFile.setFocus ();
        btnAutoWriteLogFile.setSelection (optionsResult.autoWriteLogFile);
      }
      
      @Override
      public void mouseDoubleClick (MouseEvent arg0) {}
    });
    
    final Button btnAppendLogFile = new Button(shlMoveimagestoframeOptions, SWT.CHECK);
    this.btnAppendLogFile = btnAppendLogFile;
    btnAppendLogFile.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
    btnAppendLogFile.setAlignment(SWT.RIGHT);
    btnAppendLogFile.setSelection (optionsResult.appendToLogFile);
    btnAppendLogFile.addSelectionListener (new SelectionAdapter () {
      @Override
      public void widgetSelected (SelectionEvent event) {
        optionsResult.appendToLogFile = btnAppendLogFile.getSelection ();

      }
    });
    Label lblAppendLogFile = new Label(shlMoveimagestoframeOptions, SWT.NONE);
    this.lblAppendLogFile = lblAppendLogFile;
    lblAppendLogFile.setText("Append to Log File");
    lblAppendLogFile.addMouseListener (new MouseListener() {
      @Override
      public void mouseUp (MouseEvent arg0) {}
      
      @Override
      public void mouseDown (MouseEvent arg0) {
        //clicked on label, toggle the labels control
        optionsResult.appendToLogFile = !btnAppendLogFile.getSelection ();
        btnAppendLogFile.setFocus ();
        btnAppendLogFile.setSelection (optionsResult.appendToLogFile);
      }
      
      @Override
      public void mouseDoubleClick (MouseEvent arg0) {}
    });
    
    final Button btnListFilesOnly = new Button(shlMoveimagestoframeOptions, SWT.CHECK);
    this.btnListFilesOnly = btnListFilesOnly;
    btnListFilesOnly.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
    btnListFilesOnly.setSelection (optionsResult.listFilesOnly);
    btnListFilesOnly.addSelectionListener (new SelectionAdapter () {
      @Override
      public void widgetSelected (SelectionEvent event) {
        optionsResult.listFilesOnly = btnListFilesOnly.getSelection ();
        validateListFilesOnly (btnListFilesOnly, lblListFilesOnly, btnQuietMode, lblQuietMode);
      }
    });
    final Label lblListFilesOnly = new Label(shlMoveimagestoframeOptions, SWT.NONE);
    this.lblListFilesOnly = lblListFilesOnly;
    lblListFilesOnly.setText("List Files Only");
    lblListFilesOnly.addMouseListener (new MouseListener() {
      @Override
      public void mouseUp (MouseEvent arg0) {}
      
      @Override
      public void mouseDown (MouseEvent arg0) {
        //clicked on label, toggle the labels control
        optionsResult.listFilesOnly = !btnListFilesOnly.getSelection ();
        btnListFilesOnly.setFocus ();
        btnListFilesOnly.setSelection (optionsResult.listFilesOnly);
        validateListFilesOnly (btnListFilesOnly, lblListFilesOnly, btnQuietMode, lblQuietMode);
      }
      
      @Override
      public void mouseDoubleClick (MouseEvent arg0) {}
    });
    
    final Button btnQuietMode = new Button(shlMoveimagestoframeOptions, SWT.CHECK);
    this.btnQuietMode = btnQuietMode;
    btnQuietMode.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
    btnQuietMode.setSelection (optionsResult.quietMode);
    btnQuietMode.addSelectionListener (new SelectionAdapter () {
      @Override
      public void widgetSelected (SelectionEvent event) {
        optionsResult.quietMode = btnQuietMode.getSelection ();
        validateQuietMode (btnListFilesOnly, lblListFilesOnly, btnQuietMode, lblQuietMode, btnDebugMode, lblDebugMode);
      }
    });
    final Label lblQuietMode = new Label(shlMoveimagestoframeOptions, SWT.NONE);
    this.lblQuietMode = lblQuietMode;
    lblQuietMode.setText("Quiet Mode");
    lblQuietMode.addMouseListener (new MouseListener() {
      @Override
      public void mouseUp (MouseEvent arg0) {}
      
      @Override
      public void mouseDown (MouseEvent arg0) {
        //clicked on label, toggle the labels control
        optionsResult.quietMode = !btnQuietMode.getSelection ();
        btnQuietMode.setFocus ();
        btnQuietMode.setSelection (optionsResult.quietMode);
        validateQuietMode (btnListFilesOnly, lblListFilesOnly, btnQuietMode, lblQuietMode, btnDebugMode, lblDebugMode);
      }
      
      @Override
      public void mouseDoubleClick (MouseEvent arg0) {}
    });
    
    final Button btnDebugMode = new Button(shlMoveimagestoframeOptions, SWT.CHECK);
    this.btnDebugMode = btnDebugMode;
    btnDebugMode.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
    btnDebugMode.setSelection (optionsResult.debugMode);
    btnDebugMode.addSelectionListener (new SelectionAdapter () {
      @Override
      public void widgetSelected (SelectionEvent event) {
        optionsResult.debugMode = btnDebugMode.getSelection ();
        validateDebugMode (btnQuietMode, lblQuietMode, btnDebugMode, lblDebugMode);
      }
    });
    final Label lblDebugMode = new Label(shlMoveimagestoframeOptions, SWT.NONE);
    this.lblDebugMode = lblDebugMode;
    lblDebugMode.setText("Debug Mode");
    lblDebugMode.addMouseListener (new MouseListener() {
      @Override
      public void mouseUp (MouseEvent arg0) {}
      
      @Override
      public void mouseDown (MouseEvent arg0) {
        //clicked on label, toggle the labels control
        optionsResult.debugMode = !btnDebugMode.getSelection ();
        btnDebugMode.setFocus ();
        btnDebugMode.setSelection (optionsResult.debugMode);
        validateDebugMode (btnQuietMode, lblQuietMode, btnDebugMode, lblDebugMode);
      }
      
      @Override
      public void mouseDoubleClick (MouseEvent arg0) {}
    });
    new Label(shlMoveimagestoframeOptions, SWT.NONE);
    new Label(shlMoveimagestoframeOptions, SWT.NONE);
    
    Button btnOK = new Button(shlMoveimagestoframeOptions, SWT.NONE);
    btnOK.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 2, 1));
    btnOK.setText("   OK   ");
    
    // now register the listeners
    btnOK.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        shlMoveimagestoframeOptions.close ();
      }
    });

    percentToChgSpinner.addModifyListener(new ModifyListener() {
      
      @Override
      public void modifyText (ModifyEvent arg0) {
        optionsResult.percentToChangeOnFrame = Integer.parseInt(percentToChgSpinner.getText());
      }});

    textMBtoLeaveFree.addKeyListener (new KeyListener() {
      
      @Override
      public void keyReleased (KeyEvent arg0) {}
      
      @Override
      public void keyPressed (KeyEvent keyDown) {
        handleKeyDown (keyDown);
      }
    });
    textMBtoLeaveFree.addFocusListener (new FocusListener() {
      
      @Override
      public void focusLost (FocusEvent arg0) {
        validateMBtoLeaveFree ();
      }
      
      @Override
      public void focusGained (FocusEvent arg0) {}
    });
  }

  private void validateListFilesOnly (Button btnListFilesOnly, Label lblListFilesOnly, Button btnQuietMode,
      Label lblQuietMode) {
    if (optionsResult.listFilesOnly) {
      // listFilesOnly selected, make sure quietMode is off
      if (optionsResult.quietMode) {
        // can't have quiet mode on when listFilesOnly selected
        int result = conflictingOptions (shlMoveimagestoframeOptions,
            "List Files Only mode requires \"Quiet Mode\" to be off. Do you want to Proceed?",
            lblListFilesOnly, lblQuietMode);
        if (result == SWT.YES) {
          // yes, user wants listFilesOnly mode, turn the conflicting option off
          btnQuietMode.setSelection (false);
          optionsResult.quietMode = false;
        }
        else {
          // no, the user does not want to turn off the conflicting options, turn debug mode back off
          optionsResult.listFilesOnly = false;
          btnListFilesOnly.setSelection (false);
        }
      }
    }
  }

  private int conflictingOptions (Shell shell, String errorMessage, Label...labels) {
    // the 1st label text is to be replaced by errorMessage(in red, flashing), all other labels are to flash
    int result = -1;
    Color[] labelOrgColor = null;
    final Display display = shell.getDisplay ();
    Color errorColor = display.getSystemColor(SWT.COLOR_RED);
    if (labels != null) {
      labelOrgColor = new Color[labels.length];
      for (int i=0; i<labels.length; ++i) {
        labelOrgColor[i] = labels[i].getForeground ();
        labels[i].setForeground(errorColor);
      }
      MessageBox dialog = new MessageBox (shell, SWT.ICON_WARNING | SWT.YES | SWT.NO);
      dialog.setText ("Conflicting Options");
      dialog.setMessage (errorMessage);
      result = dialog.open ();
      for (int i=0; i<labels.length; ++i) {
        // restore original label colors
        labels[i].setForeground(labelOrgColor[i]);
      }
    }
    return result;
  }

  private void validateQuietMode (Button btnListFilesOnly, Label lblListFilesOnly, Button btnQuietMode,
      Label lblQuietMode, Button btnDebugMode, Label lblDebugMode) {
    if (optionsResult.quietMode) {
      // quiet mode selected, make sure listFilesOnly and debugMode are off
      if (optionsResult.listFilesOnly || optionsResult.debugMode) {
        // can't have these on when quiet mode selected
        int result = conflictingOptions (shlMoveimagestoframeOptions,
            "Quiet mode requires \"List Files Only\" & \"Debug Mode\" to be off. Do you want to Proceed?",
            lblQuietMode, lblDebugMode, lblListFilesOnly);
        if (result == SWT.YES) {
          // yes, user wants quiet mode, turn the conflicting options off
          btnListFilesOnly.setSelection (false);
          btnDebugMode.setSelection (false);
          optionsResult.listFilesOnly = false;
          optionsResult.debugMode = false;
        }
        else {
          // no, the user does not want to turn off the conflicting options, turn quiet mode back off
          optionsResult.quietMode = false;
          btnQuietMode.setSelection (false);
        }
      }
    }
  }

  private void validateDebugMode (Button btnQuietMode, Label lblQuietMode, Button btnDebugMode, Label lblDebugMode) {
    if (optionsResult.debugMode) {
      // debug mode selected, make sure quietMode is off
      if (optionsResult.quietMode) {
        // can't have quiet mode on when debug mode selected
        int result = conflictingOptions (shlMoveimagestoframeOptions,
            "Debug mode requires \"Quiet Mode\" to be off. Do you want to Proceed?",
            lblDebugMode, lblQuietMode);
        if (result == SWT.YES) {
          // yes, user wants debug mode, turn the conflicting options off
          btnQuietMode.setSelection (false);
          optionsResult.quietMode = false;
        }
        else {
          // no, the user does not want to turn off the conflicting options, turn debug mode back off
          optionsResult.debugMode = false;
          btnDebugMode.setSelection (false);
        }
      }
    }
  }

  void handleKeyDown (KeyEvent keyDown) {
    if (keyDown.keyCode == SWT.CR || keyDown.keyCode == SWT.KEYPAD_CR) {
      // one of the Enter keys were pressed, validate and convert the value entered 
      validateMBtoLeaveFree ();
    }
  }

  private void validateMBtoLeaveFree () {
    String mbToLeaveFreeStr = textMBtoLeaveFree.getText ();
    try {
      optionsResult.mbToLeaveFree = Integer.parseInt (mbToLeaveFreeStr);
    } catch (NumberFormatException e1) {
      MessageBox dialog = new MessageBox (shlMoveimagestoframeOptions, SWT.ICON_ERROR | SWT.OK);
      dialog.setText ("Invalid Number");
      dialog.setMessage (String.format (
          "The value you entered for \"Megabytes to leave free\": \"%s\" is not a valid number, " + "Re-enter",
          mbToLeaveFreeStr));
      dialog.open ();
      textMBtoLeaveFree.setText (String.valueOf (optionsResult.mbToLeaveFree));
    }
  }
}
