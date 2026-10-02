package com.example.colorfrequency;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private TextView bestText;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(GameView.PREFS, Context.MODE_PRIVATE);
        bestText = findViewById(R.id.bestText);

        Button play = findViewById(R.id.btnPlay);
        Button how = findViewById(R.id.btnHow);
        Button about = findViewById(R.id.btnAbout);
        SwitchCompat sound = findViewById(R.id.switchSound);

        sound.setChecked(prefs.getBoolean(GameView.KEY_SOUND, true));
        sound.setOnCheckedChangeListener((b, on) ->
                prefs.edit().putBoolean(GameView.KEY_SOUND, on).apply());

        play.setOnClickListener(v -> startActivity(new Intent(this, GameActivity.class)));

        how.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.how_title)
                .setMessage(R.string.how_body)
                .setPositiveButton(R.string.ok, null)
                .show());

        about.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.about_title)
                .setMessage(R.string.about_body)
                .setPositiveButton(R.string.ok, null)
                .show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        bestText.setText("BEST " + prefs.getInt(GameView.KEY_HIGH, 0));
    }
}