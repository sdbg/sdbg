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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.sdbg.debug.core.internal.util.HttpUrlConnector;
import com.github.sdbg.utilities.Streams;

//GET /json 1.0
//
//HTTP/1.1 200 OK
//Content-Type:application/json; charset=UTF-8
//Content-Length:871
//
//[ {
// "devtoolsFrontendUrl": "/devtools/devtools.html?host=&page=3",
// "faviconUrl": "http://www.apple.com/favicon.ico",
// "thumbnailUrl": "/thumb/http://www.apple.com/",
// "title": "Apple",
// "url": "http://www.apple.com/",
// "webSocketDebuggerUrl": "ws:///devtools/page/3"
//}, {
// "devtoolsFrontendUrl": "/devtools/devtools.html?host=&page=2",
// "faviconUrl": "http://www.yahoo.com/favicon.ico",
// "thumbnailUrl": "/thumb/http://www.yahoo.com/",
// "title": "Yahoo!",
// "url": "http://www.yahoo.com/",
// "webSocketDebuggerUrl": "ws:///devtools/page/2"
//}, {
// "devtoolsFrontendUrl": "/devtools/devtools.html?ws=127.0.0.1:51069/devtools/page/1",
// "faviconUrl": "",
// "id": "1",
// "thumbnailUrl": "/thumb/chrome://newtab/",
// "title": "New Tab",
// "type": "page",
// "url": "chrome://newtab/",
// "webSocketDebuggerUrl": "ws://127.0.0.1:51069/devtools/page/1"
//} ]

/**
 * Retrieve Webkit inspection protocol debugging information from a Chromium instance.
 */
public class ChromiumConnector {

//  /**
//   * Return the correct websocket URL for connecting to the Chromium instance at localhost and the
//   * given port.
//   * 
//   * @param host
//   * @param port
//   * @param tab
//   * @return
//   */
//  public static String getWebSocketURLFor(int port, int tab) {
//    return getWebSocketURLFor(null, port, tab);
//  }
//
//  /**
//   * Return the correct websocket URL for connecting to the Chromium instance at the given host and
//   * port.
//   * 
//   * @param host
//   * @param port
//   * @param tab
//   * @return
//   */
//  public static String getWebSocketURLFor(String host, int port, int tab) {
//    if (host == null) {
//      host = NetUtils.getLoopbackAddress();
//    }
//
//    return "ws://" + host + ":" + port + "/devtools/page/" + tab;
//  }

  private ChromiumConnector() {

  }

}
