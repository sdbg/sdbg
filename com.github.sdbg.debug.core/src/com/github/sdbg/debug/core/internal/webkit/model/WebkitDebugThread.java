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

import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitCallFrame;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitDebugger.PausedReasonType;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitRemoteObject;
import com.github.sdbg.debug.core.model.ISDBGThread;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;

/**
 * The IThread implementation for the Webkit debug elements.
 */
public class WebkitDebugThread extends WebkitDebugElement implements ISDBGThread {
  private static final IBreakpoint[] EMPTY_BREAKPOINTS = new IBreakpoint[0];

  private static final IStackFrame[] EMPTY_FRAMES = new IStackFrame[0];

  private int expectedSuspendReason = DebugEvent.UNSPECIFIED;
  private int expectedResumeReason = DebugEvent.UNSPECIFIED;

  private boolean suspended;
  private IStackFrame[] suspendedFrames = EMPTY_FRAMES;
  private IBreakpoint[] suspendedBreakpoints = EMPTY_BREAKPOINTS;

  /**
   * @param target
   */
  public WebkitDebugThread(IDebugTarget target) {
    super(target);
  }

  @Override
  public boolean canResume() {
    return !isDisconnected() && isSuspended();
  }
  
  @Override
  public boolean canStepInto() {
    return isSuspended();
  }

  @Override
  public boolean canStepOver() {
    return isSuspended();
  }

  @Override
  public boolean canStepReturn() {
    // aka stepOut
    return isSuspended();
  }

  @Override
  public boolean canSuspend() {
    return !isTerminated() && !isSuspended();
  }

  @Override
  public boolean canTerminate() {
    return getDebugTarget().canTerminate();
  }

  @Override
  public IBreakpoint[] getBreakpoints() {
    return suspendedBreakpoints;
  }

  /**
   * Return a pseudo stack frame for the current isolate. This frame will be able to enumerate all
   * the libraries in the current isolate, and each library's top-level variables.
   * 
   * @return a stack frame representing the libraries and top-level variables for the isolate
   */
  @Override
  public IStackFrame getIsolateVarsPseudoFrame() {
    return new WebkitDebugIsolateFrame(this);
  }

  @Override
  public String getName() throws DebugException {
    // TODO(devoncarew): we need to be able to retrieve the list of isolates from the VM.

    return "JS Thread" + (isSuspended() ? " (Suspended)" : " (Running)");
  }

  @Override
  public int getPriority() throws DebugException {
    return 0;
  }

  @Override
  public IStackFrame[] getStackFrames() throws DebugException {
    return suspendedFrames;
  }

  @Override
  public IStackFrame getTopStackFrame() throws DebugException {
    IStackFrame[] frames = getStackFrames();

    return frames.length > 0 ? frames[0] : null;
  }

  @Override
  public boolean hasStackFrames() throws DebugException {
    return isSuspended();
  }

  @Override
  public boolean isStepping() {
    return expectedResumeReason == DebugEvent.STEP_INTO
        || expectedResumeReason == DebugEvent.STEP_OVER
        || expectedResumeReason == DebugEvent.STEP_RETURN
        || expectedSuspendReason == DebugEvent.STEP_END;
  }

  @Override
  public boolean isSuspended() {
    return suspended;
  }

  @Override
  public boolean isTerminated() {
    return getDebugTarget().isTerminated();
  }

  @Override
  public void resume() throws DebugException {
    try {
      expectedResumeReason = DebugEvent.UNSPECIFIED;

      getConnection().getDebugger().resume();
    } catch (IOException exception) {
      throw createDebugException(exception);
    }
  }

  @Override
  public void stepInto() throws DebugException {
    expectedResumeReason = DebugEvent.STEP_INTO;
    expectedSuspendReason = DebugEvent.STEP_END;

    try {
      getConnection().getDebugger().stepInto();
    } catch (IOException exception) {
      expectedResumeReason = DebugEvent.UNSPECIFIED;
      expectedSuspendReason = DebugEvent.UNSPECIFIED;

      throw createDebugException(exception);
    }
  }

