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

import com.github.sdbg.debug.core.configs.ChromeAppLaunchConfigurationDelegate;
import com.github.sdbg.debug.core.configs.ChromeLaunchConfigurationDelegate;
import com.github.sdbg.debug.core.internal.android.ADBManager;
import com.github.sdbg.debug.core.internal.util.ResourceChangeManager;
import com.github.sdbg.debug.core.util.ResourceServerManager;
import com.github.sdbg.debug.core.util.Trace;
import com.github.sdbg.utilities.StringUtilities;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;

/**
 * The plugin activator for the com.github.sdbg.debug.core plugin.
 */
public class SDBGDebugCorePlugin extends Plugin {

  public static enum BreakOnExceptions {
    none,
    uncaught,
    all
  }

  /**
   * The Dart Debug Core plug-in ID.
   */
  public static final String PLUGIN_ID = "com.github.sdbg.debug.core"; //$NON-NLS-1$

  /**
   * The Dart debug marker ID.
   */
  public static final String DEBUG_MARKER_ID = "com.github.sdbg.debug.core.breakpointMarker"; //$NON-NLS-1$

  /**
   * The debug model ID.
   */
  public static final String DEBUG_MODEL_ID = "com.github.sdbg.debug.core"; //$NON-NLS-1$

  // TODO(devoncarew): remove this when the debugger supports value modification
  public static boolean VM_SUPPORTS_VALUE_MODIFICATION = false;

  // TODO(devoncarew): the vm/webkit protocol claims to support source modification, but it does
  // not yet do anything
  public static boolean SEND_MODIFIED_DART = false;

  // TODO(devoncarew): this causes Dartium to crash
  public static boolean SEND_MODIFIED_HTML = false;

  public static final String BROWSER_LAUNCH_CONFIG_ID = "com.github.sdbg.debug.core.browserLaunchConfig";

  public static final String CHROME_LAUNCH_CONFIG_ID = "com.github.sdbg.debug.core.chromeLaunchConfig";

  public static final String CHROMECONN_LAUNCH_CONFIG_ID = "com.github.sdbg.debug.core.chromeConnLaunchConfig";

  public static final String CHROMEMOBILECONN_LAUNCH_CONFIG_ID = "com.github.sdbg.debug.core.chromeMobileConnLaunchConfig";

  public static final String CHROMEAPP_LAUNCH_CONFIG_ID = "com.github.sdbg.debug.core.chromeAppLaunchConfig";

  public static final String ANDROIDREVERSEFORWARDS_LAUNCH_CONFIG_ID = "com.github.sdbg.debug.core.androidReverseForwardsLaunchConfig";

  private static IDebugEventSetListener debugEventListener;

  private static SDBGDebugCorePlugin plugin;

  public static final String PREFS_BROWSER_NAME = "browserName";

  public static final String PREFS_USE_SOURCE_MAPS = "useSourceMaps";

  public static final String PREFS_DEFAULT_BROWSER = "defaultBrowser";

  public static final String PREFS_BROWSER_ARGS = "browserArgs";

  public static final String PREFS_BREAK_ON_EXCEPTIONS = "breakOnExceptions";

  public static final String PREFS_INVOKE_TOSTRING = "invokeToString";

  public static final String PREFS_SHOW_RUN_RESUME_DIALOG = "showRunResumeDialog";

  /**
   * Create a Status object with the given message and this plugin's ID.
   * 
   * @param message
   * @return
   */
  public static Status createErrorStatus(String message) {
    return new Status(IStatus.ERROR, PLUGIN_ID, message);
  }

  /**
   * @return the plugin singleton instance
   */
  public static SDBGDebugCorePlugin getPlugin() {
    return plugin;
  }

  /**
   * A light-weight logging mechanism for debugging the debugger.
   * 
   * @param str
   */
  public static void log(String str) {
    Trace.trace(str);
  }

  /**
   * For use during development - this method listens to and logs all Eclipse debugger events.
   */
  public static void logDebuggerEvents() {
    if (debugEventListener == null) {
      debugEventListener = new IDebugEventSetListener() {
        @Override
        public void handleDebugEvents(DebugEvent[] events) {
          for (DebugEvent event : events) {
            logInfo(event.toString());
          }
        }
      };

      DebugPlugin.getDefault().addDebugEventListener(debugEventListener);
    }
  }

