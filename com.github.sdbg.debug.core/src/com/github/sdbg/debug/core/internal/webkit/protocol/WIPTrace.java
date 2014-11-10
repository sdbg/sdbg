package com.github.sdbg.debug.core.internal.webkit.protocol;

import com.github.sdbg.debug.core.util.Trace;

public class WIPTrace {
  static void trace(String message) {
    Trace.trace(Trace.WIRE_PROTOCOL, message);
  }

  private WIPTrace() {
  }
}
