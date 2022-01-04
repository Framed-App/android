package dev.truewinter.framed;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class SearchingHelpDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("The device list shows the names of all devices on your local network with the Framed desktop app open.");
        stringBuilder.append("\n\n");
        stringBuilder.append("Not seeing your device? Make sure your phone is on the same network as your computer, and that Framed is up to date on both.");

        builder.setMessage(stringBuilder).setTitle(R.string.searchingHelpHeading)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        // Create the AlertDialog object and return it
        return builder.create();
    }
}
