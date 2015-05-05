/*
 * Copyright (c) 2011, the Dart project authors.
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

package com.github.sdbg.debug.ui.internal.presentation;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.model.elements.VariableLabelProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.jface.viewers.TreePath;

/**
 * Necessary or else the value type appears with its raw name in the Variables view when it is set
 * to show columns.
 */
@SuppressWarnings("restriction")
public class SDBGVariableLabelProvider extends VariableLabelProvider {
  private static SDBGDebugModelPresentation PRESENTATION = new SDBGDebugModelPresentation();

  @Override
  protected String getLabel(TreePath elementPath, IPresentationContext context, String columnId)
      throws CoreException {
    if (columnId == null) {
      return PRESENTATION.getText(elementPath.getLastSegment(), context);
    } else {
      return super.getLabel(elementPath, context, columnId);
    }
  }

  @Override
  protected String getValueText(IVariable variable, IValue value, IPresentationContext context)
      throws CoreException {
    // NOTE: Due to the loose coupling between ExpressionLabelProvider and ExpressionContentProvider, 
    // the value passed here is the raw one - not the logical one - even when there is a Logical Structure Type in-place!
    //
    // To avoid displaying the un-encoded JavaScript type for java classes, we use the logical structure types' cache from the content providers
    return escapeSpecialChars(PRESENTATION.getValueText(value, context));
  }

  @Override
  protected String getVariableName(IVariable variable, IPresentationContext context)
      throws CoreException {
    return PRESENTATION.getVariableName(variable, context);
  }
}
