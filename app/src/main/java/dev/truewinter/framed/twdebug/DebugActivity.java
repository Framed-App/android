package dev.truewinter.framed.twdebug;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.LinkedHashSet;
import java.util.Set;

import dev.truewinter.framed.LicensesActivity;
import dev.truewinter.framed.R;
import dev.truewinter.framed.ScannerActivity;
import dev.truewinter.framed.dialogs.DiagnosticsHelpDialog;
import dev.truewinter.framed.dialogs.LiveDataHelpDialog;
import dev.truewinter.framed.dialogs.SearchingHelpDialog;

public class DebugActivity extends AppCompatActivity implements DebugAdapter.ItemClickListener {
    private DebugAdapter debugAdapter;
    private Set<String> debugMap = new LinkedHashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.action_bar_with_text);

        TextView actionBarTitle = findViewById(R.id.actionBarTitle);
        actionBarTitle.setText("Debug");

        RecyclerView recyclerView = findViewById(R.id.debugRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        debugAdapter = new DebugAdapter(this, debugMap);
        debugAdapter.setClickListener(this);
        recyclerView.setAdapter(debugAdapter);

        debugMap.add("DiagnosticsHelpDialog");
        debugMap.add("LiveDataHelpDialog");
        debugMap.add("SearchingHelpDialog");
        debugMap.add("LicensesActivity");
        debugMap.add("ScannerActivity");

        debugAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(View view, int position) {
        handleDebug(debugAdapter.getIdFromIndex(position));
    }

    private void handleDebug(String s) {
        switch (s) {
            case "DiagnosticsHelpDialog":
                DiagnosticsHelpDialog diagnosticsHelpDialog = new DiagnosticsHelpDialog();
                diagnosticsHelpDialog.show(getSupportFragmentManager(), "debugDiagHelpDialog");
                break;
            case "LiveDataHelpDialog":
                LiveDataHelpDialog liveDataHelpDialog = new LiveDataHelpDialog();
                liveDataHelpDialog.show(getSupportFragmentManager(), "debugLDHelpDialog");
                break;
            case "SearchingHelpDialog":
                SearchingHelpDialog searchingHelpDialog = new SearchingHelpDialog();
                searchingHelpDialog.show(getSupportFragmentManager(), "debugSearchingHelpDialog");
                break;
            case "LicensesActivity":
                Intent licensesIntent = new Intent(this, LicensesActivity.class);
                startActivity(licensesIntent);
                break;
            case "ScannerActivity":
                Intent scannerIntent = new Intent(this, ScannerActivity.class);
                scannerIntent.putExtra("debug", true);
                startActivity(scannerIntent);
                break;
        }
    }
}
