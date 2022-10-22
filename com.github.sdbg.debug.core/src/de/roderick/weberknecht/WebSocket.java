// 
// Decompiled by Procyon v0.5.36
// 

package de.roderick.weberknecht;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

public class WebSocket
{
    private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final int VERSION = 13;
    static final byte OPCODE_TEXT = 1;
    static final byte OPCODE_BINARY = 2;
    static final byte OPCODE_CLOSE = 8;
    static final byte OPCODE_PING = 9;
    static final byte OPCODE_PONG = 10;
    private URI url;
    private String host;
    private int port;
    private String path;
    private WebSocketEventHandler eventHandler;
    private volatile boolean connected;
    private Socket socket;
    private DataInputStream input;
    private PrintStream output;
    private WebSocketReceiver receiver;
    private WebSocketHandshake handshake;
    private final Random random;
    
    public static int getVersion() {
        return 13;
    }
    
    public WebSocket(final String host, final int port, final String path) {
        this.url = null;
        this.eventHandler = null;
        this.connected = false;
        this.socket = null;
        this.input = null;
        this.output = null;
        this.receiver = null;
        this.handshake = null;
        this.random = new Random();
        this.host = host;
        this.port = port;
        this.path = path;
        this.handshake = new WebSocketHandshake(host, port, path, null, null);
    }
    
    public WebSocket(final URI url) throws WebSocketException {
        this(url, null, null);
    }
    
    public WebSocket(final URI url, final String protocol) throws WebSocketException {
        this(url, protocol, null);
    }
    
    public WebSocket(final URI url, final String protocol, final Map<String, String> extraHeaders) throws WebSocketException {
        this.url = null;
        this.eventHandler = null;
        this.connected = false;
        this.socket = null;
        this.input = null;
        this.output = null;
        this.receiver = null;
        this.handshake = null;
        this.random = new Random();
        this.url = url;
        this.handshake = new WebSocketHandshake(url.getHost(), url.getPort(), url.getPath(), protocol, extraHeaders);
    }
    
    public synchronized void close() throws WebSocketException {
        if (!this.connected) {
            return;
        }
        this.sendCloseHandshake();
        if (this.receiver.isRunning()) {
            this.receiver.stopit();
        }
        this.closeStreams();
        this.eventHandler.onClose();
    }
    
    public void connect() throws WebSocketException {
        try {
            if (this.connected) {
                throw new WebSocketException("already connected");
            }
            this.socket = this.createSocket();
            this.input = new DataInputStream(this.socket.getInputStream());
            (this.output = new PrintStream(this.socket.getOutputStream())).write(this.handshake.getHandshake());
            boolean handshakeComplete = false;
            final int len = 1000;
            byte[] buffer = new byte[len];
            int pos = 0;
            final ArrayList<String> handshakeLines = new ArrayList<String>();
            while (!handshakeComplete) {
                final int b = this.input.read();
                buffer[pos] = (byte)b;
                ++pos;
                if (buffer[pos - 1] == 10 && buffer[pos - 2] == 13) {
                    final String line = new String(buffer, "UTF-8");
                    if (line.trim().equals("")) {
                        handshakeComplete = true;
                    }
                    else {
                        handshakeLines.add(line.trim());
                    }
                    buffer = new byte[len];
                    pos = 0;
                }
            }
            this.handshake.verifyServerStatusLine(handshakeLines.get(0));
            handshakeLines.remove(0);
            final HashMap<String, String> headers = new HashMap<String, String>();
            for (final String line2 : handshakeLines) {
                final String[] keyValue = line2.split(": ", 2);
                headers.put(keyValue[0].toLowerCase(), keyValue[1]);
            }
            this.handshake.verifyServerHandshakeHeaders(headers);
            (this.receiver = new WebSocketReceiver(this.input, this)).start();
            this.connected = true;
            this.eventHandler.onOpen();
        }
        catch (WebSocketException wse) {
            throw wse;
        }
        catch (IOException ioe) {
            throw new WebSocketException("error while connecting: " + ioe.getMessage(), ioe);
        }
    }
    
    public WebSocketEventHandler getEventHandler() {
        return this.eventHandler;
    }
    
