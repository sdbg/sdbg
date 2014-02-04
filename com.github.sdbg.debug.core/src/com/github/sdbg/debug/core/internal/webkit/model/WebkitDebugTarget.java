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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;

import com.github.sdbg.debug.core.DebugUIHelper;
import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.SDBGDebugCorePlugin.BreakOnExceptions;
import com.github.sdbg.debug.core.SDBGLaunchConfigWrapper;
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
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitPage;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitRemoteObject;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitResult;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitScript;
import com.github.sdbg.debug.core.model.IResourceResolver;
import com.github.sdbg.debug.core.model.ISDBGDebugTarget;

/**
 * The IDebugTarget implementation for the Webkit debug elements.
 */
public class WebkitDebugTarget extends WebkitDebugElement implements ISDBGDebugTarget {
  //&&&!!!
  private class WebkitScriptStorage extends PlatformObject implements IStorage {
    private WebkitScript script;
    private String source;

    public WebkitScriptStorage(WebkitScript script, String source) {
      this.script = script;
      this.source = source;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      WebkitScriptStorage other = (WebkitScriptStorage) obj;
      if (!getOuterType().equals(other.getOuterType())) {
        return false;
      }
      if (script == null) {
        if (other.script != null) {
          return false;
        }
      } else if (!script.equals(other.script)) {
        return false;
      }
      return true;
    }

