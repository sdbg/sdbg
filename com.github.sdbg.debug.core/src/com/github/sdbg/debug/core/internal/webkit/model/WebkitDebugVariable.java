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
package com.github.sdbg.debug.core.internal.webkit.model;

import com.github.sdbg.debug.core.internal.util.DebuggerUtils;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitPropertyDescriptor;
import com.github.sdbg.debug.core.model.ISDBGVariable;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;

/**
 * The IVariable implementation of the Webkit Debug Element.
 */
public class WebkitDebugVariable extends WebkitDebugElement implements ISDBGVariable {
  private WebkitPropertyDescriptor descriptor;

  private WebkitDebugVariable parent;
  private WebkitDebugValue value;

  private boolean isSpecialObject;
  private boolean isLocal;
  private boolean isStatic;

  /**
   * Create a new Webkit Debug Variable
   * 
   * @param target
   * @param descriptor
   */
  public WebkitDebugVariable(WebkitDebugTarget target, WebkitPropertyDescriptor descriptor) {
    this(target, descriptor, false);
  }

  /**
   * Create a new Webkit Debug Variable
   * 
   * @param target
   * @param descriptor
   * @param isThisObject
   */
  public WebkitDebugVariable(WebkitDebugTarget target, WebkitPropertyDescriptor descriptor,
      boolean isSpecialObject) {
    super(target);

    this.descriptor = descriptor;
    this.isSpecialObject = isSpecialObject;
  }

//&&&  
//  public DartElement coerceToDartElement() {
//    // TODO(devoncarew): implement this
//
//    return null;
//  }
//

  @Override
  public String getName() throws DebugException {
    try {
      if (isSpecialObject) {
        return descriptor.getName();
      } else {
        return DebuggerUtils.demangleVariableName(descriptor.getName());
      }
    } catch (Throwable t) {
      throw createDebugException(t);
    }
  }

  @Override
  public String getReferenceTypeName() throws DebugException {
    // TODO: How come?
    try {
      return getValue().getReferenceTypeName();
    } catch (Throwable t) {
      throw createDebugException(t);
    }
  }

  @Override
  public IValue getValue() throws DebugException {
    try {
      if (value == null) {
        value = WebkitDebugValue.create(getTarget(), this, descriptor.getValue());
      }

      return value;
    } catch (Throwable t) {
      throw createDebugException(t);
    }
  }

  @Override
  public boolean hasValueChanged() throws DebugException {
    // TODO(keertip): check to see if value has changed
    return false;
  }

  @Override
  public boolean isLibraryObject() {
    return false;
  }

  public boolean isListValue() {
    try {
      return ((WebkitDebugValue) getValue()).isListValue();
    } catch (DebugException e) {
      return false;
    }
  }

  @Override
  public boolean isLocal() {
    return isLocal;
  }

  public boolean isPrimitiveValue() {
    try {
      return ((WebkitDebugValue) getValue()).isPrimitive();
    } catch (DebugException e) {
      return false;
    }
  }

  @Override
  public boolean isScope() {
    return isSpecialObject && !isThisObject();
  }

  @Override
  public boolean isStatic() {
    return isStatic;
  }

  @Override
  public boolean isThisObject() {
    return isSpecialObject && descriptor.getName().equals("this");
  }

  @Override
  public boolean isThrownException() {
    return isSpecialObject && descriptor.getName().equals("exception");
  }

  @Override
  public void setValue(IValue value) throws DebugException {
    setValue(value.getValueString());
  }

  @Override
  public void setValue(String expression) throws DebugException {
    // TODO(devoncarew):
    //Trace.trace("Change: " + expression);
  }

  @Override
  public boolean supportsValueModification() {
    return descriptor.isWritable() && descriptor.getValue().isPrimitive();
  }

  @Override
  public String toString() {
    return descriptor.toString();
  }

  @Override
  public boolean verifyValue(IValue value) throws DebugException {
    return verifyValue(value.getValueString());
  }

  @Override
  public boolean verifyValue(String expression) throws DebugException {
    // TODO(devoncarew): do verification for numbers

    return true;
  }

  protected boolean isClassDescriptor() {
    return descriptor.isClassDescriptor();
  }

  protected void setIsLocal(boolean value) {
    isLocal = value;
  }

  protected void setIsStatic(boolean value) {
    isStatic = value;
  }

  protected void setParent(WebkitDebugVariable parent) {
    this.parent = parent;
  }
//
//  private boolean isListMember() { // TODO XXX FIXME: How...
//    if (parent != null && parent.isListValue()) {
//      return true;
//    } else {
//      return false;
//    }
//  }
}
