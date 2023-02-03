package com.github.sdbg.debug.core.internal.browser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ChromiumBrowser extends WebkitBasedBrowser
{
    public ChromiumBrowser(File executable)
    {
        super(executable, "chromium");
    }

    public static IBrowser findBrowserByProperty()
    {
        File file = AbstractBrowser.findBrowserByProperty(getExecutableCandidates());
        if(file != null)
        {
            return new ChromiumBrowser(file);
        }
        return null;
    }
    
    private static List<String> getExecutableCandidates()
    {
        List<String> exes = new ArrayList<>();
        exes.add("chromium");
        return exes;
    }

    protected static List<String> getExecutablePathCandidates()
    {
        List<String> exes = AbstractBrowser.getExecutablePathCandidates();
        return exes;
    }

    public static IBrowser findBrowser()
    {
        File file = AbstractBrowser.findBrowser(getExecutablePathCandidates(), getExecutableCandidates());
        if(file != null)
        {
            return new ChromiumBrowser(file);
        }
        return null;
    }
}
