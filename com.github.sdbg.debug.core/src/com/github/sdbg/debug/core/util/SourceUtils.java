package com.github.sdbg.debug.core.util;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.model.ISDBGStackFrame;

import org.eclipse.core.runtime.CoreException;

public class SourceUtils {
  public static String getSourceName(Object object) throws CoreException {
    if (object instanceof String) {
      return (String) object;
    } else if (object instanceof ISDBGStackFrame) {
      ISDBGStackFrame sourceLookup = (ISDBGStackFrame) object;

      return sourceLookup.getSourceLocationPath();
    } else {
      SDBGDebugCorePlugin.logWarning("Unhandled type " + object.getClass()
          + " in DartSourceLookupParticipant.getSourceName()");

      return null;
    }
  }

  private SourceUtils() {
  }
}
