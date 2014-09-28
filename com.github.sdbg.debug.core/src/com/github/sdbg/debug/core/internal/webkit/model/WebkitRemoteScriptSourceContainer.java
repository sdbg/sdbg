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
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitScript;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.containers.AbstractSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.LocalFileStorage;

/**
 * An ISourceContainer that searches in active debug connections and returns remote script objects.
 */
public class WebkitRemoteScriptSourceContainer extends AbstractSourceContainer {
  public static final String TYPE_ID = DebugPlugin.getUniqueIdentifier()
      + ".containerType.workspace"; //$NON-NLS-1$

  public WebkitRemoteScriptSourceContainer() {

  }

  @Override
  public Object[] findSourceElements(final String name) throws CoreException {
    if (name == null) {
      return EMPTY;
    }

    // The fetch of large script files can be very slow, so use a job for that
    final Object[] result = new Object[1];
    Job job = new Job("Browser Script Loader") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        SubMonitor subMonitor = SubMonitor.convert(monitor);

        subMonitor.beginTask("Looking up " + name, 2);

        try {
          subMonitor.worked(1);
          result[0] = doFindSourceElements(name);
        } catch (CoreException e) {
          result[0] = e;
        } finally {
          subMonitor.done();
        }

        return Status.OK_STATUS;
      }
    };

    job.setPriority(Job.SHORT);
    job.setUser(true);
    job.schedule();

    try {
      job.join();
    } catch (InterruptedException e) {
      SDBGDebugCorePlugin.wrapError(e);
    }

    if (result[0] instanceof CoreException) {
      throw (CoreException) result[0];
    } else {
      return (Object[]) result[0];
    }
  }

  @Override
  public String getName() {
    return "Remote Scripts";
  }

  @Override
  public ISourceContainerType getType() {
    return getSourceContainerType(TYPE_ID);
  }

  private String convertDataUrl(String url) {
    // convert data:application/dart;base64,CiAgICAgICAgaW1wb...3J0ICd== to
    // script:application/dart

    url = url.substring("data".length());
    url = "script" + url;

    if (url.indexOf(';') != -1) {
      url = url.substring(0, url.indexOf(';'));
    }

    if (url.length() > 40) {
      url = url.substring(0, 40);
    }

    return url;
  }

  private Object[] doFindSourceElements(String name) throws CoreException {
    if (name == null) {
      return EMPTY;
    }

    WebkitDebugTarget target = WebkitDebugTarget.getActiveTarget();

    if (target != null) {
      WebkitScript script = target.getConnection().getDebugger().getScriptByUrl(name);

      if (script != null) {
        if (!script.hasScriptSource()) {
          try {
            target.getConnection().getDebugger().populateScriptSource(script);
          } catch (IOException e) {
            throw new CoreException(new Status(
                IStatus.ERROR,
                SDBGDebugCorePlugin.PLUGIN_ID,
                e.toString(),
                e));
          }
        }

        try {
          return new Object[] {getCreateStorageFor(script)};
        } catch (IOException e) {
          throw new CoreException(new Status(
              IStatus.ERROR,
              SDBGDebugCorePlugin.PLUGIN_ID,
              e.toString(),
              e));
        }
      }
    }

    return EMPTY;
  }

  private LocalFileStorage getCreateStorageFor(WebkitScript script) throws IOException {
    if (script.getPrivateData() == null) {
      String url = script.getUrl();

      if (url.startsWith("data:")) {
        // For things like data:application/dart;base64,CiAgICAgICAgaW1wb...3J0ICd==
        url = convertDataUrl(url);
      } else if (url.indexOf('/') != -1) {
        url = url.substring(url.lastIndexOf('/') + 1);
      } else if (url.indexOf('\\') != -1) {
        url = url.substring(url.lastIndexOf('\\') + 1);
      }

      File file = File.createTempFile("TMP", sanitizeFileName(url));
      file.deleteOnExit();

      Writer out = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF8"));

      try {
        out.write(script.getScriptSource());
      } finally {
        out.close();
      }

      file.setReadOnly();

      LocalFileStorage fileStorage = new LocalFileStorage(file);

      script.setPrivateData(fileStorage);
    }

    return (LocalFileStorage) script.getPrivateData();
  }

  private String sanitizeFileName(String str) {
    String fileName = str.replace(':', '~').replace('/', '_').replace('\\', '_');
    if (fileName.endsWith(".js")) {
      fileName = fileName.substring(0, fileName.length() - ".js".length()) + ".sdbgjs";
    }

    return "-" + fileName;
  }
}
