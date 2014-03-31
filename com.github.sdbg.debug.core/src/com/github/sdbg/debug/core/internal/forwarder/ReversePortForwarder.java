package com.github.sdbg.debug.core.internal.forwarder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class ReversePortForwarder {
  protected static final byte CMD_UNKNOWN_COMMAND = (byte) 1, CMD_OPEN_CHANNEL = (byte) 2,
      CMD_OPEN_CHANNEL_ACK = (byte) 3, CMD_OPEN_CHANNEL_FAIL = (byte) 4;

  private static final int CMD_MAX_LENGTH = 10;

  private static int uuid;

  protected Selector selector;

  protected ByteChannel commandChannel;
  protected ByteBuffer commandReadBuffer, commandWriteBuffer;

  private Map<Integer, Tunnel> tunnels = new HashMap<Integer, Tunnel>();
  private Map<ByteChannel, Integer> channels = new HashMap<ByteChannel, Integer>();

  protected String tracePrefix;

  protected final Logger logger;

  public ReversePortForwarder(Logger logger) {
    this.logger = logger;
  }

  protected void closeTunnel(int tunnelId) {
    Tunnel tunnel = tunnels.remove(tunnelId);
    if (tunnel != null) {
      channels.remove(tunnel.getLeftChannel());
      channels.remove(tunnel.getRightChannel());

      tunnel.close();
    }

    logger.fine("Tunnel " + tunnelId + " closed");
  }

  protected int createTunnel() throws IOException {
    createTunnel(uuid);
    return uuid++;
  }

  protected Tunnel createTunnel(int tunnelId) throws IOException {
    if (tunnels.containsKey(tunnelId)) {
      throw new IOException("Tunnel with ID " + tunnelId + " already exists");
    }

    Tunnel tunnel = new Tunnel(logger, Integer.toString(tunnelId));
    tunnels.put(tunnelId, tunnel);

    logger.fine("Tunnel " + tunnelId + " registered");
    return tunnel;
  }

  protected void done() {
    for (Tunnel tunnel : tunnels.values()) {
      tunnel.close();
    }

    tunnels.clear();
    channels.clear();

    if (selector != null) {
      try {
        selector.close();
      } catch (IOException e) {
        // Best effort
      }
      selector = null;
    }

    commandReadBuffer = commandWriteBuffer = null;
  }

  protected Tunnel getTunnel(int tunnelId) throws IOException {
    Tunnel tunnel = tunnels.get(tunnelId);
    if (tunnel != null) {
      return tunnel;
    } else {
      throw new IOException("Unknown tunnel: " + tunnelId);
    }
  }

  protected int getTunnelId(SelectionKey key) throws IOException {
    Integer tunnelId = channels.get(key.channel());
    if (tunnelId != null) {
      return tunnelId;
    } else {
      throw new IOException("Unknown channel: " + key.channel());
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
      } else if (key.isWritable()) {
        writeCommand();
      }
    } else if (key.isReadable() || key.isWritable()) {
      try {
        int tunnelId = getTunnelId(key);

        try {
          if (!getTunnel(tunnelId).spool(key)) {
            closeTunnel(tunnelId);
          }
        } catch (IOException e) {
          logger.log(Level.INFO, "Spooling error for tunnel " + tunnelId + ": " + e.getMessage(), e);
          closeTunnel(tunnelId);
        }
      } catch (IOException e) {
        logger.log(Level.SEVERE, "PROTOCOL ERROR: " + e.getMessage(), e);
      }
    }
  }

  protected void readCommand() throws IOException {
    int read = 0;
    boolean commandProcessed = false;

    do {
      read = commandChannel.read(commandReadBuffer);
      if (read == -1) {
        // Remote entity shut the socket down cleanly. Do the
        // same from our end and cancel the channel.
        throw new IOException("Command channel closed");
      }

      commandProcessed = false;
      if (commandReadBuffer.position() > 0) {
        ByteBuffer readBuffer = (ByteBuffer) commandReadBuffer.duplicate().flip();

        byte cmd = readBuffer.get();
        if (processCommand(cmd, readBuffer)) {
          readBuffer.compact();
          commandReadBuffer = readBuffer;
          commandProcessed = true;
        }
      }
    } while (read > 0 || commandProcessed);

    SelectionKey key = ((SelectableChannel) commandChannel).keyFor(selector);
    if (commandReadBuffer.remaining() < CMD_MAX_LENGTH) {
      key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
    } else {
      key.interestOps(key.interestOps() | SelectionKey.OP_READ);
    }
  }

  protected void registerLeftChannel(int tunnelId, ByteChannel leftChannel) throws IOException {
    Tunnel tunnel = getTunnel(tunnelId);
    if (tunnel.getLeftChannel() != null) {
      throw new IOException("Left channel of tunnel " + tunnelId + " is already registered");
    } else {
      logger.fine("Left channel of tunnel " + tunnelId + " registered: " + leftChannel);
      tunnel.setLeftChannel(leftChannel);
      channels.put(leftChannel, tunnelId);
      channelRegistered(tunnelId, tunnel);
    }
  }

  protected void registerRightChannel(int tunnelId, ByteChannel rightChannel) throws IOException {
    Tunnel tunnel = getTunnel(tunnelId);
    if (tunnel.getRightChannel() != null) {
      throw new IOException("Right channel of tunnel " + tunnelId + " is already registered");
    } else {
      logger.fine("Right channel of tunnel " + tunnelId + " registered: " + rightChannel);
      tunnel.setRightChannel(rightChannel);
      channels.put(rightChannel, tunnelId);
      channelRegistered(tunnelId, tunnel);
    }
  }

  protected void writeCommand() throws IOException {
    while (commandWriteBuffer.position() > 0) {
      commandWriteBuffer.flip();

      try {
        if (commandChannel.write(commandWriteBuffer) <= 0) {
          break;
        }
      } finally {
        commandWriteBuffer.compact();
      }
    }

    SelectionKey key = ((SelectableChannel) commandChannel).keyFor(selector);
    if (commandWriteBuffer.position() > 0) {
      key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
    } else {
      key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
    }
  }

  private void channelRegistered(int tunnelId, Tunnel tunnel) throws ClosedChannelException {
    if (tunnel.getLeftChannel() != null && tunnel.getRightChannel() != null) {
      ((SelectableChannel) tunnel.getLeftChannel()).register(selector, SelectionKey.OP_READ
          | SelectionKey.OP_WRITE);
      ((SelectableChannel) tunnel.getRightChannel()).register(selector, SelectionKey.OP_READ
          | SelectionKey.OP_WRITE);
      logger.fine("Tunnel " + tunnelId + " ready for work");
    }
  }
}
