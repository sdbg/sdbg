// 
// Decompiled by Procyon v0.5.36
// 

package de.roderick.weberknecht;

public class WebSocketException extends Exception
{
    private static final long serialVersionUID = 1L;
    
    public WebSocketException(final String message) {
        super(message);
    }
    
    public WebSocketException(final String message, final Throwable t) {
        super(message, t);
    }
}
