package dev.truewinter.framed;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;

import dev.truewinter.framed.dialogs.QRCodeDialog;
import dev.truewinter.framed.events.QRCodeDialogButtonEvent;

public class ScannerActivity extends AppCompatActivity {
    private DecoratedBarcodeView barcodeView;
    private final int PERMISSIONS_REQUEST_CAMERA = 1;
    private String id;
    private JSONObject deviceData;
    private String fingerprint;
    private Timer limitToastTimer = new Timer();
    private boolean ignoreQRInvalid = false;
    private boolean enableGenericQRCodeScanner = false;
    private boolean debug = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.action_bar_with_text);
        TextView actionBarTitle = findViewById(R.id.actionBarTitle);
        actionBarTitle.setText("Scan QR Code");

        this.id = getIntent().getExtras().getString("id");
        this.debug = getIntent().getExtras().getBoolean("debug");

        if (this.debug) {
            toggleGenericQR();
        }

        // Added for debugging purposes
        actionBarTitle.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (debug) return false;

                toggleGenericQR();

                return false;
            }
        });

        try {
            if (!this.debug) {
                this.deviceData = new JSONObject(getIntent().getExtras().getString("json"));

                this.fingerprint = Utils.getKeyFingerprint(new String(Base64.decode(this.deviceData.getString("publicKey"), Base64.DEFAULT)));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),
                    String.format("An error occurred: %s", e.getClass().getName()),
                    Toast.LENGTH_LONG).show();
            finish();
        }

        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA},
                    PERMISSIONS_REQUEST_CAMERA);
        } else {
            startBarcodeScanner();
        }

        EditText codeEntry = findViewById(R.id.codeEntry);
        Button continueBtn = findViewById(R.id.scannerInputContinueBtn);

        continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (debug) {
                    Toast.makeText(getApplicationContext(), "Debug mode, ignoring input", Toast.LENGTH_LONG).show();
                    return;
                }

                System.out.println(codeEntry.getText().toString());
                System.out.println(fingerprint);
                System.out.println(codeEntry.getText().toString().contains("-"));

                if (!codeEntry.getText().toString().contains("-")) {
                    Toast.makeText(getApplicationContext(), "Invalid code. Please ensure that the full code is entered, not only part of it.", Toast.LENGTH_LONG).show();
                    return;
                }

                String[] codeParts = codeEntry.getText().toString().split("-");

                if (codeParts[0].toUpperCase().equals(fingerprint)) {
                    handleContinue(codeParts[1]);
                } else {
                    Toast.makeText(getApplicationContext(), "Invalid code", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void toggleGenericQR() {
        TextView actionBarTitle = findViewById(R.id.actionBarTitle);
        if (!enableGenericQRCodeScanner) {
            enableGenericQRCodeScanner = true;
            actionBarTitle.setTextColor(getResources().getColor(R.color.colorPrimaryDark, getTheme()));
            Toast.makeText(getApplicationContext(), "Enabled generic QR code scanner", Toast.LENGTH_LONG).show();
        } else {
            enableGenericQRCodeScanner = false;
            actionBarTitle.setTextColor(getResources().getColor(R.color.colorPrimary, getTheme()));
            Toast.makeText(getApplicationContext(), "Disabled generic QR code scanner", Toast.LENGTH_LONG).show();
        }
    }

    private void startBarcodeScanner() {
        System.out.println("Starting barcode scanner");

        // https://stackoverflow.com/a/59991880
        barcodeView = findViewById(R.id.barcode_scanner);
        Collection<BarcodeFormat> formats = Arrays.asList(BarcodeFormat.QR_CODE);
        barcodeView.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(formats));
        barcodeView.getViewFinder().setBackgroundColor(Color.TRANSPARENT);
        barcodeView.initializeFromIntent(getIntent());
        barcodeView.setStatusText("");
        barcodeView.decodeContinuous(callback);
    }

    private void handleContinue(String password) {
        if (debug) {
            Toast.makeText(getApplicationContext(), "Debug mode, ignoring continue", Toast.LENGTH_LONG).show();
            return;
        }

        barcodeView.pause();
        Intent intent = new Intent(ScannerActivity.this, LiveDataActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("id", id);
        intent.putExtra("json", deviceData.toString());
        intent.putExtra("password", password);
        startActivity(intent);
        finish();
    }

    private BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            System.out.println(String.format("Generic QR code result: %b", enableGenericQRCodeScanner));
            if (!result.getText().contains("-")) {
                Toast.makeText(getApplicationContext(), "Invalid code", Toast.LENGTH_LONG).show();

                ignoreQRInvalid = true;

                limitToastTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        ignoreQRInvalid = false;
                    }
                }, 4000);

                return;
            }

            String[] codeParts = result.getText().split("-");

            if (!enableGenericQRCodeScanner && codeParts[0].equals(fingerprint)) {
                handleContinue(codeParts[1]);
            } else {
                if (ignoreQRInvalid) return;

                if (!enableGenericQRCodeScanner) {
                    Toast.makeText(getApplicationContext(), "Failed to verify QR code", Toast.LENGTH_LONG).show();

                    ignoreQRInvalid = true;

                    limitToastTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            ignoreQRInvalid = false;
                        }
                    }, 4000);
                } else {
                    ignoreQRInvalid = true;
                    barcodeView.pause();

                    QRCodeDialog qrCodeDialog = new QRCodeDialog(result.getText());
                    qrCodeDialog.show(getSupportFragmentManager(), "qrCodeDialog");
                    qrCodeDialog.setQrCodeDialogButtonListener(new QRCodeDialogButtonEvent() {
                        @Override
                        public void onButtonClick() {
                            ignoreQRInvalid = false;
                            barcodeView.resume();
                        }
                    });
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        limitToastTimer.cancel();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (barcodeView != null) barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (barcodeView != null) barcodeView.pause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CAMERA: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startBarcodeScanner();
                } else {
                    findViewById(R.id.barcode_scanner).setVisibility(View.INVISIBLE);
                    findViewById(R.id.cameraPermissionDenied).setVisibility(View.VISIBLE);
                }
            }
        }
    }
}
