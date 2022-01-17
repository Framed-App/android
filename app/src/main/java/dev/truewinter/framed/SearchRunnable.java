package dev.truewinter.framed;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import dev.truewinter.framed.events.DoneFindingDevicesEvent;
import dev.truewinter.framed.events.FoundDeviceEvent;

public class SearchRunnable implements Runnable {
    private final Timer receiveTimer = new Timer();
    private Timer stopTimer = new Timer();
    private FoundDeviceEvent foundDeviceEvent;
    private DoneFindingDevicesEvent doneFindingDevicesEvent;

    @Override
    public void run() {
        try {
            final InetAddress group = InetAddress.getByName("228.182.166.121");
            final MulticastSocket s = new MulticastSocket(19555);
            s.joinGroup(group);

            byte[] buf = new byte[3000];
            final DatagramPacket recv = new DatagramPacket(buf, buf.length);

            receiveTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        s.receive(recv);
                        String packetData = new String(recv.getData(), recv.getOffset(), recv.getLength());
                        // |+| is not likely to every be part of the data, so it is used as the delimiter
                        final String[] dataParts = packetData.split(Pattern.quote("|+|"));

                        if (dataParts.length != 3) return;
                        if (!dataParts[0].equals("Framed")) return;

                        try {
                            final JSONObject json = new JSONObject(dataParts[2]);

                            if (json.getString("messageType").equals("identify")) {
                                // Remove publicKey and sig. Done using regex to ensure the JSON order stays as-is.
                                // This is important as JSON objects are not ordered, and any change to the JSON order
                                // would render the signature invalid.
                                String regexTemplate = ",?\"***\":\"[a-zA-Z0-9=_\\-+\\/]+\"";
                                String toValidate = dataParts[2]
                                        .replaceAll(regexTemplate.replace("***", "publicKey"), "")
                                        .replaceAll(regexTemplate.replace("***", "sig"), "");

                                String sig = json.getString("sig");
                                String publicKey = json.getString("publicKey");

                                try {
                                    boolean valid = Utils.verifySignature(sig, toValidate, new String(Base64.decode(publicKey, Base64.DEFAULT)));

                                    if (valid && foundDeviceEvent != null) {
                                        foundDeviceEvent.onFoundDevice(dataParts[1], json);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        System.out.println("[" + recv.getAddress() + "] " + packetData);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, 0, 100);

            stopTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    receiveTimer.cancel();

                    if (doneFindingDevicesEvent != null) {
                        doneFindingDevicesEvent.onFinishedEvent();
                    }
                    try {
                        s.leaveGroup(group);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, 5 * 1000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cancel() {
        stopTimer.cancel();
        receiveTimer.cancel();
    }

    public void setEventListener(FoundDeviceEvent f) {
        this.foundDeviceEvent = f;
    }

    public void setDoneEventListener(DoneFindingDevicesEvent d) {
        this.doneFindingDevicesEvent = d;
    }
}
