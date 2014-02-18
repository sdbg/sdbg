package com.github.sdbg.debug.core.internal.jdt;

import com.github.sdbg.debug.core.ISourceLookupExtensions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputer;

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
  public ISourcePathComputer getSourcePathComputer(IProject project) throws CoreException {
    if (isJavaProject(project)) {
      return new JDTSourcePathComputer();
    } else {
      return null;
    }
  }

  private boolean isJavaProject(IProject project) throws CoreException {
    return project != null && project.hasNature("org.eclipse.jdt.core.javanature");
  }
}
