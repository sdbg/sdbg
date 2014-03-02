package com.github.sdbg.debug.core.internal.webkit.protocol;

import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitDebugger.PausedReasonType;

import java.util.List;

public interface WebkitResteppingManager {
  String getRestepCommand();

  boolean isResteppingNeeded();

  void onDebuggerPaused(List<WebkitCallFrame> frames, PausedReasonType reason,
      WebkitRemoteObject exception);

  void onStepping(String stepCommand);
}
