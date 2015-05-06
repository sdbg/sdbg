/*
 * Copyright (c) 2014, the Dart project authors.
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

import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitRemoteObject;

/**
 * An 'empty' value. Used as a place-holder when JS returns null values in some places.
 */
public class WebkitEmptyValue extends WebkitDebugValue {

  public WebkitEmptyValue(WebkitDebugTarget target, WebkitDebugVariable variable) {
    super(target, variable, WebkitRemoteObject.createNull());
  }

}
