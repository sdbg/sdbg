/*
 * Copyright 2012 Dart project authors.
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

package com.github.sdbg.debug.core.internal.util;

/**
 * This class contains static utility methods for use by the debugger.
 */
public class DebuggerUtils {
  public static String demangleClassName(String name) {
    return demangle(name);
  }

  public static String demangleFunctionName(String name) {
    return demangle(name);
  }

  public static String demangleVariableName(String name) {
    return demangle(name);
  }

  /**
   * @return whether the given debugger symbol name represents a private symbol
   */
  public static boolean isPrivateName(String name) {
    return false;
  }

  public static String printString(String str) {
    if (str == null) {
      return null;
    }

    if (str.indexOf('\n') != -1) {
      str = str.replace("\n", "\\n");
    }

    if (str.indexOf('\r') != -1) {
      str = str.replace("\r", "\\r");
    }

    if (str.indexOf('\t') != -1) {
      str = str.replace("\t", "\\t");
    }

    // Don't re-quote already quoted strings.
    if (str.length() > 1 && str.startsWith("\"") && str.endsWith("\"")) {
      return str;
    } else {
      return "\"" + str + "\"";
    }
  }

  private static String demangle(String name) {
    if (name == null) {
      return null;
    } else {
      // TODO XXX FIXME: GWT SuperDevMode-specific
      return name.replaceAll("_[0-9]+_g\\$$", "");
    }
  }

  private DebuggerUtils() {
  }
}
