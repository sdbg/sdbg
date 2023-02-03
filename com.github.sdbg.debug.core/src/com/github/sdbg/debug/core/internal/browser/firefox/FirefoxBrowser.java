package com.github.sdbg.debug.core.internal.browser.firefox;

import static com.github.sdbg.debug.core.SDBGDebugCorePlugin.logError;
import static com.github.sdbg.debug.core.SDBGDebugCorePlugin.logInfo;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.SDBGLaunchConfigWrapper;
import com.github.sdbg.debug.core.internal.browser.AbstractBrowser;
import com.github.sdbg.debug.core.internal.browser.BrowserManager;
import com.github.sdbg.debug.core.internal.browser.IBrowser;
import com.github.sdbg.debug.core.internal.util.ListeningStream;
import com.github.sdbg.debug.core.internal.util.LogTimer;
import com.github.sdbg.debug.core.internal.webkit.model.SourceMapManager;
import com.github.sdbg.debug.core.internal.webkit.protocol.DefaultTabInfo;
import com.github.sdbg.debug.core.model.IResourceResolver;
import com.github.sdbg.debug.core.util.IBrowserTabChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.json.JSONException;

import de.exware.remotefox.DebugConnector;
import de.exware.remotefox.TabActor;
import de.exware.remotefox.WatcherActor;
import de.exware.remotefox.WatcherActor.WatchableResource;

public class FirefoxBrowser extends AbstractBrowser
{
    private DebugConnector connector;
    private IResourceResolver resourceResolver;
    private BreakpointManager breakpointManager;
    private TabActor tab;
    private FirefoxDebugTarget target;
    private SourceMapManager sourceMapManager;
    
    public FirefoxBrowser(File executable)
    {
        super(executable, "about:welcome");
    }
    
    @Override
    public void connectToBrowserDebug(String browserName, ILaunch launch, SDBGLaunchConfigWrapper launchConfig,
        String url, IProgressMonitor monitor, LogTimer timer, boolean enableBreakpoints, String host, int port,
        long maxStartupDelay, ListeningStream browserOutput, String processDescription,
        IResourceResolver resourceResolver, IBrowserTabChooser browserTabChooser, boolean remote) throws CoreException
    {
        this.resourceResolver = resourceResolver;
        connector = new DebugConnector(host, port);
        connector.setLogWire(true);
        long start = System.currentTimeMillis();
        boolean success = false;
        while(success == false && start + 2000 > System.currentTimeMillis())
        {  //Try to connect or until timeout.
            sleep(100);
            try
            {
                connector.connect();
                success = true;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        if(success == false)
        {
            logError("Error on Firefox connect");
            throw new CoreException(SDBGDebugCorePlugin.createErrorStatus("Error on connect"));
        }
        logInfo("Debugger connected");
        TabActor tab = null;
        try
        {
            connector.start();
            sleep(500);
            tab = getTab(url);
            WatcherActor watcher = tab.getWatcher();
            watcher.watchResources(WatchableResource.SOURCE
                , WatchableResource.DOCUMENT_EVENT
                , WatchableResource.THREAD_STATE
                , WatchableResource.REFLOW
                , WatchableResource.CONSOLE_MESSAGE);
        }
        catch (Exception e1)
        {
        }
        sourceMapManager = new SourceMapManager(resourceResolver);
        breakpointManager = new BreakpointManager(connector, this, tab, sourceMapManager);
        try
        {
            breakpointManager.connect();
        }
        catch (Exception e)
        {
            logError("Could not attach BreakpointManager", e);
            throw new CoreException(SDBGDebugCorePlugin.createErrorStatus("Could not attach BreakpointManager"));
        }
        target = new FirefoxDebugTarget(this, launch, getProcess(), remote);
        BrowserManager.registerProcess(launch, launchConfig, target.getProcess(), processDescription);
        launch.addDebugTarget(target);
    }

    public BreakpointManager getBreakpointManager()
    {
        return breakpointManager;
    }

    public SourceMapManager getSourceMapManager()
    {
        return sourceMapManager;
    }

    public FirefoxDebugTarget getTarget()
    {
        return target;
    }

    public TabActor getTab()
    {
        return tab;
    }
    
    private synchronized TabActor getTab(String url) throws IOException, CoreException
    {
        if(tab == null)
        {
            try
            {
                List<TabActor> tabs = connector.getRootActor().listTabs();
                for(int i=0;i<tabs.size();i++)
                {
                    TabActor tab = tabs.get(i);
                    String turl = tab.getURL();
                    if(turl.equals(url))
                    {
                        this.tab = tab;
                    }
                }
                if(tab == null)
                {
                    for(int i=0;i<tabs.size();i++)
                    {
                        TabActor tab = tabs.get(i);
                        String turl = tab.getURL();
                        if(turl.equals(getInitialPage()))
                        {
                            this.tab = tab;
                            tab.navigateTo(url);
                            sleep(500);
                        }
                    }
                }
                if(tab == null)
                {
                    tab = tabs.get(tabs.size()-1);
                    tab.navigateTo(url);
                    sleep(500);
                }
            }
            catch (JSONException e)
            {
                throw new IOException("Could not get Tabs", e);
            }
        }
        return tab;
    }
    
    @Override
    public List<DefaultTabInfo> getAvailableTabs(String host, int port) throws IOException
    {
        return null;
    }
    
    public DebugConnector getConnector()
    {
        return connector;
    }

    public IResourceResolver getResourceResolver()
    {
        return resourceResolver;
    }

    @Override
    public void terminateExistingBrowserProcess()
    {
        connector.stop();
        super.terminateExistingBrowserProcess();
    }
    
    @Override
    protected List<String> buildArgumentsList(SDBGLaunchConfigWrapper launchConfig, String url, int devToolsPortNumber,
        List<String> extraArguments)
    {
        List<String> arguments = new ArrayList<String>();
        arguments.add(getExecutableFile().getAbsolutePath());
        if (devToolsPortNumber > -1)
        {
            // Enable remote debug over HTTP on the specified port.
            arguments.add("--start-debugger-server");
            arguments.add("" + devToolsPortNumber);
        }
        
        arguments.add("--profile");
        String tmpDir = System.getProperty("java.io.tmpdir");
        File dir = new File(tmpDir , "sdbg");
        if(dir.exists() == false)
        {
            dir.mkdirs();
        }
        File prefs = new File(dir, "prefs.js");
        if(prefs.exists() == false)
        {
            InputStream in = getClass().getResourceAsStream("/resources/firefox_prefs.js");
            try
            {
                FileOutputStream out = new FileOutputStream(new File(dir, "prefs.js"));
                byte[] buf = new byte[1024];
                int c = in.read(buf);
                while(c >= 0)
                {
                    out.write(buf, 0, c);
                    c = in.read(buf);
                }
                in.close();
                out.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        arguments.add(dir.getAbsolutePath());
        arguments.addAll(extraArguments);
        for (String arg : launchConfig.getArgumentsAsArray())
        {
            arguments.add(arg);
        }
        arguments.add(getInitialPage());
        return arguments;
    }

    public static IBrowser findBrowserByProperty()
    {
        File file = AbstractBrowser.findBrowserByProperty(getExecutableCandidates());
        if(file != null)
        {
            return new FirefoxBrowser(file);
        }
        return null;
    }
    
    private static List<String> getExecutableCandidates()
    {
        List<String> exes = new ArrayList<>();
        exes.add("firefox");
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
            return new FirefoxBrowser(file);
        }
        return null;
    }
}
