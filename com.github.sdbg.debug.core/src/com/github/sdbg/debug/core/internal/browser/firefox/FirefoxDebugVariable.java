package com.github.sdbg.debug.core.internal.browser.firefox;

import com.github.sdbg.debug.core.model.BrowserDebugElement;
import com.github.sdbg.debug.core.model.ISDBGVariable;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;

public class FirefoxDebugVariable extends BrowserDebugElement<FirefoxDebugTarget> implements ISDBGVariable
{
    private FirefoxDebugVariable parent;
    private FirefoxDebugValue value;
    private String name;
    private boolean hasChanged;
    
    public FirefoxDebugVariable(FirefoxDebugTarget target, FirefoxDebugVariable parent, String name, Object object)
    {
        super(target);
        this.name = name;
        this.parent = parent;
        this.value = new FirefoxDebugValue(getTarget(), this, object);
    }

    protected FirefoxDebugVariable getParent()
    {
        return parent;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter)
    {
        T t = super.getAdapter(adapter);
        return t;
    }
    
    @Override
    public void setValue(String value) throws DebugException
    {
        if(this.value.isString())
        {
            this.value.setStringValue(value);
        }
        else if(this.value.isBoolean())
        {
            this.value.setBooleanValue(Boolean.parseBoolean(value));
        }
        else if(this.value.isNumber())
        {
            this.value.setNumberValue(value);
        }
    }

    @Override
    public void setValue(IValue arg0) throws DebugException
    {
    }

    @Override
    public boolean supportsValueModification()
    {
        return value != null && 
            (value.isBoolean() 
             || value.isString()
             || value.isNumber()
            );
    }

    @Override
    public boolean verifyValue(String value) throws DebugException
    {
        boolean valid = false; 
        if(this.value.isString())
        {
            valid = true;
        }
        else if(this.value.isBoolean())
        {
            valid = value != null && (value.toLowerCase().equals("true") || value.toLowerCase().equals("false"));
        }
        else if(this.value.isNumber())
        {
            double d = Double.parseDouble(value);
            valid = true;
        }
        return valid;
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
        return hasChanged;
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
