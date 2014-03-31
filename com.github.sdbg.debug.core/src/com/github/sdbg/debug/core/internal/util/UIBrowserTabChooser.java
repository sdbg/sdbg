package com.github.sdbg.debug.core.internal.util;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.util.IBrowserTabChooser;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

public class UIBrowserTabChooser {
  private static IBrowserTabChooser uiBrowserTabChooser;

  public static synchronized IBrowserTabChooser get() {
    if (uiBrowserTabChooser == null) {
      IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(
          "com.github.sdbg.debug.core.uiBrowserTabChooser");
      for (IConfigurationElement element : extensionPoint.getConfigurationElements()) {
        try {
          uiBrowserTabChooser = (IBrowserTabChooser) element.createExecutableExtension("class");
          break;
        } catch (CoreException e) {
          SDBGDebugCorePlugin.logError(e);
        }
      }
    }

    return uiBrowserTabChooser;
  }

  private UIBrowserTabChooser() {
  }
}
