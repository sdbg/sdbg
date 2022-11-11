package com.github.sdbg.debug.core.internal.util;

import static com.github.sdbg.debug.core.SDBGDebugCorePlugin.logError;
import static com.github.sdbg.debug.core.SDBGDebugCorePlugin.logInfo;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.SDBGLaunchConfigWrapper;
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
import org.json.JSONObject;

import de.exware.remotefox.DebugConnector;

public class FirefoxBrowser extends AbstractBrowser
{
    private DebugConnector connector;
    
    public FirefoxBrowser(File executable)
    {
        super(executable, "about:welcome");
    }
    
    @Override
    public void connectToBrowserDebug(String name, ILaunch launch, SDBGLaunchConfigWrapper launchConfig,
        String string, IProgressMonitor monitor, LogTimer timer, boolean enableBreakpoints, String host, int port,
        long maxStartupDelay, ListeningStream browserOutput, String processDescription,
        IResourceResolver resourceResolver, IBrowserTabChooser browserTabChooser, boolean remote) throws CoreException
    {
        connector = new DebugConnector(host, port);
        connector.setLogWire(true);
        JSONObject welcome;
        long start = System.currentTimeMillis();
        boolean success = false;
        while(success == false || start > System.currentTimeMillis() + 2000)
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
        try
        {
            welcome = connector.readMessage();
        }
        catch (IOException | JSONException e)
        {
            logError("No Welcome from Firefox", e);
            throw new CoreException(SDBGDebugCorePlugin.createErrorStatus("No Welcome from Firefox"));
        }
        if(welcome == null)
        {
            trace("No Welcome from Firefox");
            throw new CoreException(SDBGDebugCorePlugin.createErrorStatus("No Welcome from Firefox"));
        }
        logInfo("Debugger connected");
        connector.start();
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
            InputStream in = getClass().getResourceAsStream("/firefox_prefs.js");
            try
            {
                FileOutputStream out = new FileOutputStream(new File(dir, "prefs.js"));
                byte[] buf = new byte[1024];
                int c = in.read(buf);
                while(c >= 0)
                {
                    out.write(buf, 0, c);
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
        if (url != null)
        {
            arguments.add(url);
        }
        return arguments;
    }
}
