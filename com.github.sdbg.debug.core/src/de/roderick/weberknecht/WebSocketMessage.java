// 
// Decompiled by Procyon v0.5.36
// 

package de.roderick.weberknecht;

import java.io.UnsupportedEncodingException;

public class WebSocketMessage
{
    private Byte[] message;
    
    public WebSocketMessage(final Byte[] message) {
        this.message = message;
    }
    
    public String getText() {
        final byte[] message = new byte[this.message.length];
        for (int i = 0; i < this.message.length; ++i) {
            message[i] = this.message[i];
        }
        try {
            return new String(message, "UTF-8");
        }
        catch (UnsupportedEncodingException uee) {
            return null;
        }
    }
}
