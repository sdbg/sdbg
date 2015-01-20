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

package com.github.sdbg.integration.jdt.gwt;

import com.github.sdbg.debug.core.model.ISDBGValue;
import com.github.sdbg.debug.core.model.ISDBGVariable;
import com.github.sdbg.debug.core.util.DecoratingSDBGValue;
import com.github.sdbg.debug.core.util.DecoratingSDBGVariable;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.ILogicalStructureTypeDelegate;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

/**
 * This ILogicalStructureTypeDelegate handles displaying of GWT SDM types. TODO: In future,
 * implement logical structures for maps and collections as in @skybrian's superdebug GWT module
 * (see issue #6)
 */
public class GWTSDMStructureType implements ILogicalStructureTypeDelegate {
  private class GWTSDMValue extends DecoratingSDBGValue {
    private boolean javaObject;

    public GWTSDMValue(boolean javaObject, ISDBGValue proxyValue, IVariable[] variables) {
      super(proxyValue, variables);
      this.javaObject = javaObject;
    }

    @Override
    public String getReferenceTypeName() throws DebugException {
      String rawReferenceTypeName = super.getReferenceTypeName();
      if (javaObject && hasGWTSuffix(rawReferenceTypeName)) {
        return removeGWTSuffix(rawReferenceTypeName);
      } else {
        return rawReferenceTypeName;
      }
    }

    @Override
    public String getValueString() throws DebugException {
      if (javaObject) {
        return getReferenceTypeName() + (getId() != null ? " [id=" + getId() + "]" : "");
      } else {
        return super.getValueString();
      }
    }
  }

  private class GWTSDMVariable extends DecoratingSDBGVariable {
    private String newName;

    private IValue decoratedValue;

    public GWTSDMVariable(String newName, ISDBGVariable proxyVariable) {
      super(proxyVariable);
      this.newName = newName;
    }

    @Override
    public String getName() throws DebugException {
      return newName;
    }

    @Override
    public String getReferenceTypeName() throws DebugException {
      return getValue().getReferenceTypeName();
    }

    @Override
    public IValue getValue() throws DebugException {
      if (decoratedValue == null) {
        IValue rawValue = super.getValue();
        if (providesLogicalStructure(rawValue)) {
          decoratedValue = getLogicalStructure(rawValue);
        } else {
          decoratedValue = rawValue;
        }
      }

      return decoratedValue;
    }
  }

  public GWTSDMStructureType() {
  }

  @Override
  public IValue getLogicalStructure(IValue value) throws DebugException {
    if (value instanceof DecoratingSDBGValue) {
      return value;
    }

    List<IVariable> translated = new ArrayList<IVariable>();
    boolean javaObject;
    if (isJavaObject((ISDBGValue) value)) {
      javaObject = true;
      fetchAllJavaFields(value, translated);
    } else {
      javaObject = false;
      for (IVariable var : value.getVariables()) {
        boolean hasLogicalStructure = providesLogicalStructure(var.getValue());
        boolean hasGWTSuffix = hasGWTSuffix(var.getName());
        if (hasLogicalStructure || hasGWTSuffix) {
          translated.add(new GWTSDMVariable(hasGWTSuffix ? removeGWTSuffix(var.getName())
              : var.getName(), (ISDBGVariable) var));
        } else {
          translated.add(var);
        }
      }
    }

    return new GWTSDMValue(javaObject, (ISDBGValue) value, translated.toArray(new IVariable[0]));
  }

  @Override
  public boolean providesLogicalStructure(IValue value) {
    if (!(value instanceof ISDBGValue) || value instanceof DecoratingSDBGValue) {
      return false;
    } else {
      try {
        return ((ISDBGValue) value).isScope() || isJavaObject((ISDBGValue) value);
      } catch (DebugException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void fetchAllJavaFields(IValue value, List<IVariable> variables) throws DebugException {
    for (IVariable var : value.getVariables()) {
      String name = var.getName();
      if (name.equals("__proto__")) {
        IValue protoValue = var.getValue();
        if (value != null) {
          fetchAllJavaFields(protoValue, variables);
        }
      } else if (hasGWTSuffix(name)) {
        name = removeGWTSuffix(name);
        if (!name.equals("$H") && !name.equals("$init") && !name.equals("___clazz$")
            && !(var.getValue() != null && ((ISDBGValue) var.getValue()).isFunction())) {
          variables.add(new GWTSDMVariable(name, (ISDBGVariable) var));
        }
      }
    }
  }

  private IVariable getOwnProperty(IValue value, String property) throws DebugException {
    for (IVariable var : value.getVariables()) {
      if (var.getName().equals(property)) {
        return var;
      }
    }

    return null;
  }

  private boolean hasGWTSuffix(String name) {
    return name.endsWith("_g$");
  }

  private boolean hasOwnProperty(IValue value, String property) throws DebugException {
    for (IVariable var : value.getVariables()) {
      if (var.getName().equals(property)) {
        return true;
      }
    }

    return false;
  }

  private boolean isJavaClass(ISDBGValue value) throws DebugException {
    return isObject(value) && hasOwnProperty(value, "___clazz$");
  }

  private boolean isJavaObject(ISDBGValue value) throws DebugException {
    if (isObject(value)) {
      IVariable proto = getOwnProperty(value, "__proto__");
      return proto instanceof ISDBGVariable && isJavaClass((ISDBGValue) proto.getValue());
    } else {
      return false;
    }
  }

  private boolean isObject(ISDBGValue value) {
    return !(value.isNull() || value.isPrimitive() || value.isListValue() || value.isFunction());
  }

  private String removeGWTSuffix(String name) {
    return name.substring(0, name.lastIndexOf('_', name.length() - 4));
  }
}
