package com.github.sdbg.debug.core.internal.firefox;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.internal.webkit.model.WebkitDebugElement;

import java.util.Map;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;

import de.exware.remotefox.StackFrameActor;

public class FirefoxDebugStackFrame extends WebkitDebugElement implements IStackFrame
{
    private FirefoxDebugThread thread;
    private StackFrameActor frame;

    public FirefoxDebugStackFrame(FirefoxDebugTarget target, FirefoxDebugThread thread, StackFrameActor frame)
    {
        super(target);
        this.thread = thread;
        this.frame = frame;
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
    public boolean canResume()
    {
        return false;
    }

    @Override
    public boolean canSuspend()
    {
        return false;
    }

    @Override
    public boolean isSuspended()
    {
        return false;
    }

    @Override
    public void resume() throws DebugException
    {
    }

    @Override
    public void suspend() throws DebugException
    {
    }

    @Override
    public boolean canTerminate()
    {
        return false;
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
    public int getCharEnd() throws DebugException
    {
        return 0;
    }

    @Override
    public int getCharStart() throws DebugException
    {
        return 0;
    }

    @Override
    public int getLineNumber() throws DebugException
    {
        return 0;
    }

    @Override
    public String getName() throws DebugException
    {
        return frame.getDisplayName() == null ? frame.getActorId() : frame.getDisplayName();
    }

    private String getFileName()
    {
//        String path = getSourceLocationPath();
//        if (path != null)
//        {
//            int index = path.lastIndexOf('/');
//
//            if (index != -1)
//            {
//                return path.substring(index + 1);
//            }
//            else
//            {
//                return path;
//            }
//        }
        return null;
    }

    @Override
    public IRegisterGroup[] getRegisterGroups() throws DebugException
    {
        return new IRegisterGroup[0];
    }

    @Override
    public IThread getThread()
    {
        return thread;
    }

    @Override
    public IVariable[] getVariables() throws DebugException
    {
        Map<String, Object> args;
        try
        {
            args = frame.getVariables();
            args.putAll(frame.getArguments());
        }
        catch (Exception e)
        {
            throw new DebugException(SDBGDebugCorePlugin.createErrorStatus("Unable to load variables"));
        }
        IVariable[] vars = new IVariable[args.size()];
        int i = 0;
        for (String name : args.keySet())
        {
            vars[i++] = new FirefoxDebugVariable((FirefoxDebugTarget) getDebugTarget(), name, args.get(name));
        }
        return vars;
    }

    @Override
    public boolean hasRegisterGroups() throws DebugException
    {
        return false;
    }

    @Override
    public boolean hasVariables() throws DebugException
    {
        return true;
    }
}
