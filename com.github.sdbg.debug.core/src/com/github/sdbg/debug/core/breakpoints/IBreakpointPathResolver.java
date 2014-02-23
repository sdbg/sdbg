package com.github.sdbg.debug.core.breakpoints;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IBreakpoint;

public interface IBreakpointPathResolver {
  String EXTENSION_ID = "com.github.sdbg.debug.core.breakpointPathResolver";

  String getPath(IBreakpoint breakpoint) throws CoreException;

  boolean isSupported(IBreakpoint breakpoint);
}
