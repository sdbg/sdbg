package com.github.sdbg.debug.core.internal.forwarder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class ShellFormatter extends Formatter {
  private String lineSeparator = System.getProperty("line.separator");

  private String prefix;

  private Date date = new Date();
  private Object args[] = new Object[1];
  private MessageFormat formatter;

  public ShellFormatter() {
    this(null);
  }

  public ShellFormatter(String prefix) {
    this.prefix = prefix;
  }

  @Override
  public synchronized String format(LogRecord record) {
    StringBuffer sb = new StringBuffer();
    if (prefix != null) {
      sb.append(prefix);
    }
    // Minimize memory allocations here.
    date.setTime(record.getMillis());
    args[0] = date;
    if (formatter == null) {
      formatter = new MessageFormat("{0,time,short}");
    }
    formatter.format(args, sb, null);
    sb.append(": ");
    String message = formatMessage(record);
    sb.append(message);
    sb.append(lineSeparator);
    if (record.getThrown() != null) {
      try {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        record.getThrown().printStackTrace(pw);
        pw.close();
        sb.append(sw.toString());
      } catch (Exception ex) {
      }
    }
    return sb.toString();
  }
}
