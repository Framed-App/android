package dev.truewinter.framed;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

public class ClientSocket {
    private Socket socket;
    private InetSocketAddress inetSocketAddress;
    private String installId;
    private String publicKey;
    private boolean connected = false;
    private PrintWriter socketOut;
    private BufferedReader socketIn;
    private ReceivedDataEvent receivedDataEvent;
    private TimeoutDisconnectEvent timeoutDisconnectEvent;
    private SocketExceptionEvent socketExceptionEvent;
    private Timer inactiveDisconnectTimer;
    private Timer sendMsgTimer;
    private Timer receiveTimer;
    private String key = Utils.getRandomString(32);
    private int lastMsgSeconds = 0;
    private long lastTimestamp = 0;
    private final int TIMEOUT = 1000;
    private final String DELIMITER ="|+|";
    private final int MSG_IN_PARTS = 4;
    private final int INACTIVE_DISCONNECT = 10;

    protected ClientSocket(final String installId, final String publicKey, InetAddress serverAddress, int serverPort) throws Exception {
        this.installId = installId;
        this.publicKey = publicKey;
        this.socket = new Socket();
        this.inetSocketAddress = new InetSocketAddress(serverAddress, serverPort);
        this.socket.connect(this.inetSocketAddress, TIMEOUT);

        inactiveDisconnectTimer = new Timer();
        inactiveDisconnectTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                lastMsgSeconds++;

                if (lastMsgSeconds > INACTIVE_DISCONNECT) {

                    if (timeoutDisconnectEvent != null) {
                        timeoutDisconnectEvent.onTimeoutDisconnect(installId);
                    }

                    try {
                        socket.close();
                        connected = false;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    connected = false;

                    cancelTimers();
                }
            }
        }, 0, 1000);

        this.socketOut = new PrintWriter(this.socket.getOutputStream(),
                true);
        this.socketIn = new BufferedReader(new InputStreamReader(
                this.socket.getInputStream()));

        System.out.println("Connected to server: " + this.socket.getInetAddress());
        connected = true;

        sendKeyExchangePacket();

        sendMsgTimer = new Timer();
        sendMsgTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                        send(createGetDataJson());
                        send(createGetDiagDataJson());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 1000);

        //send(createGetDataJson());

        receiveTimer = new Timer();
        receiveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    String line = socketIn.readLine();

                    if (line == null) return;
                    if (!connected) return;

                    lastMsgSeconds = 0;

                    if (!isValid(line)) return;

                    //System.out.println("Text received: " + line);

                    if (receivedDataEvent != null) {
                        String[] parts = getPacketParts(line);
                        JSONObject j = new JSONObject(Utils.decrypt(Base64.decode(parts[3], Base64.DEFAULT), key, parts[2]));
                        receivedDataEvent.onReceivedData(installId, j);
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    if (socketExceptionEvent != null) {
                        socketExceptionEvent.onException(e);
                    }
                }
            }
        }, 0, 100);
    }

    private JSONObject createGetDataJson() throws JSONException {
        JSONObject j = new JSONObject();
        j.put("messageType", "GetPerfData");
        return j;
    }

    private JSONObject createGetDiagDataJson() throws JSONException {
        JSONObject j = new JSONObject();
        j.put("messageType", "GetDiagData");
        j.put("lastTimestamp", lastTimestamp);
        return j;
    }

    protected void setLastTimestamp(long t) {
        this.lastTimestamp = t;
    }

    private void send(JSONObject json) {
        String iv = Utils.getRandomString(16);

        try {
            String encrypted = Utils.encrypt(json, key, iv);

            String packet = this.createPacket(encrypted, iv);
            this.socketOut.println(packet);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private String createPacket(String data, String iv) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Framed");
        stringBuilder.append(DELIMITER);
        stringBuilder.append(this.installId);
        stringBuilder.append(DELIMITER);
        stringBuilder.append(iv);
        stringBuilder.append(DELIMITER);
        stringBuilder.append(data);

        return stringBuilder.toString();
    }

    private void sendKeyExchangePacket() {
        try {
            JSONObject j = new JSONObject();
            j.put("messageType", "KeyExchange");
            j.put("key", key);

            String decodedPublicKey = new String(Base64.decode(this.publicKey, Base64.DEFAULT), StandardCharsets.UTF_8);

            String encryptedJ = Utils.encryptRSA(j.toString(), decodedPublicKey);

            String packet = createPacket(encryptedJ, "KeyExchange");

            this.socketOut.println(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void disconnect() throws IOException {
        if (!connected) return;

        connected = false;

        if (this.socket.isConnected()) {
            this.socket.close();
        }
    }

    protected void cancelTimers() {
        sendMsgTimer.cancel();
        receiveTimer.cancel();
        inactiveDisconnectTimer.cancel();
    }

    protected void setEventListener(ReceivedDataEvent r) {
        this.receivedDataEvent = r;
    }

    protected void setTimeoutDisconnectListener(TimeoutDisconnectEvent t) {
        this.timeoutDisconnectEvent = t;
    }

    protected void setSocketExceptionListener(SocketExceptionEvent s) {
        this.socketExceptionEvent = s;
    }

    private String[] getPacketParts(String packet) {
        return packet.split(Pattern.quote(DELIMITER));
    }

    private boolean isValid(String packet) {
        String[] parts = getPacketParts(packet);
        if (parts.length != MSG_IN_PARTS) return false;
        if (!parts[0].equals("Framed")) return false;
        // If we somehow get a packet not from the connected Framed app, ignore
        if (!parts[1].equals(this.installId)) return false;
        if (parts[2].length() == 0) return false;

        try {
            JSONObject j = new JSONObject(Utils.decrypt(Base64.decode(parts[3], Base64.DEFAULT), key, parts[2]));

            if (!j.has("messageType")) return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
