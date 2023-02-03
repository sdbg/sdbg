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
import com.github.sdbg.debug.core.internal.browser.BrowserManager;
import com.github.sdbg.debug.core.model.IResourceResolver;
import com.github.sdbg.debug.core.util.IBrowserTabChooser;
import com.github.sdbg.debug.core.util.IBrowserTabInfo;
import com.github.sdbg.utilities.OSUtilities;
import com.github.sdbg.utilities.instrumentation.InstrumentationBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;

//[ {
//  "title": "New Tab",
//  "type": "page",
//  "url": "chrome://newtab/",
//}, {
//  "title": "chrome-extension://becjelbpddbpmopbobpojhgneicbhlgj/_generated_background_page.html",
//  "type": "other",
//  "url": "chrome-extension://becjelbpddbpmopbobpojhgneicbhlgj/_generated_background_page.html",
//}, {
//  "title": "Packy",
//  "type": "other",
//  "url": "chrome-extension://becjelbpddbpmopbobpojhgneicbhlgj/packy.html",
//} ]

/**
 * A ILaunchConfigurationDelegate implementation that can launch Chrome applications. We
 * conceptually launch the manifest.json file which specifies a Chrome app. We currently send Chrome
 * the path to the manifest file's parent directory via the --load-extension flag.
 */
// TODO: This will not work out of the box for GWT SuperDevMode, because GWT SDM is done using a web server and the generated JS file and sourcemaps 
// do not have a fixed location on the disk - a new directory with these is created on each new SDM recompilation
public class ChromeAppLaunchConfigurationDelegate extends SDBGLaunchConfigurationDelegate {
  private static class ChromeAppBrowserTabChooser implements IBrowserTabChooser {
    @Override
    public IBrowserTabInfo chooseTab(List<? extends IBrowserTabInfo> tabs) {
      for (IBrowserTabInfo tab : tabs) {
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
  }

  private static class ChromeAppResourceResolver implements IResourceResolver {
    private IContainer container;
    private String prefix;

    public ChromeAppResourceResolver(IContainer container, IBrowserTabInfo tab) {
      this.container = container;

      prefix = tab.getUrl();
      int index = prefix.indexOf("//");

      if (index != -1) {
        index = prefix.indexOf('/', index + 2);

        if (index != -1) {
          prefix = prefix.substring(0, index + 1);
        }
      }
    }

    @Override
    public String getUrlForFile(File file) {
      IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(file.toURI());

      if (files.length > 0) {
        return getUrlForResource(files[0]);
      } else {
        return null;
      }
    }

    @Override
    public String getUrlForResource(IResource resource) {
      String relPath = calcRelPath(container, resource);

      if (relPath != null) {
        return prefix + relPath;
      } else {
        return null;
      }
    }

    @Override
    public String getUrlRegexForResource(IResource resource) {
      String relPath = calcRelPath(container, resource);
      if (relPath != null) {
        return relPath;
      }

      return resource.getFullPath().toString();
    }

    @Override
    public IResource resolveUrl(String url) {
      if (url.startsWith(prefix)) {
        return container.findMember(url.substring(prefix.length()));
      } else {
        return null;
      }
    }

    private String calcRelPath(IContainer container, IResource resource) {
      if (container == null) {
        return null;
      }

      String containerPath = container.getFullPath().toString();
      String resourcePath = resource.getFullPath().toString();

      if (resourcePath.startsWith(containerPath)) {
        String relPath = resourcePath.substring(containerPath.length());

        if (relPath.startsWith("/")) {
          return relPath.substring(1);
        } else {
          return relPath;
        }
      } else {
        return null;
      }
    }
  }

  private static BrowserManager browserManager;

  public static void dispose() {
    browserManager.dispose();
  }

  /**
   * Create a new ChromeAppLaunchConfigurationDelegate.
   */
  public ChromeAppLaunchConfigurationDelegate() {
  }

  @Override
  public void doLaunch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor, InstrumentationBuilder instrumentation) throws CoreException {
    if (!ILaunchManager.RUN_MODE.equals(mode) && !ILaunchManager.DEBUG_MODE.equals(mode)) {
      throw new CoreException(SDBGDebugCorePlugin.createErrorStatus("Execution mode '" + mode
          + "' is not supported."));
    }

    SDBGLaunchConfigWrapper wrapper = new SDBGLaunchConfigWrapper(configuration);

    IResource jsonResource = wrapper.getApplicationResource();
    if (jsonResource == null) {
      throw new CoreException(
          SDBGDebugCorePlugin.createErrorStatus("No manifest.json file specified to launch."));
    }

    List<String> extraCommandLineArgs = new ArrayList<String>();

    // This is currently only supported on the mac.
    if (OSUtilities.isMac()) {
      extraCommandLineArgs.add("--no-startup-window");
    }

    extraCommandLineArgs.add("--load-and-launch-app="
        + jsonResource.getParent().getLocation().toFile().getAbsolutePath());

    browserManager = new BrowserManager("chrome-app") {
        @Override
        protected IResourceResolver createResourceResolver(ILaunch launch,
            ILaunchConfiguration configuration, IBrowserTabInfo tab) {
          SDBGLaunchConfigWrapper wrapper = new SDBGLaunchConfigWrapper(configuration);
          return new ChromeAppResourceResolver(wrapper.getApplicationResource().getParent(), tab);
        }
    };    
    browserManager.launchBrowser(
        launch,
        configuration,
        null/*resolver*/,
        new ChromeAppBrowserTabChooser(),
        null/*url*/,
        monitor,
        ILaunchManager.DEBUG_MODE.equals(mode),
        extraCommandLineArgs);
  }
}