  /**
   * Log the given message as an error to the Eclipse log.
   * 
   * @param message
   */
  public static void logError(String message) {
    if (getPlugin() != null) {
      getPlugin().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message));
    }
  }

  /**
   * Log the given exception.
   * 
   * @param message
   * @param exception
   */
  public static void logError(String message, Throwable exception) {
    if (getPlugin() != null) {
      getPlugin().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, exception));
    }
  }

  /**
   * Log the given exception.
   * 
   * @param exception
   */
  public static void logError(Throwable exception) {
    if (getPlugin() != null) {
      getPlugin().getLog().log(
          new Status(IStatus.ERROR, PLUGIN_ID, exception.getMessage(), exception));
    }
  }

  /**
   * Log the given message as an info to the Eclipse log.
   * 
   * @param message
   */
  public static void logInfo(String message) {
    if (Trace.TRACING && getPlugin() != null) {
      getPlugin().getLog().log(new Status(IStatus.INFO, PLUGIN_ID, message));
    }
  }

  /**
   * Log the given exception.
   * 
   * @param exception
   */
  public static void logInfo(Throwable exception) {
    if (getPlugin() != null) {
      getPlugin().getLog().log(
          new Status(IStatus.INFO, PLUGIN_ID, exception.getMessage(), exception));
    }
  }

  /**
   * Log the given message as a warning to the Eclipse log.
   * 
   * @param message
   */
  public static void logWarning(String message) {
    if (getPlugin() != null) {
      getPlugin().getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, message));
    }
  }

  private IEclipsePreferences prefs;

  private IUserAgentManager userAgentManager;

  public boolean canShowRunResumeDialog() {
    return getPrefs().getBoolean(PREFS_SHOW_RUN_RESUME_DIALOG, true);
  }

  public BreakOnExceptions getBreakOnExceptions() {
    try {
      String value = getPrefs().get(PREFS_BREAK_ON_EXCEPTIONS, null);

      if (value == null) {
        return BreakOnExceptions.uncaught;
      } else {
        return BreakOnExceptions.valueOf(value);
      }
    } catch (IllegalArgumentException iae) {
      return BreakOnExceptions.uncaught;
    }
  }

  public String getBrowserArgs() {
    return getPrefs().get(PREFS_BROWSER_ARGS, "");
  }

  public String[] getBrowserArgsAsArray() {
    return StringUtilities.parseArgumentString(getBrowserArgs());
  }

  public String getBrowserName() {
    return getPrefs().get(PREFS_BROWSER_NAME, "");
  }

  public boolean getInvokeToString() {
    return getPrefs().getBoolean(PREFS_INVOKE_TOSTRING, true);
  }

  public boolean getIsDefaultBrowser() {
    return getPrefs().getBoolean(PREFS_DEFAULT_BROWSER, true);
  }

  public IEclipsePreferences getPrefs() {
    if (prefs == null) {
      prefs = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
    }

    return prefs;
  }

  public IUserAgentManager getUserAgentManager() {
    return userAgentManager;
  }

  /**
   * @return whether to use source maps for debugging
   */
  public boolean getUseSourceMaps() {
    return getPrefs().getBoolean(PREFS_USE_SOURCE_MAPS, true);
  }

  public void setBreakOnExceptions(BreakOnExceptions value) {
    getPrefs().put(PREFS_BREAK_ON_EXCEPTIONS, value.toString());

    try {
      getPrefs().flush();
    } catch (BackingStoreException exception) {
      logError(exception);
    }
  }

  public void setBrowserPreferences(boolean useDefault, String name, String args) {

    IEclipsePreferences prefs = getPrefs();

    prefs.putBoolean(PREFS_DEFAULT_BROWSER, useDefault);
    prefs.put(PREFS_BROWSER_NAME, name);
    prefs.put(PREFS_BROWSER_ARGS, args);

    try {
      getPrefs().flush();
    } catch (BackingStoreException exception) {
      logError(exception);
    }
  }

  public void setInvokeToString(boolean value) {
    getPrefs().putBoolean(PREFS_INVOKE_TOSTRING, value);
  }

  public void setShowRunResumeDialogPref(boolean value) {
    getPrefs().putBoolean(PREFS_SHOW_RUN_RESUME_DIALOG, value);
  }

  public void setUserAgentManager(IUserAgentManager userAgentManager) {
    this.userAgentManager = userAgentManager;
  }

  public void setUseSourceMaps(boolean value) {
    getPrefs().putBoolean(PREFS_USE_SOURCE_MAPS, value);

    try {
      getPrefs().flush();
    } catch (BackingStoreException e) {

    }
  }

  @Override
  public void start(BundleContext context) throws Exception {
    plugin = this;

    super.start(context);
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    ResourceChangeManager.shutdown();
    ResourceServerManager.shutdown();

    ChromeLaunchConfigurationDelegate.dispose();
    ChromeAppLaunchConfigurationDelegate.dispose();

    new ADBManager().killServer();

    if (debugEventListener != null) {
      DebugPlugin.getDefault().removeDebugEventListener(debugEventListener);

      debugEventListener = null;
    }

    super.stop(context);

    plugin = null;
  }

}
