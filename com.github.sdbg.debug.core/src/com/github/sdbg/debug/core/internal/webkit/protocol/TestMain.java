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

import com.github.sdbg.debug.core.internal.util.ChromeBasedBrowser;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitConnection.WebkitConnectionListener;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitConsole.CallFrame;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitConsole.ConsoleListener;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitDebugger.DebuggerListener;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitDebugger.PausedReasonType;

import org.json.JSONException;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * ./Chromium.app/Contents/MacOS/Chromium --remote-debugging-port=3030 --homepage=about:blank
 */
class TestMain {

  /**
   * @param args
   * @throws IOException
   * @throws InterruptedException
   * @throws JSONException
   */
  public static void main(String[] args) throws IOException, InterruptedException {
    if (args.length != 1) {
      System.out.println("usage Main <port>");
      return;
    }

    int port = Integer.parseInt(args[0]);

    List<DefaultTabInfo> tabs = new ChromeBasedBrowser(null, null).getAvailableTabs(null, port);

    //ChromiumConnector.getWebSocketURLFor(port, 1);
    URI uri = URI.create(tabs.get(0).getWebSocketDebuggerUrl());
    WebkitConnection connection = new WebkitConnection(uri);

    connection.addConnectionListener(new WebkitConnectionListener() {
      @Override
      public void connectionClosed(WebkitConnection connection) {
        System.out.println("connection closed");
      }
    });

    connection.connect();

    System.out.println("connection opened");

    // add a console listener
    connection.getConsole().addConsoleListener(new ConsoleListener() {
      @Override
      public void messageAdded(String message, String url, int line, List<CallFrame> stackTrace) {
        System.out.println("message added: " + message);
      }

      @Override
      public void messageRepeatCountUpdated(int count) {
        System.out.println("messageRepeatCountUpdated: " + count);
      }

      @Override
      public void messagesCleared() {
        System.out.println("messages cleared");
      }
    });

    // enable console events
    connection.getConsole().enable();

    // add a debugger listener
    connection.getDebugger().addDebuggerListener(new DebuggerListener() {
      @Override
      public void debuggerBreakpointResolved(WebkitBreakpoint breakpoint) {
        System.out.println("debuggerBreakpointResolved: " + breakpoint);
      }

      @Override
      public void debuggerGlobalObjectCleared() {
        System.out.println("debuggerGlobalObjectCleared");
      }

      @Override
      public void debuggerPaused(PausedReasonType reason, List<WebkitCallFrame> frames,
          WebkitRemoteObject exception) {
        System.out.println("debugger paused: " + reason);

        for (WebkitCallFrame frame : frames) {
          System.out.println("  " + frame);
        }
      }

      @Override
      public void debuggerResumed() {
        System.out.println("debugger resumed");
      }

      @Override
      public void debuggerScriptParsed(WebkitScript script) {
        System.out.println("debugger script: " + script);
      }
    });

    // enable debugger events
    connection.getDebugger().enable();

    // navigate to cheese.com
    connection.getPage().navigate("http://www.cheese.com");

    //Thread.sleep(2000);

    //connection.close();
  }

}
