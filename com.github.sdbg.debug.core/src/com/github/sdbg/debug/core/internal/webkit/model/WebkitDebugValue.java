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

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.internal.expr.IExpressionEvaluator;
import com.github.sdbg.debug.core.internal.expr.WatchExpressionResult;
import com.github.sdbg.debug.core.internal.util.DebuggerUtils;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitCallback;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitPropertyDescriptor;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitRemoteObject;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitResult;
import com.github.sdbg.debug.core.model.ISDBGValue;

import java.io.IOException;
import java.util.Collections;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.core.model.IWatchExpressionListener;
import org.json.JSONObject;

/**
 * The IValue implementation of Webkit Debug element.
 */
public class WebkitDebugValue extends WebkitDebugElement implements IValue, ISDBGValue,
    IExpressionEvaluator {

  static WebkitDebugValue create(WebkitDebugTarget target, WebkitDebugVariable variable,
      WebkitRemoteObject value) {
    if (value.isList()) {
      return new WebkitDebugIndexedValue(target, variable, value);
    } else {
      return new WebkitDebugValue(target, variable, value);
    }
  }

  private WebkitDebugVariable variable;
  protected WebkitRemoteObject value;

  private VariableCollector variableCollector;

  protected WebkitDebugValue(WebkitDebugTarget target, WebkitDebugVariable variable,
      WebkitRemoteObject value) {
    super(target);

    this.variable = variable;
    this.value = value;
  }

  @Override
  public void computeDetail(final IValueCallback callback) {
    // If the value is a primitive type, just return the display string.
    if (value.isPrimitive() || (variable != null && variable.isLibraryObject())
        || !SDBGDebugCorePlugin.getPlugin().getInvokeToString()) {
      callback.detailComputed(getDisplayString());

      return;
    }

    if (value.isNull()) {
      callback.detailComputed(getDisplayString());
      return;
    }

    // Otherwise try and call the toString() method of the object.
    try {
      getConnection().getRuntime().callToString(value.getObjectId(), new WebkitCallback<String>() {
        @Override
        public void handleResult(WebkitResult<String> result) {
          if (result.isError()) {
            callback.detailComputed(result.getErrorMessage());
          } else {
            callback.detailComputed(result.getResult());
          }
        }
      });
    } catch (IOException e) {
      SDBGDebugCorePlugin.logError(e);

      callback.detailComputed(null);
    }
  }

  @Override
  public void evaluateExpression(final String expression, final IWatchExpressionListener listener) {
    String exprText = expression;

    if (exprText.equals("this") || exprText.startsWith("this.")) {
      // do nothing

    } else if (exprText.startsWith("[")) {
      exprText = "this" + exprText;
    } else {
      exprText = "this." + exprText;
    }

    try {
      getConnection().getRuntime().callFunctionOn(
          value.getObjectId(),
          "function(){return " + exprText + ";}",
          null,
          false,
          new WebkitCallback<WebkitRemoteObject>() {
            @Override
            public void handleResult(WebkitResult<WebkitRemoteObject> result) {
              if (result.isError()) {
                listener.watchEvaluationFinished(WatchExpressionResult.error(
                    expression,
                    result.getErrorMessage()));
              } else if (result.getResult().isUndefined()) {
                listener.watchEvaluationFinished(WatchExpressionResult.error(
                    expression,
                    "undefined"));
              } else if (result.getResult().isSyntaxError()) {
                evalOnGlobalContext(expression, result, listener);
              } else {
                listener.watchEvaluationFinished(WatchExpressionResult.value(
                    expression,
                    new WebkitDebugValue(getTarget(), null, result.getResult())));
              }
            }
          });
    } catch (IOException e) {
      listener.watchEvaluationFinished(WatchExpressionResult.exception(
          expression,
          new DebugException(new Status(
              IStatus.ERROR,
              SDBGDebugCorePlugin.PLUGIN_ID,
              e.toString(),
              e))));
    }
  }

  /**
   * Return the special '@staticFields' property on this object that represents the static fields.
   * 
   * @return
   * @throws DebugException
   */
  public IValue getClassValue() {
    if (variableCollector == null) {
      populate();
    }

    try {
      for (WebkitPropertyDescriptor property : variableCollector.getWebkitProperties()) {
        if (WebkitPropertyDescriptor.STATIC_FIELDS_OBJECT.equals(property.getName())) {
          return new WebkitDebugValue(getTarget(), null, property.getValue());
        }
      }
    } catch (InterruptedException e) {

    }

    return null;
  }

  /**
   * @return a user-consumable string for the value object
   * @throws DebugException
   */
  @Override
  public String getDisplayString() {
    if (variable != null && variable.isLibraryObject()) {
      return "";
    }

    if (value.isNull()) {
      return "null";
    }

    if (value.isString()) {
      return DebuggerUtils.printString(value.getValue());
    }

    if (isPrimitive()) {
      return value.getValue();
    }

    if (isListValue()) {
      if (value.getClassName() != null) {
        return value.getClassName() + "[" + value.getListLength() + "]";
      } else {
        return "List[" + value.getListLength() + "]";
      }
    }

    return value.getDescription();
  }

  @Override
  public String getId() {
    if (value == null || value.isNull()) {
      return null;
    }

    if (!value.isPrimitive() || value.isString()) {
      return parseObjectId(value.getObjectId());
    } else {
      return null;
    }
  }

  public IValue getLibraryValue() {
    if (variableCollector == null) {
      populate();
    }

    try {
      for (WebkitPropertyDescriptor property : variableCollector.getWebkitProperties()) {
        if (WebkitPropertyDescriptor.LIBRARY_OBJECT.equals(property.getName())) {
          return new WebkitDebugValue(getTarget(), null, property.getValue());
        }
      }
    } catch (InterruptedException e) {

    }

    return null;
  }

  @Override
  public String getReferenceTypeName() {
    return value.getClassName();
  }

  @Override
  public String getValueString() throws DebugException {
    try {
      return getDisplayString();
    } catch (Throwable t) {
      throw createDebugException(t);
    }
  }

  @Override
  public IVariable[] getVariables() throws DebugException {
    try {
      if (variableCollector == null) {
        populate();
      }

      return variableCollector.getVariables();
    } catch (Throwable t) {
      throw createDebugException(t);
    }
  }

  @Override
  public boolean hasVariables() throws DebugException {
    try {
      if (isListValue()) {
        return value.getListLength() > 0;
      } else {
        return value.hasObjectId();
      }
    } catch (Throwable t) {
      throw createDebugException(t);
    }
  }

  @Override
  public boolean isAllocated() throws DebugException {
    return true;
  }

  @Override
  public boolean isListValue() {
    return false;
  }

  @Override
  public boolean isNull() {
    return value.isNull();
  }

  @Override
  public boolean isPrimitive() {
    return value.isPrimitive();
  }

  @Override
  public void reset() {
    variableCollector = null;

    fireEvent(new DebugEvent(this, DebugEvent.CHANGE, DebugEvent.CONTENT));
  }

  private void evalOnGlobalContext(final String expression,
      final WebkitResult<WebkitRemoteObject> originalResult, final IWatchExpressionListener listener) {
    try {
      getConnection().getRuntime().evaluate(
          expression,
          null,
          false,
          new WebkitCallback<WebkitRemoteObject>() {
            @Override
            public void handleResult(WebkitResult<WebkitRemoteObject> result) {
              if (result.isError()) {
                listener.watchEvaluationFinished(WatchExpressionResult.error(
                    expression,
                    originalResult.getErrorMessage()));
              } else if (result.getResult().isSyntaxError()) {
                listener.watchEvaluationFinished(WatchExpressionResult.value(
                    expression,
                    new WebkitDebugValue(getTarget(), null, originalResult.getResult())));
              } else {
                listener.watchEvaluationFinished(WatchExpressionResult.value(
                    expression,
                    new WebkitDebugValue(getTarget(), null, result.getResult())));
              }
            }
          });
    } catch (IOException e) {
      listener.watchEvaluationFinished(WatchExpressionResult.exception(
          expression,
          new DebugException(new Status(
              IStatus.ERROR,
              SDBGDebugCorePlugin.PLUGIN_ID,
              e.toString(),
              e))));
    }
  }

  private String parseObjectId(String objectId) {
    if (objectId == null) {
      return null;
    }

    try {
      JSONObject obj = new JSONObject(objectId);
      return obj.getString("id");
    } catch (Throwable t) {
      return null;
    }
  }

  private void populate() {
    if (value.hasObjectId()) {
      variableCollector = VariableCollector.createCollector(
          getTarget(),
          variable,
          Collections.singletonList(value));
    } else {
      variableCollector = VariableCollector.empty();
    }
  }

}
