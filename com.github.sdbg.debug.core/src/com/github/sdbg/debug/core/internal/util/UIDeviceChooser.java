package com.github.sdbg.debug.core.internal.util;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.util.IDeviceChooser;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

public class UIDeviceChooser {
  private static IDeviceChooser uiDeviceChooser;

  public static synchronized IDeviceChooser get() {
    if (uiDeviceChooser == null) {
      IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(
          "com.github.sdbg.debug.core.uiDeviceChooser");
      for (IConfigurationElement element : extensionPoint.getConfigurationElements()) {
        try {
          uiDeviceChooser = (IDeviceChooser) element.createExecutableExtension("class");
          break;
        } catch (CoreException e) {
          SDBGDebugCorePlugin.logError(e);
        }
      }
    }

    return uiDeviceChooser;
  }

  private UIDeviceChooser() {
  }
}
