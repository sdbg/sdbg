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

import com.github.sdbg.debug.core.model.ISDBGVariable;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementLabelProvider;

/**
 * An adaptor factory to map from SDBG debug elements to presentation label providers.
 */
@SuppressWarnings("restriction")
public class SDBGElementAdapterFactory implements IAdapterFactory {
  private IAdapterFactory defaultAdapter = new org.eclipse.debug.internal.ui.views.launch.DebugElementAdapterFactory();

  private static IElementLabelProvider VARIABLE_LABEL_PROVIDER = new SDBGVariableLabelProvider();
  private static IElementLabelProvider EXPRESSION_LABEL_PROVIDER = new SDBGExpressionLabelProvider();

  public static void init() {
    SDBGElementAdapterFactory factory = new SDBGElementAdapterFactory();

    IAdapterManager manager = Platform.getAdapterManager();
    manager.registerAdapters(factory, ISDBGVariable.class);
    manager.registerAdapters(factory, IExpression.class);
  }

  public SDBGElementAdapterFactory() {
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Object getAdapter(Object adaptableObject, Class adapterType) {
    if (adapterType.equals(IElementLabelProvider.class)) {
      if (adaptableObject instanceof ISDBGVariable) {
        return VARIABLE_LABEL_PROVIDER;
      } else if (adaptableObject instanceof IExpression) {
        return EXPRESSION_LABEL_PROVIDER;
      }
    }

    // If we don't return the default debug adapter we won't be able to expand any variables.
    return defaultAdapter.getAdapter(adaptableObject, adapterType);
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Class[] getAdapterList() {
    return new Class[] {IElementLabelProvider.class};
  }
}
