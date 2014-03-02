package com.github.sdbg.debug.core.internal.webkit.model;

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

  private SourceMapManager.SourceLocation currentLocation;
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
    WebkitCallFrame frame = frames.isEmpty() ? null : frames.get(0);
    if (frame != null) {
      IStorage storage = target.getScriptStorageFor(frame);
      if (target.getSourceMapManager().isMapSource(storage)) {
        WebkitLocation location = frame.getLocation();

        currentLocation = target.getSourceMapManager().getMappingFor(
            storage,
            location.getLineNumber(),
            location.getColumnNumber());

        if (currentLocation == null && stepLocation != null
            && (stepCommand.equals("Debugger.stepInto") || stepCommand.equals("Debugger.stepOut"))) {
          // Filter out frames that are NOT sourcemapped but are otherwise inside sourcemapped files
          restepCommand = "Debugger.stepOut";
          restep = true;
          return;
        }
      }
    } else {
      currentLocation = null;
    }

    // Restepping is needed if we are still on the same line in the same Java source file
    restepCommand = null;
    restep = exception == null && reason == PausedReasonType.other && currentLocation != null
        && stepLocation != null && currentLocation.getPath().equals(stepLocation.getPath())
        && currentLocation.getLine() == stepLocation.getLine();

    if (!restep) {
      stepLocation = null;
    }
  }

  @Override
  public void onStepping(String stepCommand) {
    this.stepCommand = stepCommand;
    stepLocation = currentLocation;
  }
}
