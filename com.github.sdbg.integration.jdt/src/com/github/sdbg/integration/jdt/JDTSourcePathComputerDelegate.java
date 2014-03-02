package com.github.sdbg.integration.jdt;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

public class JDTSourcePathComputerDelegate implements ISourcePathComputerDelegate {
  /* (non-Javadoc)
   * @see org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate#computeSourceContainers(org.eclipse.debug.core.ILaunchConfiguration, org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public ISourceContainer[] computeSourceContainers(ILaunchConfiguration configuration,
      IProgressMonitor monitor) throws CoreException {
    IRuntimeClasspathEntry[] resolved = new JDTSourcePathProvider().resolveClasspath(
        new JDTSourcePathProvider().computeUnresolvedClasspath(configuration),
        configuration);
    return JavaRuntime.getSourceContainers(resolved);
  }
}
