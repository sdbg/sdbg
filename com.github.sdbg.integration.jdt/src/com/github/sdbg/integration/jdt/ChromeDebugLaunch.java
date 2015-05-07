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
  @Override
  public void launch(IProject project, String url, String mode) {
    if (project == null || url == null || mode == null) {
      return;
    }

    try {
      ILaunchConfiguration config;

      try {
        // Select an existing configuration if one exists
        config = findConfig(project, url);
      } catch (OperationCanceledException ex) {
        return;
      }

      if (config == null) {
        // Otherwise, create a new one
        config = createConfig(project, url);
      }

      // Launch the configuration
      SDBGLaunchConfigWrapper launchWrapper = new SDBGLaunchConfigWrapper(config);
      launchWrapper.markAsLaunched();

      LaunchUtils.clearConsoles();
      LaunchUtils.launch(config, mode);
    } catch (CoreException e) {
      SDBGJDTIntegrationPlugin.wrapError(e);
    }
  }

  /**
   * Returns a configuration from the given collection of configurations that should be launched, or
   * <code>null</code> to cancel. Default implementation opens a selection dialog that allows the
   * user to choose one of the specified launch configurations. Returns the chosen configuration, or
   * <code>null</code> if the user cancels.
   * 
   * @param configList list of configurations to choose from
   * @return configuration to launch or <code>null</code> to cancel
   */
  private ILaunchConfiguration chooseConfig(List<ILaunchConfiguration> configList) {
    IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
    ElementListSelectionDialog dialog = new ElementListSelectionDialog(
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
        labelProvider);
    dialog.setElements(configList.toArray());
    dialog.setTitle("Select Configuration");
    dialog.setMessage("&Select existing configuration:");
    dialog.setMultipleSelection(false);
    int result = dialog.open();
    labelProvider.dispose();
    if (result == Window.OK) {
      return (ILaunchConfiguration) dialog.getFirstResult();
    }
    return null;
  }

  private ILaunchConfiguration createConfig(IProject project, String url) throws CoreException {
    // Create and launch a new configuration
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType type = manager.getLaunchConfigurationType(SDBGDebugCorePlugin.CHROME_LAUNCH_CONFIG_ID);
    ILaunchConfigurationWorkingCopy launchConfig = type.newInstance(
        null,
        manager.generateLaunchConfigurationName(project.getName()));

    SDBGLaunchConfigWrapper launchWrapper = new SDBGLaunchConfigWrapper(launchConfig);

    launchWrapper.setApplicationName(project.getFullPath().toString());
    launchWrapper.setProjectName(project.getProject().getName());
    if (url != null) {
      launchWrapper.setUrl(url);
    }

    launchConfig.setMappedResources(new IResource[] {project});

    return launchConfig.doSave();
  }

  /**
   * Find the launch configuration associated with the specified resource
   * 
   * @param resource the resource
   * @return the launch configuration or <code>null</code> if none
   */
  private ILaunchConfiguration findConfig(IProject project, String url)
      throws OperationCanceledException, CoreException {
    List<ILaunchConfiguration> candidateConfigs = Arrays.asList(getAssociatedConfigs(
        project,
        url));

    int candidateCount = candidateConfigs.size();
    if (candidateCount == 1) {
      return candidateConfigs.get(0);
    } else if (candidateCount > 1) {
      ILaunchConfiguration result = chooseConfig(candidateConfigs);
      if (result != null) {
        return result;
      } else {
        throw new OperationCanceledException();
      }
    }

    return null;
  }

  private ILaunchConfiguration[] getAssociatedConfigs(IProject project, String url)
      throws CoreException {
    List<ILaunchConfiguration> results = new ArrayList<ILaunchConfiguration>();

    ILaunchConfiguration[] configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(
        getConfigurationType());

    for (int i = 0; i < configs.length; i++) {
      ILaunchConfiguration config = configs[i];

      if (testSimilar(project, url, config)) {
        results.add(config);
      }
    }

    return results.toArray(new ILaunchConfiguration[results.size()]);
  }

  private ILaunchConfigurationType getConfigurationType() {
    ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    return manager.getLaunchConfigurationType(SDBGDebugCorePlugin.CHROME_LAUNCH_CONFIG_ID);
  }

  /**
   * Return whether the launch configuration is used to launch the given resource.
   * 
   * @param project
   * @param config
   * @return whether the launch configuration is used to launch the given resource
   */
  private boolean testSimilar(IProject project, String url, ILaunchConfiguration config) {
    SDBGLaunchConfigWrapper launchWrapper = new SDBGLaunchConfigWrapper(config);

    IProject otherProject = launchWrapper.getProject();
    String otherUrl = launchWrapper.getUrl();
    return project != null && project.equals(otherProject) && url != null && url.equals(otherUrl);
  }
}
