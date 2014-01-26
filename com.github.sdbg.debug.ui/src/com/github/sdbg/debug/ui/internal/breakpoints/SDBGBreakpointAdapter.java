/*
 * Copyright (c) 2012, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.sdbg.debug.ui.internal.breakpoints;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.breakpoints.SDBGBreakpoint;

/**
 * Adapter class in charge of toggling Dart breakpoints.
 * 
 * @see IToggleBreakpointsTarget
 */
public class SDBGBreakpointAdapter implements IToggleBreakpointsTarget {

  /**
   * Create a new DartBreakpointAdapter.
   */
  public SDBGBreakpointAdapter() {

  }

  @Override
  public boolean canToggleLineBreakpoints(IWorkbenchPart part, ISelection selection) {
    return getEditor(part) != null;
  }

  @Override
  public boolean canToggleMethodBreakpoints(IWorkbenchPart part, ISelection selection) {
    return false;
  }

  @Override
  public boolean canToggleWatchpoints(IWorkbenchPart part, ISelection selection) {
    return false;
  }

  protected AbstractTextEditor getEditor(IWorkbenchPart part) {
    if (part instanceof AbstractTextEditor) {
      return (AbstractTextEditor) part;
    } else {
      return null;
    }
  }

  @Override
  public void toggleLineBreakpoints(IWorkbenchPart part, ISelection selection) throws CoreException {
    AbstractTextEditor editor = getEditor(part);

    if (editor != null) {
      IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);

      ITextSelection textSelection = (ITextSelection) selection;

      int lineNumber = textSelection.getStartLine() + 1;

      IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(
          SDBGDebugCorePlugin.DEBUG_MODEL_ID);

      for (int i = 0; i < breakpoints.length; i++) {
        IBreakpoint breakpoint = breakpoints[i];

        if (resource.equals(breakpoint.getMarker().getResource())) {
          if (((ILineBreakpoint) breakpoint).getLineNumber() == lineNumber) {
            breakpoint.delete();
            return;
          }
        }
      }

      SDBGBreakpoint breakpoint = new SDBGBreakpoint(resource, lineNumber);

      DebugPlugin.getDefault().getBreakpointManager().addBreakpoint(breakpoint);
    }
  }

  @Override
  public void toggleMethodBreakpoints(IWorkbenchPart part, ISelection selection)
      throws CoreException {

  }

  @Override
  public void toggleWatchpoints(IWorkbenchPart part, ISelection selection) throws CoreException {

  }

}
