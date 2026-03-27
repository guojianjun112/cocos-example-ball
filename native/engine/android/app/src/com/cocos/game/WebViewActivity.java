package com.cocos.game;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import com.yourong.game.jumpball.R;

public class WebViewActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_webview_activity);

        String url = getIntent().getStringExtra("url");
        if (TextUtils.isEmpty(url)) {
            runOnUiThread(() -> {
                Toast.makeText(this, "参数错误", Toast.LENGTH_SHORT).show();
                finish();
            });
            return;
        }

        String title = getIntent().getStringExtra("title");
        if (!TextUtils.isEmpty(title)) {
            TextView titleView = findViewById(R.id.title);
            titleView.setText(title);
        }

        WebView webView = findViewById(R.id.webView);
        setSettings(webView);
        webView.loadUrl(url);

        ImageView backView = findViewById(R.id.back);
        backView.setOnClickListener(view -> finish());
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    private void setSettings(WebView webView) {
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return super.shouldOverrideUrlLoading(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return super.shouldOverrideUrlLoading(view, request);
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message message) {
                WebView newWebView = new WebView(view.getContext());
                newWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        Intent intent = new Intent(WebViewActivity.this, WebViewActivity.class);
                        intent.putExtra("url", url);
                        startActivity(intent);
                        return true;
                    }
                });
                WebView.WebViewTransport transport = (WebView.WebViewTransport) message.obj;
                transport.setWebView(newWebView);
                message.sendToTarget();
                return true;
            }
        });
        WebSettings settings = webView.getSettings();
        settings.setUserAgentString(Build.MODEL + "/CocosGame " + Build.VERSION.RELEASE + "/" + settings.getUserAgentString());

        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setDatabaseEnabled(true);
        settings.setSaveFormData(true);
        settings.setSavePassword(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setDomStorageEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowFileAccess(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        settings.setSupportZoom(true);
        settings.setSupportMultipleWindows(true);

        String dir = getApplicationContext().getDir("database", Context.MODE_PRIVATE).getPath();
        settings.setGeolocationDatabasePath(dir);
        settings.setMixedContentMode(WebSettings.LOAD_DEFAULT);

        webView.addJavascriptInterface(this, "android");
    }
}
