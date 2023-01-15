package com.github.sdbg.debug.core.internal.firefox;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.internal.webkit.model.SourceMapManager;
import com.github.sdbg.debug.core.internal.webkit.model.WebkitDebugElement;
import com.github.sdbg.debug.core.model.ISDBGStackFrame;

import java.util.List;
import java.util.Map;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;

import de.exware.remotefox.SourceActor;
import de.exware.remotefox.SourceLocation;
import de.exware.remotefox.StackFrameActor;

public class FirefoxDebugStackFrame extends WebkitDebugElement implements IStackFrame
 , ISDBGStackFrame
{
    private FirefoxDebugThread thread;
    private StackFrameActor frame;

    public FirefoxDebugStackFrame(FirefoxDebugTarget target, FirefoxDebugThread thread, StackFrameActor frame)
    {
        super(target);
        this.thread = thread;
        this.frame = frame;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object getAdapter(Class adapter)
    {
        if (adapter == IThread.class)
        {
            return getThread();
        }
        else
        {
            return super.getAdapter(adapter);
        }
    }
    
    @Override
    public boolean canStepInto()
    {
        return thread.canStepInto();
    }

    @Override
    public boolean canStepOver()
    {
        return thread.canStepOver();
    }

    @Override
    public boolean canStepReturn()
    {
        return thread.canStepReturn();
    }

    @Override
    public boolean isStepping()
    {
        return thread.isStepping();
    }

    @Override
    public void stepInto() throws DebugException
    {
        thread.stepInto();
    }

    @Override
    public void stepOver() throws DebugException
    {
        thread.stepOver();
    }

    @Override
    public void stepReturn() throws DebugException
    {
        thread.stepReturn();
    }

    @Override
    public boolean canResume()
    {
        return getDebugTarget().canResume();
    }

    @Override
    public boolean canSuspend()
    {
        return getDebugTarget().canSuspend();
    }

    @Override
    public boolean isSuspended()
    {
        return getDebugTarget().isSuspended();
    }

    @Override
    public void resume() throws DebugException
    {
        getDebugTarget().resume();
    }

    @Override
    public void suspend() throws DebugException
    {
        getDebugTarget().suspend();
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
        getDebugTarget().terminate();
    }

    @Override
    public int getCharEnd() throws DebugException
    {
        return -1;
    }

    @Override
    public int getCharStart() throws DebugException
    {
        return -1;
    }

    @Override
    public int getLineNumber() throws DebugException
    {
        int jsLine = frame.getLocation().getLine();
        int line = jsLine;
        line = getMappedLocation().getLine();
        return line+1;
    }

    @Override
    public String getName() throws DebugException
    {
        String name = (frame.getDisplayName() == null ? frame.getActorId() : frame.getDisplayName());
        String path = getSourceLocationPath();
        if(path != null)
        {
            String file = path.substring(path.lastIndexOf("/")+1);
            return name + " (" + file + ":" + getLineNumber() + ")";
        }
        return name;
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

    @Override
    public String getSourceLocationPath()
    {
        String path = null;
        SourceMapManager.SourceLocation location = getMappedLocation();
        if(location != null)
        {
            path = location.getPath();
        }
        return path;
    }

    private SourceMapManager.SourceLocation getMappedLocation()
    {
        SourceMapManager.SourceLocation loc = null; 
        FirefoxDebugTarget target = (FirefoxDebugTarget)getDebugTarget();
        SourceMapManager sm = target.getBrowser().getSourceMapManager();
        try
        {
            List<SourceActor> sources = target.getTab().getSourceActors();
            SourceLocation location = frame.getLocation();
            String sourceActorId = location.getSourceActor();
            for(int i=0;i<sources.size();i++)
            {
                SourceActor source = sources.get(i);
                if(source.getActorId().equals(sourceActorId))
                {
                    loc = sm.getMappingFor(source.getURL(), location.getLine()-1, location.getColumn());
                    if(loc != null)
                    {
                        break;
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return loc;
    }
    
    @Override
    public boolean isPrivate()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isUsingSourceMaps()
    {
        // TODO Auto-generated method stub
        return false;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        return super.equals(obj);
    }
    
    @Override
    public int hashCode()
    {
        return super.hashCode();
    }
}
