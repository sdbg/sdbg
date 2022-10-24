// 
// Decompiled by Procyon v0.5.36
// 

package de.roderick.weberknecht;

public interface WebSocketEventHandler
{
    void onClose();
    
    void onMessage(final WebSocketMessage p0);
    
    void onOpen();
    
    void onPing();
    
    void onPong();
}
