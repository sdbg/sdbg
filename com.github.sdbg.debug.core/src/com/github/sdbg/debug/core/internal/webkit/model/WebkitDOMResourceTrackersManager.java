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

import com.github.sdbg.debug.core.internal.util.DOMResourceTrackersManager;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitNode;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitScript;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitStyleSheetRef;
import com.github.sdbg.debug.core.model.IDOMResourceReference;
import com.github.sdbg.debug.core.model.IDOMResources;
import com.github.sdbg.utilities.Streams;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;

class WebkitDOMResourceTrackersManager extends DOMResourceTrackersManager implements IDOMResources {
  private static class WebkitDOMResourceReference implements IDOMResourceReference {
    private Type type;
    private String id;
    private String url;

    public WebkitDOMResourceReference(Type type, String id, String url) {
      this.type = type;
      this.id = id;
      this.url = url;
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public Type getType() {
      return type;
    }

    @Override
    public String getUrl() {
      return url;
    }
  }

  private WebkitDebugTarget target;

  public WebkitDOMResourceTrackersManager(WebkitDebugTarget target) {
    this.target = target;
    initialize(target.getProject(), this);
  }

  @Override
  public void dispose() {
    super.dispose();
  }

  @Override
  public Collection<IDOMResourceReference> getResources() {
    Collection<IDOMResourceReference> references = new ArrayList<IDOMResourceReference>();

    WebkitNode rootNode = target.getRootNode();
    if (rootNode != null) {
      references.add(new WebkitDOMResourceReference(
          WebkitDOMResourceReference.Type.ROOT,
          Integer.toString(rootNode.getNodeId()),
          rootNode.getDocumentURL()));
    }

    for (WebkitScript script : target.getConnection().getDebugger().getAllScripts()) {
      references.add(new WebkitDOMResourceReference(
          WebkitDOMResourceReference.Type.SCRIPT,
          script.getScriptId(),
          script.getUrl()));
    }

    for (WebkitStyleSheetRef styleSheetRef : target.getConnection().getCSS().getStyleSheets()) {
      references.add(new WebkitDOMResourceReference(
          WebkitDOMResourceReference.Type.CSS,
          styleSheetRef.getStyleSheetId(),
          styleSheetRef.getSourceURL()));
    }

    return references;
  }

  @Override
  public void reload() throws IOException {
    target.getConnection().getPage().reload();
  }

  @Override
  public void uploadNewSource(IDOMResourceReference resourceReference, Reader newContent)
      throws IOException {
    switch (resourceReference.getType()) {
      case SCRIPT:
        target.getConnection().getDebugger().setScriptSource(
            resourceReference.getId(),
            Streams.load(newContent));
        break;
      case CSS:
        target.getConnection().getCSS().setStyleSheetText(
            resourceReference.getId(),
            Streams.load(newContent));
        break;
      case ROOT:
        target.getConnection().getDom().setOuterHTML(
            Integer.parseInt(resourceReference.getId()),
            Streams.load(newContent));
        break;
      default:
        throw new UnsupportedOperationException();
    }
  }
}
