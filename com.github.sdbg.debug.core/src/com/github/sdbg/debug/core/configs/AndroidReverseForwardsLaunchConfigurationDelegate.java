/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.github.sdbg.debug.core.configs;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.SDBGReverseForwardsLaunchConfigWrapper;
import com.github.sdbg.debug.core.internal.android.ReversePortForwarderManager;
import com.github.sdbg.debug.core.internal.forwarder.HostReversePortForwarder;
import com.github.sdbg.debug.core.internal.util.IDFilterDeviceChooser;
import com.github.sdbg.debug.core.internal.util.UIDeviceChooser;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;

/**
 * A ILaunchConfigurationDelegate implementation that can forwards certain hosts and ports to
 * localhost:<xxx> on the mobile device itself.
 */
public class AndroidReverseForwardsLaunchConfigurationDelegate extends
    LaunchConfigurationDelegate {
  /**
   * Create a new ChromeConnLaunchConfigurationDelegate.
   */
  public AndroidReverseForwardsLaunchConfigurationDelegate() {
  }

  @Override
  public boolean buildForLaunch(ILaunchConfiguration configuration, String mode,
      IProgressMonitor monitor) throws CoreException {
    return false;
  }

  @Override
  public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch,
      IProgressMonitor monitor) throws CoreException {
    if (!ILaunchManager.RUN_MODE.equals(mode)) {
      throw new CoreException(SDBGDebugCorePlugin.createErrorStatus("Execution mode '" + mode
          + "' is not supported."));
    }

    SDBGReverseForwardsLaunchConfigWrapper launchConfig = new SDBGReverseForwardsLaunchConfigWrapper(
        configuration);

    List<HostReversePortForwarder.Forward> forwards = new ArrayList<HostReversePortForwarder.Forward>();
    for (String forward : launchConfig.getReverseForwards()) {
      String host = SDBGReverseForwardsLaunchConfigWrapper.getReverseForwardHost(forward);
      Integer port = SDBGReverseForwardsLaunchConfigWrapper.getReverseForwardPort(forward);
      Integer devicePort = SDBGReverseForwardsLaunchConfigWrapper.getReverseForwardDevicePort(forward);

      if (host != null && port != null && devicePort != null) {
        forwards.add(new HostReversePortForwarder.Forward(host, port, devicePort));
      }
    }

    if (forwards.isEmpty()) {
      throw new DebugException(
          SDBGDebugCorePlugin.createErrorStatus("This configuration has no configured forwards."));
    }

    new ReversePortForwarderManager().start(
        launch,
        new IDFilterDeviceChooser(launchConfig.getDevice(), UIDeviceChooser.get()),
        launchConfig.getDeviceCommandPort(),
        forwards);
  }
}
