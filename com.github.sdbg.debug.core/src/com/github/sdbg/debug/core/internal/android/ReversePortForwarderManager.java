package com.github.sdbg.debug.core.internal.android;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.internal.forwarder.DeviceReversePortForwarder;
import com.github.sdbg.debug.core.internal.forwarder.HostReversePortForwarder;
import com.github.sdbg.debug.core.internal.forwarder.HostReversePortForwarder.Forward;
import com.github.sdbg.debug.core.util.IDeviceChooser;
import com.github.sdbg.debug.core.util.IDeviceInfo;
import com.github.sdbg.utilities.NetUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.RuntimeProcess;

public class ReversePortForwarderManager {
  private static final String APP_NAME = "com.github.sdbg.android.forwarder",
      APP_FILE_NAME = APP_NAME + ".jar", APP_DEVICE_DIR = "/sdcard/tmp", APP_DIR = APP_DEVICE_DIR
          + "/" + APP_NAME, APP_DALVIK_CACHE_DIR = APP_DIR + "/dalvik-cache",
      MAIN_CLASS_NAME = "com.github.sdbg.debug.core.internal.forwarder.DeviceReversePortForwarder";

  public ReversePortForwarderManager() {
  }

  public IProcess start(ILaunch launch, IDeviceChooser deviceChooser, int deviceCommandPort,
      List<Forward> forwards) throws CoreException {
    final ADBManager manager = new ADBManager();

    List<? extends IDeviceInfo> devices = manager.getDevices();
    if (devices.isEmpty()) {
      throw new DebugException(
          new Status(
              IStatus.ERROR,
              SDBGDebugCorePlugin.PLUGIN_ID,
              "No USB-attached Android devices found.\n\nPlease make sure you have enabled USB debugging on your device and you have attached it to the PC via USB."));
    }

    final IDeviceInfo deviceInfo = deviceChooser.chooseDevice(devices);
    if (deviceInfo == null) {
      throw new DebugException(new Status(
          IStatus.INFO,
          SDBGDebugCorePlugin.PLUGIN_ID,
          "Reverse port forwarding launch cancelled."));
    }

    pushDeviceExecutable(manager, deviceInfo.getId());

    final HostReversePortForwarder forwarder = new HostReversePortForwarder(forwards);

    IProcess process = new RuntimeProcess(launch, prepareDeviceExecutableProcess(
        manager,
        deviceInfo.getId(),
        deviceCommandPort,
        forwards), "Forwarder", Collections.<String, String> emptyMap()) {
      @Override
      protected void terminated() {
        forwarder.stop();
        manager.dispose();
        super.terminated();
      }
    };

    int hostCommandPort = NetUtils.findUnusedPort(6565);
    manager.addForward(deviceInfo.getId(), "tcp:" + hostCommandPort, "tcp:" + deviceCommandPort);

//    try {
//      Thread.sleep(2000); // TODO XXX FIXME: Get rid of that
//    } catch (InterruptedException e2) {
//    }

    while (!process.isTerminated()) {
      try {
        forwarder.connect("localhost", hostCommandPort);
        forwarder.start();
        return process;
      } catch (IOException e) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e1) {
        }
      }
    }

    manager.dispose();
    throw new DebugException(new Status(
        IStatus.ERROR,
        SDBGDebugCorePlugin.PLUGIN_ID,
        "Reverse port forwarding launch failed."));
  }

  public IProcess testStart(ILaunch launch, IDeviceChooser deviceChooser, int deviceCommandPort,
      List<Forward> forwards) throws CoreException {
    final HostReversePortForwarder forwarder = new HostReversePortForwarder(forwards);

    int[] ports = new int[forwards.size()];
    for (int i = 0; i < ports.length; i++) {
      ports[i] = forwards.get(i).getDevicePort();
    }

    final DeviceReversePortForwarder drpf = new DeviceReversePortForwarder(deviceCommandPort, ports);
    final Thread dpfThread = new Thread() {
      @Override
      public void run() {
        try {
          drpf.run();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    };

    try {
      IProcess process = new RuntimeProcess(
          launch,
          new ProcessBuilder("notepad").start(),
          "Forwarder",
          Collections.<String, String> emptyMap()) {
        @Override
        protected void terminated() {
          forwarder.stop();
          try {
            dpfThread.join();
          } catch (InterruptedException e) {
          }
          super.terminated();
        }
      };

      dpfThread.start();

      int hostCommandPort = 6565;

      try {
        Thread.sleep(2000); // TODO XXX FIXME: Get rid of that
      } catch (InterruptedException e2) {
      }

      while (!process.isTerminated()) {
        try {
          forwarder.connect("localhost", hostCommandPort);
          forwarder.start();
          return process;
        } catch (IOException e) {
          try {
            Thread.sleep(100);
          } catch (InterruptedException e1) {
          }
        }
      }
    } catch (IOException e) {
      try {
        dpfThread.join();
      } catch (InterruptedException e1) {
      }

      throw new DebugException(new Status(
          IStatus.ERROR,
          SDBGDebugCorePlugin.PLUGIN_ID,
          e.getMessage(),
          e));
    }

    throw new DebugException(new Status(
        IStatus.ERROR,
        SDBGDebugCorePlugin.PLUGIN_ID,
        "Reverse port forwarding launch failed."));
  }

  private Process prepareDeviceExecutableProcess(ADBManager manager, String deviceId,
      int deviceCommandPort, List<Forward> forwards) throws CoreException {
    StringBuilder devicePortsStr = new StringBuilder();
    for (Forward forward : forwards) {
      devicePortsStr.append(" ");
      devicePortsStr.append(forward.getDevicePort());
    }

    return manager.asyncShell(deviceId, "export ANDROID_DATA=" + APP_DIR + ";dalvikvm -cp "
        + APP_DIR + "/" + APP_FILE_NAME + " " + MAIN_CLASS_NAME + " " + deviceCommandPort + " "
        + devicePortsStr);
  }

  private void pushDeviceExecutable(ADBManager manager, String deviceId) throws CoreException {
    try {
      manager.shell(deviceId, "mkdir " + APP_DIR);
      manager.shell(deviceId, "mkdir " + APP_DALVIK_CACHE_DIR);

      File tempFile = File.createTempFile("sdbg", "sdbg");

      try {
        OutputStream out = new FileOutputStream(tempFile);

        try {
          InputStream in = SDBGDebugCorePlugin.getPlugin().getBundle().getResource(
              "resources/" + APP_FILE_NAME).openStream();

          try {
            byte[] buf = new byte[8192];
            for (int read = -1; (read = in.read(buf)) > -1;) {
              out.write(buf, 0, read);
            }
          } finally {
            in.close();
          }
        } finally {
          out.close();
        }

        manager.push(deviceId, tempFile, APP_DIR + "/" + APP_FILE_NAME);
      } finally {
        tempFile.delete();
      }
    } catch (IOException e) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          SDBGDebugCorePlugin.PLUGIN_ID,
          e.toString(),
          e));
    }
  }
}
