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
package com.github.sdbg.debug.core.internal.browser;

import com.github.sdbg.debug.core.DebugUIHelper;
import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.SDBGLaunchConfigWrapper;
import com.github.sdbg.debug.core.internal.browser.firefox.FirefoxBrowser;
import com.github.sdbg.debug.core.internal.browser.webkit.ChromeBrowser;
import com.github.sdbg.debug.core.internal.browser.webkit.ChromiumBrowser;
import com.github.sdbg.debug.core.internal.browser.webkit.EdgeBrowser;
import com.github.sdbg.debug.core.internal.util.CoreLaunchUtils;
import com.github.sdbg.debug.core.internal.util.ListeningStream;
import com.github.sdbg.debug.core.internal.util.LogTimer;
import com.github.sdbg.debug.core.internal.webkit.model.WebkitDebugTarget;
import com.github.sdbg.debug.core.model.IResourceResolver;
import com.github.sdbg.debug.core.util.IBrowserTabChooser;
import com.github.sdbg.debug.core.util.IBrowserTabInfo;
import com.github.sdbg.debug.core.util.IDeviceChooser;
import com.github.sdbg.debug.core.util.Trace;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
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

  private Semaphore launchSemaphore;

  private IBrowser browser;

  public BrowserManager() {
    this.launchSemaphore = new Semaphore(1);
  }

  public void connect(ILaunch launch, ILaunchConfiguration configuration,
      IResourceResolver resourceResolver, IBrowserTabChooser browserTabChooser, String host,
      int port, IProgressMonitor monitor) throws CoreException {
    try {
      SDBGLaunchConfigWrapper launchConfig = new SDBGLaunchConfigWrapper(configuration);

      LogTimer timer = new LogTimer("Browser debug connect");

      try {
        timer.startTask("connect");

        try {
          launchConfig.markAsLaunched();

          browser = getRemoteBrowser(host, port);
          browser.connectToBrowserDebug(
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
              true/*remote*/);
          return;
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
    catch(Exception ex)
    {
        DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);
        throw new CoreException(new Status(
            IStatus.ERROR,
            SDBGDebugCorePlugin.PLUGIN_ID,
            "Could not connect to browser"));
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
      browser = null;
    }
  }

  public void launchBrowser(ILaunch launch, ILaunchConfiguration configuration,
      IResourceResolver resourceResolver, String url,
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

          getBrowser(launchConfig);

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
                  url,
                  monitor,                  
                  timer,
                  true/*enableBreakpoints*/,
                  "127.0.0.1",
                  devToolsPortNumberHolder[0],
                  20 * 1000L/*maxStartupDelay*/,
                  browserOutput,
                  processDescription.toString(),
                  resourceResolver,
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

          DebugUIHelper.getHelper().activateApplication(browser.getExecutableFile(), browser.getName());

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

  private IBrowser getBrowser(SDBGLaunchConfigWrapper launchConfig)
  {
      String[] order = launchConfig.getBrowserSearchOrder().split(",");
      //Search by Properties.
      for(int i=0;this.browser == null && i<order.length;i++)
      {
          String browser = order[i];
          if("firefox".equals(browser))
          {
              this.browser = FirefoxBrowser.findBrowserByProperty();
          }
          else if("chromium".equals(browser))
          {
              this.browser = ChromiumBrowser.findBrowserByProperty();
          }
          else if("chrome".equals(browser))
          {
              this.browser = ChromeBrowser.findBrowserByProperty();
          }
          else if("edge".equals(browser))
          {
              this.browser = EdgeBrowser.findBrowserByProperty();
          }
      }
      if(this.browser == null)
      {
          SDBGDebugCorePlugin.logInfo("Browser not found by properties or environment.");
      }
      //Search default paths
      for(int i=0;this.browser == null && i<order.length;i++)
      {
          String browser = order[i];
          if("firefox".equals(browser))
          {
              this.browser = FirefoxBrowser.findBrowser();
          }
          else if("chromium".equals(browser))
          {
              this.browser = ChromiumBrowser.findBrowser();
          }
          else if("chrome".equals(browser))
          {
              this.browser = ChromeBrowser.findBrowser();
          }
          else if("edge".equals(browser))
          {
              this.browser = EdgeBrowser.findBrowser();
          }
      }
      if(browser != null)
      {
          SDBGDebugCorePlugin.logInfo("Found " + browser.getClass().getSimpleName() + ": " + browser.getExecutableFile().getAbsolutePath());
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

  public static void registerProcess(ILaunch launch, SDBGLaunchConfigWrapper launchConfig,
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

  /**
   * Detect the Remote Browser and returns an Object for that.
   * Currently only Firefox and Chromium are distinguished. All Chromium based Browsers like Chrome and Edge
   * will be used like Chromium.
   * @param host
   * @param port
   * @return FirefoxBrowser or ChromiumBrowser, depending on the answer from the debug port.
   * @throws UnknownHostException
   * @throws IOException
   */
  private IBrowser getRemoteBrowser(String host, int port) throws UnknownHostException, IOException
  {
      IBrowser browser = null;
      try
      {
          Socket sock = new Socket(host, port);
          sock.setSoTimeout(1000);
          InputStream in = sock.getInputStream();
          int ret = in.read();
          //Firefox will send some data right after connecting, while Chromium does not.
          browser = new FirefoxBrowser(null);
      }
      catch(SocketTimeoutException ex)
      {
          browser = new ChromiumBrowser(null);
      }
      return browser;
  }
}
