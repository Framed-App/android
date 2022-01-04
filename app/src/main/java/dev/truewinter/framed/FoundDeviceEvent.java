package dev.truewinter.framed;

import org.json.JSONObject;

public interface FoundDeviceEvent {
    public void onFoundDevice(final String installId, final JSONObject data);
}
