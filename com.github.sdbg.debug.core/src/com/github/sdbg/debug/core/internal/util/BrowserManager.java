/*
 * Copyright (c) 2012, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.sdbg.debug.core.internal.util;

import com.github.sdbg.core.DartCore;
import com.github.sdbg.debug.core.DebugUIHelper;
import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.SDBGLaunchConfigWrapper;
import com.github.sdbg.debug.core.internal.android.ADBManager;
import com.github.sdbg.debug.core.internal.android.MobileBrowserUtils;
import com.github.sdbg.debug.core.internal.util.ListeningStream.StreamListener;
import com.github.sdbg.debug.core.internal.webkit.model.WebkitDebugTarget;
import com.github.sdbg.debug.core.internal.webkit.protocol.ChromiumConnector;
import com.github.sdbg.debug.core.internal.webkit.protocol.ChromiumTabInfo;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitConnection;
import com.github.sdbg.debug.core.model.IResourceResolver;
import com.github.sdbg.debug.core.util.IBrowserTabChooser;
import com.github.sdbg.debug.core.util.IBrowserTabInfo;
import com.github.sdbg.debug.core.util.IDeviceChooser;
import com.github.sdbg.debug.core.util.IDeviceInfo;
import com.github.sdbg.debug.core.util.Trace;
import com.github.sdbg.utilities.NetUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;

/**
 * A manager that launches and manages configured browsers. TODO: Move to the webkit packages as
 * this code is Chromium specific. The best we can do is to generalize it a bit so that it works for
 * Safari & Opera as well, but that's it.
 */
public class BrowserManager {
  /** The initial page to navigate to. */
  private static final String INITIAL_PAGE = "chrome://version/";

  private static final int DEVTOOLS_PORT_NUMBER = 9322;

  private static final String CHROME_EXECUTABLE_PROPERTY = "chrome.location",
      CHROME_ENVIRONMENT_VARIABLE = "CHROME_LOCATION";

  private String browserDataDirName;

  private Semaphore launchSemaphore;

  private ADBManager adbManager;

  private Process browserProcess;

  private File browserExecutable;

  public BrowserManager(String browserDataDirName) {
    this.browserDataDirName = browserDataDirName;
    this.launchSemaphore = new Semaphore(1);
    this.adbManager = new ADBManager();
  }

  private List<String> buildArgumentsList(SDBGLaunchConfigWrapper launchConfig, String url,
      int devToolsPortNumber, List<String> extraArguments) {
    List<String> arguments = new ArrayList<String>();

    arguments.add(getBrowserExecutable().getAbsolutePath());

    if (devToolsPortNumber > -1) {
      // Enable remote debug over HTTP on the specified port.
      arguments.add("--remote-debugging-port=" + devToolsPortNumber);
    }

    // In order to start up multiple Chrome processes, we need to specify a different user dir.
    arguments.add("--user-data-dir="
        + getCreateUserDataDirectory(browserDataDirName).getAbsolutePath());

    // Whether or not it's actually the first run.
    arguments.add("--no-first-run");

    // Disables the default browser check.
    arguments.add("--no-default-browser-check");

    // Bypass the error dialog when the profile lock couldn't be attained.
    arguments.add("--no-process-singleton-dialog");

    arguments.addAll(extraArguments);

    for (String arg : launchConfig.getArgumentsAsArray()) {
      arguments.add(arg);
    }

    if (url != null) {
      arguments.add(url);
    }

    return arguments;
  }

