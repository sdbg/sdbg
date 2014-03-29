package com.github.sdbg.debug.core.internal.forwarder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class DeviceReversePortForwarder extends ReversePortForwarder {
  public static void main(String[] args) throws IOException {
    for (Handler handler : LogManager.getLogManager().getLogger("").getHandlers()) {
      if (handler instanceof ConsoleHandler) {
        ConsoleHandler ch = (ConsoleHandler) handler;
        ch.setFormatter(new ShellFormatter());
        break;
      }
    }

    int commandPort = Integer.parseInt(args[0]);
    int[] ports = new int[args.length - 1];
    for (int i = 0; i < ports.length; i++) {
      ports[i] = Integer.parseInt(args[i + 1]);
    }

    new DeviceReversePortForwarder(commandPort, ports).run();
  }

  private int commandPort;
  private int[] ports;

  private ServerSocketChannel commandServerChannel;
  private Collection<ServerSocketChannel> serverChannels = new HashSet<ServerSocketChannel>();
  private Map<ByteChannel, ByteBuffer> pendingChannels = new HashMap<ByteChannel, ByteBuffer>();

  public DeviceReversePortForwarder(int commandPort, int[] ports) {
    super(Logger.getLogger(DeviceReversePortForwarder.class.getName()));
    this.commandPort = commandPort;
    this.ports = ports;
  }

  public void run() throws IOException {
    init();

    try {
      do {
        // Wait for an event on one of the registered channels
        if (commandChannel != null) {
          selector.select();
        } else {
          logger.info("Waiting for command connection on port "
              + commandServerChannel.socket().getLocalPort() + "...");

          // Command channel not established yet. Wait for 10 seconds and then timeout
          selector.select(10000);
        }

        for (Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator(); selectedKeys.hasNext();) {
          SelectionKey key = selectedKeys.next();
          selectedKeys.remove();

          if (key.isValid()) {
            processKey(key);
          }
        }
      } while (commandChannel != null);

      logger.info("Command connection timed out");
    } finally {
      done();
    }
  }

  @Override
  protected void done() {
    for (ByteChannel channel : new ArrayList<ByteChannel>(pendingChannels.keySet())) {
      close(channel);
    }
    pendingChannels.clear();

    if (commandChannel != null) {
      try {
        commandChannel.close();
      } catch (IOException e) {
      }
      commandChannel = null;
    }

    if (commandServerChannel != null) {
      try {
        commandServerChannel.close();
      } catch (IOException e) {
      }
      commandServerChannel = null;
    }

    super.done();

    for (ServerSocketChannel channel : new ArrayList<ServerSocketChannel>(serverChannels)) {
      close(channel);
    }
  }

  @Override
  protected void init() throws IOException {
    super.init();

    // Create a new non-blocking server socket channel
    createCommandServerSocketChannel();

    for (int port : ports) {
      createServerSocketChannel(port);
    }
  }

  @Override
  protected boolean processCommand(byte cmd, ByteBuffer commandBuffer) throws IOException {
    if (cmd == CMD_OPEN_CHANNEL_FAIL) {
      int tunnelId = commandBuffer.getInt();
      closeTunnel(tunnelId);
      logger.info("Opening tunnel " + tunnelId + " failed");
      return true;
    } else {
      return super.processCommand(cmd, commandBuffer);
    }
  }

  @Override
  protected void processKey(SelectionKey key) throws IOException {
    if (key.isAcceptable()) {
      if (key.channel() == commandServerChannel) {
        acceptCommand(key);
      } else {
        if (commandChannel == null) {
          throw new IOException("Unexpected");
        }

        accept(key);
      }
    } else {
      if (commandChannel == null) {
        throw new IOException("Unexpected");
      }

      if (key.isReadable() && pendingChannels.containsKey(key.channel())) {
        ByteChannel channel = (ByteChannel) key.channel();

        try {
          ByteBuffer readBuffer = pendingChannels.get(channel);
          int read = channel.read(readBuffer);
          if (read == -1) {
            throw new IOException("Pending channel closed");
          }

          if (readBuffer.position() >= 5) {
            readBuffer.flip();
            try {
              byte cmd = readBuffer.get();
              if (cmd == CMD_OPEN_CHANNEL_ACK) {
                int tunnelId = readBuffer.getInt();

                try {
                  registerLeftChannel(tunnelId, channel);
                  pendingChannels.remove(channel);
                } catch (IOException e) {
                  logger.log(Level.INFO, "Spooling error: " + e.getMessage(), e);
                  closeTunnel(tunnelId);
                }
              } else {
                throw new IOException("Unknown command");
              }
            } finally {
              readBuffer.compact();
            }
          }
        } catch (IOException e) {
          logger.log(Level.SEVERE, "PROTOCOL ERROR: " + e.getMessage(), e);
          close(channel);
        }
      } else {
        super.processKey(key);
      }
    }
  }

  private void accept(SelectionKey key) throws IOException {
    // For an accept to be pending the channel must be a server socket channel.
    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

    int tunnelId = createTunnel();
    logger.fine("New incoming connection to device port "
        + serverSocketChannel.socket().getLocalPort() + "; tunnel " + tunnelId);

    try {
      // Accept the connection and make it non-blocking
      SocketChannel socketChannel = serverSocketChannel.accept();
      socketChannel.configureBlocking(false);

      registerRightChannel(tunnelId, socketChannel);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "PROTOCOL ERROR: " + e.getMessage(), e);
      closeTunnel(tunnelId);
    }

    commandWriteBuffer.put(CMD_OPEN_CHANNEL);
    commandWriteBuffer.putInt(serverSocketChannel.socket().getLocalPort());
    commandWriteBuffer.putInt(tunnelId);
    writeCommand();
  }

  private void acceptCommand(SelectionKey key) throws IOException {
    // Accept the connection and make it non-blocking
    SocketChannel socketChannel = commandServerChannel.accept();
    socketChannel.configureBlocking(false);

    if (commandChannel == null) {
      socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
      commandChannel = socketChannel;

      commandWriteBuffer.put(CMD_HELLO);
      writeCommand();

      // Now that the command channel is opened we can start accepting connections from the other channels 
      for (ServerSocketChannel channel : serverChannels) {
        channel.register(selector, SelectionKey.OP_ACCEPT);
      }

      logger.info("Command connection established");
    } else {
      socketChannel.register(selector, SelectionKey.OP_READ);
      pendingChannels.put(socketChannel, ByteBuffer.allocate(5));
    }
  }

  private void close(ByteChannel channel) {
    pendingChannels.remove(channel);
    try {
      channel.close();
    } catch (IOException e) {
      // Best effort
    }
  }

  private void close(ServerSocketChannel channel) {
    try {
      channel.close();
    } catch (IOException e) {
      // Best effort
    }

    serverChannels.remove(channel);
  }

  private void createCommandServerSocketChannel() throws IOException {
    ServerSocketChannel channel = ServerSocketChannel.open();

    channel.configureBlocking(false);

    channel.socket().bind(new InetSocketAddress(commandPort));
    channel.register(selector, SelectionKey.OP_ACCEPT);

    commandServerChannel = channel;
  }

  private void createServerSocketChannel(int port) throws IOException {
    ServerSocketChannel channel = ServerSocketChannel.open();
    serverChannels.add(channel);

    channel.configureBlocking(false);

    channel.socket().bind(new InetSocketAddress(port));
    logger.info("Opened reverse proxy port: " + port);
  }
}
