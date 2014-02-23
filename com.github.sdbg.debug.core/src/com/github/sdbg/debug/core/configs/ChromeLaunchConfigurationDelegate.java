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
import com.github.sdbg.debug.core.model.IRemoteConnectionDelegate;
import com.github.sdbg.debug.core.util.DefaultBrowserTabChooser;
import com.github.sdbg.debug.core.util.IBrowserTabChooser;
import com.github.sdbg.utilities.instrumentation.InstrumentationBuilder;

import java.util.concurrent.Semaphore;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;

/**
 * The launch configuration delegate for the com.github.sdbg.debug.core.chromeLaunchConfig launch
 * config.
 */
public class ChromeLaunchConfigurationDelegate extends SDBGLaunchConfigurationDelegate implements
    IRemoteConnectionDelegate {
  private static Semaphore launchSemaphore = new Semaphore(1);

  private IBrowserTabChooser tabChooser;

  /**
   * Create a new DartChromiumLaunchConfigurationDelegate.
   */
  public ChromeLaunchConfigurationDelegate() {
    this(new DefaultBrowserTabChooser());
  }

  public ChromeLaunchConfigurationDelegate(IBrowserTabChooser tabChooser) {
    this.tabChooser = tabChooser;
  }

  @Override
  public void doLaunch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor, InstrumentationBuilder instrumentation) throws CoreException {

    if (!ILaunchManager.RUN_MODE.equals(mode) && !ILaunchManager.DEBUG_MODE.equals(mode)) {
      throw new CoreException(SDBGDebugCorePlugin.createErrorStatus("Execution mode '" + mode
          + "' is not supported."));
    }

    SDBGLaunchConfigWrapper launchConfig = new SDBGLaunchConfigWrapper(configuration);

    // If we're in the process of launching Dartium, don't allow a second launch to occur.
    if (launchSemaphore.tryAcquire()) {
      try {
        launchImpl(launchConfig, mode, launch, monitor);
      } finally {
        launchSemaphore.release();
      }
    }
  }

  @Override
  public IDebugTarget performRemoteConnection(String host, int port, IProgressMonitor monitor)
      throws CoreException {
    BrowserManager browserManager = new BrowserManager();

    return browserManager.performRemoteConnection(tabChooser, host, port, monitor);
  }

  private void launchImpl(SDBGLaunchConfigWrapper launchConfig, String mode, ILaunch launch,
      IProgressMonitor monitor) throws CoreException {
    launchConfig.markAsLaunched();

    boolean enableDebugging = ILaunchManager.DEBUG_MODE.equals(mode);
    //&&&&& !DartCoreDebug.DISABLE_BROWSER_DEBUGGER;

    // Launch the browser - show errors if we couldn't.
    IResource resource = null;
    String url;

    if (launchConfig.getShouldLaunchFile()) {
      resource = launchConfig.getApplicationResource();
      if (resource == null) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            SDBGDebugCorePlugin.PLUGIN_ID,
            "HTML file could not be found"));
      }
      url = resource.getLocationURI().toString();
    } else {
      url = launchConfig.getUrl();
    }

    BrowserManager manager = BrowserManager.getManager();

    if (resource instanceof IFile) {
      manager.launchBrowser(launch, launchConfig, (IFile) resource, monitor, enableDebugging);
    } else {
      manager.launchBrowser(launch, launchConfig, url, monitor, enableDebugging);
    }
  }

}
