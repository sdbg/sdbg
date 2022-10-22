// 
// Decompiled by Procyon v0.5.36
// 

package de.roderick.weberknecht;

import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class WebSocketHandshake
{
    private String host;
    private int port;
    private String path;
    private String protocol;
    private String nonce;
    private Map<String, String> extraHeaders;
    
    public WebSocketHandshake(final String host, final int port, final String path, final String protocol, final Map<String, String> extraHeaders) {
        this.protocol = null;
        this.nonce = null;
        this.extraHeaders = null;
        this.host = host;
        this.port = port;
        this.path = path;
        this.protocol = protocol;
        this.extraHeaders = extraHeaders;
        this.nonce = this.createNonce();
    }
    
    public byte[] getHandshake() {
        String hostString = this.host;
        if (hostString == null) {
            hostString = "localhost";
        }
        if (this.port != -1) {
            hostString = hostString + ":" + this.port;
        }
        final LinkedHashMap<String, String> header = new LinkedHashMap<String, String>();
        header.put("Host", hostString);
        header.put("Upgrade", "websocket");
        header.put("Connection", "Upgrade");
        header.put("Sec-WebSocket-Version", String.valueOf(WebSocket.getVersion()));
        header.put("Sec-WebSocket-Key", this.nonce);
        if (this.protocol != null) {
            header.put("Sec-WebSocket-Protocol", this.protocol);
        }
        if (this.extraHeaders != null) {
            for (final String fieldName : this.extraHeaders.keySet()) {
                if (!header.containsKey(fieldName)) {
                    header.put(fieldName, this.extraHeaders.get(fieldName));
                }
            }
        }
        String handshake = "GET " + this.path + " HTTP/1.1\r\n";
        handshake += this.generateHeader(header);
        handshake += "\r\n";
        final byte[] handshakeBytes = new byte[handshake.getBytes().length];
        System.arraycopy(handshake.getBytes(), 0, handshakeBytes, 0, handshake.getBytes().length);
        return handshakeBytes;
    }
    
    public void verifyServerHandshakeHeaders(final HashMap<String, String> headers) throws WebSocketException {
        if (!headers.get("upgrade").equalsIgnoreCase("websocket")) {
            throw new WebSocketException("connection failed: missing header field in server handshake: upgrade");
        }
        if (!headers.get("connection").equalsIgnoreCase("upgrade")) {
            throw new WebSocketException("connection failed: missing header field in server handshake: connection");
        }
    }
    
    public void verifyServerStatusLine(final String statusLine) throws WebSocketException {
        final int statusCode = Integer.valueOf(statusLine.substring(9, 12));
        if (statusCode == 407) {
            throw new WebSocketException("connection failed: proxy authentication not supported");
        }
        if (statusCode == 404) {
            throw new WebSocketException("connection failed: 404 not found");
        }
        if (statusCode != 101) {
            throw new WebSocketException("connection failed: unknown status code " + statusCode);
        }
    }
    
    private String createNonce() {
        final byte[] nonce = new byte[16];
        for (int i = 0; i < 16; ++i) {
            nonce[i] = (byte)this.rand(0, 255);
        }
        return this.encodeBase64String(nonce);
    }
    
    private String encodeBase64String(final byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }
    
    private String generateHeader(final LinkedHashMap<String, String> headers) {
        String header = new String();
        for (final String fieldName : headers.keySet()) {
            header = header + fieldName + ": " + headers.get(fieldName) + "\r\n";
        }
        return header;
    }
    
    private int rand(final int min, final int max) {
        final int rand = (int)(Math.random() * max + min);
        return rand;
    }
}
