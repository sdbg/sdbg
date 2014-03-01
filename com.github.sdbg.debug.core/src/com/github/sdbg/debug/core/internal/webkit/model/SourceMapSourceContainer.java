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

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.containers.AbstractSourceContainer;

/**
 * An ISourceContainer that searches in active debug connections and returns remote script objects.
 */
public class SourceMapSourceContainer extends AbstractSourceContainer {
  public static final String TYPE_ID = DebugPlugin.getUniqueIdentifier()
      + ".containerType.workspace"; //$NON-NLS-1$

  public SourceMapSourceContainer() {

  }

  @Override
  public Object[] findSourceElements(String name) throws CoreException {
    if (name == null) {
      return EMPTY;
    }

    WebkitDebugTarget target = WebkitDebugTarget.getActiveTarget();

    if (target != null) {
      SourceMapManager manager = target.getSourceMapManager();

      IStorage storage = manager.getSource(name);
      if (storage != null) {
        return new Object[] {storage};
      }
    }

    return EMPTY;
  }

  @Override
  public String getName() {
    return "Remote Sourcemapped Files";
  }

  @Override
  public ISourceContainerType getType() {
    return getSourceContainerType(TYPE_ID);
  }
}
