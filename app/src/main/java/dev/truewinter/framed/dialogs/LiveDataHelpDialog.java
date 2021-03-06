package dev.truewinter.framed.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import dev.truewinter.framed.R;

public class LiveDataHelpDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("This shows the data received from the Framed desktop app, and allows you to access the scene switcher.");
        stringBuilder.append("\n\n");
        stringBuilder.append("If you cannot open the scene switcher, ensure that your streaming software is open, and that Framed has successfully connected to it.");
        stringBuilder.append("\n\n");
        stringBuilder.append("Some data may not be shown here, and it may take up to a second to receive the latest data.");

        builder.setMessage(stringBuilder).setTitle(R.string.helpHeading)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        // Create the AlertDialog object and return it
        return builder.create();
    }
}
