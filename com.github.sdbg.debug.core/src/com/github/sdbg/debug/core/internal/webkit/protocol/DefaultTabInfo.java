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

package com.github.sdbg.debug.core.internal.webkit.protocol;

import com.github.sdbg.debug.core.util.IBrowserTabInfo;

import java.net.URI;
import java.util.Comparator;

/**
 * A class used to represent meta-information about an open Browser tab, including its WIP debugger
 * URL.
 * 
 * @see ChromiumConnector
 */
public class DefaultTabInfo implements IBrowserTabInfo {

  public static Comparator<DefaultTabInfo> getComparator() {
    return new Comparator<DefaultTabInfo>() {
      @Override
      public int compare(DefaultTabInfo o1, DefaultTabInfo o2) {
        // Sort by the tab order.
        String url1 = o1.getWebSocketDebuggerUrl();
        String url2 = o2.getWebSocketDebuggerUrl();

        if (url1 == url2) {
          return 0;
        } else if (url1 == null) {
          return -1;
        } else if (url2 == null) {
          return 1;
        } else {
          return url1.compareTo(url2);
        }
      }
    };
  }

  private String host;

  private int port;

  private String devtoolsFrontendUrl;

  private String faviconUrl;

  private String thumbnailUrl;

  private String title;

  private String url;

  private String webSocketDebuggerUrl;

  public DefaultTabInfo(String host, int port, String devtoolsFrontendUrl
      , String faviconUrl, String thumbnailUrl, String title, String url, String webSocketDebuggerUrl) {
    this.host = host;
    this.port = port;
    this.devtoolsFrontendUrl = devtoolsFrontendUrl;
    this.faviconUrl = faviconUrl;
    this.thumbnailUrl = thumbnailUrl;
    this.title = title;
    this.url = url;
    this.webSocketDebuggerUrl = webSocketDebuggerUrl;
  }

  public String getDevtoolsFrontendUrl() {
    return devtoolsFrontendUrl;
  }

  public String getFaviconUrl() {
    return faviconUrl;
  }

  @Override
public String getHost() {
    return host;
  }

  @Override
public int getPort() {
    return port;
  }

  public String getThumbnailUrl() {
    return thumbnailUrl;
  }

  @Override
  public String getTitle() {
    return title;
  }

  @Override
  public String getUrl() {
    return url;
  }

  @Override
public String getWebSocketDebuggerFile() {
    if (webSocketDebuggerUrl != null) {
      return URI.create(webSocketDebuggerUrl).getPath();
    } else {
      return webSocketDebuggerUrl;
    }
  }

  @Override
public String getWebSocketDebuggerUrl() {
    // Convert a 'ws:///devtools/page/3' websocket URL to a ws://host:port/devtools/page/3 url.

    if (webSocketDebuggerUrl != null && webSocketDebuggerUrl.startsWith("ws:///")) {
      return "ws://" + host + ":" + port + webSocketDebuggerUrl.substring("ws:///".length());
    } else {
      return webSocketDebuggerUrl;
    }
  }

  public boolean isChromeExtension() {
    return url != null && url.startsWith("chrome-extension://");
  }

  @Override
  public String toString() {
    return "[" + getTitle() + "," + getUrl() + "," + getWebSocketDebuggerUrl() + "]";
  }

}
