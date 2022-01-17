package dev.truewinter.framed.events;

import org.json.JSONObject;

public interface FoundDeviceEvent {
    public void onFoundDevice(final String installId, final JSONObject data);
}
