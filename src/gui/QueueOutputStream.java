package gui;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class QueueOutputStream extends OutputStream {
  private static final int DEFAULT_BUFFER_SIZE = 1024;
  public static final byte[] END_SIGNAL = new byte[] {};

  private final BlockingQueue<byte[]> queue = new LinkedBlockingDeque<> ();

  private final byte[] buffer;

  private boolean closed = false;

  private int count = 0;

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
    
    // // DEBUG
    // System.out.print ("#" + (new String (byteBuffer, offset, len)) + "#");
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

    // // DEBUG
    // System.out.print ("^" + (new String (byteBuffer)) + "^");
    
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
    queue.offer (END_SIGNAL);
    closed = true;
  }

  public BlockingQueue<byte[]> getQueue () {
    return queue;
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
      count = 0;
    }
  }
}