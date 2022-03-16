package dev.truewinter.framed.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import dev.truewinter.framed.LicensesActivity;
import dev.truewinter.framed.MainActivity;
import dev.truewinter.framed.R;

public class SearchingHelpDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("The device list shows the names of all devices on your local network with the Framed desktop app open.");
        stringBuilder.append("\n\n");
        stringBuilder.append("Not seeing your device? Make sure your phone is on the same network as your computer, and that Framed is up to date on both.");

        builder.setMessage(stringBuilder).setTitle(R.string.helpHeading)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                }).setNeutralButton("View Licenses", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(getActivity(), LicensesActivity.class);
                        startActivity(intent);
                    }
                });

        // Create the AlertDialog object and return it
        return builder.create();
    }
}
