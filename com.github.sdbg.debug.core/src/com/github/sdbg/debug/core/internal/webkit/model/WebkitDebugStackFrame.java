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

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.internal.expr.IExpressionEvaluator;
import com.github.sdbg.debug.core.internal.expr.WatchExpressionResult;
import com.github.sdbg.debug.core.internal.util.DebuggerUtils;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitCallFrame;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitCallback;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitLocation;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitRemoteObject;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitResult;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitScope;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitScript;
import com.github.sdbg.debug.core.model.IExceptionStackFrame;
import com.github.sdbg.debug.core.model.ISDBGStackFrame;
import com.github.sdbg.debug.core.model.ISDBGValue.IValueCallback;
import com.github.sdbg.debug.core.model.IVariableResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.core.model.IWatchExpressionListener;

/**
 * The IStackFrame implementation for the Webkit debug elements. This stack frame element represents
 * a Dart frame.
 */
public class WebkitDebugStackFrame extends WebkitDebugElement implements IStackFrame,
    ISDBGStackFrame, IExceptionStackFrame, IVariableResolver, IExpressionEvaluator {
  private IThread thread;

  private WebkitCallFrame webkitFrame;

  private boolean isExceptionStackFrame;

  private VariableCollector variableCollector = VariableCollector.empty();
  private IValue classValue;

  private IValue globalScopeValue;

  public WebkitDebugStackFrame(IDebugTarget target, IThread thread, WebkitCallFrame webkitFrame) {
    this(target, thread, webkitFrame, null);
  }

  public WebkitDebugStackFrame(IDebugTarget target, IThread thread, WebkitCallFrame webkitFrame,
      WebkitRemoteObject exception) {
    super(target);

    this.thread = thread;
    this.webkitFrame = webkitFrame;

    fillInWebkitVariables(exception);
  }

  @Override
  public boolean canResume() {
    return getThread().canResume();
  }

  @Override
  public boolean canStepInto() {
    return !hasException() && getThread().canStepInto();
  }

  @Override
  public boolean canStepOver() {
    return !hasException() && getThread().canStepOver();
  }

  @Override
  public boolean canStepReturn() {
    return !hasException() && getThread().canStepReturn();
  }

  @Override
  public boolean canSuspend() {
    return getThread().canSuspend();
  }

  @Override
  public boolean canTerminate() {
    return getThread().canTerminate();
  }

  @Override
  public void evaluateExpression(final String expression, final IWatchExpressionListener listener) {
    try {
      getConnection().getDebugger().evaluateOnCallFrame(
          webkitFrame.getCallFrameId(),
          expression,
          new WebkitCallback<WebkitRemoteObject>() {
            @Override
            public void handleResult(WebkitResult<WebkitRemoteObject> result) {
              if (result.isError()) {
                if (result.getError() instanceof WebkitRemoteObject) {
                  WebkitRemoteObject error = (WebkitRemoteObject) result.getError();

                  String desc;

                  if (error.isObject()) {
                    desc = error.getDescription();
                  } else if (error.isString()) {
                    desc = error.getValue();
                  } else {
                    desc = error.toString();
                  }

                  listener.watchEvaluationFinished(WatchExpressionResult.error(expression, desc));
                } else {
                  listener.watchEvaluationFinished(WatchExpressionResult.error(
                      expression,
                      result.getError().toString()));
                }
              } else {
                IValue value = WebkitDebugValue.create(getTarget(), null, result.getResult());

                listener.watchEvaluationFinished(WatchExpressionResult.value(expression, value));
              }
            }
          });
    } catch (IOException e) {
      listener.watchEvaluationFinished(WatchExpressionResult.noOp(expression));
    }
  }

  @Override
  public IVariable findVariable(String varName) throws DebugException {
    // search in locals
    for (IVariable var : getVariables()) {
      if (var.getName().equals(varName)) {
        return var;
      }
    }

    // search in instance variables
    IVariable thisVar = getThisVariable();

    if (thisVar != null) {
      IValue thisValue = thisVar.getValue();

      for (IVariable var : thisValue.getVariables()) {
        if (var.getName().equals(varName)) {
          return var;
        }
      }
    }

    // search statics
    if (getClassValue() != null) {
      for (IVariable var : getClassValue().getVariables()) {
        if (var.getName().equals(varName)) {
          return var;
        }
      }
    }

    // search globals
    if (getGlobalsScope() != null) {
      for (IVariable var : getGlobalsScope().getVariables()) {
        if (var.getName().equals(varName)) {
          return var;
        }
      }
    }

    return null;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Object getAdapter(Class adapterClass) {
    if (adapterClass == IThread.class) {
      return getThread();
    } else {
      return super.getAdapter(adapterClass);
    }
  }

  @Override
  public int getCharEnd() throws DebugException {
    return -1;
  }

  @Override
  public int getCharStart() throws DebugException {
    return -1;
  }

  @Override
  public String getExceptionDisplayText() throws DebugException {
    WebkitDebugVariable variable = (WebkitDebugVariable) getVariables()[0];
    WebkitDebugValue exceptionValue = (WebkitDebugValue) variable.getValue();

    final String[] result = new String[1];
    final CountDownLatch latch = new CountDownLatch(1);

    exceptionValue.computeDetail(new IValueCallback() {
      @Override
      public void detailComputed(String stringValue) {
        result[0] = stringValue;

        latch.countDown();
      }
    });

    try {
      latch.await(1000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      return "Exception: " + exceptionValue.getDisplayString();
    }

    return "Exception: " + result[0];
  }

  @Override
  public int getLineNumber() throws DebugException {
    try {
      if (getTarget().shouldUseSourceMapping() && isUsingSourceMaps()) {
        SourceMapManager.SourceLocation location = getMappedLocation();

        return WebkitLocation.webkitToElipseLine(location.getLine());
      } else {
        return WebkitLocation.webkitToElipseLine(webkitFrame.getLocation().getLineNumber());
      }
    } catch (Throwable t) {
      SDBGDebugCorePlugin.logError(t);

      return 1;
    }
  }

  @Override
  public String getLongName() {
    String file = getFileName();
    if (file != null) {
      try {
        int lineNumber = getLineNumber();
        if (lineNumber > 0) {
          file += ":" + lineNumber;
        }
      } catch (DebugException e) {
      }
    }

    String name = getCallerName();
    if (name != null && name.length() > 0) {
      return name + "(" + (file != null ? file : "") + ")";
    } else if (file != null) {
      return file;
    } else {
      return "(code block)";
    }
  }

  @Override
  public String getName() throws DebugException {
    return getLongName();
  }

  @Override
  public IRegisterGroup[] getRegisterGroups() throws DebugException {
    return new IRegisterGroup[0];
  }

  @Override
  public String getShortName() {
    String callerName = getCallerName();
    if (callerName != null && callerName.length() > 0) {
      return getCallerName() + "()";
    } else {
      return "(code block)";
    }
  }

  @Override
  public String getSourceLocationPath() {
    try {
      if (getTarget().shouldUseSourceMapping() && isUsingSourceMaps()) {
        return getMappedLocationPath();
      } else {
        IStorage storage = getTarget().getScriptStorageFor(webkitFrame);
        if (storage != null) {
          return storage.getFullPath().toPortableString();
        } else {
          return null;
        }
      }
    } catch (Throwable t) {
      SDBGDebugCorePlugin.logError(t);

      return null;
    }
  }

  @Override
  public IThread getThread() {
    return thread;
  }

  @Override
  public IVariable[] getVariables() throws DebugException {
    try {
      return variableCollector.getVariables();
    } catch (InterruptedException e) {
      throw new DebugException(new Status(
          IStatus.ERROR,
          SDBGDebugCorePlugin.PLUGIN_ID,
          e.toString(),
          e));
    }
  }

  @Override
  public boolean hasException() {
    return isExceptionStackFrame;
  }

  @Override
  public boolean hasRegisterGroups() throws DebugException {
    return false;
  }

  @Override
  public boolean hasVariables() throws DebugException {
    return getVariables().length > 0;
  }

  @Override
  public boolean isPrivate() {
    return DebuggerUtils.isPrivateName(webkitFrame.getFunctionName());
  }

  public boolean isPrivateMethod() {
    return webkitFrame.isPrivateMethod();
  }

  @Override
  public boolean isStepping() {
    return getThread().isStepping();
  }

  @Override
  public boolean isSuspended() {
    return getThread().isSuspended();
  }

  @Override
  public boolean isTerminated() {
    return getThread().isTerminated();
  }

  @Override
  public boolean isUsingSourceMaps() {
    return getMappedLocation() != null;
  }

  @Override
  public void resume() throws DebugException {
    getThread().resume();
  }

  @Override
  public void stepInto() throws DebugException {
    getThread().stepInto();
  }

  @Override
  public void stepOver() throws DebugException {
    getThread().stepOver();
  }

  @Override
  public void stepReturn() throws DebugException {
    getThread().stepReturn();
  }

  @Override
  public void suspend() throws DebugException {
    getThread().suspend();
  }

  @Override
  public void terminate() throws DebugException {
    getThread().terminate();
  }

  @Override
  public String toString() {
    return getShortName();
  }

  protected IValue getClassValue() throws DebugException {
    if (classValue != null) {
      return classValue;
    }

    for (WebkitScope scope : webkitFrame.getScopeChain()) {
      if (scope.isClass()) {
        classValue = new WebkitDebugValue(getTarget(), null, scope.getObject());
        break;
      }
    }

    return classValue;
  }

  protected IValue getGlobalsScope() throws DebugException {
    if (globalScopeValue != null) {
      return globalScopeValue;
    }

    for (WebkitScope scope : webkitFrame.getScopeChain()) {
      if (scope.isGlobal()) {
        globalScopeValue = new WebkitDebugValue(getTarget(), null, scope.getObject());
        break;
      }
    }

    return globalScopeValue;
  }

  protected String getMappedLocationPath() {
    SourceMapManager.SourceLocation targetLocation = getMappedLocation();

    if (SourceMapManager.isTracing()) {
      WebkitLocation sourceLocation = webkitFrame.getLocation();
      WebkitScript script = getConnection().getDebugger().getScript(sourceLocation.getScriptId());
      String scriptPath = script == null ? "null" : script.getUrl();

      SourceMapManager.trace("[" + scriptPath + "," + sourceLocation.getLineNumber() + ","
          + sourceLocation.getColumnNumber() + "] ==> mapped to " + targetLocation);
    }

//!!!
//    if (targetLocation.getStorage() instanceof IFile) {
//      return targetLocation.getStorage().getFullPath().toPortableString();
//    } else {
    return targetLocation.getPath();
//    }
  }

  protected IVariable getThisVariable() throws DebugException {
    for (IVariable var : getVariables()) {
      if (var instanceof WebkitDebugVariable) {
        if (((WebkitDebugVariable) var).isThisObject()) {
          return var;
        }
      }
    }

    return null;
  }

  protected WebkitCallFrame getWebkitFrame() {
    return webkitFrame;
  }

  /**
   * Fill in the IVariables from the Webkit variables.
   * 
   * @param exception can be null
   */
  private void fillInWebkitVariables(WebkitRemoteObject exception) {
    isExceptionStackFrame = (exception != null);

    List<WebkitRemoteObject> remoteObjects = new ArrayList<WebkitRemoteObject>();

    WebkitRemoteObject thisObject = null;

    if (!webkitFrame.isStaticMethod()) {
      thisObject = webkitFrame.getThisObject();
    }

    for (WebkitScope scope : webkitFrame.getScopeChain()) {
      if (!scope.isGlobalLike()) {
        remoteObjects.add(scope.getObject());
      }
    }

    variableCollector = VariableCollector.createCollector(
        getTarget(),
        thisObject,
        remoteObjects,
        null,
        exception);
  }

  private String getCallerName() {
    String name = null;

    if (getTarget().shouldUseSourceMapping() && isUsingSourceMaps()) {
      SourceMapManager.SourceLocation location = getMappedLocation();
      name = location.getName();
    }

    if (name == null) {
      name = DebuggerUtils.demangleVmName(webkitFrame.getFunctionName());
    }

    return name;
  }

  private String getFileName() {
    String path = getSourceLocationPath();

    if (path != null) {
      int index = path.lastIndexOf('/');

      if (index != -1) {
        return path.substring(index + 1);
      } else {
        return path;
      }
    }

    return null;
  }

  private SourceMapManager.SourceLocation getMappedLocation() {
    IStorage storage = getTarget().getScriptStorageFor(webkitFrame);

    if (getTarget().getSourceMapManager().isMapSource(storage)) {
      WebkitLocation location = webkitFrame.getLocation();

      return getTarget().getSourceMapManager().getMappingFor(
          storage,
          location.getLineNumber(),
          location.getColumnNumber());
    } else {
      return null;
    }
  }
}
