package gui;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import org.eclipse.swt.widgets.Display;

public class QueueOutputStream extends OutputStream {
  private static final int DEBUG_INDICATOR_LINE_SIZE = 120;
  private static final int DEFAULT_BUFFER_SIZE = 1024;
  public static final byte[] END_SIGNAL = new byte[] {};

  private final BlockingQueue<byte[]> queue = new LinkedBlockingDeque<> (4);

  private final byte[] buffer;
  
  private boolean closed = false;

  private int count = 0;
  
  private Display display = null;
  
  // DEBUG start
  private int indicatorCount = 0;
  // DEBUG end

  public QueueOutputStream () {
    this (DEFAULT_BUFFER_SIZE);
  }

  public QueueOutputStream (final int bufferSize) {
    if (bufferSize <= 0) {
      throw new IllegalArgumentException ("Buffer size <= 0");
    }
    this.buffer = new byte[bufferSize];
  }
  public synchronized boolean isClosed () {
    return closed;
  }
  
  @Override
  public synchronized void write (final int b) throws IOException {
    if (closed) {
      throw new IllegalStateException ("Stream is closed");
    }
    addToBuffer (b);
  }

  @Override
  public synchronized void write (final byte[] byteBuffer, final int offset, final int len) throws IOException {
    if (closed) {
      throw new IllegalStateException ("Stream is closed");
    }
    
    int endOffset = offset + len;
    for (int i = offset; i < endOffset; ++i) {
      addToBuffer (byteBuffer[i]);
    }
  }

  @Override
  public synchronized void write (final byte[] byteBuffer) throws IOException {
    if (closed) {
      throw new IllegalStateException ("Stream is closed");
    }

    int len = byteBuffer.length;
    for (int i = 0; i < len; ++i) {
      addToBuffer (byteBuffer[i]);
    }
  }

  @Override
  public synchronized void flush () throws IOException {
    if (closed) {
      throw new IllegalStateException ("Stream is closed");
    }
    flushBuffer ();
  }

  @Override
  public synchronized void close () throws IOException {
    flushBuffer ();
    
    try {
      queue.put (END_SIGNAL);
    } catch (InterruptedException e) {
      throw (new RuntimeException ("QueueOutputStream got InterruptedException, see cause", e));
    }
    closed = true;
  }

  public BlockingQueue<byte[]> getQueue () {
    return queue;
  }

  public void setDisplay (Display display) {
    this.display = display;
  }

  private void addToBuffer (final int singleByte) {
    if (count >= buffer.length) {
      flushBuffer ();
    }
    buffer[count++] = (byte) singleByte;
  }

  private void flushBuffer () {
    if (count > 0) {
      final byte[] copy = new byte[count];
      System.arraycopy (buffer, 0, copy, 0, count);
      
      try {
        queue.put (copy);
      } catch (InterruptedException e) {
        throw (new RuntimeException ("QueueOutputStream got InterruptedException, see cause", e));
      }
      if (display != null) {
        display.wake (); // wake up the display, it has data to process
      }
      count = 0;
    }
  }

  public void debugInOutIndicator (String inOut) {
    indicatorCount += inOut.length ();
    if (indicatorCount >= DEBUG_INDICATOR_LINE_SIZE) {
      System.out.print ('\n');
      indicatorCount = 0;
    }
    System.out.print (inOut);
  }
}