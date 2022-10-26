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
import com.github.sdbg.debug.core.internal.webkit.model.WebkitDebugTarget;
import com.github.sdbg.debug.core.model.IResourceResolver;
import com.github.sdbg.debug.core.util.IBrowserTabChooser;
import com.github.sdbg.debug.core.util.IBrowserTabInfo;
import com.github.sdbg.debug.core.util.IDeviceChooser;
import com.github.sdbg.debug.core.util.Trace;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;

/**
 * A manager that launches and manages configured browsers. 
 */
public class BrowserManager {

  private static final String CHROME_EXECUTABLE_PROPERTY = "chrome.location"
      , CHROME_ENVIRONMENT_VARIABLE = "CHROME_LOCATION"
      , BROWSER_EXECUTABLE_PROPERTY = "browser.location"
      , BROWSER_ENVIRONMENT_VARIABLE = "BROWSER_LOCATION";

  private String browserDataDirName;

  private Semaphore launchSemaphore;

  private IBrowser browser;

  public BrowserManager(String browserDataDirName) {
    this.browserDataDirName = browserDataDirName;
    this.launchSemaphore = new Semaphore(1);
  }

  public WebkitDebugTarget connect(ILaunch launch, ILaunchConfiguration configuration,
      IResourceResolver resourceResolver, IBrowserTabChooser browserTabChooser, String host,
      int port, IProgressMonitor monitor) throws CoreException {
    try {
      SDBGLaunchConfigWrapper launchConfig = new SDBGLaunchConfigWrapper(configuration);

      LogTimer timer = new LogTimer("Browser debug connect");

      try {
        timer.startTask("connect");

        try {
          launchConfig.markAsLaunched();

          return browser.connectToBrowserDebug(
              "Browser Remote Connection",
              launch,
              launchConfig,
              null/*url*/,
              monitor,
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
//    try {
//      SDBGLaunchConfigWrapper launchConfig = new SDBGLaunchConfigWrapper(configuration);
//
//      LogTimer timer = new LogTimer("Browser debug connect");
//
//      try {
//        timer.startTask("connect");
//
//        try {
//          launchConfig.markAsLaunched();
//
//          List<? extends IDeviceInfo> devices = adbManager.getDevices();
//          if (devices.isEmpty()) {
//            throw new DebugException(
//                new Status(
//                    IStatus.ERROR,
//                    SDBGDebugCorePlugin.PLUGIN_ID,
//                    "No USB-attached Android devices found.\n\nPlease make sure you have enabled USB debugging on your device and you have attached it to the PC via USB."));
//          }
//
//          IDeviceInfo device = deviceChooser.chooseDevice(devices);
//          if (device != null) {
//            int port = NetUtils.findUnusedPort(DEVTOOLS_PORT_NUMBER);
//            MobileBrowserUtils.addChromiumForward(adbManager, device.getId(), port);
//
//            try {
//              final String url = launchConfig.getUrl();
//              boolean launchTab = launchConfig.isLaunchTabWithUrl() && url != null
//                  && url.length() > 0;
//
//              if (launchTab) {
//                MobileBrowserUtils.launchChromeBrowser(adbManager, device.getId());
//
//                IBrowserTabInfo tab = browser.getChromiumTab(
//                    null/*runtimeProcess*/,
//                    new IBrowserTabChooser() {
//                      @Override
//                      public IBrowserTabInfo chooseTab(List<? extends IBrowserTabInfo> tabs)
//                          throws CoreException {
//                        for (IBrowserTabInfo tab : tabs) {
//                          if (tab.getUrl() != null
//                              && tab.getUrl().toLowerCase().contains(url.toLowerCase())) {
//                            return tab;
//                          }
//                        }
//
//                        return null;
//                      }
//                    },
//                    "127.0.0.1",
//                    port,
//                    10 * 1000L/*maxStartupDelay*/,
//                    null/*output*/);
//
//                if (tab == null) {
//                  MobileBrowserUtils.launchChromeBrowser(
//                      adbManager,
//                      device.getId(),
//                      launchConfig.getUrl());
//                }
//              }
//
//              return browser.connectToBrowserDebug(
//                  "Mobile Chrome Remote Connection",
//                  launch,
//                  launchConfig,
//                  null/*url*/,
//                  monitor,
//                  null/*runtimeProcess*/,
//                  timer,
//                  true/*enableBreakpoints*/,
//                  "127.0.0.1",
//                  port,
//                  launchTab ? 10 * 1000L : 0L/*maxStartupDelay*/,
//                  null/*browserOutput*/,
//                  null/*processDescription*/,
//                  resourceResolver,
//                  browserTabChooser,
//                  true/*remote*/);
//            } catch (IOException e) {
//              adbManager.removeAllForwards();
//              throw new CoreException(new Status(
//                  IStatus.ERROR,
//                  SDBGDebugCorePlugin.PLUGIN_ID,
//                  "Unable to connect to debugger in Chrome: " + e.getMessage(),
//                  e));
//            } catch (CoreException e) {
//              adbManager.removeAllForwards();
//              throw e;
//            }
//          } else {
//            throw new DebugException(new Status(
//                IStatus.INFO,
//                SDBGDebugCorePlugin.PLUGIN_ID,
//                "No Android device was chosen. Connection cancelled."));
//          }
//        } finally {
//          timer.stopTask();
//        }
//      } finally {
//        timer.stopTimer();
//      }
//    } catch (CoreException e) {
//      DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);
//      throw e;
//    }
      return null;
  }

  public void dispose() {
    if (browser != null) {
      browser.terminateExistingBrowserProcess();
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

          monitor.beginTask("Launching Browser...", enableDebugging ? 7 : 2);

          // avg: 0.434 sec (old: 0.597)
          LogTimer timer = new LogTimer("Browser debug startup");

          // avg: 55ms
          timer.startTask("Browser startup");

          // for now, check if browser is open, and connection is alive
          boolean restart = browser == null || browser.isProcessTerminated()
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

          getBrowser();

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
            // In theory we want to set the URL only after we have set the correct behavior regarding "break on exceptions" set in the debugger
            // The latter happens only after the debug connection is established
            // however for unknown reasons, this delayed setting does not always work
            // Hence, for now this delayed URL setting is switched off
            boolean delaySettingUrl = false;

            browser.terminateExistingBrowserProcess();

            StringBuilder processDescription = new StringBuilder();

            int[] devToolsPortNumberHolder = new int[1];
            ListeningStream browserOutput = browser.startNewBrowserProcess(
                launchConfig,
                delaySettingUrl ? null : url,
                monitor,
                enableDebugging,
                processDescription,
                extraCommandLineArgs,
                devToolsPortNumberHolder);

            monitor.worked(1);

            if (browser.isProcessTerminated()) {
              SDBGDebugCorePlugin.logError("Browser output: " + browserOutput.toString());

              throw new CoreException(new Status(
                  IStatus.ERROR,
                  SDBGDebugCorePlugin.PLUGIN_ID,
                  "Could not launch browser - process terminated on startup"
                      + getProcessStreamMessage(browserOutput.toString())));
            }

            if (enableDebugging) {
              browser.connectToBrowserDebug(
                  browser.getExecutableName(),
                  launch,
                  launchConfig,
                  delaySettingUrl ? url : null,
                  monitor,                  
                  timer,
                  true/*enableBreakpoints*/,
                  "127.0.0.1",
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
                  DebugPlugin.newProcess(launch, browser.getProcess(), browser.getExecutableName()
                      + " - run only, debugging DISABLED (" + new Date() + ")"),
                  processDescription.toString());
            }
          }

          DebugUIHelper.getHelper().activateApplication(browser.getExecutableFile(), "Chrome");

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

  // Only exists to support the funky Chrome Application launch
  protected IResourceResolver createResourceResolver(ILaunch launch,
      ILaunchConfiguration configuration, IBrowserTabInfo tab) {
    throw new UnsupportedOperationException("Not implemented");
  }

  private File findBrowserExecutable() {
    // First, try the system property, as user-specified value is preferred
    File file = findBrowserExecutable(
        "system property " + BROWSER_EXECUTABLE_PROPERTY,
        System.getProperty(BROWSER_EXECUTABLE_PROPERTY),
        true/*fileOrDir*/);
    if (file != null) {
      return file;
    }

    // For compatibility search the OLD Property
    file = findBrowserExecutable(
        "system property " + CHROME_EXECUTABLE_PROPERTY,
        System.getProperty(CHROME_EXECUTABLE_PROPERTY),
        true/*fileOrDir*/);
    if (file != null) {
      return file;
    }

    // Second, try the environment variable
    file = findBrowserExecutable(
        "environment vairable " + BROWSER_ENVIRONMENT_VARIABLE,
        System.getenv(BROWSER_ENVIRONMENT_VARIABLE),
        true/*fileOrDir*/);
    if (file != null) {
      return file;
    }

    // For compatibility search the OLD Property
    file = findBrowserExecutable(
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
        file = findBrowserExecutable(
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
        file = findBrowserExecutable(
            "registry setting " + regKey + "[" + regValue + "]",
            WinReg.readRegistry(regKey, regValue),
            false/*fileOrDir*/);
        if (file != null) {
          return file;
        }

        // General fallback
        regKey = "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\chrome.exe";
        regValue = "Path";
        file = findBrowserExecutable(
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
      file = findBrowserExecutable("heuristics", new File(
          userHome,
          "AppData\\Local\\Google\\Chrome\\Application"), false/*fileOrDir*/);
      if (file != null) {
        return file;
      }

      // XP
      file = findBrowserExecutable("heuristics", new File(
          userHome,
          "Local Settings\\Application Data\\Google\\Chrome\\Application"), false/*fileOrDir*/);
      if (file != null) {
        return file;
      }

      //Default PATH. Often installed here.
      file = findBrowserExecutable("default path", "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe", true);
      if (file != null) {
        return file;
      }
      
      file = findBrowserExecutable("default path", "C:/Program Files/Microsoft/Edge/Application/msedge.exe", true);
      if (file != null) {
        return file;
      }

    } else if (DartCore.isMac()) {
      file = findBrowserExecutable("heuristics", new File("/Applications"), false/*fileOrDir*/);
      if (file != null) {
        return file;
      }
    } else {
      // Search $PATH
      String path = System.getenv("PATH");
      if (path != null) {
        for (String dirStr : path.split(File.pathSeparator)) {
          file = findBrowserExecutable("$PATH entry", dirStr, false/*fileOrDir*/);
          if (file != null) {
            return file;
          }
        }
      }
    }

    throw new RuntimeException("Uable to locate the Browser executable at the usual locations.\n"
        + "Please append at the end of your eclipse.ini file the following property:\n" + "-D"
        + BROWSER_EXECUTABLE_PROPERTY + "=<path-to-browser-executable>");
  }

  private File findBrowserExecutable(String option, File location, boolean fileOrDir) {
    trace("Trying to load Browser from " + option + "=" + location.getPath());
    if (!location.exists()) {
      trace("=> Failed, location does not exist");
    } else if (location.isFile()) {
      if (fileOrDir) {
        trace("=> Found, location is a file, this is assumed to be the Browser executable");
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
    } else if (DartCore.isWindows()) {
      //Try Microsoft Edge, as it is now Chrome based, and works flawless.
      File exe = new File(location, "msedge.exe");
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

      exe = new File(location, "firefox");
      trace("=> Trying " + exe.getPath());
      if (exe.exists() && exe.isFile()) {
        trace("=> Found");
        return exe;
      }
    }

    trace("=> Failed");
    return null;
  }

  private File findBrowserExecutable(String option, String location, boolean fileOrDir) {
    if (location != null) {
      return findBrowserExecutable(option, new File(location), fileOrDir);
    } else {
      trace("Skipped loading Chrome from " + option + ": option not specified/null");
      return null;
    }
  }

  private IBrowser getBrowser()
  {
      if (browser == null)
      {
          File executable = findBrowserExecutable();
          if (executable.getPath().contains("firefox"))
          {
              browser = new FirefoxBrowser(executable);
          }
          else
          {
              browser = new GenericBrowser(executable, browserDataDirName);
          }
      }
      return browser;
  }

  static String getProcessStreamMessage(String output) {
    StringBuilder msg = new StringBuilder();

    if (output.length() != 0) {
      msg.append("Browser stdout: ").append(output).append("\n");
    }

    if (msg.length() != 0) {
      msg.insert(0, ":\n\n");
    } else {
      msg.append(".");
    }

    return msg.toString();
  }

  static void registerProcess(ILaunch launch, SDBGLaunchConfigWrapper launchConfig,
      IProcess process, String processDescription) {
    launch.setAttribute(DebugPlugin.ATTR_CONSOLE_ENCODING, "UTF-8");
    launch.addProcess(process);

    if (processDescription != null) {
      process.setAttribute(IProcess.ATTR_CMDLINE, processDescription);
    }
  }


  private void trace(String message) {
    Trace.trace(Trace.BROWSER_LAUNCHING, message);
  }

}
