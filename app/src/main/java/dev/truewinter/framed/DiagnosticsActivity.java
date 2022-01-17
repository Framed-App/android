package dev.truewinter.framed;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dev.truewinter.framed.adapters.DiagnosticsPingsAdapter;
import dev.truewinter.framed.dialogs.DiagnosticsHelpDialog;

public class DiagnosticsActivity extends AppCompatActivity {
    private String timestamp;
    private JSONObject diagData;
    private DiagnosticsPingsAdapter diagnosticsPingsAdapter;
    private List<JSONObject> pingSet = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagnostics);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.action_bar_with_text);

        this.timestamp = getIntent().getExtras().getString("time");
        Date date = new Date(Long.parseLong(timestamp));
        String dateTime = new SimpleDateFormat("D MMM YYYY h:mm:ss a").format(date);

        TextView actionBarTitle = findViewById(R.id.actionBarTitle);
        actionBarTitle.setText(dateTime);

        TextView version = findViewById(R.id.diagVersionText);
        version.setText(String.format(Locale.ENGLISH, "v%s", BuildConfig.VERSION_NAME));

        RecyclerView recyclerView = findViewById(R.id.diagPingsList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        diagnosticsPingsAdapter = new DiagnosticsPingsAdapter(this, pingSet);
        //diagnosticsPingsAdapter.setClickListener(this);
        recyclerView.setAdapter(diagnosticsPingsAdapter);

        Button helpBtn = findViewById(R.id.diagHelpBtn);
        helpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DiagnosticsHelpDialog diagnosticsHelpDialog = new DiagnosticsHelpDialog();
                diagnosticsHelpDialog.show(getSupportFragmentManager(), "diagHelp");
            }
        });

        try {
            this.diagData = new JSONObject(getIntent().getExtras().getString("json"));

            JSONObject sys = this.diagData.getJSONObject("system");

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

            TextView cpuData = findViewById(R.id.diagCpuData);
            cpuData.setText(String.format(Locale.ENGLISH, "%.2f%%", cpuPercentage));

            TextView memData = findViewById(R.id.diagMemoryData);
            memData.setText(mem);

            TextView diskReadData = findViewById(R.id.diagDiskReadData);
            diskReadData.setText(String.format(Locale.ENGLISH, "%s/s", diskRead));

            TextView diskWriteData = findViewById(R.id.diagDiskWriteData);
            diskWriteData.setText(String.format(Locale.ENGLISH, "%s/s", diskWrite));

            TextView downloadData = findViewById(R.id.diagDownloadData);
            downloadData.setText(String.format(Locale.ENGLISH, "%sps", download));

            TextView uploadData = findViewById(R.id.diagUploadData);
            uploadData.setText(String.format(Locale.ENGLISH, "%sps", upload));

            TextView frameData = findViewById(R.id.diagFrameData);
            frameData.setText(String.format(Locale.ENGLISH, "%d", diagData.getInt("frames")));


            JSONObject pings = diagData.getJSONObject("pings");

            if (pings.has("google")) {
                JSONObject n = new JSONObject();
                n.put("name", "Google");
                n.put("ping", pings.get("google"));
                pingSet.add(n);
            }

            if (pings.has("framed")) {
                JSONObject n = new JSONObject();
                n.put("name", "Framed");
                n.put("ping", pings.get("framed"));
                pingSet.add(n);
            }

            if (pings.has("truewinter")) {
                JSONObject n = new JSONObject();
                n.put("name", "TrueWinter");
                n.put("ping", pings.get("truewinter"));
                pingSet.add(n);
            }

            JSONArray twitch = pings.getJSONArray("twitch");

            for (int i = 0; i < twitch.length(); i++) {
                JSONObject j = twitch.getJSONObject(i);
                JSONObject n = new JSONObject();
                n.put("name", String.format("Twitch: %s", j.getString("name")));
                n.put("ping", j.getDouble("average"));
                pingSet.add(n);
            }

            diagnosticsPingsAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
