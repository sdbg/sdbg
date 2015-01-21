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

import com.github.sdbg.debug.core.model.ISDBGLogicalStructureTypeExtensions;
import com.github.sdbg.debug.core.model.ISDBGValue;
import com.github.sdbg.debug.core.model.ISDBGVariable;
import com.github.sdbg.debug.core.util.DecoratingSDBGValue;
import com.github.sdbg.debug.core.util.DecoratingSDBGVariable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.ILogicalStructureTypeDelegate;
import org.eclipse.debug.core.model.ILogicalStructureTypeDelegate2;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

/**
 * This ILogicalStructureTypeDelegate handles displaying of GWT SDM types. Additionally, it also
 * handles all variables in the local scope (local function variables). TODO: In future, implement
 * logical structures for maps and collections as in @skybrian's superdebug GWT module (see issue
 * #6)
 */
public class GWTSDMStructureType implements ILogicalStructureTypeDelegate,
    ILogicalStructureTypeDelegate2, ISDBGLogicalStructureTypeExtensions {
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
        return getReferenceTypeName(); // Not so useful as these IDs are not really surviving to the next breakpoint:  + (getId() != null ? " [id=" + getId() + "]" : "");
      } else {
        return super.getValueString();
      }
    }
  }

  private class GWTSDMVariable extends DecoratingSDBGVariable {
    private String newName;

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
      String rawReferenceTypeName = super.getReferenceTypeName();
      if (isJavaObject((ISDBGValue) getValue()) && hasGWTSuffix(rawReferenceTypeName)) {
        return removeGWTSuffix(rawReferenceTypeName);
      } else {
        return rawReferenceTypeName;
      }
    }
  }

  public GWTSDMStructureType() {
  }

  @Override
  public String getDescription(IValue value) {
    return "GWT SuperDevMode";
  }

  @Override
  public IValue getLogicalStructure(IValue value) throws DebugException {
    if (value instanceof GWTSDMValue) {
      return value;
    }

    List<IVariable> translated = new ArrayList<IVariable>();
    boolean javaObject = isJavaObject((ISDBGValue) value);
    if (javaObject) {
      // A real Java object
      // Fetch and display all fields then
      fetchAllJavaFields(value, translated, new HashSet<String>());
    } else if (((ISDBGValue) value).isScope()) {
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
    } else {
      return value;
    }

    return new GWTSDMValue(javaObject, (ISDBGValue) value, translated.toArray(new IVariable[0]));
  }

  @Override
  public String getVariableName(IVariable variable) throws DebugException {
    String name = variable.getName();
    if (variable instanceof ISDBGVariable) {
      ISDBGVariable svar = (ISDBGVariable) variable;
      if (svar.isLocal() && hasGWTSuffix(name)) {
        name = removeGWTSuffix(name);
      }
    }

    return name;
  }

  @Override
  public boolean isValueStringComputedByLogicalStructure(IValue value) throws DebugException {
    if (value instanceof ISDBGValue && !(value instanceof GWTSDMValue)) {
      ISDBGValue sval = (ISDBGValue) value;
      return !sval.isScope() && isJavaObject(sval);
    }

    return false;
  }

  @Override
  public boolean providesLogicalStructure(IValue value) {
    return value instanceof ISDBGValue && !(value instanceof GWTSDMValue);
  }

  // Returns all properties which look like Java fields of the supplied value
  //
  // The supplied value is assumed to be a Java object, i.e. isJavaObject(value) should hold true
  // The prototype chain is also traversed for constant properties which are not available on the main object
  private void fetchAllJavaFields(IValue value, List<IVariable> variables,
      Collection<String> visited) throws DebugException {
    for (IVariable var : value.getVariables()) {
      String name = var.getName();
      if (hasGWTSuffix(name)) {
        name = removeGWTSuffix(name);
        if (!name.equals("$H") && !name.equals("$init") && !name.equals("___clazz$")
            && !(var.getValue() != null && ((ISDBGValue) var.getValue()).isFunction())) {
          if (!visited.contains(name)) {
            visited.add(name);
            variables.add(new GWTSDMVariable(name, (ISDBGVariable) var));
          }
        }
      }
    }

    IVariable protoVar = getOwnProperty(value, "__proto__");
    if (protoVar != null) {
      IValue protoValue = protoVar.getValue();
      if (value != null && isJavaClass((ISDBGValue) protoValue)) {
        fetchAllJavaFields(protoValue, variables, visited);
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
