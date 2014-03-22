package com.github.sdbg.debug.core.internal.forwarder;

import java.io.IOException;
import java.net.BindException;
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

public class DeviceReversePortForwarder extends ReversePortForwarder {
  public static void main() throws IOException {
    new DeviceReversePortForwarder().run();
  }

  private ServerSocketChannel serverCommandChannel;
  private Map<ServerSocketChannel, Collection<Integer>> serverChannels = new HashMap<ServerSocketChannel, Collection<Integer>>();
  private Map<ByteChannel, ByteBuffer> pendingChannels = new HashMap<ByteChannel, ByteBuffer>();

  public DeviceReversePortForwarder() {
  }

  public void run() throws IOException {
    init();

    try {
      do {
        // Wait for an event one of the registered channels
        if (commandChannel != null) {
          selector.select();
        } else {
          // Command channel not established yet. Wait for 60 seconds and then timeout
          selector.select(60);
        }

        for (Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator(); selectedKeys.hasNext();) {
          SelectionKey key = selectedKeys.next();
          selectedKeys.remove();

          if (key.isValid()) {
            processKey(key);
          }
        }
      } while (commandChannel != null);
    } finally {
      done();
    }
  }

  @Override
  protected void done() {
    for (ServerSocketChannel channel : new ArrayList<ServerSocketChannel>(serverChannels.keySet())) {
      close(channel);
    }

    super.done();
  }

  @Override
  protected void init() throws IOException {
    super.init();

    // Create a new non-blocking server socket channel
    serverCommandChannel = ServerSocketChannel.open();
    serverCommandChannel.configureBlocking(false);

    // Bind the server socket to the specified address and port
    for (int i = 4000; i <= 32767; i++) {
      try {
        serverCommandChannel.socket().bind(new InetSocketAddress(i));
        break;
      } catch (BindException e) {
        // Stay silent
      }
    }

    if (!serverCommandChannel.socket().isBound()) {
      super.done();
      throw new IOException("No available port in the interval 4000 - 32767");
    }

    // Register the server socket channel, indicating an interest in 
    // accepting new connections
    serverCommandChannel.register(selector, SelectionKey.OP_ACCEPT);
    System.out.println(serverCommandChannel.socket().getLocalPort());
  }

  @Override
  protected boolean processCommand(byte cmd, ByteBuffer commandBuffer) throws IOException {
    if (cmd == CMD_OPEN_PORT) {
      int port = commandBuffer.getInt();

      try {
        createServerSocketChannel(port);

        commandWriteBuffer.put(CMD_OPEN_PORT_ACK);
        commandWriteBuffer.putInt(port);
        writeCommand();
      } catch (IOException e) {
        commandWriteBuffer.put(CMD_OPEN_PORT_FAIL);
        commandWriteBuffer.putInt(port);
        writeCommand();
      }

      return true;
    } else if (cmd == CMD_CLOSE_PORT) {
      int port = commandBuffer.getInt();

      for (ServerSocketChannel channel : serverChannels.keySet()) {
        if (channel.socket().getLocalPort() == port) {
          close(channel);

          commandWriteBuffer.put(CMD_CLOSE_PORT_ACK);
          commandWriteBuffer.putInt(port);
          writeCommand();

          return true;
        }
      }

      commandWriteBuffer.put(CMD_CLOSE_PORT_FAIL);
      commandWriteBuffer.putInt(port);
      writeCommand();

      return true;
    } else if (cmd == CMD_OPEN_CHANNEL_FAIL) {
      int tunnelId = commandBuffer.getInt();
      closeTunnel(tunnelId);
      return true;
    } else {
      return super.processCommand(cmd, commandBuffer);
    }
  }

  @Override
  protected void processKey(SelectionKey key) throws IOException {
    if (key.isAcceptable()) {
      if (key.channel() == serverCommandChannel) {
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
        ByteBuffer readBuffer = pendingChannels.get(channel);
        int read = channel.read(readBuffer);
        if (read == -1) {
          throw new IOException();
        }

        if (readBuffer.limit() >= 5) {
          readBuffer.flip();
          byte cmd = readBuffer.get();
          if (cmd == CMD_OPEN_CHANNEL_ACK) {
            int tunnelId = readBuffer.getInt();
            registerLeftChannel(tunnelId, channel);
          } else {
            pendingChannels.remove(channel);
            try {
              channel.close();
            } catch (IOException e) {
              // Best effort
            }
          }
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

    try {
      serverChannels.get(serverSocketChannel).add(tunnelId);

      // Accept the connection and make it non-blocking
      SocketChannel socketChannel = serverSocketChannel.accept();
      socketChannel.configureBlocking(false);

      // Register the new SocketChannel with our Selector, indicating
      // we'd like to be notified when there's data waiting to be read
      socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

      registerRightChannel(tunnelId, socketChannel);
    } catch (IOException e) {
      serverChannels.get(serverSocketChannel).remove(tunnelId);
      closeTunnel(tunnelId);
    }

    commandWriteBuffer.put(CMD_OPEN_CHANNEL);
    commandWriteBuffer.putInt(tunnelId);
    writeCommand();
  }

  private void acceptCommand(SelectionKey key) throws IOException {
    // Accept the connection and make it non-blocking
    SocketChannel socketChannel = serverCommandChannel.accept();
    socketChannel.configureBlocking(false);

    if (commandChannel == null) {
      socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
      commandChannel = socketChannel;
    } else {
      socketChannel.register(selector, SelectionKey.OP_READ);
      pendingChannels.put(socketChannel, ByteBuffer.allocate(5));
    }
  }

  private void close(ServerSocketChannel channel) {
    for (Integer tunnelId : serverChannels.remove(channel)) {
      try {
        getTunnel(tunnelId).close();
      } catch (IOException e) {
        // Best effort
      }
    }

    try {
      channel.close();
    } catch (IOException e) {
      // Best effort
    }
  }

  private void createServerSocketChannel(int port) throws IOException {
    ServerSocketChannel channel = ServerSocketChannel.open();
    channel.configureBlocking(false);

    try {
      channel.socket().bind(new InetSocketAddress(port));
      channel.register(selector, SelectionKey.OP_ACCEPT);
    } catch (IOException e) {
      channel.close();
      // Stay silent
    }

    serverChannels.put(channel, new HashSet<Integer>());
  }
}
