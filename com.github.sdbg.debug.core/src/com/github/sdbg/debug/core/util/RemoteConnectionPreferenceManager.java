/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.github.sdbg.debug.core.util;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.utilities.Utilities;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Manages the settings for allowing remote connections to the resource server.
 */
public class RemoteConnectionPreferenceManager {

  private static RemoteConnectionPreferenceManager manager = new RemoteConnectionPreferenceManager();

  /**
   * Allow remote connections to the web server. Enabling remote connections can be done through via
   * a user-defined property in the "editor.properties" file, or by a preference in Preferences >
   * Run and Debug
   */

  public static final String REMOTE_CONNECTION_PREFERENCE = "remoteConnect";

  public static final String REMOTE_CONNECTION_OPTION = SDBGDebugCorePlugin.PLUGIN_ID + "/"
      + REMOTE_CONNECTION_PREFERENCE;

  public static RemoteConnectionPreferenceManager getManager() {
    return manager;
  }

  public boolean canConnectRemote() {

    String value = Utilities.getUserDefinedProperty(REMOTE_CONNECTION_OPTION);
    if (value != null) {
      return value.equalsIgnoreCase("true");
    }
    return getAllowRemoteConnectionPrefs();

  }

  public boolean getAllowRemoteConnectionPrefs() {
    return SDBGDebugCorePlugin.getPlugin().getPrefs().getBoolean(REMOTE_CONNECTION_PREFERENCE, true);
  }

  public void setAllowRemoteConnectionPreference(boolean value) {
    IEclipsePreferences prefs = SDBGDebugCorePlugin.getPlugin().getPrefs();
    prefs.putBoolean(REMOTE_CONNECTION_PREFERENCE, value);
    try {
      prefs.flush();
    } catch (BackingStoreException e) {

    }

  }

}
