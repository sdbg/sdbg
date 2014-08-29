/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.github.sdbg.debug.ui.internal.objectinspector;

import com.github.sdbg.debug.core.model.ISDBGValue;
import com.github.sdbg.debug.core.model.ISDBGVariable;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.ui.IActionFilter;

/**
 * An IActionFilter implementation used to determine whether inspect actions apply to certain
 * objects, and to manage the enablement state of those actions.
 */
public class InspectorActionFilter implements IActionFilter {
  static class InspectorAdapterFactory implements IAdapterFactory {
    private InspectorActionFilter inspectorFilter = new InspectorActionFilter();

    public InspectorAdapterFactory() {

    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object getAdapter(Object adaptableObject, Class adapterType) {
      return inspectorFilter;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Class[] getAdapterList() {
      return new Class[] {IActionFilter.class};
    }
  }

  public static void registerAdapters() {
    IAdapterManager manager = Platform.getAdapterManager();

    IAdapterFactory factory = new InspectorAdapterFactory();

    manager.registerAdapters(factory, ISDBGVariable.class);
    manager.registerAdapters(factory, InspectorVariable.class);
  }

  public InspectorActionFilter() {

  }

  @Override
  public boolean testAttribute(Object object, String name, String value) {
    if ("isInspectableObject".equals(name)) {
      return Boolean.toString(isInspectableObject(object)).equals(value);
    } else if ("canInspectObject".equals(name)) {
      return Boolean.toString(canInspectObject(object)).equals(value);
    } else if ("canInspectClass".equals(name)) {
      return Boolean.toString(canInspectClass(object)).equals(value);
    } else if ("canInspectLibrary".equals(name)) {
      return Boolean.toString(canInspectLibrary(object)).equals(value);
    } else {
      return false;
    }
  }

  private boolean canInspectClass(Object object) {
    if (!isInspectableObject(object)) {
      return false;
    }

    if (object instanceof IVariable) {
      IVariable variable = (IVariable) object;

      try {
        object = variable.getValue();
      } catch (DebugException e) {
        return false;
      }
    }

    if (isPrimitive(object)) {
      return false;
    }

    return true;
  }

  private boolean canInspectLibrary(Object object) {
    if (!isInspectableObject(object)) {
      return false;
    }

    if (object instanceof IVariable) {
      IVariable variable = (IVariable) object;

      try {
        object = variable.getValue();
      } catch (DebugException e) {
        return false;
      }
    }

    if (isPrimitive(object)) {
      return false;
    }

    return true;
  }

  private boolean canInspectObject(Object object) {
    if (!isInspectableObject(object)) {
      return false;
    }

    if (object instanceof InspectorVariable) {
      return false;
    }

    return true;
  }

  private boolean isInspectableObject(Object object) {
    return object instanceof ISDBGVariable || object instanceof InspectorVariable;
  }

  private boolean isPrimitive(Object object) {
    if (object instanceof ISDBGValue) {
      ISDBGValue val = (ISDBGValue) object;

      if (val.isPrimitive()) {
        return true;
      }
    }

    return false;
  }
}
