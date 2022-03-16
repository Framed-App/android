package dev.truewinter.framed.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import dev.truewinter.framed.R;

public class SceneSwitcherHelpDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("The scene switcher allows you to switch between scenes in your streaming software directly from the Framed mobile app.");
        stringBuilder.append("\n\n");
        stringBuilder.append("Unfortunately, some features are not yet fully implemented. If using Streamlabs, you won't see the current scene icon, and studio mode is not supported.");

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
