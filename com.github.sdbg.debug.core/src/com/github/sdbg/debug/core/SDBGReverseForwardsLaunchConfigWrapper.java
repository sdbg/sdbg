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
package com.github.sdbg.debug.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

/**
 * A wrapper class around ILaunchConfiguration and ILaunchConfigurationWorkingCopy objects. It adds
 * compiler type checking to what is essentially a property map.
 */
public class SDBGReverseForwardsLaunchConfigWrapper {
  private static final String REVERSE_FORWARDS = "reverseForwards", DEVICE = "device",
      DEVICE_COMMAND_PORT = "deviceCommandPort";

  public static String getReverseForward(String host, Integer port, Integer devicePort) {
    return (host != null ? host.trim() : "") + ":" + (port != null ? port.toString() : "") + ":"
        + (devicePort != null ? devicePort.toString() : "");
  }

  public static Integer getReverseForwardDevicePort(String forward) {
    String[] str = forward.split("\\:");
    if (str[2].length() > 0) {
      return Integer.parseInt(str[2]);
    } else {
      return null;
    }
  }

  public static String getReverseForwardHost(String forward) {
    String[] str = forward.split("\\:");
    return str[0].length() > 0 ? str[0] : null;
  }

  public static Integer getReverseForwardPort(String forward) {
    String[] str = forward.split("\\:");
    if (str[1].length() > 0) {
      return Integer.parseInt(str[1]);
    } else {
      return null;
    }
  }

  private ILaunchConfiguration launchConfig;

  /**
   * Create a new DartLaunchConfigWrapper given either a ILaunchConfiguration (for read-only launch
   * configs) or ILaunchConfigurationWorkingCopy (for writeable launch configs).
   */
  public SDBGReverseForwardsLaunchConfigWrapper(ILaunchConfiguration launchConfig) {
    this.launchConfig = launchConfig;
  }

  /**
   * @return the launch configuration that this SDBGLaucnConfigWrapper wraps
   */
  public ILaunchConfiguration getConfig() {
    return launchConfig;
  }

  public String getDevice() {
    try {
      return launchConfig.getAttribute(DEVICE, "");
    } catch (CoreException e) {
      SDBGDebugCorePlugin.logError(e);

      return "";
    }
  }

  public int getDeviceCommandPort() {
    try {
      return launchConfig.getAttribute(REVERSE_FORWARDS, 6565);
    } catch (CoreException e) {
      SDBGDebugCorePlugin.logError(e);
      return 6565;
    }
  }

  /**
   * @return the last time this config was launched, or 0 or no such
   */
  public long getLastLaunchTime() {
    // TODO: The persistence of the last launch time should ideally be done outside of the .launch file itself
    // or else the launch file will constantly be dirty, which is annoying when it is stored in a source control prepository
    return 0;
//    try {
//    return 0;
//      String value = launchConfig.getAttribute(LAST_LAUNCH_TIME, "0");
//      return Long.parseLong(value);
//    } catch (NumberFormatException ex) {
//      return 0;
//    } catch (CoreException ce) {
//      SDBGDebugCorePlugin.logError(ce);
//
//      return 0;
//    }
  }

  public List<String> getReverseForwards() {
    return new ArrayList<String>(Arrays.asList(getReverseForwardsStr().split("\\,")));
  }

  public String getReverseForwardsStr() {
    try {
      return launchConfig.getAttribute(REVERSE_FORWARDS, "");
    } catch (CoreException e) {
      SDBGDebugCorePlugin.logError(e);
      return "";
    }
  }

  /**
   * Indicate that this launch configuration was just launched.
   */
  public void markAsLaunched() {
// TODO: The persistence of the last launch time should ideally be done outside of the .launch file itself
// or else the launch file will constantly be dirty, which is annoying when it is stored in a source control prepository    
//    try {
//      ILaunchConfigurationWorkingCopy workingCopy = launchConfig.getWorkingCopy();
//
//      long launchTime = System.currentTimeMillis();
//
//      workingCopy.setAttribute(LAST_LAUNCH_TIME, Long.toString(launchTime));
//
//      workingCopy.doSave();
//    } catch (CoreException ce) {
//      SDBGDebugCorePlugin.logError(ce);
//    }
  }

  /**
   * @see #getDevice()
   */
  public void setDevice(String value) {
    getWorkingCopy().setAttribute(DEVICE, value);
  }

  /**
   * @see #getDeviceCommandPort()
   */
  public void setDeviceCommandPort(int value) {
    getWorkingCopy().setAttribute(DEVICE_COMMAND_PORT, value);
  }

  public void setReverseForwards(List<String> forwards) {
    StringBuilder sb = new StringBuilder();
    for (String forward : forwards) {
      if (sb.length() > 0) {
        sb.append(",");
      }
      sb.append(forward);
    }

    setReverseForwardsStr(sb.toString());
  }

  /**
   * @see #getReverseForwardsStr()
   */
  public void setReverseForwardsStr(String value) {
    getWorkingCopy().setAttribute(REVERSE_FORWARDS, value);
  }

  protected ILaunchConfigurationWorkingCopy getWorkingCopy() {
    return (ILaunchConfigurationWorkingCopy) launchConfig;
  }

}
