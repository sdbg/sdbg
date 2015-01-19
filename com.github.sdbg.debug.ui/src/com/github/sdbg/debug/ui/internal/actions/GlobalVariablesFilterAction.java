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
package com.github.sdbg.debug.ui.internal.actions;

import com.github.sdbg.debug.core.model.ISDBGVariable;

import org.eclipse.debug.core.DebugException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Event;

/**
 * Viewer filter action for global variables
 */
public class GlobalVariablesFilterAction extends ViewFilterAction {
  public GlobalVariablesFilterAction() {
  }

  @Override
  public void runWithEvent(IAction action, Event event) {
    if (action.isChecked()) {
      if (!MessageDialog.openConfirm(
          view.getViewSite().getShell(),
          "Warning",
          "Vizualizing global variables can significantly slow down the debugger.\nWould you like to still shown them?")) {
        return;
      }
    }

    super.runWithEvent(action, event);
  }

  @Override
  public boolean select(Viewer viewer, Object parentElement, Object element) {
    if (!getValue() && element instanceof ISDBGVariable) {
      ISDBGVariable variable = (ISDBGVariable) element;
      try {
        return !variable.isScope() || !"global".equals(variable.getName());
      } catch (DebugException e) {
        throw new RuntimeException(e);
      }
    }

    return true;
  }
}
