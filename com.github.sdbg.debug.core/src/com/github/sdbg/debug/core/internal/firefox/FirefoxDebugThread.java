package com.github.sdbg.debug.core.internal.firefox;

import com.github.sdbg.debug.core.internal.webkit.model.WebkitDebugElement;
import com.github.sdbg.debug.core.model.ISDBGThread;

import java.util.List;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;

import de.exware.remotefox.StackFrameActor;
import de.exware.remotefox.TabActor;

public class FirefoxDebugThread extends WebkitDebugElement implements ISDBGThread
{
    public FirefoxDebugThread(FirefoxDebugTarget debugTarget)
    {
        super(debugTarget);
    }
    
    @Override
    public boolean isSuspended()
    {
        FirefoxDebugTarget target = (FirefoxDebugTarget) getDebugTarget();
        return target.isSuspended();
    }
    
    @Override
    public IStackFrame[] getStackFrames() throws DebugException
    {
        IStackFrame[] frames = new IStackFrame[0];
        FirefoxDebugTarget target = (FirefoxDebugTarget) getDebugTarget();
        try
        {
            TabActor tab = target.getTab();
            List<StackFrameActor> stackframes = tab.getStackFrames();
            if(stackframes != null)
            {
                frames = new IStackFrame[stackframes.size()];
                for(int i=0;i<stackframes.size();i++)
                {
                    frames[i] = new FirefoxDebugStackFrame(target, this, stackframes.get(i));
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return frames;
    }
    
    @Override
    public boolean hasStackFrames() throws DebugException
    {
        return true;
    }
    
    @Override
    public IBreakpoint[] getBreakpoints()
    {
        return new IBreakpoint[0];
    }

    @Override
    public String getName() throws DebugException
    {
        return "FirefoxThread";
    }

    @Override
    public int getPriority() throws DebugException
    {
        return 0;
    }

    @Override
    public IStackFrame getTopStackFrame() throws DebugException
    {
        return getStackFrames()[0];
    }

    @Override
    public boolean canResume()
    {
        FirefoxDebugTarget target = (FirefoxDebugTarget) getDebugTarget();
        return target.isSuspended();
    }

    @Override
    public boolean canSuspend()
    {
        return ! canResume();
    }

    @Override
    public void resume() throws DebugException
    {
        FirefoxDebugTarget target = (FirefoxDebugTarget) getDebugTarget();
        target.resume();
    }

    @Override
    public void suspend() throws DebugException
    {
        FirefoxDebugTarget target = (FirefoxDebugTarget) getDebugTarget();
        target.suspend();
    }

    @Override
    public boolean canStepInto()
    {
        return false;
    }

    @Override
    public boolean canStepOver()
    {
        return false;
    }

    @Override
    public boolean canStepReturn()
    {
        return false;
    }

    @Override
    public boolean isStepping()
    {
        return false;
    }

    @Override
    public void stepInto() throws DebugException
    {
    }

    @Override
    public void stepOver() throws DebugException
    {
    }

    @Override
    public void stepReturn() throws DebugException
    {
    }

    @Override
    public boolean canTerminate()
    {
        FirefoxDebugTarget target = (FirefoxDebugTarget) getDebugTarget();
        return target.canTerminate();
    }

    @Override
    public boolean isTerminated()
    {
        FirefoxDebugTarget target = (FirefoxDebugTarget) getDebugTarget();
        return target.isTerminated();
    }

    @Override
    public void terminate() throws DebugException
    {
        FirefoxDebugTarget target = (FirefoxDebugTarget) getDebugTarget();
        target.terminate();
    }

    @Override
    public IStackFrame getIsolateVarsPseudoFrame()
    {
        return null;
    }
}
