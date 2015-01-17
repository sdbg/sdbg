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

import org.eclipse.debug.core.model.IValue;

/**
 * A sub-class of IValue that adds additional meta-information about the value.
 */
public interface ISDBGValue extends IValue, IExpressionEvaluator {
  //&&&
  public static interface IValueCallback {
    public void detailComputed(String stringValue);
  }

  //&&&
  public void computeDetail(final IValueCallback callback);

  //&&&
  public String getDisplayString();

  /**
   * @return a user-presentable id for this value
   */
  public String getId();

  /**
   * Returns the list length, if isListValue() is true.
   */
  public int getListLength();

  /**
   * @return whether this value represents a list
   */
  public boolean isListValue();

  /**
   * @return whether this value represents a null value
   */
  public boolean isNull();

  /**
   * @return whether this value represents a primitive type
   */
  public boolean isPrimitive();

  /**
   * Clears out any cached information about this value's fields.
   */
  public void reset();

}
