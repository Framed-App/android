package dev.truewinter.framed.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import dev.truewinter.framed.R;

public class DeviceInfoDialog extends DialogFragment {
    private String id;
    private JSONObject deviceInfo;

    public DeviceInfoDialog(String id, JSONObject deviceInfo) {
        this.id = id;
        this.deviceInfo = deviceInfo;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        try {
            builder.setMessage(String.format(Locale.ENGLISH, "IP: %s\nPort: %d\nVersion: %s\nMin Client Version: %s\nID: %s",
                    this.deviceInfo.getString("ip"),
                    this.deviceInfo.getInt("port"),
                    this.deviceInfo.getString("version"),
                    this.deviceInfo.getString("minClientVersion"),
                    this.id
            )).setTitle(this.deviceInfo.getString("hostname"))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
            // Create the AlertDialog object and return it
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return builder.create();
    }
}
