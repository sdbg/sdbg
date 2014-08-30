package com.github.sdbg.debug.core.model;

public interface IDOMResourceTracker {
  void dispose();

  void initialize(IDOMResources domResources);
}
