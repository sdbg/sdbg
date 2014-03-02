package com.github.sdbg.debug.core.internal.source;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;

public class SDBGSourcePathComputer implements ISourcePathComputer {
  private static final String ID = SDBGSourcePathComputer.class.getName();

  private ISourcePathComputerDelegate[] delegates;

  public SDBGSourcePathComputer(ISourcePathComputerDelegate... delegates) {
    this.delegates = delegates;
  }

  @Override
  public ISourceContainer[] computeSourceContainers(ILaunchConfiguration configuration,
      IProgressMonitor monitor) throws CoreException {
    SubMonitor subMonitor = SubMonitor.convert(monitor, delegates.length);

    try {
      List<ISourceContainer> containers = new ArrayList<ISourceContainer>();
      for (ISourcePathComputerDelegate delegate : delegates) {
        containers.addAll(Arrays.asList(delegate.computeSourceContainers(
            configuration,
            subMonitor.newChild(1))));
      }

      return containers.toArray(new ISourceContainer[0]);
    } finally {
      subMonitor.done();
    }
  }

  @Override
  public String getId() {
    return ID;
  }
}
