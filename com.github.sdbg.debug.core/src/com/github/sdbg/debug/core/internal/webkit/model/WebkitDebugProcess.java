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

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;

/**
 * This is a Webkit specific implementation of an IProcess.
 */
class WebkitDebugProcess extends PlatformObject implements IProcess {
  private String browserName;
  private WebkitDebugTarget target;
  private Process javaProcess;
  private IStreamsProxy streamsProxy;
  private Map<String, String> attributes = new HashMap<String, String>();
  private Date launchTime;
  private WebkitStreamMonitor streamMonitor;
  private boolean isTerminated = false;

  public WebkitDebugProcess(WebkitDebugTarget target, String browserName, Process javaProcess) {
    this.target = target;
    this.browserName = browserName;
    this.javaProcess = javaProcess;

    launchTime = new Date();
    streamMonitor = new WebkitStreamMonitor();

    if (javaProcess != null) {
      new Thread(new Runnable() {
        @Override
        public void run() {
          waitForExit();
        }
      }).start();
    }
  }

  @Override
  public boolean canTerminate() {
    return !isTerminated();
  }

  void fireCreationEvent() {
    fireEvent(new DebugEvent(this, DebugEvent.CREATE));
  }

  private void fireEvent(DebugEvent event) {
    DebugPlugin manager = DebugPlugin.getDefault();
    if (manager != null) {
      manager.fireDebugEventSet(new DebugEvent[] {event});
    }
  }

  private void fireTerminateEvent() {
    isTerminated = true;

    fireEvent(new DebugEvent(this, DebugEvent.TERMINATE));
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Object getAdapter(Class adapter) {
    if (adapter == ILaunch.class) {
      return getLaunch();
    }

    if (adapter == IDebugTarget.class) {
      return target;
    }

    return super.getAdapter(adapter);
  }

  @Override
  public String getAttribute(String key) {
    if (DebugPlugin.ATTR_CONSOLE_ENCODING.equals(key)) {
      return "UTF-8";
    }

    return attributes.get(key);
  }

  @Override
  public int getExitValue() throws DebugException {
    if (javaProcess == null) {
      if (isTerminated()) {
        return 0;
      } else {
        throw new DebugException(new Status(
            IStatus.ERROR,
            SDBGDebugCorePlugin.PLUGIN_ID,
            "Not yet terminated"));
      }
    } else {
      try {
        return javaProcess.exitValue();
      } catch (IllegalThreadStateException exception) {
        throw new DebugException(new Status(
            IStatus.ERROR,
            SDBGDebugCorePlugin.PLUGIN_ID,
            exception.toString()));
      }
    }
  }

  protected Process getJavaProcess() {
    return javaProcess;
  }

  @Override
  public String getLabel() {
    return browserName + " (" + launchTime + ")";
  }

  @Override
  public ILaunch getLaunch() {
    return target.getLaunch();
  }

  protected WebkitStreamMonitor getStreamMonitor() {
    return streamMonitor;
  }

  @Override
  public IStreamsProxy getStreamsProxy() {
    if (streamsProxy == null) {
      streamsProxy = new IStreamsProxy() {
        @Override
        public IStreamMonitor getErrorStreamMonitor() {
          return null;
        }

        @Override
        public IStreamMonitor getOutputStreamMonitor() {
          return streamMonitor;
        }

        @Override
        public void write(String input) throws IOException {
          // no-op
        }
      };
    }

    return streamsProxy;
  }

  @Override
  public boolean isTerminated() {
    try {
      if (javaProcess != null) {
        javaProcess.exitValue();
        return true;
      }
    } catch (IllegalThreadStateException exception) {
      return false;
    }

    return isTerminated;
  }

  @Override
  public void setAttribute(String key, String value) {
    attributes.put(key, value);
  }

  protected void switchTo(WebkitDebugTarget target) {
    this.target = target;
  }

  @Override
  public void terminate() {
    if (javaProcess != null) {
      javaProcess.destroy();
    }
    isTerminated = true;
  }

  protected void waitForExit() {
    try {
      javaProcess.waitFor();

      fireTerminateEvent();
    } catch (InterruptedException e) {

    }
  }

}
