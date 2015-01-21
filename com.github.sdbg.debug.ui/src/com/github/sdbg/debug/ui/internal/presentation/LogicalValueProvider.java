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

package com.github.sdbg.debug.ui.internal.presentation;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.internal.ui.model.elements.VariableContentProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;

/**
 * A hack so that we can use the same logical structure type values' cache as used by the Variables
 * and Expressions views.
 */
@SuppressWarnings("restriction")
public class LogicalValueProvider extends VariableContentProvider {
  public LogicalValueProvider() {
  }

  @Override
  public IValue getLogicalValue(IValue value, IPresentationContext context) throws CoreException {
    return super.getLogicalValue(value, context);
  }
}