  public WebkitDebugTarget connect(ILaunch launch, ILaunchConfiguration configuration,
      IResourceResolver resourceResolver, IBrowserTabChooser browserTabChooser, String host,
      int port, IProgressMonitor monitor) throws CoreException {
    try {
      SDBGLaunchConfigWrapper launchConfig = new SDBGLaunchConfigWrapper(configuration);

      LogTimer timer = new LogTimer("Chrome debug connect");

      try {
        timer.startTask("connect");

        try {
          launchConfig.markAsLaunched();

          return connectToChromiumDebug(
              "Chrome Remote Connection",
              launch,
              launchConfig,
              null/*url*/,
              monitor,
              null/*runtimeProcess*/,
              timer,
              true/*enableBreakpoints*/,
              host,
              port,
              0L/*maxStartupDelay*/,
              null/*browserOutput*/,
              null/*processDescription*/,
              resourceResolver,
              browserTabChooser,
              true/*remote*/);
        } finally {
          timer.stopTask();
        }
      } finally {
        timer.stopTimer();
      }
    } catch (CoreException e) {
      DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);
      throw e;
    }
  }

  public WebkitDebugTarget connect(ILaunch launch, ILaunchConfiguration configuration,
      IResourceResolver resourceResolver, IDeviceChooser deviceChooser,
      IBrowserTabChooser browserTabChooser, IProgressMonitor monitor) throws CoreException {
    try {
      SDBGLaunchConfigWrapper launchConfig = new SDBGLaunchConfigWrapper(configuration);

      LogTimer timer = new LogTimer("Chrome debug connect");

      try {
        timer.startTask("connect");

        try {
          launchConfig.markAsLaunched();

          List<? extends IDeviceInfo> devices = adbManager.getDevices();
          if (devices.isEmpty()) {
            throw new DebugException(
                new Status(
                    IStatus.ERROR,
                    SDBGDebugCorePlugin.PLUGIN_ID,
                    "No USB-attached Android devices found.\n\nPlease make sure you have enabled USB debugging on your device and you have attached it to the PC via USB."));
          }

          IDeviceInfo device = deviceChooser.chooseDevice(devices);
          if (device != null) {
            int port = NetUtils.findUnusedPort(DEVTOOLS_PORT_NUMBER);
            MobileBrowserUtils.addChromiumForward(adbManager, device.getId(), port);

            try {
              final String url = launchConfig.getUrl();
              boolean launchTab = launchConfig.isLaunchTabWithUrl() && url != null
                  && url.length() > 0;

              if (launchTab) {
                MobileBrowserUtils.launchChromeBrowser(adbManager, device.getId());

                IBrowserTabInfo tab = getChromiumTab(
                    null/*runtimeProcess*/,
                    new IBrowserTabChooser() {
                      @Override
                      public IBrowserTabInfo chooseTab(List<? extends IBrowserTabInfo> tabs)
                          throws CoreException {
                        for (IBrowserTabInfo tab : tabs) {
                          if (tab.getUrl() != null
                              && tab.getUrl().toLowerCase().contains(url.toLowerCase())) {
                            return tab;
                          }
                        }

                        return null;
                      }
                    },
                    "127.0.0.1",
                    port,
                    10 * 1000L/*maxStartupDelay*/,
                    null/*output*/);

                if (tab == null) {
                  MobileBrowserUtils.launchChromeBrowser(
                      adbManager,
                      device.getId(),
                      launchConfig.getUrl());
                }
              }

              return connectToChromiumDebug(
                  "Mobile Chrome Remote Connection",
                  launch,
                  launchConfig,
                  null/*url*/,
                  monitor,
                  null/*runtimeProcess*/,
                  timer,
                  true/*enableBreakpoints*/,
                  "127.0.0.1",
                  port,
                  launchTab ? 10 * 1000L : 0L/*maxStartupDelay*/,
                  null/*browserOutput*/,
                  null/*processDescription*/,
                  resourceResolver,
                  browserTabChooser,
                  true/*remote*/);
            } catch (IOException e) {
              adbManager.removeAllForwards();
              throw new CoreException(new Status(
                  IStatus.ERROR,
                  SDBGDebugCorePlugin.PLUGIN_ID,
                  "Unable to connect to debugger in Chrome: " + e.getMessage(),
                  e));
            } catch (CoreException e) {
              adbManager.removeAllForwards();
              throw e;
            }
          } else {
            throw new DebugException(new Status(
                IStatus.INFO,
                SDBGDebugCorePlugin.PLUGIN_ID,
                "No Android device was chosen. Connection cancelled."));
          }
        } finally {
          timer.stopTask();
        }
      } finally {
        timer.stopTimer();
      }
    } catch (CoreException e) {
      DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);
      throw e;
    }
  }

  private WebkitDebugTarget connectToChromiumDebug(String browserName, ILaunch launch,
      SDBGLaunchConfigWrapper launchConfig, String url, IProgressMonitor monitor,
      Process runtimeProcess, LogTimer timer, boolean enableBreakpoints, String host, int port,
      long maxStartupDelay, ListeningStream browserOutput, String processDescription,
      IResourceResolver resolver, IBrowserTabChooser browserTabChooser, boolean remote)
      throws CoreException {
    monitor.worked(1);

    // avg: 383ms
    timer.startTask("get chromium tabs");

    ChromiumTabInfo tab;

    try {
      tab = getChromiumTab(
          runtimeProcess,
          browserTabChooser,
          host,
          port,
          maxStartupDelay,
          browserOutput);
    } catch (IOException e) {
      SDBGDebugCorePlugin.logError(e);
      throw new CoreException(new Status(
          IStatus.ERROR,
          SDBGDebugCorePlugin.PLUGIN_ID,
          "Unable to connect to Chrome at address " + (host != null ? host : "") + ":" + port
              + "; error: " + e.getMessage(),
          e));
    }

    monitor.worked(2);

    timer.stopTask();

    // avg: 46ms
    timer.startTask("open WIP connection");

    if (tab == null) {
      throw new DebugException(new Status(
          IStatus.INFO,
          SDBGDebugCorePlugin.PLUGIN_ID,
          "No Chrome tab was chosen. Connection cancelled."));
    }

    if (tab.getWebSocketDebuggerUrl() == null) {
      throw new DebugException(
          new Status(
              IStatus.ERROR,
              SDBGDebugCorePlugin.PLUGIN_ID,
              "Unable to connect to Chrome"
                  + (remote
                      ? ".\n\nPossible reason: another debugger (e.g. Chrome DevTools) or another Eclipse debugging session is already attached to that particular Chrome tab."
                      : "")));
    }

    // Even when Chrome has reported all the debuggable tabs to us, the debug server
    // may not yet have started up. Delay a small fixed amount of time.
    sleep(100);

    if (resolver == null) {
      resolver = createResourceResolver(launch, launchConfig.getConfig(), tab);
    }

    try {
      WebkitConnection connection = new WebkitConnection(
          tab.getHost(),
          tab.getPort(),
          tab.getWebSocketDebuggerFile());

      final WebkitDebugTarget debugTarget = new WebkitDebugTarget(
          browserName,
          connection,
          launch,
          runtimeProcess,
          launchConfig.getProject(),
          resolver,
          adbManager,
          enableBreakpoints,
          remote);

      monitor.worked(1);

      registerProcess(launch, launchConfig, debugTarget.getProcess(), processDescription);
      launch.addDebugTarget(debugTarget);

      if (browserOutput != null && launchConfig.getShowLaunchOutput()) {
        browserOutput.setListener(new StreamListener() {
          @Override
          public void handleStreamData(String data) {
            debugTarget.writeToStdout(data);
          }
        });
      }

      if (browserOutput != null) {
        debugTarget.openConnection(url, true);
      } else {
        debugTarget.openConnection();
      }

      trace("Connected to WIP debug agent on host " + host + " and port " + port);

      timer.stopTask();

      monitor.worked(1);

      return debugTarget;
    } catch (IOException e) {
      SDBGDebugCorePlugin.logError(e);
      throw new CoreException(new Status(
          IStatus.ERROR,
          SDBGDebugCorePlugin.PLUGIN_ID,
          "Unable to connect to Chrome tab at address "
              + (tab.getHost() != null ? tab.getHost() : "") + ":" + tab.getPort() + " ("
              + tab.getWebSocketDebuggerFile() + "): " + e.getMessage(),
          e));
    }
  }

  // Only exists to support the funky Chrome Application launch
  protected IResourceResolver createResourceResolver(ILaunch launch,
      ILaunchConfiguration configuration, IBrowserTabInfo tab) {
    throw new UnsupportedOperationException("Not implemented");
  }

  private void describe(List<String> arguments, StringBuilder builder) {
    for (int i = 0; i < arguments.size(); i++) {
      if (i > 0) {
        builder.append(" ");
      }
      builder.append(arguments.get(i));
    }
  }

  public void dispose() {
    if (!isProcessTerminated(browserProcess)) {
      browserProcess.destroy();
    }
  }

  private File findChromeExecutable() {
    // First, try the system property, as user-specified value is preferred
    File file = findChromeExecutable(
        "system property " + CHROME_EXECUTABLE_PROPERTY,
        System.getProperty(CHROME_EXECUTABLE_PROPERTY),
        true/*fileOrDir*/);
    if (file != null) {
      return file;
    }

    // Second, try the environment variable
    file = findChromeExecutable(
        "environment vairable " + CHROME_ENVIRONMENT_VARIABLE,
        System.getenv(CHROME_ENVIRONMENT_VARIABLE),
        true/*fileOrDir*/);
    if (file != null) {
      return file;
    }

    // On Windows, try to locate Chrome using the Uninstall windows Registry setting
    // If unsuccessful, try a heuristics in the user home directory
    if (DartCore.isWindows()) {
      try {
        String regKey = "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Google Chrome";
        String regValue = "InstallLocation";
        file = findChromeExecutable(
            "registry setting " + regKey + "[" + regValue + "]",
            WinReg.readRegistry(regKey, regValue),
            false/*fileOrDir*/);
        if (file != null) {
          return file;
        }

        // Try also two other locations, as described here:
        // http://stackoverflow.com/questions/24149207/what-windows-registry-key-holds-the-path-to-the-chrome-browser-exe

        // In case Chrome x86 is installed on a x64 machine:
        regKey = "HKEY_LOCAL_MACHINE\\SOFTWARE\\Wow6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Google Chrome";
        regValue = "InstallLocation";
        file = findChromeExecutable(
            "registry setting " + regKey + "[" + regValue + "]",
            WinReg.readRegistry(regKey, regValue),
            false/*fileOrDir*/);
        if (file != null) {
          return file;
        }

        // General fallback
        regKey = "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\chrome.exe";
        regValue = "Path";
        file = findChromeExecutable(
            "registry setting " + regKey + "[" + regValue + "]",
            WinReg.readRegistry(regKey, regValue),
            false/*fileOrDir*/);
        if (file != null) {
          return file;
        }
      } catch (IOException e) {
        // Stay silent
        SDBGDebugCorePlugin.logError(e);
      }

      // Heuristically search in user's home directory
      File userHome = new File(System.getProperty("user.home"));

      // Windows 7
      file = findChromeExecutable("heuristics", new File(
          userHome,
          "AppData\\Local\\Google\\Chrome\\Application"), false/*fileOrDir*/);
      if (file != null) {
        return file;
      }

      // XP
      file = findChromeExecutable("heuristics", new File(
          userHome,
          "Local Settings\\Application Data\\Google\\Chrome\\Application"), false/*fileOrDir*/);
      if (file != null) {
        return file;
      }
    } else if (DartCore.isMac()) {
      file = findChromeExecutable("heuristics", new File("/Applications"), false/*fileOrDir*/);
      if (file != null) {
        return file;
      }
    } else {
      // Search $PATH
      String path = System.getenv("PATH");
      if (path != null) {
        for (String dirStr : path.split(File.pathSeparator)) {
          file = findChromeExecutable("$PATH entry", dirStr, false/*fileOrDir*/);
          if (file != null) {
            return file;
          }
        }
      }
    }

    throw new RuntimeException("Uable to locate the Chrome executable at the usual locations.\n"
        + "Please append at the end of your eclipse.ini file the following property:\n" + "-D"
        + CHROME_EXECUTABLE_PROPERTY + "=<path-to-chrome-executable>");
  }

  private File findChromeExecutable(String option, File location, boolean fileOrDir) {
    trace("Trying to load Chrome from " + option + "=" + location.getPath());
    if (!location.exists()) {
      trace("=> Failed, location does not exist");
    } else if (location.isFile()) {
      if (fileOrDir) {
        trace("=> Found, location is a file, this is assumed to be the Chrome executable");
        return location;
      } else {
        trace("=> Failed, location is a file");
        return null;
      }
    } else if (DartCore.isWindows()) {
      File exe = new File(location, "chrome.exe");
      trace("=> Trying " + exe.getPath());
      if (exe.exists() && exe.isFile()) {
        trace("=> Found");
        return exe;
      }
    } else if (DartCore.isMac()) {
      // In case the directory just points to the parent of Google Chrome.app
      File exe = new File(location, "Google Chrome.app/Contents/MacOS/Google Chrome");
      trace("=> Trying " + exe.getPath());
      if (exe.exists() && exe.isFile()) {
        trace("=> Found");
        return exe;
      }

      // In case the directory just points to Google Chrome.app
      exe = new File(location, "Contents/MacOS/Google Chrome");
      trace("=> Trying " + exe.getPath());
      if (exe.exists() && exe.isFile()) {
        trace("=> Found");
        return exe;
      }

      // User was smart enough to enter inside the Google Chrome.app package
      exe = new File(location, "Google Chrome");
      trace("=> Trying " + exe.getPath());
      if (exe.exists() && exe.isFile()) {
        trace("=> Found");
        return exe;
      }
    } else {
      // Linux, Unix...

      File exe = new File(location, "google-chrome");
      trace("=> Trying " + exe.getPath());
      if (exe.exists() && exe.isFile()) {
        trace("=> Found");
        return exe;
      }

      exe = new File(location, "chrome");
      trace("=> Trying " + exe.getPath());
      if (exe.exists() && exe.isFile()) {
        trace("=> Found");
        return exe;
      }

      exe = new File(location, "chromium");
      trace("=> Trying " + exe.getPath());
      if (exe.exists() && exe.isFile()) {
        trace("=> Found");
        return exe;
      }
    }

    trace("=> Failed");
    return null;
  }

  private File findChromeExecutable(String option, String location, boolean fileOrDir) {
    if (location != null) {
      return findChromeExecutable(option, new File(location), fileOrDir);
    } else {
      trace("Skipped loading Chrome from " + option + ": option not specified/null");
      return null;
    }
  }

  private IBrowserTabInfo findTargetTab(IBrowserTabChooser browserTabChooser,
      List<? extends IBrowserTabInfo> tabs) throws CoreException {
    IBrowserTabInfo chosenTab = browserTabChooser.chooseTab(tabs);

    if (chosenTab != null) {
      for (IBrowserTabInfo tab : tabs) {
        trace("Found: " + tab.toString());
      }

      trace("Choosing: " + chosenTab);

      return chosenTab;
    }

    StringBuilder builder = new StringBuilder("Unable to locate target Chrome tab [" + tabs.size()
        + " tabs]\n");

    for (IBrowserTabInfo tab : tabs) {
      builder.append("  " + tab.toString() + " [" + tab.getTitle() + "]\n");
    }

    SDBGDebugCorePlugin.logError(builder.toString().trim());

    return null;
  }

  private File getBrowserExecutable() {
    if (browserExecutable == null) {
      browserExecutable = findChromeExecutable();
    }
    return browserExecutable;
  }

  private ChromiumTabInfo getChromiumTab(Process runtimeProcess,
      IBrowserTabChooser browserTabChooser, String host, int port, long maxStartupDelay,
      ListeningStream browserOutput) throws IOException, CoreException {
    // Give Chromium 20 seconds to start up.
    long endTime = System.currentTimeMillis() + Math.max(maxStartupDelay, 0L);
    while (true) {
      if (runtimeProcess != null && isProcessTerminated(runtimeProcess)) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            SDBGDebugCorePlugin.PLUGIN_ID,
            "Could not launch browser - process terminated while trying to connect. "
                + "Try closing any running Chrome instances."
                + getProcessStreamMessage(browserOutput.toString())));
      }

      try {
        ChromiumTabInfo targetTab = (ChromiumTabInfo) findTargetTab(
            browserTabChooser,
            ChromiumConnector.getAvailableTabs(host, port));
        if (targetTab != null || System.currentTimeMillis() > endTime && runtimeProcess == null) {
          return targetTab;
        }
      } catch (IOException exception) {
        if (System.currentTimeMillis() > endTime) {
          throw exception;
        }
      }

      if (runtimeProcess != null && System.currentTimeMillis() > endTime) {
        throw new IOException("Timed out trying to connect to Chrome");
      }

      sleep(25);
    }
  }

  /**
   * Create a Chrome user data directory, and return the path to that directory.
   * 
   * @return the user data directory path
   */
  private File getCreateUserDataDirectory(String baseName) {
    File dataDir = new File(new File(new File(System.getProperty("user.home")), ".sdbg"), baseName);

    if (!dataDir.exists()) {
      dataDir.mkdirs();
    } else {
      // Remove the "<dataDir>/Default/Current Tabs" file if it exists - it can cause old tabs to
      // restore themselves when we launch the browser.
      File defaultDir = new File(dataDir, "Default");

      if (defaultDir.exists()) {
        File tabInfoFile = new File(defaultDir, "Current Tabs");

        if (tabInfoFile.exists()) {
          tabInfoFile.delete();
        }

        File sessionInfoFile = new File(defaultDir, "Current Session");

        if (sessionInfoFile.exists()) {
          sessionInfoFile.delete();
        }
      }
    }

    return dataDir;
  }

  private String getProcessStreamMessage(String output) {
    StringBuilder msg = new StringBuilder();

    if (output.length() != 0) {
      msg.append("Chrome stdout: ").append(output).append("\n");
    }

    if (msg.length() != 0) {
      msg.insert(0, ":\n\n");
    } else {
      msg.append(".");
    }

    return msg.toString();
  }

  private boolean isProcessTerminated(Process process) {
    try {
      if (process != null) {
        process.exitValue();
      }

      return true;
    } catch (IllegalThreadStateException ex) {
      return false;
    }
  }

  public void launchBrowser(ILaunch launch, ILaunchConfiguration configuration,
      IResourceResolver resourceResolver, IBrowserTabChooser browserTabChooser, String url,
      IProgressMonitor monitor, boolean enableDebugging, List<String> extraCommandLineArgs)
      throws CoreException {
    try {
      if (launchSemaphore.tryAcquire()) {
        try {
          SDBGLaunchConfigWrapper launchConfig = new SDBGLaunchConfigWrapper(configuration);
          launchConfig.markAsLaunched();

          monitor.beginTask("Launching Chrome...", enableDebugging ? 7 : 2);

          // avg: 0.434 sec (old: 0.597)
          LogTimer timer = new LogTimer("Chrome debug startup");

          // avg: 55ms
          timer.startTask("Chrome startup");

          // for now, check if browser is open, and connection is alive
          boolean restart = browserProcess == null || isProcessTerminated(browserProcess)
              || WebkitDebugTarget.getActiveTarget() == null
              || !WebkitDebugTarget.getActiveTarget().canTerminate();

          // we only re-cycle the debug connection if we're launching the same launch configuration
          if (!restart) {
            if (!WebkitDebugTarget.getActiveTarget().getLaunch().getLaunchConfiguration().equals(
                launch.getLaunchConfiguration())) {
              restart = true;
            }
          }

          if (!restart) {
            if (enableDebugging != WebkitDebugTarget.getActiveTarget().getEnableBreakpoints()) {
              restart = true;
            }
          }

          CoreLaunchUtils.removeTerminatedLaunches();

          File browserExecutable = getBrowserExecutable();

          if (!restart && url != null && resourceResolver != null) {
            DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);

            try {
              WebkitDebugTarget.getActiveTarget().navigateToUrl(
                  launch.getLaunchConfiguration(),
                  url,
                  true/*enableBreakpoints*/,
                  resourceResolver);
            } catch (IOException e) {
              SDBGDebugCorePlugin.logError(e);
            }
          } else {
            terminateExistingBrowserProcess();

            StringBuilder processDescription = new StringBuilder();

            int[] devToolsPortNumberHolder = new int[1];
            ListeningStream browserOutput = startNewBrowserProcess(
                launchConfig,
                url,
                monitor,
                enableDebugging,
                processDescription,
                extraCommandLineArgs,
                devToolsPortNumberHolder);

            sleep(100);

            monitor.worked(1);

            if (isProcessTerminated(browserProcess)) {
              SDBGDebugCorePlugin.logError("Browser output: " + browserOutput.toString());

              throw new CoreException(new Status(
                  IStatus.ERROR,
                  SDBGDebugCorePlugin.PLUGIN_ID,
                  "Could not launch browser - process terminated on startup"
                      + getProcessStreamMessage(browserOutput.toString())));
            }

            if (enableDebugging) {
              connectToChromiumDebug(
                  browserExecutable.getName(),
                  launch,
                  launchConfig,
                  url,
                  monitor,
                  browserProcess,
                  timer,
                  true/*enableBreakpoints*/,
                  null,
                  devToolsPortNumberHolder[0],
                  20 * 1000L/*maxStartupDelay*/,
                  browserOutput,
                  processDescription.toString(),
                  resourceResolver,
                  browserTabChooser,
                  false/*remote*/);
            } else {
              registerProcess(
                  launch,
                  launchConfig,
                  DebugPlugin.newProcess(launch, browserProcess, browserExecutable.getName()
                      + " - Run only, debugging DISABLED (" + new Date() + ")"),
                  processDescription.toString());
            }
          }

          DebugUIHelper.getHelper().activateApplication(browserExecutable, "Chrome");

          timer.stopTask();
          timer.stopTimer();
          monitor.done();
        } finally {
          launchSemaphore.release();
        }
      }
    } catch (CoreException e) {
      DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);
      throw e;
    }
  }

  private ListeningStream readFromProcessPipes(final InputStream in) {
    final ListeningStream output = new ListeningStream();

    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        byte[] buffer = new byte[2048];

        try {
          int count = in.read(buffer);

          while (count != -1) {
            if (count > 0) {
              String str = new String(buffer, 0, count);

              // Log any browser process output to stdout.
              if (Trace.isTracing(Trace.BROWSER_OUTPUT)) {
                System.out.print(str);
              }

              output.appendData(str);
            }

            count = in.read(buffer);
          }

          in.close();
        } catch (IOException ioe) {
          // When the process closes, we do not want to print any errors.
        }
      }
    }, "Read from browser");

    thread.start();

    return output;
  }

  private void registerProcess(ILaunch launch, SDBGLaunchConfigWrapper launchConfig,
      IProcess process, String processDescription) {
    launch.setAttribute(DebugPlugin.ATTR_CONSOLE_ENCODING, "UTF-8");
    launch.addProcess(process);

    if (processDescription != null) {
      process.setAttribute(IProcess.ATTR_CMDLINE, processDescription);
    }
  }

  private void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (Exception exception) {

    }
  }

  /**
   * @param launchConfig
   * @param url
   * @param monitor
   * @param enableDebugging
   * @param browserLocation
   * @param browserName
   * @throws CoreException
   */
  private ListeningStream startNewBrowserProcess(SDBGLaunchConfigWrapper launchConfig, String url,
      IProgressMonitor monitor, boolean enableDebugging, StringBuilder argDescription,
      List<String> extraArguments, int[] devToolsPortNumberHolder) throws CoreException {

    Process process = null;
    monitor.worked(1);

    ProcessBuilder builder = new ProcessBuilder();
    Map<String, String> env = builder.environment();

    // Due to differences in 32bit and 64 bit environments, dartium 32bit launch does not work on
    // linux with this property.
    env.remove("LD_LIBRARY_PATH");

    Map<String, String> wrapperEnv = launchConfig.getEnvironment();
    if (!wrapperEnv.isEmpty()) {
      for (String key : wrapperEnv.keySet()) {
        env.put(key, wrapperEnv.get(key));
      }
    }

    int devToolsPortNumber = -1;
    if (enableDebugging) {
      devToolsPortNumber = NetUtils.findUnusedPort(DEVTOOLS_PORT_NUMBER);

      if (devToolsPortNumber == -1) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            SDBGDebugCorePlugin.PLUGIN_ID,
            "Unable to locate an available port for the browser debugger"));
      }

    }

    devToolsPortNumberHolder[0] = devToolsPortNumber;
    List<String> arguments = buildArgumentsList(launchConfig, enableDebugging && url != null
        ? INITIAL_PAGE : url, devToolsPortNumber, extraArguments);
    builder.command(arguments);
    builder.redirectErrorStream(true);

    describe(arguments, argDescription);

    try {
      process = builder.start();
    } catch (IOException e) {
      SDBGDebugCorePlugin.logError("Exception while starting browser", e);

      throw new CoreException(new Status(
          IStatus.ERROR,
          SDBGDebugCorePlugin.PLUGIN_ID,
          "Could not launch browser: " + e.toString()));
    }

    browserProcess = process;

    return readFromProcessPipes(browserProcess.getInputStream());
  }

  private void terminateExistingBrowserProcess() {
    if (browserProcess != null) {
      if (!isProcessTerminated(browserProcess)) {
        // TODO(devoncarew): try and use an OS mechanism to send it a graceful shutdown request?
        // This could avoid the problem w/ Chrome displaying the crashed message on the next run.

        browserProcess.destroy();

        // The process needs time to exit.
        waitForProcessToTerminate(browserProcess, 200);
        // sleep(100);
      }

      browserProcess = null;
    }
  }

  private void trace(String message) {
    Trace.trace(Trace.BROWSER_LAUNCHING, message);
  }

  private void waitForProcessToTerminate(Process process, int maxWaitTimeMs) {
    long startTime = System.currentTimeMillis();

    while ((System.currentTimeMillis() - startTime) < maxWaitTimeMs) {
      if (isProcessTerminated(process)) {
        return;
      }

      sleep(10);
    }
  }
}
