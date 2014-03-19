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
package com.github.sdbg.debug.core.configs;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.SDBGLaunchConfigWrapper;
import com.github.sdbg.debug.core.SDBGLaunchConfigurationDelegate;
import com.github.sdbg.debug.core.internal.util.BrowserManager;
import com.github.sdbg.debug.core.internal.webkit.protocol.ChromiumTabInfo;
import com.github.sdbg.debug.core.model.IResourceResolver;
import com.github.sdbg.debug.core.util.IBrowserTabChooser;
import com.github.sdbg.debug.core.util.IBrowserTabInfo;
import com.github.sdbg.utilities.instrumentation.InstrumentationBuilder;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;

/**
 * The launch configuration delegate for the com.github.sdbg.debug.core.chromeLaunchConfig launch
 * config.
 */
public class ChromeLaunchConfigurationDelegate extends SDBGLaunchConfigurationDelegate {
  private static class ChromeLaunchBrowserTabChooser implements IBrowserTabChooser {
    /** A fragment of the initial page, used to search for it in a list of open tabs. */
    private static final String CHROMIUM_INITIAL_PAGE_FRAGMENT = "chrome://version";

    public ChromeLaunchBrowserTabChooser() {
    }

    @Override
    public IBrowserTabInfo chooseTab(List<? extends IBrowserTabInfo> tabs) {
      for (IBrowserTabInfo tab : tabs) {
        if (tab.getTitle().contains(CHROMIUM_INITIAL_PAGE_FRAGMENT)) {
          return tab;
        }

        if (tab instanceof ChromiumTabInfo
            && ((ChromiumTabInfo) tab).getUrl().contains(CHROMIUM_INITIAL_PAGE_FRAGMENT)) {
          return tab;
        }
      }

      // Return the first visible, non-Chrome extension tab.
      for (IBrowserTabInfo tab : tabs) {
        if (!(tab instanceof ChromiumTabInfo) || !((ChromiumTabInfo) tab).isChromeExtension()) {
          return tab;
        }
      }

      return null;
    }

  }

  private static BrowserManager browserManager = new BrowserManager(".sdbg-chrome");

  public static void dispose() {
    browserManager.dispose();
  }

  /**
   * Create a new ChromeLaunchConfigurationDelegate.
   */
  public ChromeLaunchConfigurationDelegate() {
  }

  @Override
  public void doLaunch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor, InstrumentationBuilder instrumentation) throws CoreException {

    if (!ILaunchManager.RUN_MODE.equals(mode) && !ILaunchManager.DEBUG_MODE.equals(mode)) {
      throw new CoreException(SDBGDebugCorePlugin.createErrorStatus("Execution mode '" + mode
          + "' is not supported."));
    }

    SDBGLaunchConfigWrapper launchConfig = new SDBGLaunchConfigWrapper(configuration);
    IResourceResolver resourceResolver = getResourceResolver(new SDBGLaunchConfigWrapper(
        configuration));

    browserManager.launchBrowser(
        launch,
        configuration,
        resourceResolver,
        new ChromeLaunchBrowserTabChooser(),
        resolveLaunchUrl(resourceResolver, launchConfig),
        monitor,
        ILaunchManager.DEBUG_MODE.equals(mode),
        Collections.<String> emptyList());
  }
}
