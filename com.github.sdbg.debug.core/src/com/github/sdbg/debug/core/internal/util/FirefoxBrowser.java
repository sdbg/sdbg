package com.github.sdbg.debug.core.internal.util;

import java.io.File;

public class FirefoxBrowser extends GenericBrowser
{
    public FirefoxBrowser(File executable)
    {
        super(executable, "about:welcome");
    }
    
    @Override
    protected HttpUrlConnector getURLConnector(String host, int port)
    {
        return new HttpUrlConnector(host, port, "/json/list");
    }
}
