package com.example.colorfrequency;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class AdManager {
    private final Activity activity;
    private RewardedAd rewardedAd;
    private boolean loading = false;

    public AdManager(Activity activity) {
        this.activity = activity;
        MobileAds.initialize(activity, status -> { });
        load();
    }

    public boolean isReady() { return rewardedAd != null; }

    public void load() {
        if (loading || rewardedAd != null) return;
        loading = true;
        RewardedAd.load(activity, activity.getString(R.string.admob_rewarded_id),
                new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                        loading = false;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        rewardedAd = null;
                        loading = false;
                    }
                });
    }

    /**
     * Shows the ad. onRewarded runs after the ad closes IF the user earned the reward.
     * onFailed runs if no ad is ready, the ad fails, or the user closes it early.
     */
    public void show(Runnable onRewarded, Runnable onFailed) {
        if (rewardedAd == null) {
            load();
            onFailed.run();
            return;
        }
        final boolean[] earned = {false};
        final RewardedAd ad = rewardedAd;
        rewardedAd = null; // an ad can only be shown once

        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                load();
                if (earned[0]) onRewarded.run(); else onFailed.run();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                load();
                onFailed.run();
            }
        });
        ad.show(activity, item -> earned[0] = true);
    }
}
