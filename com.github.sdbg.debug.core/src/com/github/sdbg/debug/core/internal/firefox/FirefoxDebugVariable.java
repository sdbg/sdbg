package com.github.sdbg.debug.core.internal.firefox;

import com.github.sdbg.debug.core.internal.webkit.model.WebkitDebugElement;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

public class FirefoxDebugVariable extends WebkitDebugElement implements IVariable
{
    private FirefoxDebugValue value;
    private String name;
    
    public FirefoxDebugVariable(FirefoxDebugTarget target, String name, Object object)
    {
        super(target);
        this.name = name;
        this.value = new FirefoxDebugValue((FirefoxDebugTarget) getDebugTarget(), object);
    }

    @Override
    public void setValue(String arg0) throws DebugException
    {
    }

    @Override
    public void setValue(IValue arg0) throws DebugException
    {
    }

    @Override
    public boolean supportsValueModification()
    {
        return false;
    }

    @Override
    public boolean verifyValue(String arg0) throws DebugException
    {
        return false;
    }

    @Override
    public boolean verifyValue(IValue arg0) throws DebugException
    {
        return false;
    }

    @Override
    public String getName() throws DebugException
    {
        return name;
    }

    @Override
    public String getReferenceTypeName() throws DebugException
    {
        return getValue().getReferenceTypeName();
    }

    @Override
    public IValue getValue() throws DebugException
    {
        return value;
    }

    @Override
    public boolean hasValueChanged() throws DebugException
    {
        return false;
    }

}
