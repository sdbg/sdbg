package com.github.sdbg.debug.core.internal.util;

import com.github.sdbg.debug.core.SDBGLaunchConfigWrapper;
import com.github.sdbg.debug.core.model.IResourceResolver;
import com.github.sdbg.debug.core.util.IBrowserTabChooser;
import com.github.sdbg.debug.core.util.IBrowserTabInfo;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;

/**
 * Interface for different Browsers
 */
public interface IBrowser
{
    public static final int DEVTOOLS_PORT_NUMBER = 9322;

    boolean isProcessTerminated();

    ListeningStream startNewBrowserProcess(SDBGLaunchConfigWrapper launchConfig, String string,
        IProgressMonitor monitor, boolean enableDebugging, StringBuilder processDescription,
        List<String> extraCommandLineArgs, int[] devToolsPortNumberHolder) throws CoreException;

    void connectToBrowserDebug(String name, ILaunch launch, SDBGLaunchConfigWrapper launchConfig,
        String string, IProgressMonitor monitor, LogTimer timer, boolean enableBreakpoints,
        String host, int port, long maxStartupDelay, ListeningStream browserOutput, String processDescription,
        IResourceResolver resourceResolver, IBrowserTabChooser browserTabChooser, boolean remote) throws CoreException;

    Process getProcess();

    void terminateExistingBrowserProcess();

    IBrowserTabInfo getTab(IBrowserTabChooser iBrowserTabChooser, String host, int port,
        long maxStartupDelay, ListeningStream browserOutput) throws IOException, CoreException;

    String getExecutableName();
    
    File getExecutableFile();
}
