package dev.truewinter.framed;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements DevicesAdapter.ItemClickListener {
    private boolean searching = false;
    private Map<String, JSONObject> devicesFound = new HashMap<>();
    private DevicesAdapter devicesAdapter;
    private SearchRunnable searchRunnable;
    private Thread networkThread;
    private final String MIN_SERVER_VERSION = "0.0.5";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        try {
            getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            getSupportActionBar().setDisplayShowCustomEnabled(true);
            getSupportActionBar().setCustomView(R.layout.action_bar);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        TextView versionText = findViewById(R.id.versionText);
        versionText.setText(String.format("v%s", BuildConfig.VERSION_NAME));

        RecyclerView recyclerView = findViewById(R.id.deviceList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        devicesAdapter = new DevicesAdapter(this, devicesFound);
        devicesAdapter.setClickListener(this);
        recyclerView.setAdapter(devicesAdapter);

        findViewById(R.id.searchTryAgainBtn).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (searching) return;

                devicesFound.clear();
                devicesAdapter.notifyDataSetChanged();

                searchStarted();
                doSearch();
            }
        });

        findViewById(R.id.searchingHelpBtn).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SearchingHelpDialog searchingHelpDialog = new SearchingHelpDialog();
                searchingHelpDialog.show(getFragmentMgr(), "searchHelp");
            }
        });

        doSearch();
    }

    public FragmentManager getFragmentMgr() {
        return this.getSupportFragmentManager();
    }

    @Override
    public void onItemClick(View view, int position) {
        stopSearch();

        try {
            JSONObject j = devicesFound.get(devicesAdapter.getIdFromIndex(position));
            if (!j.getBoolean("_compatible")) {
                if (j.getString("_incompatibleSide").equals("server")) {
                    Toast.makeText(
                            getApplicationContext(),
                            String.format(
                                    "Server is out of date, please update the desktop app to at least v%s",
                                    MIN_SERVER_VERSION
                            ), Toast.LENGTH_LONG).show();
                } else if (j.getString("_incompatibleSide").equals("client")) {
                    Toast.makeText(
                            getApplicationContext(),
                            String.format(
                                    "Client is out of date, please update the mobile app to at least v%s",
                                    j.getString("minClientVersion")
                            ), Toast.LENGTH_LONG).show();
                }

                return;
            }

            Intent intent = new Intent(this, LiveDataActivity.class);
            intent.putExtra("id", devicesAdapter.getIdFromIndex(position));
            intent.putExtra("json", devicesFound.get(devicesAdapter.getIdFromIndex(position)).toString());
            startActivity(intent);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Failed to check version compatibility", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLongClick(View view, int position) {
        DeviceInfoDialog dialog = new DeviceInfoDialog(devicesAdapter.getIdFromIndex(position),
                devicesFound.get(devicesAdapter.getIdFromIndex(position)));
        dialog.show(this.getSupportFragmentManager(), "dialog");
    }

    private void searchStarted() {
        TextView searchingText = findViewById(R.id.searchingText);
        TextView searchingTextFound = findViewById(R.id.searchingTextFound);
        ProgressBar searchingProgressBar = findViewById(R.id.searchingProgressBar);
        Button searchTryAgainBtn = findViewById(R.id.searchTryAgainBtn);

        searchingTextFound.setVisibility(View.INVISIBLE);
        searchTryAgainBtn.setVisibility(View.INVISIBLE);

        searchingTextFound.setText(R.string.searchingFoundApps);
        searchingText.setVisibility(View.VISIBLE);
        searchingProgressBar.setVisibility(View.VISIBLE);
    }

    private void searchStopped() {
        TextView searchingText = findViewById(R.id.searchingText);
        TextView searchingTextFound = findViewById(R.id.searchingTextFound);
        ProgressBar searchingProgressBar = findViewById(R.id.searchingProgressBar);
        Button searchTryAgainBtn = findViewById(R.id.searchTryAgainBtn);

        searchingText.setVisibility(View.INVISIBLE);
        searchingProgressBar.setVisibility(View.INVISIBLE);

        searchingTextFound.setText(searchingTextFound.getText().toString().replace("{{count}}", Integer.toString(devicesFound.size())));
        searchingTextFound.setVisibility(View.VISIBLE);
        searchTryAgainBtn.setVisibility(View.VISIBLE);
    }

    private void doSearch() {
        if (!isNetworkAvailable()) {
            Toast.makeText(getApplicationContext(), "Network connection not available", Toast.LENGTH_LONG).show();
            searchStopped();
            return;
        }

        searching = true;
        searchRunnable = new SearchRunnable();

        searchRunnable.setEventListener(new FoundDeviceEvent() {
            @Override
            public void onFoundDevice(final String installId, final JSONObject data) {
                if (!devicesFound.containsKey(installId)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                boolean compatible = true;
                                String incompatibleSide = "";
                                if (data.getString("version").compareTo(MIN_SERVER_VERSION) < 0) {
                                    compatible = false;
                                    incompatibleSide = "server";
                                } else if (data.getString("minClientVersion").compareTo(BuildConfig.VERSION_NAME) > 0) {
                                    compatible = false;
                                    incompatibleSide = "client";
                                }

                                data.put("_compatible", compatible);
                                data.put("_incompatibleSide", incompatibleSide);

                                devicesFound.put(installId, data);
                                devicesAdapter.notifyDataSetChanged();
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Toast.makeText(getApplicationContext(), "Failed to check version compatibility", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });

        searchRunnable.setDoneEventListener(new DoneFindingDevicesEvent() {
            @Override
            public void onFinishedEvent() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stopSearch();
                    }
                });
            }
        });

        networkThread = new Thread(searchRunnable);
        networkThread.start();
    }

    private void stopSearch() {
        System.out.println("Stopping search thread");
        searchRunnable.cancel();
        networkThread.interrupt();
        searching = false;

        searchStopped();
    }


    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
