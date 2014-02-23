package com.github.sdbg.debug.core.util;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.utilities.StringUtilities;

import org.eclipse.core.runtime.Platform;

public class Trace {
  public static final boolean TRACING = isOptionTrue("debug/all");

  private static long tracingStart = System.currentTimeMillis();

  public static void trace(String trace) { // TODO: Switch to standard java.util.logging, with this class only supporting the enablement/disablement of various loggers
    if (isTracingEnabled()) {
      if (TRACING) {
        System.out.printf(
            "SDBG [%.3f]: %s\n",
            ((System.currentTimeMillis() - tracingStart) / 1000.0),
            trace);
      }
    }
  }

  /**
   * @return <code>true</code> if option has value "true".
   */
  private static boolean isOptionTrue(String optionSuffix) {
    return isOptionValue(optionSuffix, "true");
  }

  /**
   * @return <code>true</code> if option has "expected" value.
   */
  private static boolean isOptionValue(String optionSuffix, String expected) {
    String option = SDBGDebugCorePlugin.PLUGIN_ID + "/" + optionSuffix;
    String value = Platform.getDebugOption(option);
    return StringUtilities.equalsIgnoreCase(value, expected);
  }

  private static boolean isTracingEnabled() {
    return SDBGDebugCorePlugin.getPlugin().isDebugging();
  }

  private Trace() {
  }
}
