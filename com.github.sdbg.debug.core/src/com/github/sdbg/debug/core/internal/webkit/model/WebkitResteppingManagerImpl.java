package com.github.sdbg.debug.core.internal.webkit.model;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitCallFrame;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitDebugger.PausedReasonType;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitLocation;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitRemoteObject;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitResteppingManager;

import java.util.List;

import org.eclipse.core.resources.IStorage;

public class WebkitResteppingManagerImpl implements WebkitResteppingManager {
  private WebkitDebugTarget target;

  private String stepCommand;

  private SourceMapManager.SourceLocation stepLocation;

  private boolean restep;
  private String restepCommand;

  public WebkitResteppingManagerImpl(WebkitDebugTarget target) {
    this.target = target;
  }

  @Override
  public String getRestepCommand() {
    return restepCommand != null ? restepCommand : stepCommand;
  }

  @Override
  public boolean isResteppingNeeded() {
    return restep;
  }

  @Override
  public void onDebuggerPaused(List<WebkitCallFrame> frames, PausedReasonType reason,
      WebkitRemoteObject exception) {
    SourceMapManager.SourceLocation currentLocation = null;
    WebkitCallFrame frame = frames.isEmpty() ? null : frames.get(0);
    if (frame != null) {
      IStorage storage = target.getScriptStorageFor(frame);
      if (target.getSourceMapManager().isMapSource(storage)) {
        WebkitLocation location = frame.getLocation();

        currentLocation = target.getSourceMapManager().getMappingFor(
            storage,
            location.getLineNumber(),
            location.getColumnNumber());
      }
    }

    if (SDBGDebugCorePlugin.getPlugin().getUseSmartStepInOut() && currentLocation == null
        && stepLocation != null) {
      // Filter out frames that are NOT sourcemapped but are otherwise inside sourcemapped files
      restepCommand = stepCommand.equals("Debugger.stepOver") ? "Debugger.stepInto" : stepCommand;
      restep = true;
      return;
    } else if (SDBGDebugCorePlugin.getPlugin().getUseSmartStepOver() && exception == null
        && reason == PausedReasonType.other && currentLocation != null && stepLocation != null
        && currentLocation.getPath().equals(stepLocation.getPath())
        && currentLocation.getLine() == stepLocation.getLine()) {
      // Restepping is needed if we are still on the same line in the same Java source file
      // TODO: The above comparison is way too simplistic, the whole set of stackframes should be compared
      restepCommand = null;
      restep = true;
      return;
    }

    stepLocation = currentLocation;
    restep = false;
  }

  @Override
  public void onStepping(String stepCommand) {
    this.stepCommand = stepCommand;
  }
}
