package com.github.sdbg.debug.ui.internal;

import com.github.sdbg.core.util.instrumentation.InstrumentationBuilder;
import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.SDBGLaunchConfigWrapper;

import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * Helper class for instrumenting launch and debug services
 */
public class DebugInstrumentationUtilities {

  public static void recordLaunchConfiguration(SDBGLaunchConfigWrapper launch,
      InstrumentationBuilder instrumentation) {

    instrumentation.data("LaunchConfig-ApplicationName", launch.getApplicationName());
    instrumentation.data("LaunchConfig-getProjectName", launch.getProjectName());
    instrumentation.data("LaunchConfig-getUrl", launch.getUrl());
    instrumentation.data("LaunchConfig-getWorkingDirectory", launch.getWorkingDirectory());

    instrumentation.metric("LaunchConfig-getArguments", launch.getArguments());
    instrumentation.metric(
        "LaunchConfig-getBrowserName",
        SDBGDebugCorePlugin.getPlugin().getBrowserName());
    instrumentation.metric("LaunchConfig-getCheckedMode", String.valueOf(launch.getCheckedMode()));
    instrumentation.metric("LaunchConfig-getLastLaunchTime", launch.getLastLaunchTime());
    instrumentation.metric(
        "LaunchConfig-getShouldLaunchFile",
        String.valueOf(launch.getShouldLaunchFile()));
    instrumentation.metric(
        "LaunchConfig-getShowLaunchOutput",
        String.valueOf(launch.getShowLaunchOutput()));
    instrumentation.metric(
        "LaunchConfig-getUseDefaultBrowser",
        String.valueOf(SDBGDebugCorePlugin.getPlugin().getIsDefaultBrowser()));
    instrumentation.metric(
        "LaunchConfig-getUseWebComponents",
        String.valueOf(launch.isEnableExperimentalWebkitFeatures()));
    instrumentation.metric("LaunchConfig-getVmArgumentsAsArray", launch.getVmArgumentsAsArray());

  }

  public static void recordLaunchConfiguration(ILaunchConfiguration launch,
      InstrumentationBuilder instrumentation) {

    try {
      instrumentation.metric("launchConfig-getCategory", launch.getCategory());
      instrumentation.metric("launchConfig-getClass", launch.getClass().toString());

      instrumentation.data("launchConfig-getName", launch.getName());

    } catch (Exception e) {
    }
  }

}
