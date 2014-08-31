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

package com.github.sdbg.debug.core.internal.util;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.model.IDOMResourceReference;
import com.github.sdbg.debug.core.model.IDOMResourceTracker;
import com.github.sdbg.debug.core.model.IDOMResources;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

public class GenericDOMResourceTracker implements ResourceChangeParticipant, IDOMResourceTracker {
  private IDOMResources domResources;

  public GenericDOMResourceTracker() {
  }

  @Override
  public void dispose() {
    ResourceChangeManager.removeChangeParticipant(this);
    domResources = null;
  }

  @Override
  public void handleFileAdded(IFile file) {
    handleFileChanged(file);
  }

  @Override
  public void handleFileChanged(IFile file) {
    IProject project = file.getProject();
    if (project != null && "SDBG_HOT_CODE_REPLACE".equals(project.getName())) {
      for (IDOMResourceReference rr : domResources.getResources()) {
        try {
          URI uri = new URI(rr.getUrl());
          String path = uri.getPath();

          String host = uri.getHost();
          if (host != null) {
            if (uri.getScheme().equals("http") && uri.getPort() != 80
                || uri.getScheme().equals("https") && uri.getPort() != 443) {
              host += "@" + Integer.toString(uri.getPort());
            }

            path = "/" + host + path;
          }

          if (file.getFullPath().equals(Path.fromPortableString(path))) {
            String fileEncoding = file.getCharset();
            InputStreamReader reader = fileEncoding != null ? new InputStreamReader(
                file.getContents(true),
                fileEncoding) : new InputStreamReader(file.getContents(true));

            try {
              domResources.uploadNewSource(rr, reader);
            } finally {
              reader.close();
            }

            break;
          }
        } catch (URISyntaxException e) {
          SDBGDebugCorePlugin.logError(e);
        } catch (CoreException e) {
          SDBGDebugCorePlugin.logError(e);
        } catch (IOException e) {
          SDBGDebugCorePlugin.logError(e);
        }
      }
    }
  }

  @Override
  public void handleFileRemoved(IFile file) {
  }

  @Override
  public void initialize(IProject project, IDOMResources domResources) {
    this.domResources = domResources;
    ResourceChangeManager.getManager().addChangeParticipant(this);
  }
}
