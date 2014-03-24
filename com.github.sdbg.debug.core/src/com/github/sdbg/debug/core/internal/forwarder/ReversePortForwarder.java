package com.github.sdbg.debug.core.internal.forwarder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashMap;
import java.util.Map;

public abstract class ReversePortForwarder {
  protected static final byte CMD_UNKNOWN_COMMAND = (byte) 1, CMD_OPEN_CHANNEL = (byte) 2,
      CMD_OPEN_CHANNEL_ACK = (byte) 3, CMD_OPEN_CHANNEL_FAIL = (byte) 4;

  private static final int CMD_MAX_LENGTH = 10;

  protected Selector selector;

  protected ByteChannel commandChannel;
  protected ByteBuffer commandReadBuffer, commandWriteBuffer;

  private Map<Integer, Tunnel> tunnels = new HashMap<Integer, Tunnel>();
  private Map<ByteChannel, Integer> channels = new HashMap<ByteChannel, Integer>();

  private static int uuid;

  public ReversePortForwarder() {
  }

  protected void closeTunnel(int tunnelId) {
    Tunnel tunnel = tunnels.remove(tunnelId);
    if (tunnel != null) {
      if (tunnel.getLeftChannel() != null) {
        ((SelectableChannel) tunnel.getLeftChannel()).keyFor(selector).cancel();
      }

      if (tunnel.getRightChannel() != null) {
        ((SelectableChannel) tunnel.getRightChannel()).keyFor(selector).cancel();
      }

      channels.remove(tunnel.getLeftChannel());
      channels.remove(tunnel.getRightChannel());
    }

    tunnel.close();
  }

  protected int createTunnel() throws IOException {
    int tunnelId = uuid;
    createTunnel(tunnelId);

    try {
      return tunnelId;
    } finally {
      uuid++;
    }
  }

  protected Tunnel createTunnel(int tunnelId) throws IOException {
    if (tunnels.containsKey(tunnelId)) {
      throw new IOException("Tunnel with that ID alreay exists");
    }

    Tunnel tunnel = new Tunnel();
    tunnels.put(tunnelId, tunnel);

    return tunnel;
  }

  protected void done() {
    for (Tunnel tunnel : tunnels.values()) {
      tunnel.close();
    }

    tunnels.clear();
    channels.clear();

    try {
      selector.close();
    } catch (IOException e) {
      // Best effort
    }
    selector = null;

    commandReadBuffer = commandWriteBuffer = null;
  }

  protected Tunnel getTunnel(int tunnelId) throws IOException {
    Tunnel tunnel = tunnels.get(tunnelId);
    if (tunnel != null) {
      return tunnel;
    } else {
      throw new IOException("Unknown tunnel");
    }
  }

  protected Tunnel getTunnel(SelectionKey key) throws IOException {
    Integer tunnelId = channels.get(key.channel());
    if (tunnelId != null) {
      return getTunnel(tunnelId);
    } else {
      throw new IOException("Unknown channel");
    }
  }

  protected void init() throws IOException {
    // Create a new selector
    selector = SelectorProvider.provider().openSelector();

    commandReadBuffer = ByteBuffer.allocate(8192);
    commandWriteBuffer = ByteBuffer.allocate(8192);
  }

  protected boolean processCommand(byte cmd, ByteBuffer commandBuffer) throws IOException {
    commandWriteBuffer.put(CMD_UNKNOWN_COMMAND);
    commandWriteBuffer.put(cmd);
    writeCommand();

    return true;
  }

  protected void processKey(SelectionKey key) throws IOException {
    // Check what event is available and deal with it
    if (key.channel() == commandChannel) {
      if (key.isReadable()) {
        readCommand();

        if (commandReadBuffer.remaining() < CMD_MAX_LENGTH) {
          key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
        } else {
          key.interestOps(key.interestOps() | SelectionKey.OP_READ);
        }
      } else {
        writeCommand();

        if (commandWriteBuffer.limit() == 0) {
          key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        } else {
          key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        }
      }
    } else if (key.isReadable() || key.isWritable()) {
      getTunnel(key).spool(key);
    }
  }

  protected void readCommand() throws IOException {
    // Attempt to read off the channel
    int numRead = commandChannel.read(commandReadBuffer);
    if (numRead == -1) {
      // Remote entity shut the socket down cleanly. Do the
      // same from our end and cancel the channel.
      throw new IOException();
    }

    SelectionKey key = ((SelectableChannel) commandChannel).keyFor(selector);
    if (commandReadBuffer.limit() > 0) {
      key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);

      ByteBuffer readBuffer = (ByteBuffer) commandReadBuffer.duplicate().flip();

      byte cmd = readBuffer.get();
      if (processCommand(cmd, readBuffer)) {
        readBuffer.compact();
        commandReadBuffer = readBuffer;
      }
    } else {
      key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
    }
  }

  protected void registerLeftChannel(int tunnelId, ByteChannel leftChannel) throws IOException {
    Tunnel tunnel = getTunnel(tunnelId);
    if (tunnel.getLeftChannel() != null) {
      throw new IOException("Left channel of the tunnel is already registered");
    } else {
      tunnel.setLeftChannel(leftChannel);
      ((SelectableChannel) leftChannel).keyFor(selector).interestOps(
          SelectionKey.OP_READ | SelectionKey.OP_WRITE);
      channels.put(leftChannel, tunnelId);
    }
  }

  protected void registerRightChannel(int tunnelId, ByteChannel rightChannel) throws IOException {
    Tunnel tunnel = getTunnel(tunnelId);
    if (tunnel.getRightChannel() != null) {
      throw new IOException("Right channel of the tunnel is already registered");
    } else {
      tunnel.setRightChannel(rightChannel);
      ((SelectableChannel) rightChannel).keyFor(selector).interestOps(
          SelectionKey.OP_READ | SelectionKey.OP_WRITE);
      channels.put(rightChannel, tunnelId);
    }
  }

  protected void trace(String str) {
  }

  protected void trace(Throwable t) {
  }

  protected void writeCommand() throws IOException {
    commandWriteBuffer.flip();
    commandChannel.write(commandWriteBuffer);
    commandWriteBuffer.compact();

    if (!commandWriteBuffer.hasRemaining()) {
      SelectionKey key = ((SelectableChannel) commandChannel).keyFor(selector);
      key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
    }
  }
}
