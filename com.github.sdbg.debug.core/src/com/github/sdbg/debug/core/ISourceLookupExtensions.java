package com.github.sdbg.debug.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;

public interface ISourceLookupExtensions {
  String EXTENSION_ID = "com.github.sdbg.debug.core.sourceLookupExtensions";

  ISourceLookupParticipant[] getSourceLookupParticipants(IProject project);

  ISourcePathComputerDelegate getSourcePathComputer(IProject project);
}
