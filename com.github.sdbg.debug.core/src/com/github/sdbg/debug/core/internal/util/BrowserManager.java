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
import com.github.sdbg.debug.core.internal.util.ListeningStream.StreamListener;
import com.github.sdbg.debug.core.internal.webkit.model.WebkitDebugTarget;
import com.github.sdbg.debug.core.internal.webkit.protocol.ChromiumConnector;
import com.github.sdbg.debug.core.internal.webkit.protocol.ChromiumTabInfo;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitConnection;
import com.github.sdbg.debug.core.model.IResourceResolver;
import com.github.sdbg.debug.core.util.DefaultBrowserTabChooser;
import com.github.sdbg.debug.core.util.IBrowserTabChooser;
import com.github.sdbg.debug.core.util.IBrowserTabInfo;
import com.github.sdbg.debug.core.util.ResourceServerManager;
import com.github.sdbg.debug.core.util.Trace;
import com.github.sdbg.utilities.NetUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;

/**
 * A manager that launches and manages configured browsers.
 */
public class BrowserManager {
  /** The initial page to navigate to. */
  private static final String INITIAL_PAGE = "chrome://version/";

  private static final int DEVTOOLS_PORT_NUMBER = 9322;

  private static final String CHROME_EXECUTABLE_PROPERTY = "chrome.location";

  private static BrowserManager manager = new BrowserManager();

