package com.github.sdbg.debug.core.internal.browser.webkit;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.internal.browser.AbstractBrowser;
import com.github.sdbg.debug.core.internal.browser.IBrowser;
import com.github.sdbg.debug.core.internal.util.WinReg;
import com.github.sdbg.utilities.OSUtilities;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChromeBrowser extends WebkitBasedBrowser
{
    public ChromeBrowser(File executable)
    {
        super(executable, "chrome");
    }

    public static IBrowser findBrowserByProperty()
    {
        File file = AbstractBrowser.findBrowserByProperty(getExecutableCandidates());
        if(file != null)
        {
            return new ChromeBrowser(file);
        }
        return null;
    }
    
    private static List<String> getExecutableCandidates()
    {
        List<String> exes = new ArrayList<>();
        exes.add("chrome");
        exes.add("google-chrome");
        exes.add("Google Chrome.app/Contents/MacOS/Google Chrome");      
        exes.add("Contents/MacOS/Google Chrome");
        exes.add("Google Chrome");
        return exes;
    }

    protected static List<String> getExecutablePathCandidates()
    {
        List<String> paths = AbstractBrowser.getExecutablePathCandidates();
        if (OSUtilities.isWindows()) 
        {
            // On Windows, try to locate Chrome using the Uninstall windows Registry setting
            addRegPath(paths
                , "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Google Chrome"
                , "InstallLocation");
            // In case Chrome x86 is installed on a x64 machine:
            addRegPath(paths
                , "HKEY_LOCAL_MACHINE\\SOFTWARE\\Wow6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Google Chrome"
                , "InstallLocation");
            // General fallback
            addRegPath(paths
                , "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\chrome.exe"
                , "Path");
        }
        return paths;
    }

    private static void addRegPath(List<String> paths, String regKey, String regValue)
    {
        try
        {
            String path = WinReg.readRegistry(regKey, regValue);
            if(path != null)
            {
                paths.add(path);
            }
        }
        catch (IOException e)
        {
            SDBGDebugCorePlugin.logError(e);
        }
    }
    
    public static IBrowser findBrowser()
    {
        File file = AbstractBrowser.findBrowser(getExecutablePathCandidates(), getExecutableCandidates());
        if(file != null)
        {
            return new ChromeBrowser(file);
        }
        return null;
    }
}
