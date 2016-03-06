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

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.model.ISDBGLogicalStructureTypeExtensions;
import com.github.sdbg.debug.core.model.ISDBGValue;
import com.github.sdbg.debug.core.model.ISDBGVariable;
import com.github.sdbg.debug.core.util.DecoratingSDBGValue;
import com.github.sdbg.debug.core.util.DecoratingSDBGVariable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.ILogicalStructureTypeDelegate;
import org.eclipse.debug.core.model.ILogicalStructureTypeDelegate2;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

/**
 * This ILogicalStructureTypeDelegate handles displaying of GWT SDM types, as described here:
 * https://docs.google.com/document/d/1-of2yVcVVzOeaOh6AUjFfuM3_6WgR8nEwkXM_wBCPXo/edit#heading=
 * h.c69j6hnt9f5l . Additionally, it also handles all variables in the local scope (local function
 * variables). TODO: In future, implement logical structures for maps and collections as in
 * 
 * @skybrian's superdebug GWT module (see issue #6)
 */
public class GWTSDMStructureType implements ILogicalStructureTypeDelegate,
    ILogicalStructureTypeDelegate2, ISDBGLogicalStructureTypeExtensions {
  private static class ExactMatcher implements Matcher {
    private String name;

    public ExactMatcher(String name) {
      this.name = name;
    }

    @Override
    public boolean matches(String name) {
      return this.name.equals(name);
    }
  }

  private class GWTSDMLong extends GWTSDMValue {
    private Long value;

    public GWTSDMLong(ISDBGValue proxyValue, Long value) {
      super(false, proxyValue, new IVariable[0]);
      this.value = value;
    }

    @Override
    public void computeDetail(IValueCallback callback) {
      callback.detailComputed(value.toString());
    }

    @Override
    public String getReferenceTypeName() throws DebugException {
      return "long";
    }

    @Override
    public String getValueString() throws DebugException {
      return value.toString();
    }

    @Override
    public boolean isNumber() {
      return true;
    }

    @Override
    public boolean isObject() {
      return false;
    }

    @Override
    public boolean isPrimitive() {
      return true;
    }
  }

  private class GWTSDMValue extends DecoratingSDBGValue {
    private boolean javaObject;

    public GWTSDMValue(boolean javaObject, ISDBGValue proxyValue, IVariable[] variables) {
      super(proxyValue, variables);
      this.javaObject = javaObject;
    }

    @Override
    public String getId() {
      return null;
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
        // Commented out because these IDs are not so useful - they are not really surviving to the next breakpoint  
        // ... + (getId() != null ? " [id=" + getId() + "]" : "");
        return getReferenceTypeName();
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
      // The hacks below are necessary, because this method is called by the UI *before* the value
      // held in this variable gets converted to its logical representation, if we don't do it, some
      // things which should be hidden may show through
      String rawReferenceTypeName = super.getReferenceTypeName();
      if (!isExcludedFromLogicalStructure(getValue()) && isJavaObject((ISDBGValue) getValue())
          && hasGWTSuffix(rawReferenceTypeName)) {
        return removeGWTSuffix(rawReferenceTypeName);
      } else {
        return rawReferenceTypeName;
      }
    }
  }

  private static interface Matcher {
    boolean matches(String name);
  }

  public GWTSDMStructureType() {
  }

  @Override
  public String getDescription(IValue value) {
    return "GWT SuperDevMode";
  }

  @Override
  public ISDBGValue getLogicalStructure(IValue value) throws DebugException {
    ISDBGValue sValue = (ISDBGValue) value;
    if (sValue instanceof GWTSDMValue || isExcludedFromLogicalStructure(sValue)) {
      return sValue;
    }

    boolean javaObject = isJavaObject(sValue);
    if (javaObject || sValue.isScope()) {
      List<IVariable> translated = new ArrayList<IVariable>();

      if (javaObject) {
        // A real Java object
        // Fetch and display all fields then
        fetchAllJavaFields(value, translated, new HashSet<String>());
      } else {
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

      return new GWTSDMValue(javaObject, sValue, translated.toArray(new IVariable[0]));
    } else {
      Long longValue = getLong(sValue);
      if (longValue != null) {
        return new GWTSDMLong(sValue, longValue);
      } else {
        return sValue;
      }
    }
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
  public boolean isValueDetailStringComputedByLogicalStructure(IValue value) throws DebugException {
    if (value instanceof ISDBGValue && !(value instanceof GWTSDMValue)) {
      ISDBGValue sval = (ISDBGValue) value;
      return !sval.isScope() && getLong(sval) != null;
    }

    return false;
  }

  @Override
  public boolean isValueStringComputedByLogicalStructure(IValue value) throws DebugException {
    if (value instanceof ISDBGValue && !(value instanceof GWTSDMValue)
        && !isExcludedFromLogicalStructure(value)) {
      ISDBGValue sval = (ISDBGValue) value;
      return !sval.isScope() && (isJavaObject(sval) || getLong(sval) != null);
    }

    return false;
  }

  @Override
  public boolean providesLogicalStructure(IValue value) {
    try {
      return value instanceof ISDBGValue && !(value instanceof GWTSDMValue)
          && !isExcludedFromLogicalStructure(value);
    } catch (DebugException e) {
      SDBGDebugCorePlugin.logError(e);
      return false;
    }
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
        if (!name.equals("$H") && !isGWTInit(name) && !isGWTClass(name)
            && !(var.getValue() != null && ((ISDBGValue) var.getValue()).isFunction())) {
          name = removeGWTSuffix(name);
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

  private Long getLong(ISDBGValue value) throws DebugException {
    if (!value.isObject() || value.isScope()) {
      return null;
    } else {
      IVariable[] variables = value.getVariables();
      if (variables.length != 4) {
        return null;
      }

      long l = 0;
      for (IVariable var : variables) {
        String name = var.getName();
        if (!name.equals("__proto__")) {
          if (!name.equals("h") && !name.equals("m") && !name.equals("l")) {
            return null;
          }

          IValue v = var.getValue();
          if (!(value instanceof ISDBGValue)) {
            return null;
          }

          ISDBGValue sv = (ISDBGValue) v;
          Object rawValue = sv.getRawValue();
          if (!(rawValue instanceof Number)) {
            return null;
          }

          long ll = ((Number) rawValue).longValue();
          l |= ll << (name.equals("h") ? 44 : name.equals("m") ? 22 : 0);
        }
      }

      return l;
    }
  }

  private IVariable getOwnProperty(IValue value, Matcher matcher) throws DebugException {
    for (IVariable var : value.getVariables()) {
      if (matcher.matches(var.getName())) {
        return var;
      }
    }

    return null;
  }

  private IVariable getOwnProperty(IValue value, String property) throws DebugException {
    return getOwnProperty(value, new ExactMatcher(property));
  }

  private boolean hasGWTSuffix(String name) {
    return name.endsWith("_g$");
  }

  private boolean hasOwnProperty(IValue value, Matcher matcher) throws DebugException {
    return getOwnProperty(value, matcher) != null;
  }

  private boolean isExcludedFromLogicalStructure(IValue value) throws DebugException {
    if (!(value instanceof ISDBGValue)) {
      return false;
    }

    String excludeFromLogicalStructureStr = SDBGDebugCorePlugin.getPlugin().getExcludeFromLogicalStructure();
    if (excludeFromLogicalStructureStr != null
        && excludeFromLogicalStructureStr.trim().length() > 0) {
      return Pattern.matches(excludeFromLogicalStructureStr, value.getReferenceTypeName());
    } else {
      return false;
    }
  }

  private boolean isGWTClass(String name) {
    return name.equals("___clazz$") || hasGWTSuffix(name) && name.startsWith("___clazz_");
  }

  private boolean isGWTInit(String name) {
    return name.equals("$init") || hasGWTSuffix(name) && name.startsWith("$init_");
  }

  private boolean isJavaClass(ISDBGValue value) throws DebugException {
    return value.isObject() && hasOwnProperty(value, new Matcher() {
      @Override
      public boolean matches(String name) {
        return isGWTClass(name);
      }
    });
  }

  private boolean isJavaObject(ISDBGValue value) throws DebugException {
    if (value.isObject() && !value.isScope()) {
      IVariable proto = getOwnProperty(value, "__proto__");
      return proto instanceof ISDBGVariable && isJavaClass((ISDBGValue) proto.getValue());
    } else {
      return false;
    }
  }

  private String removeGWTSuffix(String name) {
    int pos = name.lastIndexOf('_', name.length() - 3);
    if (pos > -1) {
      return name.substring(0, pos);
    } else {
      return name.substring(0, name.length() - 3);
    }
  }
}
