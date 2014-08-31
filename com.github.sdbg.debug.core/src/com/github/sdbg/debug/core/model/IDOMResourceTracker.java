package com.github.sdbg.debug.core.model;

import org.eclipse.core.resources.IProject;

public interface IDOMResourceTracker {
  void dispose();

  void initialize(IProject project, IDOMResources domResources);
}
