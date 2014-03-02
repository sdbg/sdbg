package com.github.sdbg.integration.jdt;

import com.github.sdbg.debug.core.ISourceLookupExtensions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;

public class JDTSourceLookupExtensions implements ISourceLookupExtensions {
  public JDTSourceLookupExtensions() {
  }

  @Override
  public ISourceLookupParticipant[] getSourceLookupParticipants(IProject project)
      throws CoreException {
    if (isJavaProject(project)) {
      return new ISourceLookupParticipant[] {new JDTSourceLookupParticipant()};
    } else {
      return new ISourceLookupParticipant[0];
    }
  }

  @Override
  public ISourcePathComputerDelegate getSourcePathComputerDelegate(IProject project)
      throws CoreException {
    if (isJavaProject(project)) {
      return new JDTSourcePathComputerDelegate();
    } else {
      return null;
    }
  }

  private boolean isJavaProject(IProject project) throws CoreException {
    return project != null && project.hasNature("org.eclipse.jdt.core.javanature");
  }
}
