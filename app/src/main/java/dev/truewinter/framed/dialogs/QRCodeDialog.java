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
import dev.truewinter.framed.events.QRCodeDialogButtonEvent;

public class QRCodeDialog extends DialogFragment {
    private String data;
    private QRCodeDialogButtonEvent qrCodeDialogButtonEvent;

    public QRCodeDialog(String data) {
        this.data = data;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(data).setTitle("Scanned QR Code Data")
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();

                        if (qrCodeDialogButtonEvent != null) {
                            qrCodeDialogButtonEvent.onOKButtonClick();
                        }
                    }
                }).setNeutralButton("Copy", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();

                        if (qrCodeDialogButtonEvent != null) {
                            qrCodeDialogButtonEvent.onCopyButtonClick();
                        }
                    }
                });

        return builder.create();
    }

    public void setQrCodeDialogButtonListener(QRCodeDialogButtonEvent q) {
        this.qrCodeDialogButtonEvent = q;
    }
}
