package com.github.sdbg.integration.jdt;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.SDBGLaunchConfigWrapper;
import com.github.sdbg.debug.ui.internal.util.LaunchUtils;
import com.google.gdt.eclipse.core.IDebugLaunch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

public class ChromeDebugLaunch implements IDebugLaunch {

  /**
   * Returns a configuration from the given collection of configurations that should be launched, or
   * <code>null</code> to cancel. Default implementation opens a selection dialog that allows the
   * user to choose one of the specified launch configurations. Returns the chosen configuration, or
   * <code>null</code> if the user cancels.
   * 
   * @param configList list of configurations to choose from
   * @return configuration to launch or <code>null</code> to cancel
   */
  private ILaunchConfiguration chooseConfiguration(List<ILaunchConfiguration> configList) {
    IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
    ElementListSelectionDialog dialog = new ElementListSelectionDialog(
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
        labelProvider);
    dialog.setElements(configList.toArray());
    dialog.setTitle("Select Dart Application");
    dialog.setMessage("&Select existing configuration:");
    dialog.setMultipleSelection(false);
    int result = dialog.open();
    labelProvider.dispose();
    if (result == Window.OK) {
      return (ILaunchConfiguration) dialog.getFirstResult();
    }
    return null;
  }

  /**
   * Find the launch configuration associated with the specified resource
   * 
   * @param resource the resource
   * @return the launch configuration or <code>null</code> if none
   */
  private final ILaunchConfiguration findConfig(IResource resource)
      throws OperationCanceledException {
    List<ILaunchConfiguration> candidateConfigs = Arrays.asList(getAssociatedLaunchConfigurations(resource));

    int candidateCount = candidateConfigs.size();

    if (candidateCount == 1) {
      return candidateConfigs.get(0);
    } else if (candidateCount > 1) {
      ILaunchConfiguration result = chooseConfiguration(candidateConfigs);
      if (result != null) {
        return result;
      } else {
        throw new OperationCanceledException();
      }
    }

    return null;
  }

  private ILaunchConfiguration[] getAssociatedLaunchConfigurations(IResource resource) {
    List<ILaunchConfiguration> results = new ArrayList<ILaunchConfiguration>();

    try {
      ILaunchConfiguration[] configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(
          getConfigurationType());

      if (resource != null) {
        for (int i = 0; i < configs.length; i++) {
          ILaunchConfiguration config = configs[i];

          if (testSimilar(resource, config)) {
            results.add(config);
          }
        }
      }
    } catch (CoreException e) {
      SDBGJDTIntegrationPlugin.wrapError(e);
    }

    return results.toArray(new ILaunchConfiguration[results.size()]);
  }

  private ILaunchConfigurationType getConfigurationType() {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType type = manager.getLaunchConfigurationType(SDBGDebugCorePlugin.CHROME_LAUNCH_CONFIG_ID);

    return type;
  }

  @Override
  public void launch(IProject resource, String url, String mode) {
    if (resource == null || url == null || mode == null) {
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
        SDBGJDTIntegrationPlugin.wrapError(ce);
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
        SDBGJDTIntegrationPlugin.wrapError(e);
        return;
      }
    }

    SDBGLaunchConfigWrapper launchWrapper = new SDBGLaunchConfigWrapper(config);
    launchWrapper.markAsLaunched();

    LaunchUtils.clearConsoles();
    LaunchUtils.launch(config, mode);
  }

  /**
   * Return whether the launch configuration is used to launch the given resource.
   * 
   * @param resource
   * @param config
   * @return whether the launch configuration is used to launch the given resource
   */
  private boolean testSimilar(IResource resource, ILaunchConfiguration config) {
    SDBGLaunchConfigWrapper launchWrapper = new SDBGLaunchConfigWrapper(config);

    IResource appResource = launchWrapper.getApplicationResource();

    return appResource == resource || appResource != null && appResource.equals(resource);
  }
}
