package com.github.sdbg.debug.core.util;

public interface IBrowserTabInfo
{
    String getTitle();

    String getUrl();

    Object getWebSocketDebuggerUrl();

    String getHost();

    int getPort();

    String getWebSocketDebuggerFile();
}
