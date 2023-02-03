package com.github.sdbg.debug.core.internal.browser;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.SDBGLaunchConfigWrapper;
import com.github.sdbg.debug.core.internal.util.HttpUrlConnector;
import com.github.sdbg.debug.core.internal.util.ListeningStream;
import com.github.sdbg.debug.core.internal.util.LogTimer;
import com.github.sdbg.debug.core.internal.util.ListeningStream.StreamListener;
import com.github.sdbg.debug.core.internal.webkit.model.WebkitDebugTarget;
import com.github.sdbg.debug.core.internal.webkit.protocol.DefaultTabInfo;
import com.github.sdbg.debug.core.internal.webkit.protocol.JsonUtils;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitConnection;
import com.github.sdbg.debug.core.model.IResourceResolver;
import com.github.sdbg.debug.core.util.IBrowserTabChooser;
import com.github.sdbg.debug.core.util.IBrowserTabInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WebkitBasedBrowser extends AbstractBrowser
{
    private String browserDataDirName;

    public WebkitBasedBrowser(File executable, String browserDataDirName)
    {
        super(executable);
        this.browserDataDirName = browserDataDirName;
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
            arguments.add("--remote-debugging-port=" + devToolsPortNumber);
        }
        // In order to start up multiple Browser processes, we need to specify a
        // different user dir.
        arguments.add("--user-data-dir=" + getCreateUserDataDirectory(browserDataDirName).getAbsolutePath());
        // Whether or not it's actually the first run.
//        arguments.add("--no-first-run");
        // Disables the default browser check.
        arguments.add("--no-default-browser-check");
        // Bypass the error dialog when the profile lock couldn't be attained.
        arguments.add("--no-process-singleton-dialog");
        arguments.addAll(extraArguments);
        for (String arg : launchConfig.getArgumentsAsArray())
        {
            arguments.add(arg);
        }
        if (url != null)
        {
            arguments.add(url);
        }

        return arguments;
    }

    /**
     * Create a Chrome user data directory, and return the path to that
     * directory.
     * 
     * @return the user data directory path
     */
    private File getCreateUserDataDirectory(String baseName)
    {
        File dataDir = new File(new File(new File(System.getProperty("user.home")), ".sdbg"), baseName);

        if (!dataDir.exists())
        {
            dataDir.mkdirs();
        }
        else
        {
            // Remove the "<dataDir>/Default/Current Tabs" file if it exists -
            // it can cause old tabs to
            // restore themselves when we launch the browser.
            File defaultDir = new File(dataDir, "Default");

            if (defaultDir.exists())
            {
                File tabInfoFile = new File(defaultDir, "Current Tabs");

                if (tabInfoFile.exists())
                {
                    tabInfoFile.delete();
                }

                File sessionInfoFile = new File(defaultDir, "Current Session");

                if (sessionInfoFile.exists())
                {
                    sessionInfoFile.delete();
                }
            }
        }

        return dataDir;
    }

    @Override
    public void connectToBrowserDebug(String browserName, ILaunch launch,
        SDBGLaunchConfigWrapper launchConfig, String url, IProgressMonitor monitor,
        LogTimer timer, boolean enableBreakpoints, String host, int port, long maxStartupDelay,
        ListeningStream browserOutput, String processDescription, IResourceResolver resolver,
        IBrowserTabChooser browserTabChooser, boolean remote) throws CoreException
    {
        monitor.worked(1);

        // avg: 383ms
        timer.startTask("get browser tabs");

        IBrowserTabInfo tab;

        try
        {
            tab = getTab(browserTabChooser, host, port, maxStartupDelay, browserOutput);
        }
        catch (IOException e)
        {
            SDBGDebugCorePlugin.logError(e);
            throw new CoreException(
                new Status(IStatus.ERROR, SDBGDebugCorePlugin.PLUGIN_ID, "Unable to connect to Browser at address "
                    + (host != null ? host : "") + ":" + port + "; error: " + e.getMessage(), e));
        }

        monitor.worked(2);

        timer.stopTask();

        // avg: 46ms
        timer.startTask("open WIP connection");

        if (tab == null)
        {
            throw new DebugException(new Status(IStatus.INFO, SDBGDebugCorePlugin.PLUGIN_ID,
                "No Browser tab was chosen. Connection cancelled."));
        }

        if (tab.getWebSocketDebuggerUrl() == null)
        {
            throw new DebugException(new Status(IStatus.ERROR, SDBGDebugCorePlugin.PLUGIN_ID,
                "Unable to connect to Browser" + (remote
                    ? ".\n\nPossible reason: another debugger (e.g. Chrome DevTools) or another Eclipse debugging session is already attached to that particular Browser tab."
                    : "")));
        }

        // Even when Chrome has reported all the debuggable tabs to us, the
        // debug server
        // may not yet have started up. Delay a small fixed amount of time.
        sleep(100);

        try
        {
            WebkitConnection connection = new WebkitConnection(tab.getHost(), tab.getPort(),
                tab.getWebSocketDebuggerFile());

            final WebkitDebugTarget debugTarget = new WebkitDebugTarget(browserName, connection, launch, getProcess()
                , launchConfig.getProject(), resolver, null/* adbManager */, enableBreakpoints, remote);

            monitor.worked(1);

            BrowserManager.registerProcess(launch, launchConfig, debugTarget.getProcess(), processDescription);
            launch.addDebugTarget(debugTarget);

            if (browserOutput != null && launchConfig.getShowLaunchOutput())
            {
                browserOutput.setListener(new StreamListener()
                {
                    @Override
                    public void handleStreamData(String data)
                    {
                        debugTarget.writeToStdout(data);
                    }
                });
            }

            if (browserOutput != null)
            {
                debugTarget.openConnection(url, true);
            }
            else
            {
                debugTarget.openConnection();
            }

            trace("Connected to WIP debug agent on host " + host + " and port " + port);

            timer.stopTask();

            monitor.worked(1);
        }
        catch (IOException e)
        {
            SDBGDebugCorePlugin.logError(e);
            throw new CoreException(new Status(IStatus.ERROR, SDBGDebugCorePlugin.PLUGIN_ID,
                "Unable to connect to Browser tab at address " + (tab.getHost() != null ? tab.getHost() : "") + ":"
                    + tab.getPort() + " (" + tab.getWebSocketDebuggerFile() + "): " + e.getMessage(),
                e));
        }
    }

    /**
     * Return the list of open tabs for this browser.
     * 
     * @param host
     * @param port
     * @return
     * @throws IOException
     */
    @Override
    public List<DefaultTabInfo> getAvailableTabs(String host, int port) throws IOException
    {
        HttpUrlConnector connection = getURLConnector(host, port);
        String text = readText(connection, connection.getInputStream());
        try
        {
            JSONArray arr = new JSONArray(text);
            List<DefaultTabInfo> tabs = new ArrayList<DefaultTabInfo>();
            for (int i = 0; i < arr.length(); i++)
            {
                JSONObject object = arr.getJSONObject(i);

                DefaultTabInfo tab = createTabInfo(host, port, object);

                tabs.add(tab);
            }
            Collections.sort(tabs, DefaultTabInfo.getComparator());
            return tabs;
        }
        catch (JSONException exception)
        {
            throw new IOException(exception);
        }
    }
    
    DefaultTabInfo createTabInfo(String host, int port, JSONObject object) throws JSONException
    {
        DefaultTabInfo tab = new DefaultTabInfo(host, port
            , JsonUtils.getString(object, "devtoolsFrontendUrl")
            , JsonUtils.getString(object, "faviconUrl")
            , JsonUtils.getString(object, "thumbnailUrl")
            , JsonUtils.getString(object, "title")
            , JsonUtils.getString(object, "url")
            , JsonUtils.getString(object, "webSocketDebuggerUrl"));
        return tab;
        
    }
}
