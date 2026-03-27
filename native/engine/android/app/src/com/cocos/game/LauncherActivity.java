package com.cocos.game;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;

import com.yourong.game.jumpball.R;

public class LauncherActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (!isTaskRoot() && intent != null &&
                intent.hasCategory(Intent.CATEGORY_LAUNCHER) &&
                Intent.ACTION_MAIN.equals(intent.getAction())) {
            finish();
            return;
        }

        onCreateCustom();
    }

    private void onCreateCustom() {
        SharedPreferences game = getSharedPreferences("game", Context.MODE_PRIVATE);
        int privacy = game.getInt("privacy", 0);
        String url = getString(R.string.app_privacy);
        if (privacy == 1 || TextUtils.isEmpty(url)) {
            startActivity(new Intent(this, AppActivity.class));
            finish();
            return;
        }

        WebViewDialog policyDialog = new WebViewDialog(this, url);
        policyDialog.setCanceledOnTouchOutside(false);
        policyDialog.setCancelable(false);
        policyDialog.setCallback(status -> {
            if (status) {
                game.edit().putInt("privacy", 1).apply();
                startActivity(new Intent(this, AppActivity.class));
            }
            finish();
        });
        policyDialog.setLauncher(true);
        policyDialog.show();
    }
}
