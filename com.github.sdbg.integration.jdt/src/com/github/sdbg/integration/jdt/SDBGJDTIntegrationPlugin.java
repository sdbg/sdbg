package com.github.sdbg.integration.jdt;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;

import org.eclipse.core.runtime.CoreException;

public class SDBGJDTIntegrationPlugin {
  public static final String PLUGIN_ID = "com.github.sdbg.integration.jdt";
  
  private SDBGJDTIntegrationPlugin() {}
  
  public static CoreException wrapError(Exception e) {
    return SDBGDebugCorePlugin.wrapError(PLUGIN_ID, e);
  }
}