    public void handleReceiverError() {
        try {
            if (this.connected) {
                this.close();
            }
        }
        catch (WebSocketException wse) {
            wse.printStackTrace();
        }
    }
    
    public synchronized void send(final String data) throws WebSocketException {
        if (!this.connected) {
            throw new WebSocketException("error while sending text data: not connected");
        }
        try {
            this.sendFrame((byte)1, true, data.getBytes("UTF-8"));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void setEventHandler(final WebSocketEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }
    
    private void closeStreams() throws WebSocketException {
        try {
            this.input.close();
            this.output.close();
            this.socket.close();
        }
        catch (IOException ioe) {
            throw new WebSocketException("error while closing websocket connection: ", ioe);
        }
    }
    
    private Socket createSocket() throws WebSocketException {
        final String scheme = (this.url == null) ? "ws" : this.url.getScheme();
        final String host = (this.url == null) ? this.host : this.url.getHost();
        int port = (this.url == null) ? this.port : this.url.getPort();
        Socket socket = null;
        if (scheme != null && scheme.equals("ws")) {
            if (port == -1) {
                port = 80;
            }
            try {
                socket = new Socket(host, port);
                return socket;
            }
            catch (UnknownHostException uhe) {
                throw new WebSocketException("unknown host: " + host, uhe);
            }
            catch (IOException ioe) {
                throw new WebSocketException("error while creating socket to " + ((this.url == null) ? this.path : this.url), ioe);
            }
        }
        if (scheme != null && scheme.equals("wss")) {
            if (port == -1) {
                port = 443;
            }
            try {
                final SocketFactory factory = SSLSocketFactory.getDefault();
                socket = factory.createSocket(host, port);
                return socket;
            }
            catch (UnknownHostException uhe) {
                throw new WebSocketException("unknown host: " + host, uhe);
            }
            catch (IOException ioe) {
                throw new WebSocketException("error while creating secure socket to " + ((this.url == null) ? this.path : this.url), ioe);
            }
        }
        throw new WebSocketException("unsupported protocol: " + scheme);
    }
    
    private byte[] generateMask() {
        final byte[] mask = new byte[4];
        this.random.nextBytes(mask);
        return mask;
    }
    
    private byte[] intToByteArray(final int number) {
        final byte[] bytes = ByteBuffer.allocate(4).putInt(number).array();
        return bytes;
    }
    
    private synchronized void sendCloseHandshake() throws WebSocketException {
        if (!this.connected) {
            throw new WebSocketException("error while sending close handshake: not connected");
        }
        if (!this.connected) {
            throw new WebSocketException("error while sending close");
        }
        try {
            this.sendFrame((byte)8, true, new byte[0]);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        this.connected = false;
    }
    
    private synchronized void sendFrame(final byte opcode, final boolean masking, final byte[] data) throws WebSocketException, IOException {
        int headerLength = 2;
        if (masking) {
            headerLength += 4;
        }
        final ByteArrayOutputStream frame = new ByteArrayOutputStream(data.length + headerLength);
        final byte fin = -128;
        final byte startByte = (byte)(fin | opcode);
        frame.write(startByte);
        int length = data.length;
        int length_field = 0;
        if (length < 126) {
            if (masking) {
                length |= 0x80;
            }
            frame.write((byte)length);
        }
        else if (length <= 65535) {
            length_field = 126;
            if (masking) {
                length_field |= 0x80;
            }
            frame.write((byte)length_field);
            final byte[] lengthBytes = this.intToByteArray(length);
            frame.write(lengthBytes[2]);
            frame.write(lengthBytes[3]);
        }
        else {
            length_field = 127;
            if (masking) {
                length_field |= 0x80;
            }
            frame.write((byte)length_field);
            frame.write(new byte[] { 0, 0, 0, 0 });
            frame.write(this.intToByteArray(length));
        }
        byte[] mask = null;
        if (masking) {
            mask = this.generateMask();
            frame.write(mask);
            for (int i = 0; i < data.length; ++i) {
                final int n = i;
                data[n] ^= mask[i % 4];
            }
        }
        frame.write(data);
        this.output.write(frame.toByteArray());
        this.output.flush();
    }
}
