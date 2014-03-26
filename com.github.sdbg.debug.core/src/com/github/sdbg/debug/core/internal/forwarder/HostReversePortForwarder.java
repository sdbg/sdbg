package com.github.sdbg.debug.core.internal.forwarder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HostReversePortForwarder extends ReversePortForwarder {
  public static class Forward {
    private String host;
    private int port;
    private int devicePort;

    public Forward(String host, int port, int devicePort) {
      this.host = host;
      this.port = port;
      this.devicePort = devicePort;
    }

    public int getDevicePort() {
      return devicePort;
    }

    public String getHost() {
      return host;
    }

    public int getPort() {
      return port;
    }
  }

  private Map<Integer, Forward> forwards = new HashMap<Integer, Forward>();

  private Object mainMonitor = new Object();

  private boolean stopRequest;
  private Thread thread;

  public HostReversePortForwarder(Forward... forwards) {
    this(Arrays.asList(forwards));
  }

  public HostReversePortForwarder(List<Forward> forwards) {
    for (Forward forward : forwards) {
      this.forwards.put(forward.getDevicePort(), forward);
    }
  }

  public void connect(String commandHost, int commandPort) throws IOException {
    // Create a new non-blocking socket channel
    commandChannel = SocketChannel.open(commandHost != null ? new InetSocketAddress(
        commandHost,
        commandPort) : new InetSocketAddress(commandPort));

    ((SelectableChannel) commandChannel).configureBlocking(false);
  }

  public boolean isConnected() {
    return commandChannel != null;
  }

  public void start() throws IOException {
    if (thread != null) {
      throw new IOException("Already started");
    }

    try {
      init();
    } catch (IOException e) {
      done();
      throw e;
    }

    thread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          HostReversePortForwarder.this.run();
        } catch (IOException e) {
          //throw new RuntimeException(e);
        }
      }
    }, "Host Reverse Forwarder");

    thread.start();
  }

  public void stop() {
    if (thread != null) {
      try {
        synchronized (mainMonitor) {
          if (!stopRequest) {
            stopRequest = true;
            selector.wakeup();

            try {
              mainMonitor.wait();
            } catch (InterruptedException e) {
            }
          }
        }

        try {
          thread.join();
        } catch (InterruptedException e) {
        }

        thread = null;
      } finally {
        done();
      }
    } else if (commandChannel != null) {
      try {
        commandChannel.close();
      } catch (IOException e) {
      }

      commandChannel = null;
    }

  }

  @Override
  protected void done() {
    if (commandChannel != null) {
      try {
        commandChannel.close();
      } catch (IOException e) {
      }

      commandChannel = null;
    }

    super.done();
    forwards.clear();
  }

  @Override
  protected void init() throws IOException {
    if (commandChannel == null) {
      throw new IOException("Unexpected");
    }

    super.init();

    ((SelectableChannel) commandChannel).register(selector, SelectionKey.OP_READ
        | SelectionKey.OP_WRITE);
  }

  @Override
  protected boolean processCommand(byte cmd, ByteBuffer commandBuffer) throws IOException {
    if (cmd == CMD_OPEN_CHANNEL) {
      if (commandBuffer.remaining() >= 8) {
        int devicePort = commandBuffer.getInt();
        Forward forward = forwards.get(devicePort);
        int tunnelId = commandBuffer.getInt();

        Tunnel tunnel = createTunnel(tunnelId);

        try {
          registerLeftChannel(tunnelId, openChannel(forward.getHost(), forward.getPort()));
          registerRightChannel(
              tunnelId,
              openChannel("localhost", ((SocketChannel) commandChannel).socket().getPort())); // TODO XXX FIXME

          tunnel.getLeftToRight().put(CMD_OPEN_CHANNEL_ACK);
          tunnel.getLeftToRight().putInt(tunnelId);

          if (!Tunnel.spool(
              selector,
              tunnel.getLeftChannel(),
              tunnel.getRightChannel(),
              tunnel.getLeftToRight())) {
            trace("Tunnel " + tunnelId + " closed");
            closeTunnel(tunnelId);
          }
        } catch (IOException e) {
          trace("IO spooling error: " + e.getMessage());
          closeTunnel(tunnelId);
          commandWriteBuffer.put(CMD_OPEN_CHANNEL_FAIL);
          commandWriteBuffer.putInt(tunnelId);
          writeCommand();
        }

        return true;
      } else {
        return false;
      }
    } else {
      return super.processCommand(cmd, commandBuffer);
    }
  }

  private ByteChannel openChannel(String host, int port) throws IOException {
    SocketChannel channel = SocketChannel.open(new InetSocketAddress(host, port));
    channel.configureBlocking(false);

    return channel;
  }

  private void run() throws IOException {
    try {
      while (true) {
        // Wait for an event one of the registered channels
        selector.select();

        synchronized (mainMonitor) {
          if (stopRequest) {
            break;
          }
        }

        for (Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator(); selectedKeys.hasNext();) {
          SelectionKey key = selectedKeys.next();
          selectedKeys.remove();

          if (key.isValid()) {
            processKey(key);
          }
        }
      }
    } finally {
      synchronized (mainMonitor) {
        stopRequest = true;
        mainMonitor.notify();
      }
    }
  }
}
