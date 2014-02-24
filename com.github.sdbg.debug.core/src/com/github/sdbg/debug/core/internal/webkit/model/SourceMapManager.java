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

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.internal.source.WorkspaceSourceContainer;
import com.github.sdbg.debug.core.internal.sourcemaps.SourceMap;
import com.github.sdbg.debug.core.internal.sourcemaps.SourceMapInfo;
import com.github.sdbg.debug.core.model.IResourceResolver;
import com.github.sdbg.debug.core.util.Trace;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.URIUtil;

// TODO(devoncarew): use the symbol name information in the maps?
// it's possible this will help us de-mangle the method names for frames

/**
 * A class to help manage parsing and querying source maps. It automatically parses source maps and
 * keeps that info up to date. It also helps retrieve information about map sources and map targets.
 * A source map contains information mapping locations in source files to locations in target files.
 * For instance foo.dart.js ==> [foo.dart, bar.dart, baz.dart]. The reverse direction is from
 * targets ==> sources.
 * 
 * @see SourceMap
 */
//&&&!!!
public class SourceMapManager {
  public static class SourceLocation {
    private IStorage storage; // TODO: Besides this, an IPath member is needed, because the storage may not always get resolved
    private String path;
    private int line;
    private int column;
    private String name;

    public SourceLocation(IStorage storage, String path, int line, int column, String name) {
      this.storage = storage;
      this.path = path;
      this.line = line;
      this.column = column;
      this.name = name;
    }

    public int getColumn() {
      return column;
    }

    public int getLine() {
      return line;
    }

    public String getName() {
      return name;
    }

    public String getPath() {
      return path;
    }

    public IStorage getStorage() {
      return storage;
    }

    @Override
    public String toString() {
      return "[" + (storage != null ? storage : "") + "," + path + "," + line + "," + column + ","
          + name + "]";
    }
  }

  private static class URLStorage extends PlatformObject implements IStorage {
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
      return url.toString();
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

  private IResourceResolver resourceResolver;

  private Map<IStorage, IStorage> sourceMapsStorages = new HashMap<IStorage, IStorage>();
  private Map<IStorage, SourceMap> sourceMaps = new HashMap<IStorage, SourceMap>();

  public SourceMapManager(IResourceResolver resourceResolver) {
    this.resourceResolver = resourceResolver;
  }

  public void dispose() {
  }

  /**
   * Given a source (foo.dart.js) file and a location, return the corresponding target location (in
   * foo.dart).
   * 
   * @param storage
   * @param line
   * @param column
   * @return
   */
  public SourceLocation getMappingFor(IStorage storage, int line, int column) {
    if (Trace.TRACING) {
      Trace.trace("Get mappings for " + storage + ":" + line + ":" + column);
    }

    synchronized (sourceMaps) {
      IStorage mapStorage = sourceMapsStorages.get(storage);
      if (mapStorage != null) {
        SourceMap map = sourceMaps.get(mapStorage);
        if (map != null) {
          SourceMapInfo mapping = map.getMappingFor(line, column);

          if (mapping != null) {
            SourceLocation location = new SourceLocation(
                resolveStorage(mapStorage, mapping.getFile()),
                relativisePath(mapStorage, mapping.getFile()),
                mapping.getLine(),
                mapping.getColumn(),
                mapping.getName());

            if (Trace.TRACING) {
              Trace.trace("Found mapping: " + location);
            }

            return location;
          }
        }
      }
    }

    return null;
  }

