/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.github.sdbg.debug.core.configs;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.SDBGLaunchConfigWrapper;
import com.github.sdbg.debug.core.SDBGLaunchConfigurationDelegate;
import com.github.sdbg.debug.core.internal.util.BrowserManager;
import com.github.sdbg.debug.core.internal.webkit.protocol.ChromiumConnector;
import com.github.sdbg.debug.core.internal.webkit.protocol.ChromiumTabInfo;
import com.github.sdbg.debug.core.util.IBrowserTabChooser;
import com.github.sdbg.debug.core.util.IBrowserTabInfo;
import com.github.sdbg.utilities.instrumentation.InstrumentationBuilder;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;

/**
 * A ILaunchConfigurationDelegate implementation that can attach to a running Chrome instance.
 */
public class ChromeConnLaunchConfigurationDelegate extends SDBGLaunchConfigurationDelegate {
  /**
   * Create a new ChromeConnLaunchConfigurationDelegate.
   */
  public ChromeConnLaunchConfigurationDelegate() {
  }

  @Override
  public void doLaunch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor, InstrumentationBuilder instrumentation) throws CoreException {
    if (!ILaunchManager.DEBUG_MODE.equals(mode)) {
      throw new CoreException(SDBGDebugCorePlugin.createErrorStatus("Execution mode '" + mode
          + "' is not supported."));
    }

    SDBGLaunchConfigWrapper wrapper = new SDBGLaunchConfigWrapper(configuration);
    final String url = wrapper.getUrl();
    IBrowserTabChooser tabChooser = new IBrowserTabChooser() {
      @Override
      public IBrowserTabInfo chooseTab(List<? extends IBrowserTabInfo> tabs) {
        return null;
      }
    };

    BrowserManager browserManager = new BrowserManager();
    browserManager.connect(
        launch,
        configuration,
        tabChooser,
        wrapper.getConnectionHost(),
        wrapper.getConnectionPort(),
        monitor);
  }

// TODO: Unfinished
  private ChromiumTabInfo findTargetTab(List<ChromiumTabInfo> tabs) {
    for (ChromiumTabInfo tab : tabs) {
      if (tab.getTitle().startsWith("chrome-extension://")) {
        continue;
      }

      // chrome-extension://kohcodfehgoaolndkcophkcmhjenpfmc/_generated_background_page.html
      if (tab.getTitle().endsWith("_generated_background_page.html")) {
        continue;
      }

      // chrome-extension://nkeimhogjdpnpccoofpliimaahmaaome/background.html
      if (tab.getUrl().endsWith("_generated_background_page.html")
          || tab.getUrl().endsWith("/background.html")) {
        continue;
      }

      if (tab.getUrl().startsWith("chrome-extension://") && tab.getTitle().length() > 0) {
        return tab;
      }
    }

    return null;
  }

  private ChromiumTabInfo getChromiumTab(int port) throws CoreException {
    // Give Chromium 10 seconds to start up.
    final int maxStartupDelay = 10 * 1000;

    long endTime = System.currentTimeMillis() + maxStartupDelay;

    while (true) {
      try {
        List<ChromiumTabInfo> tabs = ChromiumConnector.getAvailableTabs(port);

        ChromiumTabInfo targetTab = findTargetTab(tabs);

        if (targetTab != null) {
          for (ChromiumTabInfo tab : tabs) {
            SDBGDebugCorePlugin.log("Found: " + tab.toString());
          }

          SDBGDebugCorePlugin.log("Choosing: " + targetTab);

          return targetTab;
        }
      } catch (IOException exception) {
        if (System.currentTimeMillis() > endTime) {
          throw new CoreException(new Status(
              IStatus.ERROR,
              SDBGDebugCorePlugin.PLUGIN_ID,
              "Could not connect to Chrome",
              exception));
        }
      }

      if (System.currentTimeMillis() > endTime) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            SDBGDebugCorePlugin.PLUGIN_ID,
            "Timed out trying to connect to Chrome"));
      }

      sleep(250);
    }
  }

  private DebugException newDebugException(String message) {
    return new DebugException(new Status(IStatus.ERROR, SDBGDebugCorePlugin.PLUGIN_ID, message));
  }

  private DebugException newDebugException(Throwable t) {
    return new DebugException(new Status(
        IStatus.ERROR,
        SDBGDebugCorePlugin.PLUGIN_ID,
        t.toString(),
        t));
  }

  private void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (Exception exception) {

    }
  }
}
