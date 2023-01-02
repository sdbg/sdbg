package com.github.sdbg.debug.core.internal.firefox;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;

public class FirefoxDebugProcess extends PlatformObject implements IProcess
{
    private FirefoxDebugTarget target;

    public FirefoxDebugProcess(FirefoxDebugTarget target, Process process)
    {
        this.target = target;
        fireCreationEvent();
        Thread t = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    process.waitFor();
                    fireTerminateEvent();
                    target.fireTerminateEvent();
                }
                catch (InterruptedException e)
                {
                }
            }
        };
        t.start();
    }

    void fireCreationEvent()
    {
        fireEvent(new DebugEvent(this, DebugEvent.CREATE));
    }

    private void fireEvent(DebugEvent event)
    {
        DebugPlugin manager = DebugPlugin.getDefault();
        if (manager != null)
        {
            manager.fireDebugEventSet(new DebugEvent[] { event });
        }
    }

    @Override
    public Object getAdapter(Class adapter)
    {
        if (adapter == ILaunch.class)
        {
            return getLaunch();
        }

        if (adapter == IDebugTarget.class)
        {
            return target;
        }

        return super.getAdapter(adapter);
    }
   
    @Override
    public boolean canTerminate()
    {
        return true;
    }

    @Override
    public boolean isTerminated()
    {
        return false;
    }

    @Override
    public void terminate() throws DebugException
    {
    }

    @Override
    public String getAttribute(String arg0)
    {
        return null;
    }

    @Override
    public int getExitValue() throws DebugException
    {
        return 0;
    }

    @Override
    public String getLabel()
    {
        return "XY";
    }

    @Override
    public ILaunch getLaunch()
    {
        return target.getLaunch();
    }

    @Override
    public IStreamsProxy getStreamsProxy()
    {
        return null;
    }

    @Override
    public void setAttribute(String arg0, String arg1)
    {
    }

    private void fireTerminateEvent()
    {
        if(target != null)
        {
            fireEvent(new DebugEvent(this, DebugEvent.TERMINATE));
        }
    }
}