  /**
   * Given a target location (in foo.dart), return the corresponding source location (in
   * foo.dart.js).
   * 
   * @param storage
   * @param line
   * @return
   */
  public List<SourceLocation> getReverseMappingsFor(String targetPath, int line) {
    if (Trace.TRACING) {
      Trace.trace("Get reverse mappings for " + targetPath + ":" + line);
    }

    List<SourceLocation> mappings = new ArrayList<SourceMapManager.SourceLocation>();

    synchronized (sourceMaps) {
      for (IStorage scriptStorage : sourceMapsStorages.keySet()) {
        IStorage mapStorage = sourceMapsStorages.get(scriptStorage);
        SourceMap map = sourceMaps.get(mapStorage);

        for (String path : map.getSourceNames()) {
          // TODO(devoncarew): the files in the maps should all be pre-resolved
          String relativePath = relativisePath(mapStorage, path);
          if (Trace.TRACING
              && (targetPath.endsWith(relativePath) || relativePath.endsWith(targetPath))) {
            Trace.trace("Potential match: " + relativePath + "(" + mapStorage + ", " + path + ")");
          }

          if (targetPath.equals(relativePath)) {
            List<SourceMapInfo> reverseMappings = map.getReverseMappingsFor(path, line);
            for (SourceMapInfo reverseMapping : reverseMappings) {
              if (reverseMapping != null) {
                IStorage mapSource = scriptStorage; //&&&!!! map.getMapSource();

                if (mapSource != null) {
                  mappings.add(new SourceLocation(
                      mapSource,
                      mapSource.getFullPath().toPortableString(),
                      reverseMapping.getLine(),
                      reverseMapping.getColumn(),
                      reverseMapping.getName()));
                }
              }
            }

            if (Trace.TRACING && !mappings.isEmpty()) {
              Trace.trace("Found reverse mappings: " + mappings);
            }
          }
        }
      }
    }

    return mappings;
  }

  public List<IStorage> getSources(IStorage storage) {
    List<IStorage> sources = new ArrayList<IStorage>();

    synchronized (sourceMaps) {
      IStorage mapStorage = sourceMapsStorages.get(storage);
      if (mapStorage != null) {
        SourceMap map = sourceMaps.get(mapStorage);
        if (map != null) {
          String[] sourceNames = map.getSourceNames();

          if (sourceNames != null) {
            for (String sourceName : sourceNames) {
              IStorage resolvedStorage = resolveStorage(mapStorage, sourceName);

              if (resolvedStorage != null) {
                sources.add(resolvedStorage);
              }
            }
          }
        }
      }
    }

    return sources;
  }

  /**
   * Returns true if the the source map manager contains mapping information for the given file back
   * to original resources.
   * 
   * @param resource
   * @return true if the the source map manager contains mapping information for the given file
   */
  public boolean isMapSource(IStorage storage) { //&&&!!! There can be race conditions because of that method
    if (storage != null) {
      if (Trace.TRACING) {
        Trace.trace("Check for map source: " + storage);
      }

      synchronized (sourceMaps) {
        boolean result = sourceMapsStorages.containsKey(storage);

        if (Trace.TRACING && result) {
          Trace.trace("Confirmed - map source");
        }

        return result;
      }
    }

    return false;
  }

  public boolean isMapTarget(String targetPath) { //&&&!!! There can be race conditions because of that method
    if (targetPath != null) {
      if (Trace.TRACING) {
        Trace.trace("Check for map target: " + targetPath);
      }

      synchronized (sourceMaps) {
        for (IStorage mapStorage : sourceMaps.keySet()) {
          SourceMap map = sourceMaps.get(mapStorage);
          for (String path : map.getSourceNames()) {
            String relativePath = relativisePath(mapStorage, path);
            if (Trace.TRACING
                && (targetPath.endsWith(relativePath) || relativePath.endsWith(targetPath))) {
              Trace.trace("Potential match: " + relativePath + "(" + mapStorage + ", " + path + ")");
            }

            if (targetPath.equals(relativePath)) {
              if (Trace.TRACING) {
                Trace.trace("Confirmed - map target");
              }
              return true;
            }
          }
        }
      }
    }

    return false;
  }

  void handleGlobalObjectCleared() {
    synchronized (sourceMaps) {
      sourceMapsStorages.clear();
      sourceMaps.clear();
    }
  }

  void handleScriptParsed(IStorage script, String sourceMapUrl) {
    synchronized (sourceMaps) {
      IStorage mapStorage = sourceMapsStorages.remove(script);
      if (mapStorage != null) {
        sourceMaps.remove(script);
      }
      Trace.trace("Checking script for sourcemaps: " + script);
      processScript(script, sourceMapUrl);
    }
  }

  private SourceMap parseSourceMap(IStorage mapStorage) {
    if (mapStorage != null) {
      try {
        return SourceMap.createFrom(mapStorage);
      } catch (IOException e) {
        throw new RuntimeException(e);
      } catch (CoreException e) {
        throw new RuntimeException(e);
      }
    }

    return null;
  }

