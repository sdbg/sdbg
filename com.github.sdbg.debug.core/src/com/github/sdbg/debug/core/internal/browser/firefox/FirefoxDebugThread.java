package com.github.sdbg.debug.core.internal.browser.firefox;

import com.github.sdbg.debug.core.internal.webkit.model.WebkitDebugElement;
import com.github.sdbg.debug.core.model.ISDBGThread;

import java.util.List;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;

import de.exware.remotefox.StackFrameActor;
import de.exware.remotefox.TabActor;
import de.exware.remotefox.ThreadActor.ResumeType;

public class FirefoxDebugThread extends WebkitDebugElement implements ISDBGThread
{
    private IStackFrame[] frames = new IStackFrame[0];

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
        if(frames == null)
        {
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
        }
        return frames;
    }
    
    @Override
    public boolean hasStackFrames() throws DebugException
    {
        return isSuspended();
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
        IStackFrame[] frames = getStackFrames();
        IStackFrame frame = null;
        if(frames.length > 0)
        {
            frame = frames[0];
        }
        return frame;
    }

    @Override
    public boolean canResume()
    {
        return isSuspended();
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
        return isSuspended();
    }

    @Override
    public boolean canStepOver()
    {
        return isSuspended();
    }

    @Override
    public boolean canStepReturn()
    {
        return isSuspended();
    }

    @Override
    public boolean isStepping()
    {
        return true;
    }

    @Override
    public void stepInto() throws DebugException
    {
        FirefoxDebugTarget target = (FirefoxDebugTarget) getDebugTarget();
        target.resume(ResumeType.STEP_INTO);
    }

    @Override
    public void stepOver() throws DebugException
    {
        FirefoxDebugTarget target = (FirefoxDebugTarget) getDebugTarget();
        target.resume(ResumeType.STEP_OVER);
    }

    @Override
    public void stepReturn() throws DebugException
    {
        FirefoxDebugTarget target = (FirefoxDebugTarget) getDebugTarget();
        target.resume(ResumeType.STEP_OUT);
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
        return new FirefoxDebugIsolateFrame(this);
    }

    public void dropStackFrames()
    {
        frames = new IStackFrame[0];
    }

    public void createStackFrames()
    {
        frames = null;
    }
}
