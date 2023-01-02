package com.github.sdbg.debug.core.internal.util;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.SDBGLaunchConfigWrapper;
import com.github.sdbg.debug.core.internal.webkit.protocol.DefaultTabInfo;
import com.github.sdbg.debug.core.util.IBrowserTabChooser;
import com.github.sdbg.debug.core.util.IBrowserTabInfo;
import com.github.sdbg.debug.core.util.Trace;
import com.github.sdbg.utilities.NetUtils;
import com.github.sdbg.utilities.Streams;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public abstract class AbstractBrowser implements IBrowser
{
    /** The initial page to navigate to. */
    private String initial_page;
    private Process process;
    private File executable;

    public AbstractBrowser(File executable)
    {
        this(executable, "chrome://version/");
    }

    protected AbstractBrowser(File executable, String initalPage)
    {
        this.executable = executable;
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

    @Override
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

    protected void sleep(int millis)
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
    @Override
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

    protected void trace(String message)
    {
        Trace.trace(Trace.BROWSER_LAUNCHING, message);
    }

    @Override
    public Process getProcess()
    {
        return process;
    }

    @Override
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
    abstract public List<DefaultTabInfo> getAvailableTabs(String host, int port) throws IOException;

    protected HttpUrlConnector getURLConnector(String host, int port)
    {
        return new HttpUrlConnector(host, port, "/json");
    }
    
    protected static String readText(HttpUrlConnector connection, InputStream in) throws IOException
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

    abstract protected List<String> buildArgumentsList(SDBGLaunchConfigWrapper launchConfig, String url, int devToolsPortNumber,
        List<String> extraArguments);

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
    
    @Override
    public String getName()
    {
        return getExecutableFile().getName();
    }
}
