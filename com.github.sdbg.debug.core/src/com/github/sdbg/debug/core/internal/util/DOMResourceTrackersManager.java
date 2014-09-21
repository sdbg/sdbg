package com.github.sdbg.debug.core.internal.util;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.model.IDOMResourceTracker;
import com.github.sdbg.debug.core.model.IDOMResources;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

public class DOMResourceTrackersManager {
  private Collection<IDOMResourceTracker> trackers;

  public void dispose() {
    for (IDOMResourceTracker tracker : trackers) {
      try {
        tracker.dispose();
      } catch (Exception e) {
        SDBGDebugCorePlugin.logError(e);
      }
    }

    trackers.clear();
  }

  protected void initialize(IProject project, IDOMResources domResources) {
    trackers = new ArrayList<IDOMResourceTracker>();
    IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(
        "com.github.sdbg.debug.core.domResourceTracker");
    for (IConfigurationElement element : extensionPoint.getConfigurationElements()) {
      try {
        IDOMResourceTracker tracker = (IDOMResourceTracker) element.createExecutableExtension("class");
        tracker.initialize(project, domResources);
        trackers.add(tracker);
      } catch (CoreException e) {
        SDBGDebugCorePlugin.logError(e);
      }
    }
  }
}