  private void processScript(IStorage script, String sourceMapUrl) {
    try {
      if (sourceMapUrl == null) {
        BufferedReader reader;
        if (script instanceof IFile) {
          reader = new BufferedReader(new InputStreamReader(
              script.getContents(),
              ((IFile) script).getCharset()));
        } else {
          reader = new BufferedReader(new InputStreamReader(script.getContents()));
        }

        try {
          String sourceMapUrlLine = null;
          for (sourceMapUrlLine = reader.readLine(); sourceMapUrlLine != null; sourceMapUrlLine = reader.readLine()) {
            sourceMapUrlLine = sourceMapUrlLine.trim();
            if (sourceMapUrlLine.startsWith("//#") || sourceMapUrlLine.startsWith("//@")) {
              sourceMapUrlLine = sourceMapUrlLine.substring(2).trim();

              if (sourceMapUrlLine.matches("sourceMappingURL\\s*\\=")) {
                break;
              }
            }
          }

          if (sourceMapUrlLine != null) {
            Properties properties = new Properties();
            properties.load(new StringReader(sourceMapUrlLine));
            sourceMapUrl = properties.getProperty("sourceMappingURL");
            Trace.trace("Sourcemap detected in a // comment");
          }
        } finally {
          reader.close();
        }
      }

      IStorage mapStorage;
      if (sourceMapUrl != null) {
        mapStorage = resolveStorage(script, sourceMapUrl);
        Trace.trace("Found sourcemap with URL: " + sourceMapUrl);
      } else {
        mapStorage = null;
      }

      if (mapStorage != null) {
        SourceMap map = parseSourceMap(mapStorage);
        if (map != null) {
          sourceMapsStorages.put(script, mapStorage);
          sourceMaps.put(mapStorage, map);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (CoreException e) {
      throw new RuntimeException(e);
    }
  }

  // TODO: It may turn out that this processing is language specific 
  // and should be assisted by the language-specific integrations
  private String relativisePath(IStorage relativeStorage, String path) {
    URI uri = null;

    try {
      uri = new URI(path);
      if (uri != null && uri.getScheme() != null) {
        // In case the path is a full URI, only keep the path part of it
        path = uri.getPath();
      }
    } catch (URISyntaxException e) {
      // Do nothing
    }

    if (path.startsWith("/")) {
      IPath parentPath = null;
      if (relativeStorage instanceof IFile) {
        parentPath = ((IFile) relativeStorage).getFullPath();
      } else if (relativeStorage instanceof URLStorage) {
        parentPath = ((URLStorage) relativeStorage).getFullPath();
      }

      if (parentPath != null) {
        parentPath = parentPath.removeLastSegments(1);
        String sParentPath = parentPath.toPortableString();
        if (path.startsWith(sParentPath)) {
          path = path.substring(sParentPath.length());
          if (path.startsWith("/")) {
            path = path.substring(1);
          }
        }
      }
    }

    return path;
  }

  private IStorage resolveStorage(IStorage relativeStorage, String path) {
    if (path.startsWith("file:")) {
      try {
        // These incoming uris are not properly uri encoded. If we uri encoded them, we would then
        // not be able to handle properly uri encoded uris. Instead we handle certain illegal chars.
        //String encodedPath = URIUtilities.uriEncode(path);
        String encodedPath = path.replaceAll(" ", "%20");

        URI uri = new URI(encodedPath);

        IResource resource = WorkspaceSourceContainer.locatePathAsResource(uri.getPath());

        if (resource instanceof IFile) {
          return (IFile) resource;
        }
      } catch (URISyntaxException ex) {
        SDBGDebugCorePlugin.logError(ex);
      }

      return null;
    } else if (relativeStorage instanceof IFile) {
      IFile file = ((IFile) relativeStorage).getParent().getFile(new Path(path));

      if (file.exists()) {
        return file;
      } else {
        return null;
      }
    } else {
      try {
        URI uri = URIUtil.fromString(path);
        if (relativeStorage instanceof URLStorage) {
          IPath newPath = Path.fromPortableString(((URLStorage) relativeStorage).getURL().getPath()).removeLastSegments(
              1).append(uri.getPath());
          uri = new URI(
              uri.getScheme(),
              null,
              uri.getHost(),
              uri.getPort(),
              newPath.toPortableString(),
              null,
              null);
        }

        IResource resource = resourceResolver.resolveUrl(uri.toString());

        if (resource instanceof IFile) {
          return (IFile) resource;
        }

        if (uri.getScheme() != null
            && (uri.getScheme().equals("http") || uri.getScheme().equals("https"))
            && uri.getHost() != null) {
          return new URLStorage(new URL(path));
        }

        return null;
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
