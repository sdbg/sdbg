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

import com.github.sdbg.debug.core.model.ISDBGVariable;

/**
 * A logical debug variable - used to display implementation formats in more user friendly
 * structures.
 */
public class DecoratingSDBGVariable extends DecoratingVariable implements ISDBGVariable {
  private ISDBGVariable proxyVariable;

  public DecoratingSDBGVariable(ISDBGVariable proxyVariable) {
    super(proxyVariable);
    this.proxyVariable = proxyVariable;
  }

  @Override
  public boolean isLibraryObject() {
    return proxyVariable.isLibraryObject();
  }

  @Override
  public boolean isLocal() {
    return proxyVariable.isLocal();
  }

  @Override
  public boolean isScope() {
    return proxyVariable.isScope();
  }

  @Override
  public boolean isStatic() {
    return proxyVariable.isStatic();
  }

  @Override
  public boolean isThisObject() {
    return proxyVariable.isThisObject();
  }

  @Override
  public boolean isThrownException() {
    return proxyVariable.isThrownException();
  }
}
