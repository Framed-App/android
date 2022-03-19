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
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.regex.Pattern;

import dev.truewinter.framed.events.ReceivedDataEvent;
import dev.truewinter.framed.events.SocketExceptionEvent;
import dev.truewinter.framed.events.TimeoutDisconnectEvent;

public class ClientSocket {
    private Socket socket;
    private InetSocketAddress inetSocketAddress;
    private String installId;
    private String publicKey;
    private String password;
    private boolean connected = false;
    private PrintWriter socketOut;
    private BufferedReader socketIn;
    private ReceivedDataEvent receivedDataEvent;
    private TimeoutDisconnectEvent timeoutDisconnectEvent;
    private SocketExceptionEvent socketExceptionEvent;
    private Timer inactiveDisconnectTimer;
    private Timer sendMsgTimer;
    // tick = 50ms
    private Timer sendMsgNextTickTimer;
    private Timer receiveTimer;
    private String key = Utils.getRandomString(32);
    private int lastMsgSeconds = 0;
    private long lastTimestamp = 0;
    private boolean hasSuccessfulConnection = false;

    private final int TIMEOUT = 1000;
    private final String DELIMITER ="|+|";
    private final int MSG_IN_PARTS = 4;
    private final int INACTIVE_DISCONNECT = 10;

    private Map<Integer, JSONObject> messageQueue = new TreeMap<>();
    private int queueIdCounter = 0;

    protected ClientSocket(final String installId, final String publicKey, InetAddress serverAddress, int serverPort, String password) throws Exception {
        this.installId = installId;
        this.publicKey = publicKey;
        this.password = password;
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

        // Couldn't find any better way of doing this on the network thread, so a tick-based system will have to do
        sendMsgNextTickTimer = new Timer();
        sendMsgNextTickTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for (Map.Entry<Integer, JSONObject> entry : messageQueue.entrySet()) {
                    send(entry.getValue());
                    messageQueue.remove(entry.getKey());
                }
            }
        }, 0, 50);

        receiveTimer = new Timer();
        receiveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (!shouldReceive()) return;

                    String line = socketIn.readLine();

                    if (line == null) return;

                    lastMsgSeconds = 0;

                    JSONObject j = getDataIfValid(line);

                    if (j == null) return;

                    //System.out.println("Text received: " + line);

                    if (receivedDataEvent != null) {
                        if (j.getString("messageType").equals("ConnectionStatus")) {
                            if (j.getBoolean("success")) {
                                hasSuccessfulConnection = true;
                            } else {
                                String errorMsg = "An error occurred while establishing a connection with the desktop app.";
                                if (j.has("error")) {
                                    errorMsg = j.getString("error");
                                }

                                receivedDataEvent.onConnectionError(errorMsg);
                            }
                        } else if (j.getString("messageType").equals("SceneListError")) {
                            receivedDataEvent.onSceneListError(j.getString("error"));
                        } else {
                            if (!hasSuccessfulConnection) return;
                            receivedDataEvent.onReceivedData(installId, j);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    if (socketExceptionEvent != null) {
                        if (!hasSuccessfulConnection) return;
                        socketExceptionEvent.onException(e);
                    }
                }
            }
        }, 0, 100);
    }

    // To handle a weird edge case
    private boolean shouldReceive() {
        if (!connected) return false;
        if (this.socket.isInputShutdown()) return false;
        if (this.socket.isOutputShutdown()) return false;
        if (this.socket.isClosed()) return false;

        return true;
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

    private JSONObject createGetSceneListJson() throws JSONException {
        JSONObject j = new JSONObject();
        j.put("messageType", "GetSceneList");
        return j;
    }

    private JSONObject createSwitchScenesJson(String sceneName) throws  JSONException {
        JSONObject j = new JSONObject();
        j.put("messageType", "SwitchScenes");
        j.put("sceneName", sceneName);
        return j;
    }

    protected void addSceneListRequestToQueue() {
        try {
            messageQueue.put(queueIdCounter, createGetSceneListJson());
            queueIdCounter++;
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void addSceneSwitchToQueue(String sceneName) {
        try {
            messageQueue.put(queueIdCounter, createSwitchScenesJson(sceneName));
            queueIdCounter++;
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
            j.put("password", password);

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

        if (!this.socket.isInputShutdown()) {
            this.socket.shutdownInput();
        }

        if (!this.socket.isOutputShutdown()) {
            this.socket.shutdownOutput();
        }

        if (!this.socket.isClosed()) {
            this.socket.close();
        }

        messageQueue.clear();
    }

    protected void cancelTimers() {
        sendMsgTimer.cancel();
        sendMsgNextTickTimer.cancel();
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

    private JSONObject getDataIfValid(String packet) {
        String[] parts = getPacketParts(packet);
        if (parts.length != MSG_IN_PARTS) return null;
        if (!parts[0].equals("Framed")) return null;
        // If we somehow get a packet not from the connected Framed app, ignore
        if (!parts[1].equals(this.installId)) return null;
        if (parts[2].length() == 0) return null;

        JSONObject j;

        try {
            j = new JSONObject(Utils.decrypt(Base64.decode(parts[3], Base64.DEFAULT), key, parts[2]));

            if (!j.has("messageType")) return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return j;
    }
}
