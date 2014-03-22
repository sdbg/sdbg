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
package com.github.sdbg.debug.core;

import com.github.sdbg.debug.core.internal.util.CoreLaunchUtils;
import com.github.sdbg.debug.core.internal.util.LaunchConfigResourceResolver;
import com.github.sdbg.debug.core.model.IResourceResolver;
import com.github.sdbg.debug.core.util.IBrowserTabChooser;
import com.github.sdbg.debug.core.util.ResourceServerManager;
import com.github.sdbg.utilities.instrumentation.Instrumentation;
import com.github.sdbg.utilities.instrumentation.InstrumentationBuilder;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;

/**
 * Super class for all launch configuration delegates
 */
public abstract class SDBGLaunchConfigurationDelegate extends LaunchConfigurationDelegate {
  private static IBrowserTabChooser uiBrowserTabChooser;

  protected static synchronized IBrowserTabChooser getUIBrowserTabChooser() {
    if (uiBrowserTabChooser == null) {
      IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(
          "com.github.sdbg.debug.core.uiBrowserTabChooser");
      for (IConfigurationElement element : extensionPoint.getConfigurationElements()) {
        try {
          uiBrowserTabChooser = (IBrowserTabChooser) element.createExecutableExtension("class");
          break;
        } catch (CoreException e) {
          SDBGDebugCorePlugin.logError(e);
        }
      }
    }

    return uiBrowserTabChooser;
  }

  @Override
  public final boolean buildForLaunch(ILaunchConfiguration configuration, String mode,
      IProgressMonitor monitor) throws CoreException {
    return false;
  }

  public abstract void doLaunch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor, InstrumentationBuilder instrumentation) throws CoreException;

  @Override
  public final void launch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor) throws CoreException {
    InstrumentationBuilder instrumentation = Instrumentation.builder(this.getClass());

    try {
      instrumentation.metric("Mode", mode);

      doLaunch(configuration, mode, launch, monitor, instrumentation);

    } catch (CoreException e) {
      DebugUIHelper.getHelper().showError("Error Launching Application", e.getMessage());
      CoreLaunchUtils.removeLaunch(launch);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  protected IProject[] getBuildOrder(ILaunchConfiguration configuration, String mode)
      throws CoreException {
    // indicate which project to save before launch
    SDBGLaunchConfigWrapper launchConfig = new SDBGLaunchConfigWrapper(configuration);

    IResource resource = launchConfig.getApplicationResource();
    if (resource != null) {
      return new IProject[] {resource.getProject()};
    }

    if (launchConfig.getProject() != null) {
      return new IProject[] {launchConfig.getProject()};
    }

    return null;
  }

  protected IResourceResolver getResourceResolver(SDBGLaunchConfigWrapper launchConfig)
      throws CoreException {
    return launchConfig.getShouldLaunchFile() ? getResourceServer()
        : new LaunchConfigResourceResolver(launchConfig);
  }

  protected String resolveLaunchUrl(IResourceResolver resourceResolver,
      SDBGLaunchConfigWrapper launchConfig) throws CoreException {
    String url;

    if (launchConfig.getShouldLaunchFile()) {
      IResource resource = launchConfig.getApplicationResource();
      if (resource == null) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            SDBGDebugCorePlugin.PLUGIN_ID,
            "HTML file could not be found"));
      }

      if (resource instanceof IFile) {
        url = resourceResolver.getUrlForResource(resource);
      } else {
        url = resource.getLocationURI().toString();
      }
    } else {
      url = launchConfig.getUrl();
    }

    return launchConfig.appendQueryParams(url);
  }

  private IResourceResolver getResourceServer() throws CoreException {
    try {
      return ResourceServerManager.getServer();
    } catch (IOException ioe) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          SDBGDebugCorePlugin.PLUGIN_ID,
          ioe.getMessage(),
          ioe));
    }
  }
}
