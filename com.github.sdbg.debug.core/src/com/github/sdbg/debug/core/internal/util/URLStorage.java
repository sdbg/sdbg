package com.github.sdbg.debug.core.internal.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PlatformObject;

public class URLStorage extends PlatformObject implements IStorage {
  private URL url;

  public URLStorage(URL url) {
    this.url = url;
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
    URLStorage other = (URLStorage) obj;
    if (url == null) {
      if (other.url != null) {
        return false;
      }
    } else if (!url.equals(other.url)) {
      return false;
    }
    return true;
  }

  @Override
  public InputStream getContents() throws CoreException {
    try {
      return url.openStream();
    } catch (IOException e) {
      throw new CoreException(null); // FIXME
    }
  }

  @Override
  public IPath getFullPath() {
    return Path.fromPortableString(url.getPath());
  }

  @Override
  public String getName() {
    return getFullPath().lastSegment();
  }

  public URL getURL() {
    return url;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((url == null) ? 0 : url.hashCode());
    return result;
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }

  @Override
  public String toString() {
    return "URLStorage[" + url + "]";
  }
}
