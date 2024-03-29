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

import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitConnection;
import com.github.sdbg.debug.core.model.BrowserDebugElement;

import org.eclipse.debug.core.model.IDebugTarget;

/**
 * The abstract super class of the Browser debug elements. This provides common functionality like
 * access to the debug target and ILaunch object, as well as making sure that all browser debug
 * elements return the return SDBGDebugCorePlugin.DEBUG_MODEL_ID debug model identifier.
 */
public abstract class WebkitDebugElement extends BrowserDebugElement<WebkitDebugTarget> {

  /**
   * Create a new Webkit debug element.
   * 
   * @param target
   */
  public WebkitDebugElement(IDebugTarget target) {
    super(target);
  }

  protected WebkitConnection getConnection() {
    return getTarget().getWebkitConnection();
  }
}
