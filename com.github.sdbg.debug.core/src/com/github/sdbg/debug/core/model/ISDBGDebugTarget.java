package com.github.sdbg.debug.core.model;

import java.io.IOException;

import org.eclipse.debug.core.model.IDebugTarget;

public interface ISDBGDebugTarget extends IDebugTarget {
  ISDBGDebugTarget reconnect() throws IOException;
}