    @Override
    public InputStream getContents() throws CoreException {
      try {
        return new ByteArrayInputStream(source != null ? source.getBytes("UTF-8") : new byte[0]);
      } catch (UnsupportedEncodingException e) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            SDBGDebugCorePlugin.PLUGIN_ID,
            e.toString(),
            e));
      }
    }

    @Override
    public IPath getFullPath() {
      try {
        return Path.fromPortableString(URIUtil.fromString(script.getUrl()).getPath());
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public String getName() {
      return script.getScriptId();
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((script == null) ? 0 : script.hashCode());
      return result;
    }

    @Override
    public boolean isReadOnly() {
      return true;
    }

    @Override
    public String toString() {
      return script.toString();
    }

    private WebkitDebugTarget getOuterType() {
      return WebkitDebugTarget.this;
    }
  }

  private static WebkitDebugTarget activeTarget;

  public static WebkitDebugTarget getActiveTarget() {
    return activeTarget;
  }

  private static void setActiveTarget(WebkitDebugTarget target) {
    activeTarget = target;
  }

  private String debugTargetName;
  private WebkitConnection connection;
  private ILaunch launch;
  private IProject currentProject;
  private WebkitDebugProcess process;
  private IResourceResolver resourceResolver;
  private boolean enableBreakpoints;
  private WebkitDebugThread debugThread;
  private BreakpointManager breakpointManager;
  private CssScriptManager cssScriptManager;
  private HtmlScriptManager htmlScriptManager;
  private DartCodeManager dartCodeManager;
  private boolean canSetScriptSource;

  private SourceMapManager sourceMapManager;

  /**
   * @param target
   */
  public WebkitDebugTarget(String debugTargetName, WebkitConnection connection, ILaunch launch,
      Process javaProcess, IResourceResolver resourceResolver, boolean enableBreakpoints,
      boolean isRemote) {
    super(null);

    setActiveTarget(this);

    this.debugTargetName = debugTargetName;
    this.connection = connection;
    this.launch = launch;
    this.resourceResolver = resourceResolver;
    this.enableBreakpoints = enableBreakpoints;

    debugThread = new WebkitDebugThread(this);

    if (javaProcess != null || isRemote) {
      process = new WebkitDebugProcess(this, debugTargetName, javaProcess);
    }

    breakpointManager = new BreakpointManager(this);

    cssScriptManager = new CssScriptManager(this);

    if (SDBGDebugCorePlugin.SEND_MODIFIED_HTML) {
      htmlScriptManager = new HtmlScriptManager(this);
    }

    if (SDBGDebugCorePlugin.SEND_MODIFIED_DART) {
      dartCodeManager = new DartCodeManager(this);
    }

    SDBGLaunchConfigWrapper wrapper = new SDBGLaunchConfigWrapper(launch.getLaunchConfiguration());

    currentProject = wrapper.getProject();
    sourceMapManager = new SourceMapManager(resourceResolver);
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
        target.resourceResolver,
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
    return connection.isConnected();
  }

  @Override
  public void disconnect() throws DebugException {
    throw new UnsupportedOperationException("disconnect is not supported");
  }

  @Override
  public void fireTerminateEvent() {
    setActiveTarget(null);

    breakpointManager.dispose(false);

    cssScriptManager.dispose();

    if (htmlScriptManager != null) {
      htmlScriptManager.dispose();
    }

    if (dartCodeManager != null) {
      dartCodeManager.dispose();
    }

    sourceMapManager.dispose();

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
    return debugTargetName;
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

    if (enableBreakpoints) {
      connection.getDebugger().setBreakpointsActive(true);
      connection.getDebugger().setPauseOnExceptions(getPauseType(), null);
    } else {
      connection.getDebugger().setBreakpointsActive(false);
      connection.getDebugger().setPauseOnExceptions(PauseOnExceptionsType.none);
    }

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
        cssScriptManager.handleLoadEventFired();

        if (htmlScriptManager != null) {
          htmlScriptManager.handleLoadEventFired();
        }
      }
    });
    connection.getPage().enable();

    connection.getCSS().enable();

    if (SDBGDebugCorePlugin.SEND_MODIFIED_HTML) {
      connection.getDom().addDomListener(new DomListener() {
        @Override
        public void documentUpdated() {
          if (htmlScriptManager != null) {
            htmlScriptManager.handleDocumentUpdated();
          }
        }
      });
    }

    if (listenForInspectorDetach) {
      connection.getDom().addInspectorListener(new InspectorListener() {
        @Override
        public void detached(String reason) {
          handleInspectorDetached(reason);
        }

        @Override
        public void targetCrashed() {

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
        breakpointManager.handleGlobalObjectCleared();
        //sourceMapManager.handleGlobalObjectCleared();
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

        System.out.println("Script " + script + " loaded");

        if (script.hasScriptSource() || script.getSourceMapURL() != null) {
          IStorage storage = new WebkitScriptStorage(script, script.getScriptSource());
          sourceMapManager.handleScriptParsed(storage, script.getSourceMapURL());
          breakpointManager.updateBreakpointsConcerningScript(storage);
//        } else {
//          try {
//            connection.getDebugger().getScriptSource(
//                script.getScriptId(),
//                new WebkitCallback<String>() {
//                  @Override
//                  public void handleResult(WebkitResult<String> result) {
//                    if (!result.isError()) {
//                      IStorage storage = new WebkitScriptStorage(script, result.getResult());
//                      sourceMapManager.handleScriptParsed(storage, script.getSourceMapURL());
//                      breakpointManager.updateBreakpointsConcerningScript(storage);
//                    }
//                  }
//                });
//          } catch (IOException e) {
//            throw new RuntimeException(e);
//          }
        }
      }
    });
    connection.getDebugger().enable();

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
    System.out.println("About to resolve breakpoints");
    breakpointManager.connect();
    System.out.println("Breakpoints resolved");

    connection.getDebugger().setBreakpointsActive(enableBreakpoints);

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

//&&&    
//    if (currentProject == null) {
    return true;
//    } else {
//      return currentProject.equals(breakpoint.getMarker().getResource().getProject());
//    }
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
    process.terminate();
  }

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

  protected BreakpointManager getBreakpointManager() {
    return breakpointManager;
  }

  protected IResourceResolver getResourceResolver() {
    return resourceResolver;
  }

  //&&&!!!
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
//      DebugUIHelper.getHelper().showError(
//          "Debugger Connection Closed",
//          "The debugger connection has been closed by the remote host.");
      DebugUIHelper.getHelper().showDevtoolsDisconnectError("Debugger Connection Closed", this);
    }
  }

  protected void printExceptionToStdout(WebkitRemoteObject exception) {
    try {
      getConnection().getRuntime().callToString(
          exception.getObjectId(),
          new WebkitCallback<String>() {
            @Override
            public void handleResult(WebkitResult<String> result) {
              if (!result.isError()) {
                String text = result.getResult();

                int index = text.indexOf('\n');

                if (index != -1) {
                  text = text.substring(0, index).trim();
                }

                process.getStreamMonitor().messageAdded("Breaking on exception: " + text + "\n");
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
    final BreakOnExceptions boe = SDBGDebugCorePlugin.getPlugin().getBreakOnExceptions();
    PauseOnExceptionsType pauseType = PauseOnExceptionsType.none;

    if (boe == BreakOnExceptions.uncaught) {
      pauseType = PauseOnExceptionsType.uncaught;
    } else if (boe == BreakOnExceptions.all) {
      pauseType = PauseOnExceptionsType.all;
    }

    return pauseType;
  }
}
