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
package com.github.sdbg.debug.ui.internal.browser;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.SDBGLaunchConfigWrapper;
import com.github.sdbg.debug.core.SDBGLaunchConfigurationDelegate;
import com.github.sdbg.debug.core.util.ResourceServer;
import com.github.sdbg.debug.core.util.ResourceServerManager;
import com.github.sdbg.debug.ui.internal.SDBGDebugUIPlugin;
import com.github.sdbg.utilities.OSUtilities;
import com.github.sdbg.utilities.ProcessRunner;
import com.github.sdbg.utilities.instrumentation.InstrumentationBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

/**
 * Launches the Dart application (compiled to js) in the browser.
 */
public class BrowserLaunchConfigurationDelegate extends SDBGLaunchConfigurationDelegate {

  public static void openBrowser(String url) throws CoreException {
    IWebBrowser browser = null;
    try {
      browser = PlatformUI.getWorkbench().getBrowserSupport().createBrowser(
          IWorkbenchBrowserSupport.AS_EXTERNAL,
          "defaultBrowser",
          "Default Browser",
          "Browser");
      if (browser != null) {
        final IWebBrowser defaultBrowser = browser;
        final URL urlToOpen = new URL(url);

        Display.getDefault().asyncExec(new Runnable() {

          @Override
          public void run() {
            try {
              defaultBrowser.openURL(urlToOpen);
            } catch (PartInitException e) {
              SDBGDebugCorePlugin.logError(
                  Messages.BrowserLaunchConfigurationDelegate_DefaultBrowserNotFound,
                  e);
            }
          }
        });
      } else {
        throw new CoreException(new Status(
            IStatus.ERROR,
            SDBGDebugCorePlugin.PLUGIN_ID,
            Messages.BrowserLaunchConfigurationDelegate_DefaultBrowserNotFound));
      }
    } catch (MalformedURLException e) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          SDBGDebugCorePlugin.PLUGIN_ID,
          Messages.BrowserLaunchConfigurationDelegate_UrlError));
    }
  }

  /**
   * Match both the input and id, so that different types of editor can be opened on the same input.
   */
  @Override
  public void doLaunch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor, InstrumentationBuilder instrumentation) throws CoreException {

    mode = ILaunchManager.RUN_MODE;

    SDBGLaunchConfigWrapper wrapper = new SDBGLaunchConfigWrapper(configuration);
    wrapper.markAsLaunched();

    String url;

    if (wrapper.getShouldLaunchFile()) {
      IResource resource = wrapper.getApplicationResource();

      if (resource == null) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            SDBGDebugCorePlugin.PLUGIN_ID,
            Messages.BrowserLaunchConfigurationDelegate_HtmlFileNotFound));
      }

      try {
        // This returns just a plain file: url.
        ResourceServer server = ResourceServerManager.getServer();

        url = server.getUrlForResource(resource);

        url = wrapper.appendQueryParams(url);
      } catch (IOException ioe) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            SDBGDebugCorePlugin.PLUGIN_ID,
            "Could not launch browser - unable to start embedded server",
            ioe));
      }
    } else {
      url = wrapper.getUrl();

      try {
        String scheme = new URI(url).getScheme();

        if (scheme == null) { // add scheme else browser will not launch
          url = "http://" + url;
        }
      } catch (URISyntaxException e) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            SDBGDebugCorePlugin.PLUGIN_ID,
            Messages.BrowserLaunchConfigurationDelegate_UrlError));
      }
    }

    if (SDBGDebugCorePlugin.getPlugin().getIsDefaultBrowser()) {
      openBrowser(url);
    } else {
      launchInExternalBrowser(url);
    }

    DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);

  }

  private void launchInExternalBrowser(final String url) throws CoreException {

    String browserName = SDBGDebugCorePlugin.getPlugin().getBrowserName();
    if (browserName.length() == 0) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          SDBGDebugUIPlugin.PLUGIN_ID,
          "Specify browser to launch in Preferences > Run and Debug"));
    }

    List<String> cmd = new ArrayList<String>();

    if (OSUtilities.isMac()) {
      // use open command on mac
      cmd.add("/usr/bin/open");
      cmd.add("-a");
    }
    cmd.add(browserName);
    cmd.add(url);

    if (SDBGDebugCorePlugin.getPlugin().getBrowserArgs().length() != 0) {
      if (OSUtilities.isMac()) {
        cmd.add("--args");
        cmd.add(SDBGDebugCorePlugin.getPlugin().getBrowserArgs());
      } else {
        cmd.addAll(Arrays.asList(SDBGDebugCorePlugin.getPlugin().getBrowserArgsAsArray()));
      }
    }

    try {
      ProcessBuilder builder = new ProcessBuilder(cmd);
      ProcessRunner runner = new ProcessRunner(builder);

      runner.runAsync();
      runner.await(new NullProgressMonitor(), 500);

      if (runner.getExitCode() != 0) {
        if (OSUtilities.isWindows()) {
          if (browserName.toLowerCase().indexOf("firefox") != -1) {
            if (runner.getExitCode() == 1) {
              // In this case, the application was opened in a new tab successfully.
              // Don't throw an exception.

              return;
            }
          }
        }

        throw new CoreException(new Status(
            IStatus.ERROR,
            SDBGDebugUIPlugin.PLUGIN_ID,
            "Could not launch browser \"" + browserName + "\" : \n\n" + runner.getStdErr()));
      }
    } catch (IOException e) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          SDBGDebugCorePlugin.PLUGIN_ID,
          Messages.BrowserLaunchConfigurationDelegate_BrowserNotFound,
          e));
    }
  }

  @SuppressWarnings("unused")
  private IResource locateMappedFile(IResource resourceFile) {
    String mappingPath = null; // &&& DartCore.getResourceRemapping((IFile) resourceFile);

    if (mappingPath != null) {
      IResource mappedResource = ResourcesPlugin.getWorkspace().getRoot().findMember(
          Path.fromPortableString(mappingPath));

      if (mappedResource != null && mappedResource.exists()) {
        return mappedResource;
      }
    }
    return null;
  }

}
