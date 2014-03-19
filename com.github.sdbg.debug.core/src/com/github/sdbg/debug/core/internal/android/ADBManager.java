package com.github.sdbg.debug.core.internal.android;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.utilities.ProcessRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class ADBManager {
  private static interface Converter<T> {
    T convert(String line) throws IOException, CoreException;
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
      try {
        executeADB("-s", deviceId, "forward", local, remote);
      } finally {
        forwards.add(forward);
      }
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
            List<String> data = getData(
                null,
                "-s",
                id,
                "shell",
                "getprop",
                "ro.build.version.release");
            return new ADBDeviceInfo(id, data.isEmpty() ? null : data.get(0));
          }
        }

        return null;
      }
    },
        "devices");
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

  private String executeADB(String... arguments) throws CoreException {
    try {
      List<String> cmdLine = new ArrayList<String>();
      cmdLine.add("C:\\Users\\ivan\\AppData\\Local\\Applications\\adt\\sdk\\platform-tools\\adb.exe");
      cmdLine.addAll(Arrays.asList(arguments));

      ProcessRunner runner = new ProcessRunner(new ProcessBuilder(cmdLine));
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
}
