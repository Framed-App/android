package dev.truewinter.framed;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LicensesActivity extends AppCompatActivity {
    private int prevId;
    private boolean first = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_licenses);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.action_bar_with_text);

        TextView actionBar = findViewById(R.id.actionBarTitle);
        actionBar.setText("Licenses");

        ConstraintLayout ll = findViewById(R.id.licensesLayout);
        ConstraintSet cs = new ConstraintSet();

        try {
            String[] assets = getAssets().list("licenses/");
            System.out.println("assets");

            for (int i = 0; i < assets.length; i++) {
                String name = assets[i].replaceAll("\\.txt$", "");
                if (name.equals("_framed")) name = "Framed";

                String license = "";
                InputStream is = getAssets().open("licenses/" + assets[i]);
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line = null;
                while ((line = br.readLine()) != null) {
                    license += line + "\n";
                }
                br.close();

                addLicense(ll, cs, name, license);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleConstraint(ConstraintLayout ll, ConstraintSet cs, View view) {
        cs.clone(ll);
        if (first) {
            cs.connect(view.getId(), ConstraintSet.TOP, ll.getId(), ConstraintSet.TOP, 60);
            first = false;
        } else {
            cs.connect(view.getId(), ConstraintSet.TOP, prevId, ConstraintSet.BOTTOM, 60);
        }
        prevId = view.getId();
        cs.applyTo(ll);
    }

    private void addLicense(ConstraintLayout ll, ConstraintSet cs, String name, String license) {
        TextView nameView = new TextView(this);
        nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        nameView.setTypeface(nameView.getTypeface(), Typeface.BOLD);
        nameView.setText(name);
        nameView.setId(TextView.generateViewId());
        ll.addView(nameView);

        handleConstraint(ll, cs, nameView);

        TextView licenseView = new TextView(this);
        licenseView.setText(license);
        licenseView.setId(TextView.generateViewId());
        ll.addView(licenseView);

        handleConstraint(ll, cs, licenseView);
    }
}
