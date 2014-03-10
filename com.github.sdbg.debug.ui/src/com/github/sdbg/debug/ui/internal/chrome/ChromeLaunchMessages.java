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
package com.github.sdbg.debug.ui.internal.chrome;

import org.eclipse.osgi.util.NLS;

/**
 *
 */
public class ChromeLaunchMessages extends NLS {
  private static final String BUNDLE_NAME = "com.github.sdbg.debug.ui.internal.chrome.messages"; //$NON-NLS-1$
  public static String ChromeMainTab_SelectHtmlFile;
  public static String ChromeMainTab_HtmlFileLabel;
  public static String ChromeMainTab_LaunchTarget;
  public static String ChromeMainTab_Message;
  public static String ChromeMainTab_Name;
  public static String ChromeMainTab_NoHtmlFile;
  public static String ChromeMainTab_NoProject;
  public static String ChromeMainTab_InvalidProject;
  public static String ChromeMainTab_NoUrl;
  public static String ChromeMainTab_InvalidURL;
  public static String ChromeMainTab_ProjectLabel;
  public static String ChromeMainTab_SelectHtml;
  public static String ChromeMainTab_SelectProject;
  public static String ChromeMainTab_SelectProjectTitle;
  public static String ChromeMainTab_SelectProjectMessage;
  public static String ChromeMainTab_UrlLabel;
  public static String ChromeMainTab_UrlRegexpLabel;

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, ChromeLaunchMessages.class);
  }

  private ChromeLaunchMessages() {
  }
}
