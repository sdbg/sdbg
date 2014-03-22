package com.github.sdbg.debug.core.internal.forwarder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class HostReversePortForwarder extends ReversePortForwarder {
  private Object mainMonitor = new Object();

  private int commandPort;

  private Map<Integer, Object[]> forwardInfos = new HashMap<Integer, Object[]>();
  private Map<Integer, Collection<Integer>> forwards = new HashMap<Integer, Collection<Integer>>();

  private Runnable command;
  private Thread thread;

  public HostReversePortForwarder(int commandPort) {
    this.commandPort = commandPort;
  }

  public void addForward(final String host, final int port, final int devicePort) {
    execute(new Runnable() {
      @Override
      public void run() {
        forwardInfos.put(devicePort, new Object[] {host, port});
        forwards.put(devicePort, new HashSet<Integer>());
      }
    });
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

  public void removeForward(final int devicePort) {
    execute(new Runnable() {
      @Override
      public void run() {
        forwardInfos.remove(devicePort);
        for (Integer tunnelId : forwards.remove(devicePort)) {
          try {
            getTunnel(tunnelId).close();
          } catch (IOException e) {
            // Best effort
          }
        }
      }
    });
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

    for (int devicePort : forwards.keySet()) {
      removeForward(devicePort);
    }

    forwardInfos.clear();
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
        Object[] hostAndPort = forwardInfos.get(devicePort);
        int tunnelId = commandBuffer.getInt();

        ByteChannel channel = openChannel((String) hostAndPort[0], (Integer) hostAndPort[1]);
        Tunnel tunnel = createTunnel(tunnelId);
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
      while (true) { // TODO: Option to stop
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
