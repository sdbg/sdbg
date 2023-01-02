package com.github.sdbg.debug.core.internal.firefox;

import static com.github.sdbg.debug.core.SDBGDebugCorePlugin.logError;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.internal.util.FirefoxBrowser;
import com.github.sdbg.debug.core.model.ISDBGDebugTarget;

import java.io.IOException;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;

import de.exware.remotefox.PauseActor.PauseType;
import de.exware.remotefox.TabActor;
import de.exware.remotefox.event.PauseEvent;
import de.exware.remotefox.event.PauseListener;

public class FirefoxDebugTarget extends DebugElement
    implements ISDBGDebugTarget
{
    private ILaunch launch;
    private FirefoxBrowser browser;
    private IProcess process;
    private FirefoxDebugThread thread; 
    
    public FirefoxDebugTarget(FirefoxBrowser browser, ILaunch launch, Process process)
    {
        super(null);
        this.launch = launch;
        this.browser = browser;
        this.process = new FirefoxDebugProcess(this, process);
        thread = new FirefoxDebugThread(this);
        TabActor tab;
        try
        {
            tab = browser.getTab();
            tab.addPauseListener(new PauseListener()
            {
                @Override
                public void resumed(PauseEvent event)
                {
                    int reason = DebugEvent.RESUME;
                    fireResumeEvent(reason);
                }
                
                @Override
                public void paused(PauseEvent event)
                {
                    if(event.getPauseType() == PauseType.BREAKPOINT)
                    {
                        int reason = DebugEvent.BREAKPOINT;
                        fireSuspendEvent(reason);
                    }
                    else if(event.getPauseType() == PauseType.DEBUGGERSTATEMENT)
                    {
                        int reason = DebugEvent.UNSPECIFIED;
                        fireSuspendEvent(reason);
                    }
                }
            });
        }
        catch (Exception e)
        {
            logError("", e);
        }
        fireCreationEvent();
    }

    @Override
    public void fireTerminateEvent()
    {
        DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(browser.getBreakpointManager());
        thread = null;
        // Check for null on system shutdown.
        if (DebugPlugin.getDefault() != null)
        {
            super.fireTerminateEvent();
            try
            {
                launch.terminate();
            }
            catch (DebugException e)
            {
            }            
            DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);
        }
    }

    public FirefoxBrowser getBrowser()
    {
        return browser;
    }

    public TabActor getTab() throws IOException, CoreException
    {
        return browser.getTab();
    }

    @Override
    public IProcess getProcess()
    {
        if(browser.isProcessTerminated())
        {
            process = null;
            return null;
        }
        return process;
    }

    @Override
    public String getModelIdentifier()
    {
        return SDBGDebugCorePlugin.DEBUG_MODEL_ID;
    }

    @Override
    public String getName() throws DebugException
    {
        return "firefox";
    }

    @Override
    public IThread[] getThreads() throws DebugException
    {
        if (thread != null)
        {
            return new IThread[] { thread };
        }
        else
        {
            return new IThread[0];
        }
    }

    @Override
    public boolean hasThreads() throws DebugException
    {
        return true;
    }

    @Override
    public boolean supportsBreakpoint(IBreakpoint arg0)
    {
        return true;
    }

    @Override
    public boolean canTerminate()
    {
        return browser.isProcessTerminated() ? false : true;
    }

    @Override
    public boolean isTerminated()
    {
        return browser.isProcessTerminated();
    }

    @Override
    public void terminate() throws DebugException
    {
        browser.terminateExistingBrowserProcess();
    }

    @Override
    public boolean canResume()
    {
        try
        {
            return browser.getTab().isPaused();
        }
        catch (Exception e)
        {
            logError("Resume check failed", e);
            return false;
        }
    }

    @Override
    public boolean canSuspend()
    {
        return true;
    }

    @Override
    public boolean isSuspended()
    {
        return canResume();
    }

    @Override
    public void resume() throws DebugException
    {
        try
        {
            browser.getTab().resume();
        }
        catch (Exception e)
        {
            logError("Resume failed", e);
        }
    }

    @Override
    public void suspend() throws DebugException
    {
        try
        {
            browser.getTab().interrupt();
        }
        catch (Exception e)
        {
            logError("Resume failed", e);
        }
    }

    @Override
    public void breakpointAdded(IBreakpoint arg0)
    {
    }

    @Override
    public IDebugTarget getDebugTarget()
    {
        return this;
    }
    
    @Override
    public void breakpointChanged(IBreakpoint arg0, IMarkerDelta arg1)
    {
    }

    @Override
    public void breakpointRemoved(IBreakpoint arg0, IMarkerDelta arg1)
    {
    }

    @Override
    public boolean canDisconnect()
    {
        return true;
    }

    @Override
    public void disconnect() throws DebugException
    {
        browser.terminateExistingBrowserProcess();
    }

    @Override
    public boolean isDisconnected()
    {
        return browser.isProcessTerminated();
    }

    @Override
    public IMemoryBlock getMemoryBlock(long arg0, long arg1) throws DebugException
    {
        return null;
    }

    @Override
    public boolean supportsStorageRetrieval()
    {
        return false;
    }

    @Override
    public ISDBGDebugTarget reconnect() throws IOException
    {
        return null;
    }

    @Override
    public void writeToStdout(String message)
    {
    }
    
    @Override
    public ILaunch getLaunch()
    {
        return launch;
    }
}
