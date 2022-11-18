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
package com.github.sdbg.debug.core.internal;

/**
 * An Object describing a browser script.
 */
public class ScriptDescriptor {

  private String sourceMapURL;
  private boolean isContentScript;
  private int endLine;
  private int endColumn;
  private int startColumn;
  private int startLine;
  private String url;
  private String scriptId;
  private String scriptSource;
  private Object privateData;
  
  public ScriptDescriptor(String scriptId, String url, String sourceMapURL, boolean isContentScript, int startLine, int startColumn, int endLine, int endColumn)
  {
    super();
    this.scriptId = scriptId;
    this.url = url;
    this.sourceMapURL = sourceMapURL;
    this.isContentScript = isContentScript;
    this.startLine = startLine;
    this.startColumn = startColumn;
    this.endLine = endLine;
    this.endColumn = endColumn;
    patchupScriptUrl();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }

    if (obj instanceof ScriptDescriptor) {
      ScriptDescriptor other = (ScriptDescriptor) obj;

      return scriptId.equals(other.getScriptId());
    }

    return false;
  }

  public int getEndColumn() {
    return endColumn;
  }

  public int getEndLine() {
    return endLine;
  }

  public Object getPrivateData() {
    return privateData;
  }

  public String getScriptId() {
    return scriptId;
  }

  public String getScriptSource() {
    return scriptSource;
  }

  public String getSourceMapURL() {
    return sourceMapURL;
  }

  public int getStartColumn() {
    return startColumn;
  }

  public int getStartLine() {
    return startLine;
  }

  public String getUrl() {
    return url;
  }

  @Override
  public int hashCode() {
    return scriptId.hashCode();
  }

  public boolean hasScriptSource() {
    return scriptSource != null;
  }

  public boolean isChromeExtensionUrl() {
    // chrome-extension://kohcodfehgoaolndkcophkcmhjenpfmc/demo.dart
    return url.startsWith("chrome-extension://");
  }

  /**
   * Determines whether this script is a user extension script.
   */
  public boolean isContentScript() {
    return isContentScript;
  }

  public boolean isDataUrl() {
    // For things like data:application/dart;base64,CiAgICAgICAgaW1wb...3J0ICd==
    return url.startsWith("data:");
  }

  public boolean isSystemScript() {
    if (url == null) {
      return false;
    }

    if (url.startsWith("dart:")) {
      return true;
    }

    // TODO(devoncarew): this hack has to do with how Dartium is built, and will go away at some point
    String normalizedUrl = url.replace('\\', '/');
    if (normalizedUrl.contains("build/slave/dartium")) {
      return true;
    }

    return false;
  }

  public void setPrivateData(Object object) {
    this.privateData = object;
  }

  public void setScriptSource(String source) {
    this.scriptSource = source;
  }

  @Override
  public String toString() {
    if (isContentScript()) {
      return "[" + url + "," + scriptId + ",isContentScript=" + isContentScript() + "]";
    } else {
      return "[" + url + "," + scriptId + "]";
    }
  }

  private void patchupScriptUrl() {
    // TODO(devoncarew): this method converts unqualified library urls to slightly better formed urls.
    if (url != null) {
      // bootstrap_impl ==> dart:bootstrap_impl
      if (url.equals("bootstrap_impl")) {
        url = "dart:" + url;
      }
    }

    // Strip off any url get parameters.
    if (url != null && url.indexOf('?') != -1) {
      url = url.substring(0, url.indexOf('?'));
    }
  }

}
