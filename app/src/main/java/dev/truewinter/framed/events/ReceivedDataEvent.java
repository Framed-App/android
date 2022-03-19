package dev.truewinter.framed.events;

import org.json.JSONObject;

public interface ReceivedDataEvent {
    public void onReceivedData(String installId, JSONObject data);
    public void onConnectionError(String errorMsg);
    public void onSceneListError(String errorMsg);
}
