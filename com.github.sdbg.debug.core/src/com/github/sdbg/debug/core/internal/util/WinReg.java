package com.github.sdbg.debug.core.internal.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Invokes the Windows command line utility "reg" to read the value of a registry key
 */
public class WinReg {
  public static final String readRegistry(String key, String value) throws IOException {
    Process process = Runtime.getRuntime().exec(new String[] {"reg", "query", key, "/v", value});

    StringBuilder outBuf = new StringBuilder();

    InputStream is = process.getInputStream();

    try {
      for (int c = -1; (c = is.read()) != -1;) {
        outBuf.append((char) c);
      }
    } finally {
      is.close();
    }

    try {
      int result = process.waitFor();
      if (result != 0) {
        return null;
      }
    } catch (InterruptedException e) {
      return null;
    }

    String out = outBuf.toString().trim();
    if (!out.startsWith(key)) {
      return null;
    }

    out = out.substring(key.length()).trim();
    if (!out.startsWith(value)) {
      return null;
    }

    out = out.substring(value.length()).trim();
    if (!out.startsWith("REG_SZ")) {
      return null;
    }

    out = out.substring("REG_SZ".length()).trim();
    return out;
  }

  private WinReg() {
  }
}
