package com.github.sdbg.debug.core.internal.forwarder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Tunnel {
  private Logger logger;
  private String id;

  private ByteChannel leftChannel, rightChannel;
  private ByteBuffer leftToRight, rightToLeft;

  public Tunnel(Logger logger, String id) {
    this.logger = logger;
    this.id = id;
  }

  public void close() {
    close(leftChannel);
    close(rightChannel);
  }

  public ByteChannel getLeftChannel() {
    return leftChannel;
  }

  public ByteBuffer getLeftToRight() {
    if (leftToRight == null) {
      leftToRight = ByteBuffer.allocate(8192);
    }

    return leftToRight;
  }

  public ByteChannel getRightChannel() {
    return rightChannel;
  }

  public ByteBuffer getRightToLeft() {
    if (rightToLeft == null) {
      rightToLeft = ByteBuffer.allocate(8192);
    }

    return rightToLeft;
  }

  public void setLeftChannel(ByteChannel leftChannel) {
    this.leftChannel = leftChannel;
  }

  public void setRightChannel(ByteChannel rightChannel) {
    this.rightChannel = rightChannel;
  }

  public boolean spool(SelectionKey key) throws IOException {
    if (key.isReadable()) {
      if (key.channel() == leftChannel) {
        return spoolLeftToRight(key.selector());
      } else {
        return spoolRightToLeft(key.selector());
      }
    } else if (key.isWritable()) {
      if (key.channel() == rightChannel) {
        return spoolLeftToRight(key.selector());
      } else {
        return spoolRightToLeft(key.selector());
      }
    } else {
      return true;
    }
  }

  public boolean spoolLeftToRight(Selector selector) throws IOException {
    return spool(selector, leftChannel, rightChannel, getLeftToRight());
  }

  public boolean spoolRightToLeft(Selector selector) throws IOException {
    return spool(selector, rightChannel, leftChannel, getRightToLeft());
  }

  private void close(Channel channel) {
    if (channel != null) {
      try {
        channel.close();
      } catch (IOException e) {
        // Best effort
      }
    }
  }

  private boolean spool(Selector selector, ReadableByteChannel from, WritableByteChannel to,
      ByteBuffer buff) throws IOException {
    int read = 0, written = 0;

    do {
      if (read > -1 && buff.hasRemaining()) {
        int oldPos = buff.position();
        read = from.read(buff);
        if (logger != null && logger.isLoggable(Level.FINEST) && oldPos < buff.position()) {
          logger.finest("Tunnel " + id + " spooling:\n" + "==== BEGIN DUMP ====\n"
              + new String(buff.array(), oldPos, buff.position()) + "\n==== END DUMP ====");
        }
      } else {
        read = 0;
      }

      if (buff.position() > 0) {
        buff.flip();
        written = to.write(buff);
        buff.compact();
      } else {
        written = 0;
      }
    } while (read > 0 || written > 0);

    SelectionKey fromKey = null;
    if (selector != null && from instanceof SelectableChannel) {
      fromKey = ((SelectableChannel) from).keyFor(selector);
    }
    if (fromKey != null) {
      if (!buff.hasRemaining()) {
        fromKey.interestOps(fromKey.interestOps() & ~SelectionKey.OP_READ);
      } else {
        fromKey.interestOps(fromKey.interestOps() | SelectionKey.OP_READ);
      }
    }

    SelectionKey toKey = null;
    if (selector != null && to instanceof SelectableChannel) {
      toKey = ((SelectableChannel) to).keyFor(selector);
    }
    if (toKey != null) {
      if (buff.position() == 0) {
        toKey.interestOps(toKey.interestOps() & ~SelectionKey.OP_WRITE);
      } else {
        toKey.interestOps(toKey.interestOps() | SelectionKey.OP_WRITE);
      }
    }

    return (read > -1 || buff.position() > 0) && written > -1;
  }
}
