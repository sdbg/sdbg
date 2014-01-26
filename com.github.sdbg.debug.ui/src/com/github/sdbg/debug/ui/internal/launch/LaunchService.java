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
package com.github.sdbg.debug.ui.internal.launch;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;

import com.github.sdbg.debug.ui.internal.util.LaunchUtils;
import com.google.common.collect.Lists;

/**
 * Launching service.
 */
public class LaunchService {

  /**
   * Provides launch lifecycle notifications.
   */
  public interface LaunchListener {

    void launchStarted(ILaunch launch);

    void launchTerminated(ILaunch launch);
  }

  private final List<LaunchListener> listeners = Lists.newArrayList();

  private final ILaunchesListener2 debugLaunchListener = new ILaunchesListener2() {

    @Override
    public void launchesAdded(ILaunch[] launches) {
      notifyLaunchStarted(launches);
    }

    @Override
    public void launchesChanged(ILaunch[] launches) {
      //Ignore
    }

    @Override
    public void launchesRemoved(ILaunch[] launches) {
      //Ignore
    }

    @Override
    public void launchesTerminated(ILaunch[] launches) {
      notifyLaunchTerminated(launches);
    }
  };

  private static LaunchService instance;

  public static LaunchService getInstance() {
    if (instance == null) {
      instance = new LaunchService();
    }
    return instance;
  }

  private LaunchService() {
    DebugPlugin.getDefault().getLaunchManager().addLaunchListener(debugLaunchListener);
  }

  public void addListener(LaunchListener listener) {
    listeners.add(listener);
  }

  private void launch(IResource resource, ILaunchShortcut launchShortcut) {
    ISelection launchedSelection = new StructuredSelection(resource);
    launchShortcut.launch(launchedSelection, ILaunchManager.DEBUG_MODE);
  }

  public void launchInChrome(IResource resource) {
    launch(resource, LaunchUtils.getChromeLaunchShortcut());
  }

  private void notifyLaunchStarted(ILaunch[] launches) {
    for (LaunchListener listener : listeners) {
      for (ILaunch launch : launches) {
        listener.launchStarted(launch);
      }
    }
  }

  private void notifyLaunchTerminated(ILaunch[] launches) {
    for (LaunchListener listener : listeners) {
      for (ILaunch launch : launches) {
        listener.launchStarted(launch);
      }
    }
  }

  public void removeListener(LaunchListener listener) {
    listeners.remove(listener);
  }

}
