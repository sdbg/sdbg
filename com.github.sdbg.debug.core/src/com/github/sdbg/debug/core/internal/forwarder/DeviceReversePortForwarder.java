package com.github.sdbg.debug.core.internal.forwarder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectableChannel;
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
  public static void main(String[] args) throws IOException {
    int commandPort = Integer.parseInt(args[0]);
    int[] ports = new int[args.length - 1];
    for (int i = 1; i < args.length; i++) {
      ports[i] = Integer.parseInt(args[i - 1]);
    }

    new DeviceReversePortForwarder(commandPort, ports).run();
  }

  private int commandPort;
  private int[] ports;

  private ServerSocketChannel commandServerChannel;
  private Map<ServerSocketChannel, Collection<Integer>> serverChannels = new HashMap<ServerSocketChannel, Collection<Integer>>();
  private Map<ByteChannel, ByteBuffer> pendingChannels = new HashMap<ByteChannel, ByteBuffer>();

  public DeviceReversePortForwarder(int commandPort, int[] ports) {
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
    for (ByteChannel channel : new ArrayList<ByteChannel>(pendingChannels.keySet())) {
      close(channel);
    }
    pendingChannels.clear();

    for (ServerSocketChannel channel : new ArrayList<ServerSocketChannel>(serverChannels.keySet())) {
      close(channel);
    }
    serverChannels.clear();

    ((SelectableChannel) commandChannel).keyFor(selector).cancel();
    try {
      commandChannel.close();
    } catch (IOException e) {
    }
    commandChannel = null;

    commandServerChannel.keyFor(selector).cancel();
    try {
      commandServerChannel.close();
    } catch (IOException e) {
    }
    commandServerChannel = null;

    super.done();
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
            throw new IOException();
          }

          if (readBuffer.limit() >= 5) {
            readBuffer.flip();
            byte cmd = readBuffer.get();
            if (cmd == CMD_OPEN_CHANNEL_ACK) {
              registerLeftChannel(readBuffer.getInt(), channel);
              pendingChannels.remove(channel);
            } else {
              throw new IOException();
            }
          }
        } catch (IOException e) {
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
    trace("New incoming connection to device port " + serverSocketChannel.socket().getLocalPort());

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
    SocketChannel socketChannel = commandServerChannel.accept();
    socketChannel.configureBlocking(false);

    if (commandChannel == null) {
      socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
      commandChannel = socketChannel;

      // Now that the command channel is opened we can start accepting connections from the other channels 
      for (ServerSocketChannel channel : serverChannels.keySet()) {
        channel.keyFor(selector).interestOps(SelectionKey.OP_ACCEPT);
      }
    } else {
      socketChannel.register(selector, SelectionKey.OP_READ);
      pendingChannels.put(socketChannel, ByteBuffer.allocate(5));
    }
  }

  private void close(ByteChannel channel) {
    pendingChannels.remove(channel);
    ((SelectableChannel) channel).keyFor(selector).cancel();
    try {
      channel.close();
    } catch (IOException e) {
      // Best effort
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

    channel.keyFor(selector).cancel();

    try {
      channel.close();
    } catch (IOException e) {
      // Best effort
    }
  }

  private void createCommandServerSocketChannel() throws IOException {
    ServerSocketChannel channel = ServerSocketChannel.open();

    channel.configureBlocking(false);

    channel.socket().bind(new InetSocketAddress(commandPort));
    channel.register(selector, SelectionKey.OP_ACCEPT);

    commandServerChannel = channel;
    trace("Listening for commands on device port " + commandPort);
  }

  private void createServerSocketChannel(int port) throws IOException {
    ServerSocketChannel channel = ServerSocketChannel.open();
    serverChannels.put(channel, new HashSet<Integer>());

    channel.configureBlocking(false);

    channel.socket().bind(new InetSocketAddress(port));
    trace("Reverse proxy port " + port + " opened on the device");
  }
}
