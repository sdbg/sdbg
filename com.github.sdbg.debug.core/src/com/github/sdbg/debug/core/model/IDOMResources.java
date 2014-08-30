package com.github.sdbg.debug.core.model;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;

import org.eclipse.core.runtime.CoreException;

public interface IDOMResources {
  Collection<IDOMResourceReference> getResources();

  void uploadNewSource(IDOMResourceReference resourceReference, Reader newContent)
      throws IOException, CoreException;
}
