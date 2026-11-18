package com.ray.kanban;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {
    private WebView web;

    public class Bridge {
        @JavascriptInterface
        public void saveCounts(int wait, int block, int run, String ts) {
            SharedPreferences sp = getSharedPreferences(BoardWidget.PREFS, MODE_PRIVATE);
            sp.edit()
              .putInt(BoardWidget.K_WAIT, wait)
              .putInt(BoardWidget.K_BLOCK, block)
              .putInt(BoardWidget.K_RUN, run)
              .putString(BoardWidget.K_TS, ts)
              .apply();
            BoardWidget.refreshAll(MainActivity.this);
        }

        @JavascriptInterface
        public String getUrl() {
            SharedPreferences sp = getSharedPreferences(BoardWidget.PREFS, MODE_PRIVATE);
            return sp.getString("url", "");
        }

        @JavascriptInterface
        public void setUrl(String u) {
            SharedPreferences sp = getSharedPreferences(BoardWidget.PREFS, MODE_PRIVATE);
            sp.edit().putString("url", u).apply();
        }

        @JavascriptInterface
        public void setWidgetStyle(int bgIndex, int alpha) {
            SharedPreferences sp = getSharedPreferences(BoardWidget.PREFS, MODE_PRIVATE);
            sp.edit().putInt(BoardWidget.K_BG, bgIndex)
              .putInt(BoardWidget.K_ALPHA, alpha).apply();
            BoardWidget.refreshAll(MainActivity.this);
        }

        @JavascriptInterface
        public int[] getWidgetStyle() {
            SharedPreferences sp = getSharedPreferences(BoardWidget.PREFS, MODE_PRIVATE);
            return new int[]{ sp.getInt(BoardWidget.K_BG, 0), sp.getInt(BoardWidget.K_ALPHA, 100) };
        }

        @JavascriptInterface
        public boolean pinWidget(int idx) {
            Class<?>[] cs = { Widget4x1.class, Widget4x2.class, Widget2x2.class, BoardWidget.class };
            if (idx < 0 || idx >= cs.length) return false;
            AppWidgetManager mgr = AppWidgetManager.getInstance(MainActivity.this);
            if (mgr.isRequestPinAppWidgetSupported()) {
                ComponentName cn = new ComponentName(MainActivity.this, cs[idx]);
                return mgr.requestPinAppWidget(cn, null, null);
            }
            Toast.makeText(MainActivity.this, "桌面不支持一键添加，请长按桌面手动添加", Toast.LENGTH_LONG).show();
            return false;
        }

        @JavascriptInterface
        public void toast(String m) {
            Toast.makeText(MainActivity.this, m, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        web.setBackgroundColor(0xFF0F1216);
        web.addJavascriptInterface(new Bridge(), "Android");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) {
            web.evaluateJavascript("try{window.refresh && window.refresh()}catch(e){}", null);
        }
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
