package com.github.sdbg.debug.core.internal.webkit.model;

import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitBreakpoint;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitLocation;

import java.io.IOException;

import org.eclipse.core.resources.IStorage;
import org.eclipse.debug.core.model.IBreakpoint;

interface ISDBGBreakpointManager {

  public void addBreakpointsConcerningScript(IStorage script);

  public void connect() throws IOException;

  public void dispose(boolean deleteAll);

  public IBreakpoint getBreakpointFor(WebkitLocation location);

  public void handleBreakpointResolved(WebkitBreakpoint breakpoint);

  public void handleGlobalObjectCleared();

  public void removeBreakpointsConcerningScript(IStorage script);
}