package dev.truewinter.framed;

import org.json.JSONObject;

public interface ReceivedDataEvent {
    public void onReceivedData(String installId, JSONObject data);
}
