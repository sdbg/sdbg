package com.github.sdbg.debug.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputer;

public interface ISourceLookupExtensions {
  String EXTENSION_ID = "com.github.sdbg.debug.core.sourceLookupExtensions";

  ISourceLookupParticipant[] getSourceLookupParticipants(IProject project) throws CoreException;

  ISourcePathComputer getSourcePathComputer(IProject project) throws CoreException;
}
