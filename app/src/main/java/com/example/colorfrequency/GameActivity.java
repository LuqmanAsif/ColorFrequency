package com.example.colorfrequency;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class GameActivity extends AppCompatActivity implements GameView.Listener{
    private GameView gameView;
    private AdManager adManager;

    private View overlay;
    private TextView finalScore, finalBest, newBest;
    private Button btnRevive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        overlay = findViewById(R.id.overlay);
        finalScore = findViewById(R.id.finalScore);
        finalBest = findViewById(R.id.finalBest);
        newBest = findViewById(R.id.newBest);
        btnRevive = findViewById(R.id.btnRevive);

        gameView = new GameView(this);
        gameView.setListener(this);
        ((FrameLayout) findViewById(R.id.gameContainer)).addView(gameView);

        adManager = new AdManager(this);

        btnRevive.setOnClickListener(v -> {
            btnRevive.setEnabled(false);
            adManager.show(
                    () -> { // rewarded
                        overlay.setVisibility(View.GONE);
                        gameView.revive();
                    },
                    () -> { // not rewarded / unavailable
                        btnRevive.setEnabled(true);
                        Toast.makeText(this, R.string.ad_unavailable, Toast.LENGTH_SHORT).show();
                    });
        });

        findViewById(R.id.btnRetry).setOnClickListener(v -> {
            overlay.setVisibility(View.GONE);
            gameView.restart();
        });

        findViewById(R.id.btnMenu).setOnClickListener(v -> finish());

        hideSystemBars();
    }

    @Override
    public void onGameOver(int score, int best, boolean isNewBest, boolean canRevive) {
        finalScore.setText(String.valueOf(score));
        finalBest.setText("BEST " + best);
        newBest.setVisibility(isNewBest ? View.VISIBLE : View.GONE);
        btnRevive.setVisibility(canRevive ? View.VISIBLE : View.GONE);
        btnRevive.setEnabled(true);
        overlay.setVisibility(View.VISIBLE);
    }

    private void hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat c =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        c.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars());
        c.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.onPause();
    }
}
