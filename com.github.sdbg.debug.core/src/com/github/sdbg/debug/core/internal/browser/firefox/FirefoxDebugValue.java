package com.github.sdbg.debug.core.internal.browser.firefox;

import com.github.sdbg.debug.core.SDBGDebugCorePlugin;
import com.github.sdbg.debug.core.model.BrowserDebugElement;
import com.github.sdbg.debug.core.model.ISDBGValue;

import java.math.BigDecimal;
import java.util.Map;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.core.model.IWatchExpressionListener;

import de.exware.remotefox.ObjectActor;

public class FirefoxDebugValue extends BrowserDebugElement<FirefoxDebugTarget> implements IValue, ISDBGValue
{
    private FirefoxDebugVariable variable;
    private Object value;
    
    public FirefoxDebugValue(FirefoxDebugTarget target, FirefoxDebugVariable variable, Object value)
    {
        super(target);
        this.value = value;
        this.variable = variable;
    }

    @Override
    public String getReferenceTypeName() throws DebugException
    {
        return "Typename";
    }

    @Override
    public String getValueString() throws DebugException
    {
        if(value instanceof ObjectActor)
        {
            ObjectActor actor = (ObjectActor)value;
            String id = actor.getActorId();
            return actor.getClassName() + " " + id.substring(id.lastIndexOf("/obj") + 4);
        }
        return value == null ? "null" : value.toString();
    }

    @Override
    public IVariable[] getVariables() throws DebugException
    {
        IVariable[] vars = null;
        if(value instanceof ObjectActor)
        {
            Map<String, Object> map;
            try
            {
                map = ((ObjectActor)value).getProperties();
            }
            catch (Exception e)
            {
                throw new DebugException(SDBGDebugCorePlugin.createErrorStatus("Unable to load variables"));
            }
            vars = new IVariable[map.size()];
            int i = 0;
            for (String name : map.keySet())
            {
                vars[i++] = new FirefoxDebugVariable(getTarget(), variable, name, map.get(name));
            }
        }
        return vars;
    }

    @Override
    public boolean hasVariables() throws DebugException
    {
        return value instanceof ObjectActor;
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
        Object detail = null;
        try
        {
            String eval = getVariableExpressionPath() + ".toString()";
            detail = getTarget().getTab().evaluate(eval);
        }
        catch (Exception e)
        {
            detail = e.getMessage();
            String eval;
            try
            {
                eval = getVariableExpressionPath() + " == null";
                detail = getTarget().getTab().evaluate(eval);
                if((Boolean)detail)
                {
                    detail = null;
                }
            }
            catch (Exception e1)
            {
            }
        }
        if(detail == null)
        {
            callback.detailComputed(null);
        }
        else
        {
            callback.detailComputed("" + detail);
        }
    }

    String getVariableExpressionPath() throws DebugException
    {
        String path = variable.getName();
        FirefoxDebugVariable var = variable.getParent();
        while(var != null)
        {
            path = var.getName() + "." + path;
            var = var.getParent();
        }
        return path;
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
        return value instanceof Boolean;
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
        return value == null;
    }

    @Override
    public boolean isNumber()
    {
        return value instanceof Number;
    }

    @Override
    public boolean isObject()
    {
        return value instanceof ObjectActor;
    }

    @Override
    public boolean isPrimitive()
    {
        return value instanceof Number
            || value instanceof Boolean;
    }

    @Override
    public boolean isScope()
    {
        return false;
    }

    @Override
    public boolean isString()
    {
        return value instanceof String;
    }

    @Override
    public void reset()
    {
    }

    public void setStringValue(String value)
    {
        value = "'" + value + "'";
        setValue(value);
    }

    public void setBooleanValue(Boolean value)
    {
        setValue(value.toString());
    }
    
    private void setValue(String value)
    {
        try
        {
            String eval = getVariableExpressionPath() + "=" + value;
            getTarget().getTab().evaluate(eval);
            this.value = value;
            getTarget().fakeResumePause();
        }
        catch (Exception e)
        {
        }
    }

    public void setNumberValue(String value)
    {
        if(this.value instanceof Integer)
        {
            setValue("" + Integer.parseInt(value));
        }
        else if(this.value instanceof Long)
        {
            setValue("" + Long.parseLong(value));
        }
        else if(this.value instanceof Double)
        {
            setValue("" + Double.parseDouble(value));
        }
        else if(this.value instanceof BigDecimal)
        {
            setValue("" + Double.parseDouble(value));
        }
    }
}
