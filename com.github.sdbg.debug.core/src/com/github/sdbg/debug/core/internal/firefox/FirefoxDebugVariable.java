package com.github.sdbg.debug.core.internal.firefox;

import com.github.sdbg.debug.core.internal.webkit.model.WebkitDebugElement;
import com.github.sdbg.debug.core.model.ISDBGVariable;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;

public class FirefoxDebugVariable extends WebkitDebugElement implements ISDBGVariable
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
    public <T> T getAdapter(Class<T> adapter)
    {
        T t = super.getAdapter(adapter);
        return t;
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

    @Override
    public boolean isLibraryObject()
    {
        return false;
    }

    @Override
    public boolean isLocal()
    {
        return true;
    }

    @Override
    public boolean isScope()
    {
        return false;
    }

    @Override
    public boolean isStatic()
    {
        return true;
    }

    @Override
    public boolean isThisObject()
    {
        return true;
    }

    @Override
    public boolean isThrownException()
    {
        return false;
    }

}
