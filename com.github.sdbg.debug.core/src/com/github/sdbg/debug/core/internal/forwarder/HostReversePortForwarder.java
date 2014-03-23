package com.github.sdbg.debug.core.internal.forwarder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
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

  private int commandPort;
  private Map<Integer, Forward> forwards = new HashMap<Integer, Forward>();

  private Object mainMonitor = new Object();

  private Runnable command;
  private Thread thread;

  public HostReversePortForwarder(int commandPort, Forward... forwards) {
    this.commandPort = commandPort;
    for (Forward forward : forwards) {
      this.forwards.put(forward.getDevicePort(), forward);
    }
  }

  public void dispose() {
    execute(new Runnable() {
      @Override
      public void run() {
        throw new RuntimeException("Dispose");
      }
    });

    try {
      thread.join();
    } catch (InterruptedException e) {
    }
  }

  public void start() {
    thread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          HostReversePortForwarder.this.run();
        } catch (IOException e) {
        }
      }
    }, "Host Reverse Forwarder Thread");
  }

  @Override
  protected void done() {
    super.done();
    forwards.clear();
  }

  @Override
  protected void init() throws IOException {
    super.init();

    // Create a new non-blocking socket channel
    commandChannel = SocketChannel.open(new InetSocketAddress(commandPort));
  }

  @Override
  protected boolean processCommand(byte cmd, ByteBuffer commandBuffer) throws IOException {
    if (cmd == CMD_OPEN_CHANNEL) {
      if (commandBuffer.limit() >= 4) {
        int devicePort = commandBuffer.getInt();
        Forward forward = forwards.get(devicePort);
        int tunnelId = commandBuffer.getInt();

        Tunnel tunnel = createTunnel(tunnelId);

        try {
          ByteChannel channel = openChannel(forward.getHost(), forward.getPort());
          tunnel.setLeftChannel(channel);

          ByteChannel rightChannel = openChannel("localhost", devicePort);
          tunnel.setRightChannel(rightChannel);

          tunnel.getLeftToRight().put(CMD_OPEN_CHANNEL_ACK);
          tunnel.getLeftToRight().putInt(devicePort);
          Tunnel.spool(
              selector,
              tunnel.getLeftChannel(),
              tunnel.getRightChannel(),
              tunnel.getLeftToRight());
        } catch (IOException e) {
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

  private void execute(final Runnable command) {
    synchronized (mainMonitor) {
      this.command = new Runnable() {
        @Override
        public void run() {
          synchronized (mainMonitor) {
            try {
              command.run();
            } finally {
              mainMonitor.notify();
            }
          }
        }
      };

      selector.wakeup();

      try {
        mainMonitor.wait();
      } catch (InterruptedException e) {
      }
    }
  }

  private ByteChannel openChannel(String host, int port) throws IOException {
    return SocketChannel.open(new InetSocketAddress(host, port));
  }

  private void run() throws IOException {
    init();

    try {
      while (true) {
        // Wait for an event one of the registered channels
        selector.select();

        synchronized (mainMonitor) {
          if (command != null) {
            try {
              command.run();
            } finally {
              command = null;
            }
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
      done();
    }
  }
}
