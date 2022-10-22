// 
// Decompiled by Procyon v0.5.36
// 

package de.roderick.weberknecht;

import java.util.List;
import java.io.IOException;
import java.util.ArrayList;
import java.io.DataInputStream;

public class WebSocketReceiver extends Thread
{
    private DataInputStream input;
    private WebSocket websocket;
    private WebSocketEventHandler eventHandler;
    private volatile boolean stop;
    
    public WebSocketReceiver(final DataInputStream input, final WebSocket websocket) {
        this.input = null;
        this.websocket = null;
        this.eventHandler = null;
        this.stop = false;
        this.input = input;
        this.websocket = websocket;
        this.eventHandler = websocket.getEventHandler();
    }
    
    public boolean isRunning() {
        return !this.stop;
    }
    
    @Override
    public void run() {
        final List<Byte> messageBytes = new ArrayList<Byte>();
        while (!this.stop) {
            try {
                final byte b = this.input.readByte();
                final byte opcode = (byte)(b & 0xF);
                final byte length = this.input.readByte();
                long payload_length = 0L;
                if (length < 126) {
                    payload_length = length;
                }
                else if (length == 126) {
                    payload_length = ((0xFF & this.input.readByte()) << 8 | (0xFF & this.input.readByte()));
                }
                else if (length == 127) {
                    payload_length = this.input.readLong();
                }
                for (int i = 0; i < payload_length; ++i) {
                    messageBytes.add(this.input.readByte());
                }
                final Byte[] message = messageBytes.toArray(new Byte[messageBytes.size()]);
                final WebSocketMessage ws_message = new WebSocketMessage(message);
                this.eventHandler.onMessage(ws_message);
                messageBytes.clear();
            }
            catch (IOException ioe) {
                this.handleError();
            }
        }
    }
    
    public void stopit() {
        this.stop = true;
    }
    
    private void handleError() {
        this.stopit();
        this.websocket.handleReceiverError();
    }
}
