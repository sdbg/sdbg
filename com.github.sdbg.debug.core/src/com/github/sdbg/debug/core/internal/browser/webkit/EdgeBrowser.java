package com.github.sdbg.debug.core.internal.browser.webkit;

import com.github.sdbg.debug.core.internal.browser.AbstractBrowser;
import com.github.sdbg.debug.core.internal.browser.IBrowser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EdgeBrowser extends WebkitBasedBrowser
{
    public EdgeBrowser(File executable)
    {
        super(executable, "edge");
    }

    public static IBrowser findBrowserByProperty()
    {
        File file = AbstractBrowser.findBrowserByProperty(getExecutableCandidates());
        if(file != null)
        {
            return new EdgeBrowser(file);
        }
        return null;
    }
    
    private static List<String> getExecutableCandidates()
    {
        List<String> exes = new ArrayList<>();
        exes.add("msedge");
        return exes;
    }

    protected static List<String> getExecutablePathCandidates()
    {
        List<String> paths = AbstractBrowser.getExecutablePathCandidates();
        //Default Paths
        paths.add("C:/Program Files (x86)/Microsoft/Edge/Application/");
        paths.add("C:/Program Files/Microsoft/Edge/Application");
        return paths;
    }

    public static IBrowser findBrowser()
    {
        File file = AbstractBrowser.findBrowser(getExecutablePathCandidates(), getExecutableCandidates());
        if(file != null)
        {
            return new EdgeBrowser(file);
        }
        return null;
    }
}
