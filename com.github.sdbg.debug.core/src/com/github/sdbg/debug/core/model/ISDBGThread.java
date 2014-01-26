package com.github.sdbg.debug.core.model;

import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;

//&&&
public interface ISDBGThread extends IThread {
  public IStackFrame getIsolateVarsPseudoFrame();
}
