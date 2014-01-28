package com.github.sdbg.utilities;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class Streams {
  public static byte[] loadAndClose(InputStream in) throws IOException {
    byte[] buf = new byte[4000];
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (int read = in.read(buf); read >= 0; read = in.read(buf)) {
      out.write(buf, 0, read);
    }

    in.close();
    return out.toByteArray();
  }

  public static String loadAndClose(Reader reader) throws IOException {
    StringBuilder sb = new StringBuilder();
    char[] buf = new char[4000];
    for (int read = reader.read(buf); read >= 0; read = reader.read(buf)) {
      sb.append(buf, 0, read);
    }

    reader.close();
    return sb.toString();
  }

  public static List<String> loadLinesAndClose(Reader reader) throws IOException {
    List<String> result = new ArrayList<String>();
    BufferedReader breader = reader instanceof BufferedReader ? (BufferedReader) reader
        : new BufferedReader(reader);
    for (String line = breader.readLine(); line != null; line = breader.readLine()) {
      result.add(line);
    }

    breader.close();
    return result;
  }

  public static void storeAndClose(String str, Writer writer) throws IOException {
    writer.write(str);
    writer.close();
  }

  private Streams() {
  }
}
