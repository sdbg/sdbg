package com.github.sdbg.debug.core.internal.util;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.SDBGLaunchConfigWrapper;
import com.github.sdbg.debug.core.internal.android.ADBManager;
import com.github.sdbg.debug.core.internal.util.ListeningStream.StreamListener;
import com.github.sdbg.debug.core.internal.webkit.model.WebkitDebugTarget;
import com.github.sdbg.debug.core.internal.webkit.protocol.ChromiumConnector;
import com.github.sdbg.debug.core.internal.webkit.protocol.DefaultTabInfo;
import com.github.sdbg.debug.core.internal.webkit.protocol.WebkitConnection;
import com.github.sdbg.debug.core.model.IResourceResolver;
import com.github.sdbg.debug.core.util.IBrowserTabChooser;
import com.github.sdbg.debug.core.util.IBrowserTabInfo;
import com.github.sdbg.debug.core.util.Trace;
import com.github.sdbg.utilities.NetUtils;
import com.github.sdbg.utilities.Streams;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GenericBrowser implements IBrowser
{
    /** The initial page to navigate to. */
    private String initial_page;
    private Process process;
    private File executable;
    private String browserDataDirName;

    public GenericBrowser(File executable, String browserDataDirName)
    {
        this(executable, browserDataDirName, "chrome://version/");
    }

    protected GenericBrowser(File executable, String browserDataDirName, String initalPage)
    {
        this.executable = executable;
        this.browserDataDirName = browserDataDirName;
        initial_page = initalPage;
    }

    @Override
    public boolean isProcessTerminated()
    {
        try
        {
            if (process != null)
            {
                process.exitValue();
            }
            return true;
        }
        catch (IllegalThreadStateException ex)
        {
            return false;
        }
    }

    public void terminateExistingBrowserProcess()
    {
        if (!isProcessTerminated())
        {
            // TODO(devoncarew): try and use an OS mechanism to send it a
            // graceful shutdown request?
            // This could avoid the problem w/ Chrome displaying the crashed
            // message on the next run.

            process.destroy();

            // The process needs time to exit.
            waitForProcessToTerminate(process, 200);
            // sleep(100);
        }
        process = null;
    }

    private void waitForProcessToTerminate(Process process, int maxWaitTimeMs)
    {
        long startTime = System.currentTimeMillis();

        while ((System.currentTimeMillis() - startTime) < maxWaitTimeMs)
        {
            if (isProcessTerminated())
            {
                return;
            }
            sleep(10);
        }
    }

    private void sleep(int millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (Exception exception)
        {

        }
    }

    /**
     * @param launchConfig
     * @param url
     * @param monitor
     * @param enableDebugging
     * @param browserLocation
     * @param browserName
     * @throws CoreException
     */
    public ListeningStream startNewBrowserProcess(SDBGLaunchConfigWrapper launchConfig, String url,
        IProgressMonitor monitor, boolean enableDebugging, StringBuilder argDescription, List<String> extraArguments,
        int[] devToolsPortNumberHolder) throws CoreException
    {
        process = null;
        monitor.worked(1);

        ProcessBuilder builder = new ProcessBuilder();
        Map<String, String> env = builder.environment();

        // Due to differences in 32bit and 64 bit environments, dartium 32bit
        // launch does not work on
        // linux with this property.
        env.remove("LD_LIBRARY_PATH");

        Map<String, String> wrapperEnv = launchConfig.getEnvironment();
        if (!wrapperEnv.isEmpty())
        {
            for (String key : wrapperEnv.keySet())
            {
                env.put(key, wrapperEnv.get(key));
            }
        }

        int devToolsPortNumber = -1;
        if (enableDebugging)
        {
            devToolsPortNumber = NetUtils.findUnusedPort(DEVTOOLS_PORT_NUMBER);

            if (devToolsPortNumber == -1)
            {
                throw new CoreException(new Status(IStatus.ERROR, SDBGDebugCorePlugin.PLUGIN_ID,
                    "Unable to locate an available port for the browser debugger"));
            }

        }

        devToolsPortNumberHolder[0] = devToolsPortNumber;
        List<String> arguments = buildArgumentsList(launchConfig, url != null ? url : initial_page, devToolsPortNumber,
            extraArguments);
        builder.command(arguments);
        builder.redirectErrorStream(true);

        describe(arguments, argDescription);

        try
        {
            process = builder.start();
        }
        catch (IOException e)
        {
            SDBGDebugCorePlugin.logError("Exception while starting browser", e);

            throw new CoreException(
                new Status(IStatus.ERROR, SDBGDebugCorePlugin.PLUGIN_ID, "Could not launch browser: " + e.toString()));
        }

        ListeningStream line = readFromProcessPipes(process.getInputStream());
        sleep(100);
        return line;
    }

    private ListeningStream readFromProcessPipes(final InputStream in)
    {
        final ListeningStream output = new ListeningStream();

        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                byte[] buffer = new byte[2048];

                try
                {
                    int count = in.read(buffer);

                    while (count != -1)
                    {
                        if (count > 0)
                        {
                            String str = new String(buffer, 0, count);

                            // Log any browser process output to stdout.
                            if (Trace.isTracing(Trace.BROWSER_OUTPUT))
                            {
                                System.out.print(str);
                            }

                            output.appendData(str);
                        }

                        count = in.read(buffer);
                    }

                    in.close();
                }
                catch (IOException ioe)
                {
                    // When the process closes, we do not want to print any
                    // errors.
                }
            }
        }, "Read from browser");

        thread.start();

        return output;
    }

    @Override
    public WebkitDebugTarget connectToBrowserDebug(String browserName, ILaunch launch,
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

            return debugTarget;
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

    private void trace(String message)
    {
        Trace.trace(Trace.BROWSER_LAUNCHING, message);
    }

    @Override
    public Process getProcess()
    {
        return process;
    }

    public IBrowserTabInfo getTab(IBrowserTabChooser browserTabChooser, String host, int port, long maxStartupDelay,
        ListeningStream browserOutput) throws IOException, CoreException
    {
        // Give Chromium 20 seconds to start up.
        long endTime = System.currentTimeMillis() + Math.max(maxStartupDelay, 0L);
        while (true)
        {
            if (isProcessTerminated())
            {
                throw new CoreException(new Status(IStatus.ERROR, SDBGDebugCorePlugin.PLUGIN_ID,
                    "Could not launch browser - process terminated while trying to connect. "
                        + "Try closing any running Browser instances."
                        + BrowserManager.getProcessStreamMessage(browserOutput.toString())));
            }

            try
            {
                DefaultTabInfo targetTab = (DefaultTabInfo) findTargetTab(browserTabChooser,
                    getAvailableTabs(host, port));
                if (targetTab != null || System.currentTimeMillis() > endTime)
                {
                    return targetTab;
                }
            }
            catch (IOException exception)
            {
                if (System.currentTimeMillis() > endTime)
                {
                    throw exception;
                }
            }

            if (System.currentTimeMillis() > endTime)
            {
                throw new IOException("Timed out trying to connect to Browser");
            }

            sleep(25);
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

                DefaultTabInfo tab = DefaultTabInfo.fromJson(host, port, object);

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

    protected HttpUrlConnector getURLConnector(String host, int port)
    {
        return new HttpUrlConnector(host, port, "/json");
    }
    
    private static String readText(HttpUrlConnector connection, InputStream in) throws IOException
    {
        return Streams.loadAndClose(new InputStreamReader(in, "UTF-8"));
    }

    private IBrowserTabInfo findTargetTab(IBrowserTabChooser browserTabChooser, List<? extends IBrowserTabInfo> tabs)
        throws CoreException
    {
        IBrowserTabInfo chosenTab = browserTabChooser.chooseTab(tabs);
        if (chosenTab != null)
        {
            for (IBrowserTabInfo tab : tabs)
            {
                trace("Found: " + tab.toString());
            }
            trace("Choosing: " + chosenTab);
            return chosenTab;
        }

        StringBuilder builder = new StringBuilder("Unable to locate target Browser tab [" + tabs.size() + " tabs]\n");
        for (IBrowserTabInfo tab : tabs)
        {
            builder.append("  " + tab.toString() + " [" + tab.getTitle() + "]\n");
        }

        SDBGDebugCorePlugin.logError(builder.toString().trim());
        return null;
    }

    private List<String> buildArgumentsList(SDBGLaunchConfigWrapper launchConfig, String url, int devToolsPortNumber,
        List<String> extraArguments)
    {
        List<String> arguments = new ArrayList<String>();
        arguments.add(executable.getAbsolutePath());
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

    private void describe(List<String> arguments, StringBuilder builder)
    {
        for (int i = 0; i < arguments.size(); i++)
        {
            if (i > 0)
            {
                builder.append(" ");
            }
            builder.append(arguments.get(i));
        }
    }

    @Override
    public String getExecutableName()
    {
        return executable.getName();
    }

    @Override
    public File getExecutableFile()
    {
        return executable;
    }
}
