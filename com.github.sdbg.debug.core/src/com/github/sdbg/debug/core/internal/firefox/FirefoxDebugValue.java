package com.github.sdbg.debug.core.internal.firefox;

import com.github.sdbg.debug.core.internal.webkit.model.WebkitDebugElement;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

public class FirefoxDebugValue extends WebkitDebugElement implements IValue
{
    private Object value;
    
    public FirefoxDebugValue(FirefoxDebugTarget target, Object value)
    {
        super(target);
        this.value = value;
    }

    @Override
    public String getReferenceTypeName() throws DebugException
    {
        return "Typename";
    }

    @Override
    public String getValueString() throws DebugException
    {
        return value == null ? "null" : value.toString();
    }

    @Override
    public IVariable[] getVariables() throws DebugException
    {
        return null;
    }

    @Override
    public boolean hasVariables() throws DebugException
    {
        return false;
    }

    @Override
    public boolean isAllocated() throws DebugException
    {
        return true;
    }

}
