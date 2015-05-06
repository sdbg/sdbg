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
package com.github.sdbg.debug.core.internal.webkit.model;

import com.github.sdbg.debug.core.DebugUIHelper;
import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.SDBGDebugCorePlugin.BreakOnExceptions;
import com.github.sdbg.debug.core.breakpoints.IBreakpointPathResolver;
import com.github.sdbg.debug.core.breakpoints.SDBGBreakpoint;
import com.github.sdbg.debug.core.internal.android.ADBManager;
import com.github.sdbg.debug.core.internal.util.DOMResourceTrackersManager;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitBreakpoint;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitCallFrame;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitCallback;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitConnection;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitConnection.WebkitConnectionListener;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitDebugger.DebuggerListenerAdapter;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitDebugger.PauseOnExceptionsType;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitDebugger.PausedReasonType;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitDom.DomListener;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitDom.InspectorListener;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitNode;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitPage;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitRemoteObject;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitResult;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitScript;
import com.github.sdbg.debug.core.model.IResourceResolver;
import com.github.sdbg.debug.core.model.ISDBGDebugTarget;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.IBreakpointManagerListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;

/**
 * The IDebugTarget implementation for the Webkit debug elements.
 */
public class WebkitDebugTarget extends WebkitDebugElement implements IBreakpointManagerListener,
    ISDBGDebugTarget {
  private static WebkitDebugTarget activeTarget;

  private String debugTargetName;

  private String disconnectMessage;

  private WebkitConnection connection;
  private ILaunch launch;
  private WebkitDebugProcess process;
  private IResourceResolver resourceResolver;
  private boolean enableBreakpoints;
  private WebkitDebugThread debugThread;
  private ISDBGBreakpointManager breakpointManager;
  private DOMResourceTrackersManager domResourceTrackersManager;
  private boolean canSetScriptSource;
  private SourceMapManager sourceMapManager;
  private ADBManager adbManager;
  private IProject project;

  private WebkitNode rootNode;

  public static WebkitDebugTarget getActiveTarget() {
    return activeTarget;
  }

  private static void setActiveTarget(WebkitDebugTarget target) {
    activeTarget = target;
  }

  /**
   * @param target
   */
  public WebkitDebugTarget(String debugTargetName, WebkitConnection connection, ILaunch launch,
      Process javaProcess, IProject project, IResourceResolver resourceResolver,
      ADBManager adbManager, boolean enableBreakpoints, boolean isRemote) {
    super(null);

    setActiveTarget(this);

    this.debugTargetName = debugTargetName;
    this.connection = connection;
    this.launch = launch;
    this.project = project;
    this.resourceResolver = resourceResolver;
    this.adbManager = adbManager;
    this.enableBreakpoints = enableBreakpoints;

    debugThread = new WebkitDebugThread(this);

    if (javaProcess != null || isRemote) {
      process = new WebkitDebugProcess(this, debugTargetName, javaProcess);
    }

    if (enableBreakpoints) {
      breakpointManager = new BreakpointManager(this);
    } else {
      breakpointManager = new BreakpointManager.NullBreakpointManager();
    }

    domResourceTrackersManager = new WebkitDOMResourceTrackersManager(this);

    sourceMapManager = new SourceMapManager(resourceResolver);

    connection.getDebugger().setResteppingManager(new WebkitResteppingManagerImpl(this));
  }

  /**
   * A copy constructor for WebkitDebugTarget.
   * 
   * @param target
   */
  public WebkitDebugTarget(WebkitDebugTarget target) {
    this(
        target.debugTargetName,
        new WebkitConnection(target.connection),
        target.launch,
        null,
        target.project,
        target.resourceResolver,
        target.adbManager,
        target.enableBreakpoints,
        false);

    this.process = target.process;
    this.process.switchTo(this);
  }

  @Override
  public void breakpointAdded(IBreakpoint breakpoint) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void breakpointManagerEnablementChanged(boolean enabled) {
    try {
      getConnection().getDebugger().setBreakpointsActive(enableBreakpoints && enabled);
    } catch (IOException e) {
      SDBGDebugCorePlugin.logError(e);
    }
  }

  @Override
  public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean canDisconnect() {
    return false;
  }

  @Override
  public boolean canResume() {
    return debugThread == null ? false : debugThread.canResume();
  }

  @Override
  public boolean canSuspend() {
    return debugThread == null ? false : debugThread.canSuspend();
  }

  @Override
  public boolean canTerminate() {
    return connection != null && connection.isConnected();
  }

  @Override
  public void disconnect() throws DebugException {
    throw new UnsupportedOperationException("disconnect is not supported");
  }

  @Override
  public void fireTerminateEvent() {
    setActiveTarget(null);

    // NOTE: We are also setting some class members explicitly to NULL, because
    // even though our instance is terminated, Eclipse will continue to keep a reference to it 
    // for visualization purposes in Eclipse Debug view, until we clean it up
    //
    // However, the last thing we want is to keep a reference to e.g. sourceMapManager when not needed, 
    // as it occupies huge amounts of RAM

    breakpointManager.dispose(false);
    breakpointManager = null;

    domResourceTrackersManager.dispose();

    sourceMapManager.dispose();
    sourceMapManager = null;

    if (adbManager != null) {
      adbManager.removeAllForwards();
      adbManager = null;
    }

    IBreakpointManager eclipseBpManager = DebugPlugin.getDefault().getBreakpointManager();
    eclipseBpManager.removeBreakpointManagerListener(this);

    debugThread = null;

    // Check for null on system shutdown.
    if (DebugPlugin.getDefault() != null) {
      super.fireTerminateEvent();
    }
  }

  /**
   * @return the connection
   */
  @Override
  public WebkitConnection getConnection() {
    return connection;
  }

  @Override
  public IDebugTarget getDebugTarget() {
    return this;
  }

  public boolean getEnableBreakpoints() {
    return enableBreakpoints;
  }

  @Override
  public ILaunch getLaunch() {
    return launch;
  }

  @Override
  public IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException {
    return null;
  }

  @Override
  public String getName() {
    if (disconnectMessage != null) {
      return debugTargetName + " <" + disconnectMessage + ">";
    } else {
      return debugTargetName;
    }
  }

  @Override
  public IProcess getProcess() {
    return process;
  }

  @Override
  public IThread[] getThreads() throws DebugException {
    if (debugThread != null) {
      return new IThread[] {debugThread};
    } else {
      return new IThread[0];
    }
  }

  @Override
  public boolean hasThreads() throws DebugException {
    return true;
  }

  @Override
  public boolean isDisconnected() {
    return false;
  }

  @Override
  public boolean isSuspended() {
    return debugThread == null ? false : debugThread.isSuspended();
  }

  @Override
  public boolean isTerminated() {
    return debugThread == null;
  }

  /**
   * Recycle the current Webkit debug connection; attempt to reset it to a fresh state beforehand.
   * 
   * @param url
   * @throws IOException
   */
  public void navigateToUrl(ILaunchConfiguration launchConfig, final String url,
      boolean enableBreakpoints, IResourceResolver resolver) throws IOException {
    this.resourceResolver = resolver;

    IBreakpointManager eclipseBpManager = DebugPlugin.getDefault().getBreakpointManager();

    connection.getDebugger().setBreakpointsActive(enableBreakpoints && eclipseBpManager.isEnabled());
    connection.getDebugger().setPauseOnExceptions(getPauseType());

    getConnection().getPage().navigate(url);
  }

  public void openConnection() throws IOException {
    openConnection(null, false);
  }

  public void openConnection(final String url, boolean listenForInspectorDetach) throws IOException {
    connection.addConnectionListener(new WebkitConnectionListener() {
      @Override
      public void connectionClosed(WebkitConnection connection) {
        fireTerminateEvent();
      }
    });

    connection.connect();

    process.getStreamMonitor().connectTo(connection);

    connection.getPage().addPageListener(new WebkitPage.PageListenerAdapter() {
      @Override
      public void loadEventFired(int timestamp) {
        resyncRootNode();
      }
    });
    connection.getPage().enable();

    connection.getCSS().enable();

    connection.getDom().addDomListener(new DomListener() {
      @Override
      public void documentUpdated() {
        resyncRootNode();
      }
    });

    if (listenForInspectorDetach) {
      connection.getDom().addInspectorListener(new InspectorListener() {
        @Override
        public void detached(String reason) {
          handleInspectorDetached(reason);
        }

        @Override
        public void targetCrashed() {
          handleTargetCrashed();
        }
      });
    }

    connection.getDebugger().addDebuggerListener(new DebuggerListenerAdapter() {
      @Override
      public void debuggerBreakpointResolved(WebkitBreakpoint breakpoint) {
        breakpointManager.handleBreakpointResolved(breakpoint);
      }

      @Override
      public void debuggerGlobalObjectCleared() {
    	// It is important to first remove the sourcemaps 
    	// and only then the breakpoints  
        sourceMapManager.handleGlobalObjectCleared();
        breakpointManager.handleGlobalObjectCleared();
      }

      @Override
      public void debuggerPaused(PausedReasonType reason, List<WebkitCallFrame> frames,
          WebkitRemoteObject exception) {
        if (exception != null) {
          printExceptionToStdout(exception);
        }

        debugThread.handleDebuggerSuspended(reason, frames, exception);
      }

      @Override
      public void debuggerResumed() {
        debugThread.handleDebuggerResumed();
      }

      @Override
      public void debuggerScriptParsed(final WebkitScript script) {
        checkForDebuggerExtension(script);
        //TODO: Too chatty Trace.trace("Script " + script + " loaded");

        if (script.hasScriptSource() || script.getSourceMapURL() != null) {
          IStorage storage = new WebkitScriptStorage(script, script.getScriptSource());
          breakpointManager.removeBreakpointsConcerningScript(storage);
          sourceMapManager.handleScriptParsed(storage, script.getSourceMapURL());
          breakpointManager.addBreakpointsConcerningScript(storage);
        }
      }
    });
    connection.getDebugger().enable();

    IBreakpointManager eclipseBpManager = DebugPlugin.getDefault().getBreakpointManager();
    eclipseBpManager.addBreakpointManagerListener(this);

    getConnection().getDebugger().setBreakpointsActive(
        enableBreakpoints && eclipseBpManager.isEnabled());

    connection.getDebugger().canSetScriptSource(new WebkitCallback<Boolean>() {
      @Override
      public void handleResult(WebkitResult<Boolean> result) {
        if (!result.isError() && result.getResult() != null) {
          canSetScriptSource = result.getResult().booleanValue();
        }
      }
    });

    fireCreationEvent();
    process.fireCreationEvent();

    // Set our existing breakpoints and start listening for new breakpoints.
    breakpointManager.connect();

    // TODO(devoncarew): listen for changes to DartDebugCorePlugin.PREFS_BREAK_ON_EXCEPTIONS

    if (url == null) {
      connection.getDebugger().setPauseOnExceptions(getPauseType());
    } else {
      connection.getDebugger().setPauseOnExceptions(
          getPauseType(),
          createNavigateWebkitCallback(url));
    }
  }

  /**
   * Attempt to re-connect to a debug target. If successful, it will return a new WebkitDebugTarget.
   * 
   * @return
   * @throws IOException
   */
  @Override
  public WebkitDebugTarget reconnect() throws IOException {
    WebkitDebugTarget newTarget = new WebkitDebugTarget(this);

    newTarget.reopenConnection();

    ILaunch launch = newTarget.getLaunch();
    launch.addDebugTarget(newTarget);

    for (IDebugTarget target : launch.getDebugTargets()) {
      if (target.isTerminated()) {
        launch.removeDebugTarget(target);
      }
    }

    return newTarget;
  }

  public void reopenConnection() throws IOException {
    openConnection(null, true);
  }

  @Override
  public void resume() throws DebugException {
    debugThread.resume();
  }

  @Override
  public boolean supportsBreakpoint(IBreakpoint breakpoint) {
    if (!(breakpoint instanceof ILineBreakpoint)) {
      return false;
    }

    if (breakpoint instanceof SDBGBreakpoint) {
      return true;
    }

    for (IBreakpointPathResolver resolver : BreakpointManager.getBreakpointPathResolvers()) {
      if (resolver.isSupported(breakpoint)) {
        return true;
      }
    }

    return false;
  }

  public boolean supportsSetScriptSource() {
    return canSetScriptSource;
  }

  @Override
  public boolean supportsStorageRetrieval() {
    return false;
  }

  @Override
  public void suspend() throws DebugException {
    debugThread.suspend();
  }

  @Override
  public void terminate() throws DebugException {
    try {
      connection.close();
    } catch (IOException e) {

    }

    if (process != null) {
      process.terminate();
    }

    process = null;
  }

  @Override
  public void writeToStdout(String message) {
    process.getStreamMonitor().messageAdded(message);
  }

  protected WebkitCallback<Boolean> createNavigateWebkitCallback(final String url) {
    return new WebkitCallback<Boolean>() {
      @Override
      public void handleResult(WebkitResult<Boolean> result) {
        // Once all other requests have been processed, then navigate to the given url.
        try {
          if (connection.isConnected()) {
            connection.getPage().navigate(url);
          }
        } catch (IOException e) {
          SDBGDebugCorePlugin.logError(e);
        }
      }
    };
  }

  protected ISDBGBreakpointManager getBreakpointManager() {
    return breakpointManager;
  }

  protected IResourceResolver getResourceResolver() {
    return resourceResolver;
  }

  protected IStorage getScriptStorage(WebkitScript script) {
    if (script != null) {
      String url = script.getUrl();

      if (script.isSystemScript() || script.isDataUrl() || script.isChromeExtensionUrl()) {
        return new WebkitScriptStorage(script, null);
      }

      if (url.startsWith("package:")) {
        return new WebkitScriptStorage(script, null);
      }

      IResource resource = getResourceResolver().resolveUrl(url);

      if (resource != null && resource instanceof IStorage) {
        return (IStorage) resource;
      }

      return new WebkitScriptStorage(script, null);
    }

    return null;
  }

  protected IStorage getScriptStorageFor(WebkitCallFrame webkitFrame) {
    WebkitScript script = getConnection().getDebugger().getScript(
        webkitFrame.getLocation().getScriptId());
    return getScriptStorage(script);
  }

  protected SourceMapManager getSourceMapManager() {
    return sourceMapManager;
  }

  protected WebkitConnection getWebkitConnection() {
    return connection;
  }

  protected void handleInspectorDetached(String reason) {
    // "replaced_with_devtools", "target_closed", ...

    final String replacedWithDevTools = "replaced_with_devtools";

    if (replacedWithDevTools.equalsIgnoreCase(reason)) {
      // When the user opens the Webkit inspector our debug connection is closed.
      // We warn the user when this happens, since it otherwise isn't apparent to them
      // when the debugger connection is closing.
      if (enableBreakpoints) {
        // Only show this message if the user launched Dartium with debugging enabled.
        disconnectMessage = "devtools disconnect";

        DebugUIHelper.getHelper().handleDevtoolsDisconnect(this);
      }
    }
  }

  protected void handleTargetCrashed() {
    process.getStreamMonitor().messageAdded("<debug target crashed>");

    try {
      terminate();
    } catch (DebugException e) {
      SDBGDebugCorePlugin.logInfo(e);
    }
  }

  protected void printExceptionToStdout(final WebkitRemoteObject exception) {
    try {
      getConnection().getRuntime().callToString(
          exception.getObjectId(),
          new WebkitCallback<String>() {
            @Override
            public void handleResult(WebkitResult<String> result) {
              if (!result.isError()) {
                String text = result.getResult();

                if (exception != null && exception.isPrimitive()) {
                  text = exception.getValue();
                }

                if (text != null) {
                  int index = text.indexOf('\n');

                  if (index != -1) {
                    text = text.substring(0, index).trim();
                  }

                  process.getStreamMonitor().messageAdded("Breaking on exception: " + text + "\n");
                }
              }
            }
          });
    } catch (IOException e) {
      SDBGDebugCorePlugin.logError(e);
    }
  }

  protected boolean shouldUseSourceMapping() {
    return SDBGDebugCorePlugin.getPlugin().getUseSourceMaps();
  }

  IProject getProject() {
    return project;
  }

  WebkitNode getRootNode() {
    return rootNode;
  }

  /**
   * Check for the presence of Chrome extensions content scripts. It seems like many (all?) of these
   * prevent debugging from working.
   * 
   * @param script the Debugger.scriptParsed event
   * @see dartbug.com/10298
   */
  private void checkForDebuggerExtension(WebkitScript script) {
    // {"method":"Debugger.scriptParsed","params":{"startLine":0,"libraryId":0,"endLine":154,
    //   "startColumn":0,"scriptId":"26","url":"chrome-extension://ognampngfcbddbfemdapefohjiobgbdl/data_loader.js",
    //   "isContentScript":true,"endColumn":1}}

    if (script.isContentScript() && script.isChromeExtensionUrl()) {
      SDBGDebugCorePlugin.logWarning("Chrome extension content script detected: " + script);

      writeToStdout("WARNING: Chrome content script extension detected. Many of these extensions "
          + "interfere with the debug\nexperience, including preventing breakpoints from working. "
          + "These extensions include but are not limited\nto SpeedTracer and the WebGL inspector. "
          + "You can disable them in Webkit via Tools > Extensions.");
      writeToStdout("(content script extension: " + script.getUrl() + ")");
    }
  }

  private PauseOnExceptionsType getPauseType() {
    if (!enableBreakpoints) {
      return PauseOnExceptionsType.none;
    }

    final BreakOnExceptions boe = SDBGDebugCorePlugin.getPlugin().getBreakOnExceptions();
    PauseOnExceptionsType pauseType = PauseOnExceptionsType.none;

    if (boe == BreakOnExceptions.uncaught) {
      pauseType = PauseOnExceptionsType.uncaught;
    } else if (boe == BreakOnExceptions.all) {
      pauseType = PauseOnExceptionsType.all;
    }

    return pauseType;
  }

  private void resyncRootNode() {
    // Flush everything.
    rootNode = null;

    // Get the root node.
    try {
      // TODO(devoncarew): check if the connection is no longer open?

      // {"id":13,"result":{"root":{"childNodeCount":3,"localName":"","nodeId":1,"documentURL":"http://127.0.0.1:3030/Users/devoncarew/projects/dart/dart/samples/solar/solar.html","baseURL":"http://127.0.0.1:3030/Users/devoncarew/projects/dart/dart/samples/solar/solar.html","nodeValue":"","nodeName":"#document","xmlVersion":"","children":[{"localName":"","nodeId":2,"internalSubset":"","publicId":"","nodeValue":"","nodeName":"html","systemId":"","nodeType":10},{"localName":"","nodeId":3,"nodeValue":" Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file\n     for details. All rights reserved. Use of this source code is governed by a\n     BSD-style license that can be found in the LICENSE file. ","nodeName":"","nodeType":8},{"childNodeCount":2,"localName":"html","nodeId":4,"nodeValue":"","nodeName":"HTML","children":[{"childNodeCount":3,"localName":"head","nodeId":5,"nodeValue":"","nodeName":"HEAD","attributes":[],"nodeType":1},{"childNodeCount":6,"localName":"body","nodeId":6,"nodeValue":"","nodeName":"BODY","attributes":[],"nodeType":1}],"attributes":[],"nodeType":1}],"nodeType":9}}}
      getConnection().getDom().getDocument(new WebkitCallback<WebkitNode>() {
        @Override
        public void handleResult(WebkitResult<WebkitNode> result) {
          rootNode = result.getResult();
        }
      });
    } catch (IOException e) {
      SDBGDebugCorePlugin.logError(e);
    }
  }
}
