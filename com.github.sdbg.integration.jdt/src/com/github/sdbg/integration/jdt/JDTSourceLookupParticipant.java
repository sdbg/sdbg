package com.github.sdbg.integration.jdt;

import com.github.sdbg.debug.core.util.SourceUtils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaSourceLookupParticipant;

public class JDTSourceLookupParticipant extends JavaSourceLookupParticipant {
  public JDTSourceLookupParticipant() {
  }

  @Override
  public String getSourceName(Object object) throws CoreException {
    return SourceUtils.getSourceName(object);
  }
}
