package com.github.sdbg.debug.core.internal.android;

import com.github.sdbg.core.DartCore;
import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.utilities.ProcessRunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

public class ADBManager {
  private static interface Converter<T> {
    T convert(String line) throws IOException, CoreException;
  }

  private static File adbExecutable;

  private static void copy(String resource, File toDir) throws IOException {
    InputStream in = SDBGDebugCorePlugin.getPlugin().getBundle().getResource(resource).openStream();

    try {
      toDir.mkdirs();

      int pos = resource.lastIndexOf('/');
      String fileName = pos >= 0 ? resource.substring(pos) : resource;

      OutputStream out = new FileOutputStream(new File(toDir, fileName));

      try {
        byte[] buf = new byte[4086];
        for (int read = -1; (read = in.read(buf)) >= 0;) {
          out.write(buf, 0, read);
        }
      } finally {
        out.close();
      }
    } finally {
      in.close();
    }
  }

  private static synchronized File getAdbExecutable() throws IOException {
    if (adbExecutable == null) {
      String platform = Platform.getOS() + "_" + Platform.getOSArch();
      String platformDir = "/resources/adb/" + platform + "/";

      File adbDir = new File(new File(
          new File(new File(System.getProperty("user.home")), ".sdbg"),
          "adb"), platform);
      String adbExecutableName = "adb" + (DartCore.isWindows() ? ".exe" : "");

      if (platform.equals("win32_x86") || platform.equals("win32_x86_64")) {
        copy(platformDir + adbExecutableName, adbDir);
        copy(platformDir + "AdbWinApi.dll", adbDir);
        copy(platformDir + "AdbWinUsbApi.dll", adbDir);
      } else if (platform.equals("macosx_x86_64") || platform.equals("linux_x86")
          || platform.equals("linux_x86_64")) {
        copy(platformDir + adbExecutableName, adbDir);
        new ProcessRunner(new ProcessBuilder("chmod", "+x", adbExecutable.getAbsolutePath())).runSync(null);
      } else {
        throw new IOException("Unsupported platform: " + platform);
      }

      adbExecutable = new File(adbDir, adbExecutableName);
    }

    return adbExecutable;
  }

  private Collection<String> forwards = new HashSet<String>();

  public ADBManager() {
  }

  public void addChromiumForward(String deviceId, int localTCPPort) throws CoreException {
    addForward(
        deviceId,
        "tcp:" + Integer.toString(localTCPPort),
        "localabstract:chrome_devtools_remote");
  }

  public void addForward(String deviceId, String local, String remote) throws CoreException {
    String forward = deviceId + "=" + local;
    if (!forwards.contains(forward)) {
      executeADB("-s", deviceId, "forward", local, remote);
      forwards.add(forward);
    }
  }

  public void addForwardNoRebind(String deviceId, String local, String remote) throws CoreException {
    String forward = deviceId + "=" + local;
    if (!forwards.contains(forward)) {
      executeADB("-s", deviceId, "forward", "--no-rebind", local, remote);
      forwards.add(forward);
    }
  }

  public Process asyncShell(String deviceId, String command) throws CoreException {
    try {
      return prepareADB("-s", deviceId, "shell", command).start();
    } catch (IOException e) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          SDBGDebugCorePlugin.PLUGIN_ID,
          e.toString(),
          e));
    }
  }

  public void dispose() {
    removeAllForwards();
  }

  public List<ADBDeviceInfo> getDevices() throws CoreException {
    return getData(new Converter<ADBDeviceInfo>() {
      @Override
      public ADBDeviceInfo convert(String line) throws CoreException {
        if (!line.startsWith("*") && !line.contains("List of devices attached")) {
          StringTokenizer stok = new StringTokenizer(line, " \t");
          if (stok.hasMoreTokens()) {
            String id = stok.nextToken();
            List<String> data = getData(null, "-s", id, "shell", "getprop", "ro.product.model");
            return new ADBDeviceInfo(id, data.isEmpty() ? null : data.get(0));
          }
        }

        return null;
      }
    }, "devices");
  }

  public void pull(String deviceId, String remote, File fileOrDir) throws CoreException {
    executeADB("-s", deviceId, "pull", remote, fileOrDir.getAbsolutePath());
  }

  public void push(String deviceId, File fileOrDir, String remote) throws CoreException {
    executeADB("-s", deviceId, "push", fileOrDir.getAbsolutePath(), remote);
  }

  public void removeAllForwards() {
    while (!forwards.isEmpty()) {
      String forward = forwards.iterator().next();
      String deviceId = forward.substring(0, forward.indexOf('='));
      String local = forward.substring(forward.indexOf('=') + 1);
      try {
        removeForward(deviceId, local);
      } catch (CoreException e) {
        // Best effort
        SDBGDebugCorePlugin.logError(e);
      }
    }
  }

  public void removeForward(String deviceId, String local) throws CoreException {
    String forward = deviceId + "=" + local;
    if (forwards.contains(forward)) {
      try {
        executeADB("-s", deviceId, "forward", "--remove", local);
      } finally {
        forwards.remove(forward);
      }
    }
  }

  public void removeTCPForward(String deviceId, int localTCPPort) throws CoreException {
    removeForward(deviceId, "tcp:" + Integer.toString(localTCPPort));
  }

  public void shell(String deviceId, String command) throws CoreException {
    executeADB("-s", deviceId, "shell", command);
  }

  private String executeADB(String... arguments) throws CoreException {
    try {
      ProcessRunner runner = new ProcessRunner(prepareADB(arguments));
      runner.runSync(null);

      if (runner.getExitCode() != 0) {
        throw new IOException("ADB returned unexpected error code: " + runner.getExitCode()
            + "\n\nERR STREAM: " + runner.getStdErr());
      }

      return runner.getStdOut();
    } catch (IOException e) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          SDBGDebugCorePlugin.PLUGIN_ID,
          e.toString(),
          e));
    }
  }

  private <T> List<T> getData(Converter<T> converter, String... commands) throws CoreException {
    try {
      List<T> data = new ArrayList<T>();
      BufferedReader reader = new BufferedReader(new StringReader(executeADB(commands)));
      for (String line = reader.readLine(); line != null; line = reader.readLine()) {
        @SuppressWarnings("unchecked")
        T entry = converter != null ? converter.convert(line.trim()) : (T) line.trim();
        if (entry != null) {
          data.add(entry);
        }
      }

      return data;
    } catch (IOException e) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          SDBGDebugCorePlugin.PLUGIN_ID,
          e.toString(),
          e));
    }
  }

  private ProcessBuilder prepareADB(String... arguments) throws CoreException {
    try {
      List<String> cmdLine = new ArrayList<String>();
      cmdLine.add(getAdbExecutable().getAbsolutePath());
      cmdLine.addAll(Arrays.asList(arguments));

      return new ProcessBuilder(cmdLine);
    } catch (IOException e) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          SDBGDebugCorePlugin.PLUGIN_ID,
          e.toString(),
          e));
    }
  }
}
