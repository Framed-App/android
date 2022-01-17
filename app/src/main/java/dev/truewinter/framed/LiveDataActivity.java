package dev.truewinter.framed;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import dev.truewinter.framed.adapters.DiagnosticsAdapter;
import dev.truewinter.framed.dialogs.LiveDataHelpDialog;
import dev.truewinter.framed.events.ReceivedDataEvent;
import dev.truewinter.framed.events.SocketExceptionEvent;
import dev.truewinter.framed.events.TimeoutDisconnectEvent;

public class LiveDataActivity extends AppCompatActivity implements DiagnosticsAdapter.ItemClickListener {
    private String id;
    private JSONObject deviceData;
    private DiagnosticsAdapter diagnosticsAdapter;
    private Map<String, JSONObject> conDiagData = new TreeMap<>(Collections.reverseOrder());
    private ClientSocket clientSocket;
    private Thread networkThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_data);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.action_bar_with_text);

        this.id = getIntent().getExtras().getString("id");

        try {
            this.deviceData = new JSONObject(getIntent().getExtras().getString("json"));

            TextView actionBarTitle = findViewById(R.id.actionBarTitle);
            actionBarTitle.setText(deviceData.getString("hostname"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        TextView versionText = findViewById(R.id.versionTextLDA);
        versionText.setText(String.format("v%s", BuildConfig.VERSION_NAME));

        RecyclerView recyclerView = findViewById(R.id.diagList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        diagnosticsAdapter = new DiagnosticsAdapter(this, conDiagData);
        diagnosticsAdapter.setClickListener(this);
        recyclerView.setAdapter(diagnosticsAdapter);

        Button helpBtn = findViewById(R.id.ldaHelpBtn);
        helpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LiveDataHelpDialog liveDataHelpDialog = new LiveDataHelpDialog();
                liveDataHelpDialog.show(getSupportFragmentManager(), "ldaHelp");
            }
        });

        networkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    clientSocket = new ClientSocket(
                            id,
                            deviceData.getString("publicKey"),
                            InetAddress.getByName(deviceData.getString("ip")),
                            deviceData.getInt("port")
                    );

                    clientSocket.setEventListener(new ReceivedDataEvent() {
                        @Override
                        public void onReceivedData(final String installId, final JSONObject data) {
                            //System.out.println(String.format("Received data from %s", installId));

                            try {
                                if (!data.has("data")) return;
                                if (data.get("data").toString().equals("{}")) return;

                                switch (data.getString("messageType")) {
                                    case "PerfData":
                                        //System.out.println(data.toString());

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    JSONObject sys = data.getJSONObject("data")
                                                            .getJSONObject("system");

                                                    Double cpuPercentage = Math.round(
                                                            sys.getJSONObject("cpu")
                                                                .getDouble("percentage")
                                                                * 100.0) / 100.0;

                                                    String mem = Utils.humanReadableByteCount(
                                                            sys.getJSONObject("memory")
                                                                .getLong("memUsed")
                                                    );

                                                    String diskRead = Utils.humanReadableByteCount(
                                                            sys.getJSONObject("disk")
                                                                .getLong("read")
                                                    );

                                                    String diskWrite = Utils.humanReadableByteCount(
                                                            sys.getJSONObject("disk")
                                                                    .getLong("write")
                                                    );

                                                    String download = Utils.humanReadableBitCount(
                                                            sys.getJSONObject("network")
                                                                .getLong("inBytes")
                                                            * 8
                                                    );

                                                    String upload = Utils.humanReadableBitCount(
                                                            sys.getJSONObject("network")
                                                                    .getLong("outBytes")
                                                                    * 8
                                                    );

                                                    TextView cpuData = findViewById(R.id.cpuData);
                                                    cpuData.setText(String.format(Locale.ENGLISH, "%.2f%%", cpuPercentage));

                                                    TextView memData = findViewById(R.id.memoryData);
                                                    memData.setText(mem);

                                                    TextView diskReadData = findViewById(R.id.diskReadData);
                                                    diskReadData.setText(String.format(Locale.ENGLISH, "%s/s", diskRead));

                                                    TextView diskWriteData = findViewById(R.id.diskWriteData);
                                                    diskWriteData.setText(String.format(Locale.ENGLISH, "%s/s", diskWrite));

                                                    TextView downloadData = findViewById(R.id.downloadData);
                                                    downloadData.setText(String.format(Locale.ENGLISH, "%sps", download));

                                                    TextView uploadData = findViewById(R.id.uploadData);
                                                    uploadData.setText(String.format(Locale.ENGLISH, "%sps", upload));
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        });

                                        break;
                                    case "DiagData":
                                        JSONObject diagData = data.getJSONObject("data");
                                        long diagNewestTimestamp = 0;
                                        Iterator<String> keys = diagData.keys();
                                        List<String> sortedKeys = new ArrayList<>();

                                        while(keys.hasNext()) {
                                            String key = keys.next();
                                            if (diagData.get(key) instanceof JSONObject) {
                                                if (conDiagData.containsKey(key)) return;
                                                conDiagData.put(key, diagData.getJSONObject(key));

                                                long l = Long.parseLong(key);

                                                if (diagNewestTimestamp < l) {
                                                    diagNewestTimestamp = l;
                                                }
                                            }
                                        }

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                diagnosticsAdapter.notifyDataSetChanged();
                                            }
                                        });

                                        if (clientSocket != null) {
                                            clientSocket.setLastTimestamp(diagNewestTimestamp);
                                        }
                                        break;
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    // See comments in ClientSocket
                    clientSocket.setTimeoutDisconnectListener(new TimeoutDisconnectEvent() {
                        @Override
                        public void onTimeoutDisconnect(String installId) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Toast errorToast = Toast.makeText(
                                                // Use default toast colours instead of Framed colours
                                                getApplicationContext(),
                                                String.format("No data received from %s in a while", deviceData.getString("hostname")),
                                                Toast.LENGTH_LONG);
                                        errorToast.show();
                                        finish();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    });

                    clientSocket.setSocketExceptionListener(new SocketExceptionEvent() {
                        @Override
                        public void onException(final Exception e) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Toast errorToast = Toast.makeText(
                                                // Use default toast colours instead of Framed colours
                                                getApplicationContext(),
                                                String.format("ClientSocket %s error: %s", deviceData.getString("hostname"), e.getClass().getName()),
                                                Toast.LENGTH_LONG);
                                        errorToast.show();
                                        finish();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    });
                } catch (final Exception e) {
                    e.printStackTrace();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String errorCode = e.getClass().getName();

                            try {
                                Toast errorToast = Toast.makeText(
                                        // Use default toast colours instead of Framed colours
                                        getApplicationContext(),
                                        String.format("Failed to connect to %s: %s", deviceData.getString("hostname"), errorCode),
                                        Toast.LENGTH_LONG);
                                errorToast.show();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            finish();
                        }
                    });
                }
            }
        });

        networkThread.start();
    }

    @Override
    public void onItemClick(View view, int position) {
        System.out.println(conDiagData.get(diagnosticsAdapter.getIdFromIndex(position)).toString());

        Intent intent = new Intent(this, DiagnosticsActivity.class);
        intent.putExtra("time", diagnosticsAdapter.getIdFromIndex(position));
        intent.putExtra("json", conDiagData.get(diagnosticsAdapter.getIdFromIndex(position)).toString());
        startActivity(intent);
    }

    private void destroyClientSocket() {
        try {
            if (this.clientSocket != null) {
                this.clientSocket.cancelTimers();
                this.clientSocket.disconnect();
                this.clientSocket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        destroyClientSocket();
        networkThread.interrupt();

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        destroyClientSocket();
        finish();
    }
}
