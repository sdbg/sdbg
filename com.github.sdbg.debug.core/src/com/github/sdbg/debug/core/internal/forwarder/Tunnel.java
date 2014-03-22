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

public class Tunnel {
  public static void spool(Selector selector, ReadableByteChannel from, WritableByteChannel to,
      ByteBuffer buff) throws IOException {
    int read = from != null ? from.read(buff) : 0;
    if (read == -1) {
      throw new IOException("Reached EOF");
    }

    if (read > 0) {
      buff.flip();
      to.write(buff);
      buff.compact();
    }

    SelectionKey fromKey = null;
    if (selector != null && from instanceof SelectableChannel) {
      fromKey = ((SelectableChannel) from).keyFor(selector);
    }
    if (fromKey != null) {
      if (buff.remaining() == 0) {
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
      if (buff.remaining() == buff.limit()) {
        toKey.interestOps(toKey.interestOps() & ~SelectionKey.OP_WRITE);
      } else {
        toKey.interestOps(toKey.interestOps() | SelectionKey.OP_WRITE);
      }
    }
  }

  private static void close(Channel channel) {
    try {
      channel.close();
    } catch (IOException e) {
      // Best effort
    }
  }

  private ByteChannel leftChannel, rightChannel;
  private ByteBuffer leftToRight, rightToLeft;

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

  public void spool(SelectionKey key) throws IOException {
    if (key.isReadable()) {
      if (key.channel() == leftChannel) {
        spool(key.selector(), leftChannel, rightChannel, getLeftToRight());
      } else {
        spool(key.selector(), rightChannel, leftChannel, getRightToLeft());
      }
    } else if (key.isWritable()) {
      if (key.channel() == rightChannel) {
        spool(key.selector(), leftChannel, rightChannel, getLeftToRight());
      } else {
        spool(key.selector(), rightChannel, leftChannel, getRightToLeft());
      }
    }
  }
}
