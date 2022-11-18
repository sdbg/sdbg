package com.github.sdbg.debug.core.internal.webkit.model;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.internal.ScriptDescriptor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;

public class WebkitScriptStorage extends PlatformObject implements IStorage {
  private ScriptDescriptor script;
  private String source;

  public WebkitScriptStorage(ScriptDescriptor script, String source) {
    this.script = script;
    this.source = source;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    WebkitScriptStorage other = (WebkitScriptStorage) obj;
    if (script == null) {
      if (other.script != null) {
        return false;
      }
    } else if (!script.equals(other.script)) {
      return false;
    }
    return true;
  }

  @Override
  public InputStream getContents() throws CoreException {
    try {
      return new ByteArrayInputStream(source != null ? source.getBytes("UTF-8") : new byte[0]);
    } catch (UnsupportedEncodingException e) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          SDBGDebugCorePlugin.PLUGIN_ID,
          e.toString(),
          e));
    }
  }

  @Override
  public IPath getFullPath() {
    try {
      return Path.fromPortableString(URIUtil.fromString(script.getUrl()).getPath());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getName() {
    return getFullPath().lastSegment();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((script == null) ? 0 : script.hashCode());
    return result;
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }

  @Override
  public String toString() {
    return script.toString();
  }
}