  @Override
  public void stepOver() throws DebugException {
    expectedResumeReason = DebugEvent.STEP_OVER;
    expectedSuspendReason = DebugEvent.STEP_END;

    try {
      getConnection().getDebugger().stepOver();
    } catch (IOException exception) {
      expectedResumeReason = DebugEvent.UNSPECIFIED;
      expectedSuspendReason = DebugEvent.UNSPECIFIED;

      throw createDebugException(exception);
    }
  }

  @Override
  public void stepReturn() throws DebugException {
    expectedResumeReason = DebugEvent.STEP_RETURN;
    expectedSuspendReason = DebugEvent.STEP_END;

    try {
      getConnection().getDebugger().stepOut();
    } catch (IOException exception) {
      expectedResumeReason = DebugEvent.UNSPECIFIED;
      expectedSuspendReason = DebugEvent.UNSPECIFIED;

      throw createDebugException(exception);
    }
  }

  /**
   * Causes this element to suspend its execution, generating a <code>SUSPEND</code> event. Has no
   * effect on an already suspended element. Implementations may be blocking or non-blocking.
   * 
   * @exception DebugException on failure. Reasons include: <br>
   *              TARGET_REQUEST_FAILED - The request failed in the target <br>
   *              NOT_SUPPORTED - The capability is not supported by the target
   */
  @Override
  public void suspend() throws DebugException {
    expectedSuspendReason = DebugEvent.CLIENT_REQUEST;

    try {
      getConnection().getDebugger().pause();
    } catch (IOException exception) {
      expectedSuspendReason = DebugEvent.UNSPECIFIED;

      throw createDebugException(exception);
    }
  }

  @Override
  public void terminate() throws DebugException {
    getDebugTarget().terminate();
  }

  protected void handleDebuggerSuspended(PausedReasonType pausedReason,
      List<WebkitCallFrame> webkitFrames, WebkitRemoteObject exception) {
    int reason = DebugEvent.BREAKPOINT;

    if (expectedSuspendReason != DebugEvent.UNSPECIFIED) {
      reason = expectedSuspendReason;
      expectedSuspendReason = DebugEvent.UNSPECIFIED;
    } else {
      IBreakpoint breakpoint = getBreakpointFor(webkitFrames);

      if (breakpoint != null) {
        suspendedBreakpoints = new IBreakpoint[] {breakpoint};
        reason = DebugEvent.BREAKPOINT;
      }
    }

    suspended = true;

    suspendedFrames = createFrames(webkitFrames, exception);

    fireSuspendEvent(reason);
  }

  void handleDebuggerResumed() {
    // clear data
    suspended = false;
    suspendedFrames = EMPTY_FRAMES;
    suspendedBreakpoints = EMPTY_BREAKPOINTS;

    // send event
    int reason = expectedResumeReason;
    expectedResumeReason = DebugEvent.UNSPECIFIED;

    fireResumeEvent(reason);
    getTarget().fireResumeEvent(DebugEvent.RESUME);
  }

  private IStackFrame[] createFrames(List<WebkitCallFrame> webkitFrames,
      WebkitRemoteObject exception) {
    List<IStackFrame> frames = new ArrayList<IStackFrame>();

    for (int i = 0; i < webkitFrames.size(); i++) {
      WebkitCallFrame webkitFrame = webkitFrames.get(i);

      WebkitDebugStackFrame frame;

      if (i == 0 && exception != null) {
        frame = new WebkitDebugStackFrame(getTarget(), this, webkitFrame, exception);
      } else {
        frame = new WebkitDebugStackFrame(getTarget(), this, webkitFrame);
      }

      frames.add(frame);
    }

    return frames.toArray(new IStackFrame[frames.size()]);
  }

  private IBreakpoint getBreakpointFor(List<WebkitCallFrame> frames) {
    if (frames.size() > 0) {
      return getBreakpointFor(frames.get(0));
    } else {
      return null;
    }
  }

  private IBreakpoint getBreakpointFor(WebkitCallFrame frame) {
    ISDBGBreakpointManager breakpointManager = getTarget().getBreakpointManager();

    if (breakpointManager != null) {
      return breakpointManager.getBreakpointFor(frame.getLocation());
    } else {
      return null;
    }
  }

  private boolean isDisconnected() {
    return getDebugTarget().isDisconnected();
  }
}
