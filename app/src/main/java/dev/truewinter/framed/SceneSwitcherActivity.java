package dev.truewinter.framed;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import dev.truewinter.framed.adapters.ScenesAdapter;
import dev.truewinter.framed.dialogs.LiveDataHelpDialog;
import dev.truewinter.framed.dialogs.SceneSwitcherHelpDialog;

public class SceneSwitcherActivity extends AppCompatActivity implements ScenesAdapter.ItemClickListener {
    private JSONObject sceneList;
    private Set<String> sceneSet;
    private String currentScene;
    private ScenesAdapter scenesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scene_switcher);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.action_bar_with_text);

        try {
            this.sceneList = new JSONObject(getIntent().getExtras().getString("sceneList"));
            this.sceneSet = new LinkedHashSet<>();
            this.currentScene = this.sceneList.getString("currentScene");

            JSONArray scenes = this.sceneList.getJSONArray("scenes");

            for (int i = 0; i < scenes.length(); i++) {
                sceneSet.add(scenes.get(i).toString());
            }

            System.out.println(this.sceneList.toString());

            TextView actionBarTitle = findViewById(R.id.actionBarTitle);
            actionBarTitle.setText("Scene Switcher");
        } catch (Exception e) {
            e.printStackTrace();
        }

        TextView versionText = findViewById(R.id.versionText);
        versionText.setText(String.format("v%s", BuildConfig.VERSION_NAME));

        RecyclerView recyclerView = findViewById(R.id.sceneList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        scenesAdapter = new ScenesAdapter(this, sceneSet);
        scenesAdapter.setClickListener(this);
        scenesAdapter.setCurrentScene(currentScene);
        recyclerView.setAdapter(scenesAdapter);

        Button helpBtn = findViewById(R.id.sceneSwitcherHelpBtn);
        helpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SceneSwitcherHelpDialog sceneSwitcherHelpDialog = new SceneSwitcherHelpDialog();
                sceneSwitcherHelpDialog.show(getSupportFragmentManager(), "sceneSwitcherHelp");
            }
        });
    }

    @Override
    public void onItemClick(View view, int position) {
        System.out.println(scenesAdapter.getIdFromIndex(position));

        Intent intent = new Intent();
        intent.putExtra("scene", scenesAdapter.getIdFromIndex(position));
        setResult(RESULT_OK, intent);
        finish();
    }
}
