package gui;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;

public class QueueInputStream extends InputStream {
  
  // private QueueOutputStream qOS;
  private boolean closed;
  private BlockingQueue<byte[]> queue;
  private byte[] buffer;
  private int offsetInBuffer;
  private byte[] endSignal;
  
  public QueueInputStream () {
    initClass ();
  }

  public QueueInputStream (QueueOutputStream qOS) throws IOException {
    if (qOS == null || qOS.isClosed ())
      throwClosedException ();
    initClass ();
    // this.qOS = qOS;
    queue = qOS.getQueue ();
    endSignal = QueueOutputStream.END_SIGNAL;
  }

  @Override
  public synchronized int read () throws IOException {
    if (closed) {
      throwClosedException ();
    }
    if (buffer == null || offsetInBuffer >= buffer.length) {
      // we need a new buffer
      try {
        buffer = queue.take ();
        offsetInBuffer = 0;
        if (buffer == endSignal) {
          return -1; // we have gotten the END_SIGNAL
        }
      } catch (InterruptedException e) {
        closed = true;
        // re-throw InterruptedException
        Thread.currentThread ().interrupt ();
      }
    }
    return (int) (buffer[offsetInBuffer++] & 0x00ff);
  }

  @Override
  public synchronized int read (final byte[] byteBuffer) throws IOException {
    if (closed) {
      throwClosedException ();
    }
    return read (byteBuffer, 0, byteBuffer.length);
  }

  @Override
  public synchronized int read (final byte[] byteBuffer, final int offset, final int len) throws IOException {
    if (closed) {
      throwClosedException ();
    }
    int numberOfBytesRead = 0;
    int userOffset = offset;
    
    int bytesInBuffer;
    int numBytesWanted;
    int bytesToMoveToByteBuffer;
    do {
      // get a new buffer if we needed it
      if (!getBufferFromQueue ()) {
        // we have read to the end of the queue, no more is coming
        // number of bytes read or -1 if no bytes read
        return numberOfBytesRead > 0 ? numberOfBytesRead : -1; 
      }
      // determine # of bytes in buffer
      bytesInBuffer = buffer.length - offsetInBuffer;
      numBytesWanted = len - userOffset;
      bytesToMoveToByteBuffer = bytesInBuffer > numBytesWanted ? numBytesWanted : bytesInBuffer;
      System.arraycopy (buffer, offsetInBuffer, byteBuffer, userOffset, bytesToMoveToByteBuffer);
      offsetInBuffer += bytesToMoveToByteBuffer;
      userOffset += bytesToMoveToByteBuffer;
      numBytesWanted -= bytesToMoveToByteBuffer;
      numberOfBytesRead += bytesToMoveToByteBuffer;
    } while (numBytesWanted > 0);
    
    return numberOfBytesRead;
  }

  @Override
  public synchronized int available () throws IOException {
    if (closed) {
      throwClosedException ();
    }
    byte[] nextArray = queue.peek ();
    if (nextArray == null)
      return 0;
    return nextArray.length;
  }

  @Override
  public synchronized void close () throws IOException {
    closed = true;
  }

  @Override
  public synchronized boolean markSupported () {
    return false;
  }

  private boolean getBufferFromQueue () {
    // return false if we have received an "endSignal", true if there is more data in buffer or we have a new one
    if (buffer == null || offsetInBuffer >= buffer.length) {
      // we need a new buffer
      if (buffer == endSignal) {
        return false; // we have gotten the END_SIGNAL
      }
      try {
        buffer = queue.take ();
        offsetInBuffer = 0;
        if (buffer == endSignal) {
          return false; // we have gotten the END_SIGNAL
        }
      } catch (InterruptedException e) {
        closed = true;
        // re-throw InterruptedException
        Thread.currentThread ().interrupt ();
      }
    }
    return true;
  }

  @Override
  public synchronized void mark (int readlimit) {
    
  }

  @Override
  public synchronized void reset () throws IOException {
    
  }

  @Override
  public long skip (long n) throws IOException {
    return -1L;
  }

  private void throwClosedException () throws IOException {
    // we are closed, throw exception
    throw new IOException ("QueueInputStream is closed");
  }

  private void initClass () {
    // qOS = null;
    closed = false;
    queue = null;
    buffer = null;
    offsetInBuffer = 0;
  }
}
