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

package com.github.sdbg.debug.core.util;

import com.github.sdbg.debug.core.model.ISDBGValue;

import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.core.model.IWatchExpressionListener;

/**
 * A logical debug value - used to display implementation formats in more user friendly structures.
 */
public class DecoratingSDBGValue extends DecoratingValue implements ISDBGValue {
  private final ISDBGValue proxyValue;

  public DecoratingSDBGValue(ISDBGValue proxyValue, IVariable[] variables) {
    super(proxyValue, variables);
    this.proxyValue = proxyValue;
  }

  @Override
  public void computeDetail(IValueCallback callback) {
    proxyValue.computeDetail(callback);
  }

  @Override
  public void evaluateExpression(String expression, IWatchExpressionListener listener) {
    proxyValue.evaluateExpression(expression, listener);
  }

  @Override
  public String getId() {
    return proxyValue.getId();
  }

  @Override
  public int getListLength() {
    return proxyValue.getListLength();
  }

  @Override
  public boolean isFunction() {
    return proxyValue.isFunction();
  }

  @Override
  public boolean isListValue() {
    return proxyValue.isListValue();
  }

  @Override
  public boolean isNull() {
    return proxyValue.isNull();
  }

  @Override
  public boolean isPrimitive() {
    return proxyValue.isPrimitive();
  }

  @Override
  public boolean isScope() {
    return proxyValue.isScope();
  }

  @Override
  public void reset() {
    proxyValue.reset();
  }
}
