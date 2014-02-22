package com.github.sdbg.integration.jdt.ui;

import org.eclipse.core.resources.IMarker;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.ui.ISourcePresentation;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.debug.core.IJavaBreakpoint;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

public class JDTSourcePresentation implements ISourcePresentation {
  public JDTSourcePresentation() {
  }

  /**
   * @see ISourcePresentation#getEditorId(IEditorInput, Object)
   */
  @Override
  public String getEditorId(IEditorInput input, Object inputObject) {
    try {
      return IDE.getEditorDescriptor(input.getName()).getId();
    } catch (PartInitException e) {
      return null;
    }
  }

  @SuppressWarnings("restriction")
  @Override
  public IEditorInput getEditorInput(Object item) {
    if (item instanceof IMarker) {
      item = getBreakpoint((IMarker) item);
    }
    if (item instanceof IJavaBreakpoint) {
      IType type = org.eclipse.jdt.internal.debug.ui.BreakpointUtils.getType((IJavaBreakpoint) item);
      if (type == null) {
        // if the breakpoint is not associated with a type, use its resource
        item = ((IJavaBreakpoint) item).getMarker().getResource();
      } else {
        item = type;
      }
    }

    // for types that correspond to external files, return null so we do not
    // attempt to open a non-existing workspace file on the breakpoint (bug 184934)
    if (item instanceof IType) {
      IType type = (IType) item;
      if (!type.exists()) {
        return null;
      }
    }
    return org.eclipse.jdt.internal.ui.javaeditor.EditorUtility.getEditorInput(item);
  }

  private IBreakpoint getBreakpoint(IMarker marker) {
    return DebugPlugin.getDefault().getBreakpointManager().getBreakpoint(marker);
  }
}
