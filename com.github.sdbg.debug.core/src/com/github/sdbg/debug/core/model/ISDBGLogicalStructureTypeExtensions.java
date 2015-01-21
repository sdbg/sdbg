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
package com.github.sdbg.debug.core.model;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

public interface ISDBGLogicalStructureTypeExtensions {
  /**
   * Allows renaming of all variables, including those which are in the function's local scope and
   * are thus not owned by an IValue
   */
  String getVariableName(IVariable variable) throws CoreException;

  /**
   * Allows the value's display string to be computed based on the value returned by the logical
   * structure, rather than based on the raw one
   */
  boolean isValueStringComputedByLogicalStructure(IValue value) throws CoreException;
}
