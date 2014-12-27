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

package com.github.sdbg.debug.core.internal.webkit.model;

import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitCallback;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitPropertyDescriptor;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitRemoteObject;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IIndexedValue;
import org.eclipse.debug.core.model.IVariable;

/**
 * This subclass of WebkitDebugValue is used specifically for array types. The Eclipse debugging
 * framework will display arrays in groups of 100 elements if it can identify which IValues are
 * arrays.
 */
public class WebkitDebugIndexedValue extends WebkitDebugValue implements IIndexedValue {

  WebkitDebugIndexedValue(WebkitDebugTarget target, WebkitDebugVariable variable,
      WebkitRemoteObject value) {
    super(target, variable, value);
  }

  @Override
  public int getInitialOffset() {
    return 0;
  }

  @Override
  public int getSize() throws DebugException {
    return value.getListLength(getTarget().getConnection());
  }

  @Override
  public IVariable getVariable(int offset) throws DebugException {
    try {
      WebkitRemoteObject result = getIndexAt(value, offset);

      if (result == null) {
        result = WebkitRemoteObject.createNull();
      }

      return new WebkitDebugVariable(getTarget(), WebkitPropertyDescriptor.createIndexProperty(
          offset,
          result));
    } catch (IOException e) {
      throw createDebugException(e);
    }
  }

  @Override
  public IVariable[] getVariables(int offset, int length) throws DebugException {
    IVariable[] results = new IVariable[length];

    for (int i = 0; i < length; i++) {
      results[i] = getVariable(offset + i);
    }

    return results;
  }

  @Override
  public boolean hasVariables() throws DebugException {
    return true;
  }

  @Override
  public boolean isListValue() {
    return true;
  }

  @Override
  protected void populate() {
    try {
      int length = value.getListLength(getTarget().getConnection());

      IVariable[] variables = getVariables(0, length);
      List<IVariable> variablesList = new ArrayList<IVariable>();

      for (int i = 0; i < length; i++) {
        variablesList.add(variables[i]);
      }

      variableCollector = VariableCollector.fixed(getTarget(), variablesList);
    } catch (DebugException e) {
      variableCollector = VariableCollector.empty();
    }
  }

  private WebkitRemoteObject getIndexAt(WebkitRemoteObject listObject, int offset)
      throws IOException {
    final WebkitRemoteObject[] results = new WebkitRemoteObject[1];

    final CountDownLatch latch = new CountDownLatch(1);

    getConnection().getRuntime().callFunctionOn(
        listObject.getObjectId(),
        "function(){return this[" + offset + "];}",
        null,
        false,
        new WebkitCallback<WebkitRemoteObject>() {
          @Override
          public void handleResult(WebkitResult<WebkitRemoteObject> result) {
            results[0] = result.getResult();
            latch.countDown();
          }
        });

    try {
      latch.await(3, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      return null;
    }

    return results[0];
  }

}
