package com.github.sdbg.debug.core.util;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;

import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugTrace;

public class Trace {
  public static final String BROWSER_LAUNCHING = "/browser/launching",
      BROWSER_OUTPUT = "/browser/output", SOURCEMAPS = "/sourcemaps", BREAKPOINTS = "/breakpoints",
      ECLIPSE_DEBUGGER_EVENTS = "/eclipseDebuggerEvents", WIRE_PROTOCOL = "/wireProtocol",
      RESOURCE_SERVING = "/resourceServing", TIMER = "/timer";

  private static DebugOptions options;
  private static DebugTrace trace;

  public static synchronized DebugTrace get() {
    return trace;
  }

  public static synchronized boolean isTracing() {
    return options != null && options.isDebugEnabled();
  }

  public static synchronized boolean isTracing(String component) {
    if (options != null) {
      return options.isDebugEnabled() && options.getBooleanOption(component, false);
    } else {
      return false;
    }
  }

  public static synchronized void setOptions(DebugOptions options) {
    Trace.options = options;
    Trace.trace = options.newDebugTrace(SDBGDebugCorePlugin.PLUGIN_ID);
  }

  public static void trace(String message) {
    trace(null, message);
  }

  public static void trace(String component, String message) {
    DebugTrace dt = get();
    if (dt != null) {
      dt.trace(component, message);
    }
  }

  private Trace() {
  }
}
