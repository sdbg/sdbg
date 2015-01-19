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
import com.github.sdbg.debug.core.util.LogicalDebugValue;
import com.github.sdbg.debug.core.util.LogicalDebugVariable;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.ILogicalStructureTypeDelegate;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

/**
 * This ILogicalStructureTypeDelegate handles displaying of GWT SDM types. TODO: In future,
 * implement everything @skybrian explained in issue #6
 */
public class GWTSDMStructureType implements ILogicalStructureTypeDelegate {
  public GWTSDMStructureType() {
  }

  @Override
  public IValue getLogicalStructure(IValue value) throws CoreException {
    boolean translate = false;
    for (IVariable variable : value.getVariables()) {
      if (variable instanceof ISDBGVariable) {
        ISDBGVariable var = (ISDBGVariable) variable;
        if (var.getName().endsWith("_g$")) {
          translate = true;
        }
      }
    }

    if (translate) {
      List<IVariable> translated = new ArrayList<IVariable>();
      for (IVariable variable : value.getVariables()) {
        if (variable instanceof ISDBGVariable) {
          ISDBGVariable var = (ISDBGVariable) variable;
          if (var.getName().endsWith("_g$")) {
            // TODO: In future also check that the value is an actual GWT object
            translated.add(new LogicalDebugVariable(
                var.getName().replaceAll("_[0-9]+_g\\$$", ""),
                new LogicalDebugValue(variable.getValue(), variable.getValue().getVariables()) {
                  @Override
                  public String getReferenceTypeName() throws DebugException {
                    String result = super.getReferenceTypeName();
                    if (result != null && result.endsWith("_g$")) {
                      return result.replaceAll("_[0-9]+_g\\$$", "");
                    } else {
                      return result;
                    }
                  }
                }));
          } else {
            translated.add(var);
          }
        }
      }

      return new LogicalDebugValue(value, translated.toArray(new IVariable[0]));
    } else {
      return value;
    }
  }

  @Override
  public boolean providesLogicalStructure(IValue value) {
    if (1 == 1) {
      return false; // TODO
    }

    if (!(value instanceof ISDBGValue)) {
      return false;
    }

    ISDBGValue val = (ISDBGValue) value;
    if (val.isNull() || val.isPrimitive()) {
      return false;
    } else {
      try {
        return value.hasVariables();
      } catch (DebugException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