  /**
   * Create a Chrome user data directory, and return the path to that directory.
   * 
   * @return the user data directory path
   */
  public static String getCreateUserDataDirectoryPath(String baseName) {
    String dataDirPath = System.getProperty("user.home") + File.separator + "." + baseName;

    File dataDir = new File(dataDirPath);

    if (!dataDir.exists()) {
      dataDir.mkdir();
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

    return dataDirPath;
  }

  public static BrowserManager getManager() {
    return manager;
  }

  private Process browserProcess = null;

  private IResourceResolver resourceResolver;

  private File chromeExecutable;

  private int devToolsPortNumber;

  private IBrowserTabChooser tabChooser;

  public BrowserManager() {
    this.tabChooser = new DefaultBrowserTabChooser();
  }

// TODO: Unfinished
  public void connect(ILaunch launch, ILaunchConfiguration configuration,
      IBrowserTabChooser tabChooser, String host, int port, IProgressMonitor monitor)
      throws CoreException {
    monitor.beginTask("Opening Connection...", IProgressMonitor.UNKNOWN);

    try {
      List<? extends IBrowserTabInfo> tabs = ChromiumConnector.getAvailableTabs(host, port);

      ChromiumTabInfo tab = (ChromiumTabInfo) findTargetTab(tabChooser, tabs);

      if (tab == null || tab.getWebSocketDebuggerUrl() == null) {
        throw new DebugException(new Status(
            IStatus.ERROR,
            SDBGDebugCorePlugin.PLUGIN_ID,
            "Unable to connect to Chrome"));
      }

      monitor.worked(1);

      WebkitConnection connection = new WebkitConnection(
          tab.getHost(),
          tab.getPort(),
          tab.getWebSocketDebuggerFile());

      final WebkitDebugTarget debugTarget = new WebkitDebugTarget(
          "Remote",
          connection,
          launch,
          null,
          getResourceServer(),
          true,
          true);

      launch.setAttribute(DebugPlugin.ATTR_CONSOLE_ENCODING, "UTF-8");
      launch.addDebugTarget(debugTarget);
      launch.addProcess(debugTarget.getProcess());

      debugTarget.openConnection();

      monitor.worked(1);
    } catch (IOException e) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          SDBGDebugCorePlugin.PLUGIN_ID,
          e.toString(),
          e));
    } finally {
      monitor.done();
    }
  }

  public void dispose() {
    if (!isProcessTerminated(browserProcess)) {
      browserProcess.destroy();
    }
  }

  public File findChrome() {
    if (chromeExecutable == null) {
      chromeExecutable = manager.findChromeExecutable();
    }
    return chromeExecutable;
  }

  /**
   * Launch the browser and open the given file. If debug mode also connect to browser.
   */
  public void launchBrowser(ILaunch launch, SDBGLaunchConfigWrapper launchConfig, IFile file,
      IProgressMonitor monitor, boolean enableDebugging) throws CoreException {
    launchBrowser(launch, launchConfig, file, null, monitor, enableDebugging);
  }

  /**
   * Launch the browser and open the given url. If debug mode also connect to browser.
   */
  public void launchBrowser(ILaunch launch, SDBGLaunchConfigWrapper launchConfig, String url,
      IProgressMonitor monitor, boolean enableDebugging) throws CoreException {
    launchBrowser(launch, launchConfig, null, url, monitor, enableDebugging);
  }

  public IDebugTarget performRemoteConnection(IBrowserTabChooser tabChooser, String host, int port,
      IProgressMonitor monitor) throws CoreException {

    ILaunch launch = null;

    monitor.beginTask("Opening Connection...", IProgressMonitor.UNKNOWN);

    try {
      List<? extends IBrowserTabInfo> tabs = ChromiumConnector.getAvailableTabs(host, port);

      ChromiumTabInfo tab = (ChromiumTabInfo) findTargetTab(tabChooser, tabs);

      if (tab == null || tab.getWebSocketDebuggerUrl() == null) {
        throw new DebugException(new Status(
            IStatus.ERROR,
            SDBGDebugCorePlugin.PLUGIN_ID,
            "Unable to connect to Chrome"));
      }

      monitor.worked(1);

      launch = CoreLaunchUtils.createTemporaryLaunch(
          SDBGDebugCorePlugin.CHROME_LAUNCH_CONFIG_ID,
          host + "[" + port + "]");

      CoreLaunchUtils.addLaunch(launch);

      WebkitConnection connection = new WebkitConnection(
          tab.getHost(),
          tab.getPort(),
          tab.getWebSocketDebuggerFile());

      final WebkitDebugTarget debugTarget = new WebkitDebugTarget(
          "Remote",
          connection,
          launch,
          null,
          getResourceServer(),
          true,
          true);

      launch.setAttribute(DebugPlugin.ATTR_CONSOLE_ENCODING, "UTF-8");
      launch.addDebugTarget(debugTarget);
      launch.addProcess(debugTarget.getProcess());

      debugTarget.openConnection();

      monitor.worked(1);

      return debugTarget;
    } catch (IOException e) {
      if (launch != null) {
        CoreLaunchUtils.removeLaunch(launch);
      }

      throw new CoreException(new Status(
          IStatus.ERROR,
          SDBGDebugCorePlugin.PLUGIN_ID,
          e.toString(),
          e));
    } finally {
      monitor.done();
    }
  }

  protected void launchBrowser(ILaunch launch, SDBGLaunchConfigWrapper launchConfig, IFile file,
      String url, IProgressMonitor monitor, boolean enableDebugging) throws CoreException {

    // For now, we always start a debugging connection, even when we're not really debugging.
    boolean enableBreakpoints = enableDebugging;

    monitor.beginTask("Launching Chrome...", enableDebugging ? 7 : 2);

    File chromeExe = findChrome();

    IPath browserLocation = new Path(chromeExe.getAbsolutePath());

    String browserName = chromeExe.getName();

    // avg: 0.434 sec (old: 0.597)
    LogTimer timer = new LogTimer("Chrome debug startup");

    // avg: 55ms
    timer.startTask(browserName + " startup");

    url = resolveLaunchUrl(file, url);

    url = launchConfig.appendQueryParams(url);

    IResourceResolver resolver = launchConfig.getShouldLaunchFile() ? getResourceServer()
        : new LaunchConfigResourceResolver(launchConfig);

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

    if (!restart) {
      DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);

      try {
        WebkitDebugTarget.getActiveTarget().navigateToUrl(
            launch.getLaunchConfiguration(),
            url,
            enableBreakpoints,
            resolver);
      } catch (IOException e) {
        SDBGDebugCorePlugin.logError(e);
      }
    } else {
      terminateExistingBrowserProcess();

      StringBuilder processDescription = new StringBuilder();

      ListeningStream dartiumOutput = startNewBrowserProcess(
          launchConfig,
          url,
          monitor,
          enableDebugging,
          browserLocation,
          browserName,
          processDescription);

      sleep(100);

      monitor.worked(1);

      if (isProcessTerminated(browserProcess)) {
        SDBGDebugCorePlugin.logError("Chrome output: " + dartiumOutput.toString());

        throw new CoreException(new Status(
            IStatus.ERROR,
            SDBGDebugCorePlugin.PLUGIN_ID,
            "Could not launch browser - process terminated on startup"
                + getProcessStreamMessage(dartiumOutput.toString())));
      }

      connectToChromiumDebug(
          browserName,
          launch,
          launchConfig,
          url,
          monitor,
          browserProcess,
          timer,
          enableBreakpoints,
          devToolsPortNumber,
          dartiumOutput,
          processDescription.toString(),
          resolver);
    }

    DebugUIHelper.getHelper().activateApplication(chromeExe, "Chrome");

    timer.stopTask();
    timer.stopTimer();
    monitor.done();
  }

  /**
   * Launch browser and open file url. If debug mode also connect to browser.
   */
  void connectToChromiumDebug(String browserName, ILaunch launch,
      SDBGLaunchConfigWrapper launchConfig, String url, IProgressMonitor monitor,
      Process runtimeProcess, LogTimer timer, boolean enableBreakpoints, int devToolsPortNumber,
      ListeningStream dartiumOutput, String processDescription, IResourceResolver resolver)
      throws CoreException {
    monitor.worked(1);

    try {
      // avg: 383ms
      timer.startTask("get chromium tabs");

      ChromiumTabInfo tab = getChromiumTab(runtimeProcess, devToolsPortNumber, dartiumOutput);

      monitor.worked(2);

      timer.stopTask();

      // avg: 46ms
      timer.startTask("open WIP connection");

      if (tab == null || tab.getWebSocketDebuggerUrl() == null) {
        throw new DebugException(new Status(
            IStatus.ERROR,
            SDBGDebugCorePlugin.PLUGIN_ID,
            "Unable to connect to Chrome"));
      }

      // Even when Chrome has reported all the debuggable tabs to us, the debug server
      // may not yet have started up. Delay a small fixed amount of time.
      sleep(100);

      WebkitConnection connection = new WebkitConnection(
          tab.getHost(),
          tab.getPort(),
          tab.getWebSocketDebuggerFile());

      final WebkitDebugTarget debugTarget = new WebkitDebugTarget(
          browserName,
          connection,
          launch,
          runtimeProcess,
          resolver,
          enableBreakpoints,
          false);

      monitor.worked(1);

      launch.setAttribute(DebugPlugin.ATTR_CONSOLE_ENCODING, "UTF-8");
      launch.addDebugTarget(debugTarget);
      launch.addProcess(debugTarget.getProcess());
      debugTarget.getProcess().setAttribute(IProcess.ATTR_CMDLINE, processDescription);

      if (launchConfig.getShowLaunchOutput()) {
        dartiumOutput.setListener(new StreamListener() {
          @Override
          public void handleStreamData(String data) {
            debugTarget.writeToStdout(data);
          }
        });
      }

      debugTarget.openConnection(url, true);
      Trace.trace("Connected to WIP debug agent on port " + devToolsPortNumber);

      timer.stopTask();
    } catch (IOException e) {
      DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);

      IStatus status;

      // Clean up the error message on certain connection failures to Chrome.
      // http://code.google.com/p/dart/issues/detail?id=4435
      if (e.toString().indexOf("connection failed: unknown status code 500") != -1) {
        SDBGDebugCorePlugin.logError(e);

        status = new Status(
            IStatus.ERROR,
            SDBGDebugCorePlugin.PLUGIN_ID,
            "Unable to connect to Chrome");
      } else {
        status = new Status(IStatus.ERROR, SDBGDebugCorePlugin.PLUGIN_ID, e.toString(), e);
      }

      throw new CoreException(status);
    }

    monitor.worked(1);
  }

  private List<String> buildArgumentsList(SDBGLaunchConfigWrapper launchConfig,
      IPath browserLocation, String url, boolean enableDebugging, int devToolsPortNumber) {
    List<String> arguments = new ArrayList<String>();

    arguments.add(browserLocation.toOSString());

    // Enable remote debug over HTTP on the specified port.
    arguments.add("--remote-debugging-port=" + devToolsPortNumber);

    // In order to start up multiple Chrome processes, we need to specify a different user dir.
    arguments.add("--user-data-dir=" + getCreateUserDataDirectoryPath("chrome"));

    if (launchConfig.isEnableExperimentalWebkitFeatures()) {
      arguments.add("--enable-experimental-web-platform-features");
      arguments.add("--enable-html-imports");
    }

    // Whether or not it's actually the first run.
    arguments.add("--no-first-run");

    // Disables the default browser check.
    arguments.add("--no-default-browser-check");

    // Bypass the error dialog when the profile lock couldn't be attained.
    arguments.add("--no-process-singleton-dialog");

    for (String arg : launchConfig.getArgumentsAsArray()) {
      arguments.add(arg);
    }

    if (enableDebugging) {
      // Start up with a blank page.
      arguments.add(INITIAL_PAGE);
    } else {
      arguments.add(url);
    }

    return arguments;
  }

  private void describe(List<String> arguments, StringBuilder builder) {
    for (int i = 0; i < arguments.size(); i++) {
      if (i > 0) {
        builder.append(" ");
      }
      builder.append(arguments.get(i));
    }
  }

  private File findChromeExecutable() {
    // First, try the system property, as user-specified value is preferred
    String chromeLocationStr = System.getProperty(
        CHROME_EXECUTABLE_PROPERTY,
        System.getenv(CHROME_EXECUTABLE_PROPERTY));
    if (chromeLocationStr != null) {
      File chromeLocation = new File(chromeLocationStr);
      if (chromeLocation.exists()) {
        if (chromeLocation.isDirectory()) {
          chromeLocation = findChromeExecutable(chromeLocation);
        }

        if (chromeLocation != null) {
          return chromeLocation;
        }
      }
    }

    // On Windows, try to locate Chrome using the Uninstall windows Registry setting
    // If unsuccessful, try a heuristics in the user home directory
    if (DartCore.isWindows()) {
      File chromeDirectory = null;

      try {
        String installLocation = WinReg.readRegistry(
            "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Google Chrome",
            "InstallLocation");
        if (installLocation != null) {
          chromeDirectory = new File(installLocation);
        }
      } catch (IOException e) {
        // Stay silent
        SDBGDebugCorePlugin.logError(e);
      }

      if (!isExistingDirectory(chromeDirectory)) {
        // Heuristically search in user's home directory
        File userHome = new File(System.getProperty("user.home"));

        // Windows 7
        chromeDirectory = new File(userHome, "AppData\\Local\\Google\\Chrome\\Application");
        if (!isExistingDirectory(chromeDirectory)) {
          // XP
          chromeDirectory = new File(
              userHome,
              "Local Settings\\Application Data\\Google\\Chrome\\Application");
        }
      }

      File chromeExecutable = findChromeExecutable(chromeDirectory);
      if (chromeExecutable != null) {
        return chromeExecutable;
      }
    } else if (DartCore.isMac()) {
      return findChromeExecutable(new File("/Applications"));
    } else {
      // Search $PATH
      String path = System.getenv("PATH");
      if (path != null) {
        for (String dirStr : path.split(File.pathSeparator)) {
          File dir = new File(dirStr);
          if (dir.isDirectory()) {
            File chromeExecutable = findChromeExecutable(dir);
            if (chromeExecutable != null) {
              return chromeExecutable;
            }
          }
        }
      }
    }

    throw new RuntimeException("Uable to locate the Chrome executable at the usual locations.\n"
        + "Please append at the end of your eclipse.ini file the following property:\n" + "-D"
        + CHROME_EXECUTABLE_PROPERTY + "=<path-to-chrome-executable>");
  }

  private File findChromeExecutable(File dir) {
    if (!isExistingDirectory(dir)) {
      return null;
    }

    if (DartCore.isWindows()) {
      File exe = new File(dir, "chrome.exe");
      if (exe.exists() && exe.isFile()) {
        return exe;
      }
    } else if (DartCore.isMac()) {
      // In case the directory just points to the parent of Google Chrome.app
      File exe = new File(dir, "Google Chrome.app/Contents/MacOS/Google Chrome");
      if (exe.exists() && exe.isFile()) {
        return exe;
      }

      // In case the directory just points to Google Chrome.app
      exe = new File(dir, "Contents/MacOS/Google Chrome");
      if (exe.exists() && exe.isFile()) {
        return exe;
      }

      // User was smart enough to enter inside the Google Chrome.app package
      exe = new File(dir, "Google Chrome");
      if (exe.exists() && exe.isFile()) {
        return exe;
      }
    } else {
      // Linux, Unix...

      File exe = new File(dir, "google-chrome");
      if (exe.exists() && exe.isFile()) {
        return exe;
      }

      exe = new File(dir, "chrome");
      if (exe.exists() && exe.isFile()) {
        return exe;
      }

      exe = new File(dir, "chromium");
      if (exe.exists() && exe.isFile()) {
        return exe;
      }
    }

    return null;
  }

  private IBrowserTabInfo findTargetTab(IBrowserTabChooser tabChooser,
      List<? extends IBrowserTabInfo> tabs) {
    IBrowserTabInfo chosenTab = tabChooser.chooseTab(tabs);

    if (chosenTab != null) {
      for (IBrowserTabInfo tab : tabs) {
        SDBGDebugCorePlugin.log("Found: " + tab.toString());
      }

      SDBGDebugCorePlugin.log("Choosing: " + chosenTab);

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

  private ChromiumTabInfo getChromiumTab(Process runtimeProcess, int port,
      ListeningStream dartiumOutput) throws IOException, CoreException {
    // Give Chromium 20 seconds to start up.
    final int maxStartupDelay = 20 * 1000;

    long endTime = System.currentTimeMillis() + maxStartupDelay;

    while (true) {
      if (isProcessTerminated(runtimeProcess)) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            SDBGDebugCorePlugin.PLUGIN_ID,
            "Could not launch browser - process terminated while trying to connect. "
                + "Try closing any running Chrome instances."
                + getProcessStreamMessage(dartiumOutput.toString())));
      }

      try {
        List<ChromiumTabInfo> tabs = ChromiumConnector.getAvailableTabs(port);

        ChromiumTabInfo targetTab = (ChromiumTabInfo) findTargetTab(tabChooser, tabs);

        if (targetTab != null) {
          return targetTab;
        }
      } catch (IOException exception) {
        if (System.currentTimeMillis() > endTime) {
          throw exception;
        }
      }

      if (System.currentTimeMillis() > endTime) {
        throw new IOException("Timed out trying to connect to Chrome");
      }

      sleep(25);
    }
  }

  private String getProcessStreamMessage(String output) {
    StringBuilder msg = new StringBuilder();

    if (output.length() != 0) {
      msg.append("Chrome stdout: ").append(output).append("\n");
    }

    boolean expired = false;

    if (output.length() != 0) {
      if (output.indexOf("Chrome build has expired") != -1) {
        expired = true;
      }

      if (expired) {
        msg.append("\nThis build of Chrome has expired.\n\n");
        msg.append("Please download a new Chrome build");
      }
    }

    if (msg.length() != 0) {
      msg.insert(0, ":\n\n");
    } else {
      msg.append(".");
    }

    return msg.toString();
  }

  private IResourceResolver getResourceServer() throws CoreException {
    if (resourceResolver == null) {
      try {
        resourceResolver = ResourceServerManager.getServer();
      } catch (IOException ioe) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            SDBGDebugCorePlugin.PLUGIN_ID,
            ioe.getMessage(),
            ioe));
      }
    }

    return resourceResolver;
  }

  private boolean isExistingDirectory(File dir) {
    return dir != null && dir.exists() && dir.isDirectory();
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

  private ListeningStream readFromProcessPipes(final String processName, final InputStream in) {
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
              if (Trace.TRACING) {
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
    }, "Read from " + processName);

    thread.start();

    return output;
  }

  /**
   * @param file
   * @throws CoreException
   */
  private String resolveLaunchUrl(IFile file, String url) throws CoreException {
    if (file != null) {
      return getResourceServer().getUrlForResource(file);
    }

    return url;
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
      IProgressMonitor monitor, boolean enableDebugging, IPath browserLocation, String browserName,
      StringBuilder argDescription) throws CoreException {

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

    devToolsPortNumber = DEVTOOLS_PORT_NUMBER;

    if (enableDebugging) {
      devToolsPortNumber = NetUtils.findUnusedPort(DEVTOOLS_PORT_NUMBER);

      if (devToolsPortNumber == -1) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            SDBGDebugCorePlugin.PLUGIN_ID,
            "Unable to locate an available port for the browser debugger"));
      }
    }

    List<String> arguments = buildArgumentsList(
        launchConfig,
        browserLocation,
        url,
        enableDebugging,
        devToolsPortNumber);
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

    return readFromProcessPipes(browserName, browserProcess.getInputStream());
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
