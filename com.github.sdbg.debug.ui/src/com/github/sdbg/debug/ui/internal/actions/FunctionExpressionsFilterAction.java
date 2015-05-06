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

import org.eclipse.debug.core.model.IExpression;
import org.eclipse.jface.viewers.Viewer;

/**
 * Viewer filter action for function variables
 */
public class FunctionExpressionsFilterAction extends FunctionVariablesFilterAction {
  public FunctionExpressionsFilterAction() {
  }

  @Override
  public boolean select(Viewer viewer, Object parentElement, Object element) {
    if (element instanceof IExpression) {
      return select(((IExpression) element).getValue());
    } else {
      return super.select(viewer, parentElement, element);
    }
  }
}
