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
package com.github.sdbg.debug.ui.internal.chrome;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.SDBGLaunchConfigWrapper;
import com.github.sdbg.debug.ui.internal.DartUtil;
import com.github.sdbg.debug.ui.internal.util.AbstractLaunchShortcut;
import com.github.sdbg.debug.ui.internal.util.ILaunchShortcutExt;
import com.github.sdbg.debug.ui.internal.util.LaunchUtils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;

/**
 * A launch shortcut to allow users to launch applications in Chrome.
 */
public class ChromeLaunchShortcut extends AbstractLaunchShortcut implements ILaunchShortcutExt {

  public ChromeLaunchShortcut() {
    super("Chrome");
  }

  @Override
  public boolean canLaunch(IResource resource) {
//&&&    
//    if (!DartSdkManager.getManager().hasSdk()) {
//      return false;
//    }
//
//    if (!DartSdkManager.getManager().getSdk().isDartiumInstalled()) {
//      return false;
//    }
//
    if (resource instanceof IFile) {
      if ("html".equalsIgnoreCase(resource.getFileExtension())) {
        return true;
      }
    }

    return isBrowserApplication(resource);
  }

  @Override
  protected ILaunchConfigurationType getConfigurationType() {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType type = manager.getLaunchConfigurationType(SDBGDebugCorePlugin.CHROME_LAUNCH_CONFIG_ID);

    return type;
  }

  @Override
  public void launch(IResource resource, String mode) {
    launch(resource, mode, null);
  }

  public void launch(IResource resource, String mode, String url) {
    if (resource == null) {
      return;
    }

    // Launch an existing configuration if one exists
    ILaunchConfiguration config;

    try {
      config = findConfig(resource);
    } catch (OperationCanceledException ex) {
      return;
    }

    if (config == null) {
      // Create and launch a new configuration
      ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
      ILaunchConfigurationType type = manager.getLaunchConfigurationType(SDBGDebugCorePlugin.CHROME_LAUNCH_CONFIG_ID);
      ILaunchConfigurationWorkingCopy launchConfig = null;
      try {
        launchConfig = type.newInstance(
            null,
            manager.generateLaunchConfigurationName(resource.getName()));
      } catch (CoreException ce) {
        DartUtil.logError(ce);
        return;
      }

      SDBGLaunchConfigWrapper launchWrapper = new SDBGLaunchConfigWrapper(launchConfig);

      launchWrapper.setApplicationName(resource.getFullPath().toString());
      launchWrapper.setProjectName(resource.getProject().getName());
      if (url != null) {
        launchWrapper.setUrl(url);
      }

      launchConfig.setMappedResources(new IResource[] {resource});

      try {
        config = launchConfig.doSave();
      } catch (CoreException e) {
        DartUtil.logError(e);
        return;
      }
    }

    SDBGLaunchConfigWrapper launchWrapper = new SDBGLaunchConfigWrapper(config);
    launchWrapper.markAsLaunched();
    LaunchUtils.clearConsoles();

    LaunchUtils.launch(config, mode);
  }

  @Override
  protected boolean testSimilar(IResource resource, ILaunchConfiguration config) {
    return super.testSimilar(resource, config);
  }

}
