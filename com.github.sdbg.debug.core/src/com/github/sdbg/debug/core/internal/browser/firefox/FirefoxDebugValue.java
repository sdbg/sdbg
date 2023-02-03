package com.github.sdbg.debug.core.internal.browser.firefox;

import com.github.sdbg.debug.core.internal.webkit.model.WebkitDebugElement;
import com.github.sdbg.debug.core.model.ISDBGValue;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.core.model.IWatchExpressionListener;

public class FirefoxDebugValue extends WebkitDebugElement implements IValue, ISDBGValue
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

    @Override
    public void evaluateExpression(String expression, IWatchExpressionListener listener)
    {
    }

    @Override
    public void computeDetail(IValueCallback callback)
    {
    }

    @Override
    public String getId()
    {
        return null;
    }

    @Override
    public int getListLength()
    {
        return 0;
    }

    @Override
    public Object getRawValue()
    {
        return null;
    }

    @Override
    public boolean isBoolean()
    {
        return false;
    }

    @Override
    public boolean isFunction()
    {
        return false;
    }

    @Override
    public boolean isListValue()
    {
        return false;
    }

    @Override
    public boolean isNull()
    {
        return false;
    }

    @Override
    public boolean isNumber()
    {
        return false;
    }

    @Override
    public boolean isObject()
    {
        return false;
    }

    @Override
    public boolean isPrimitive()
    {
        return false;
    }

    @Override
    public boolean isScope()
    {
        return false;
    }

    @Override
    public boolean isString()
    {
        return false;
    }

    @Override
    public void reset()
    {
    }

}
