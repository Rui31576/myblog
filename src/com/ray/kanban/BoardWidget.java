package com.ray.kanban;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BoardWidget extends AppWidgetProvider {

    public static final String PREFS = "kanban";
    public static final String K_WAIT = "n_wait";
    public static final String K_BLOCK = "n_block";
    public static final String K_RUN = "n_run";
    public static final String K_TS = "ts";
    public static final String K_URL = "url";
    public static final String K_BG = "w_bg";
    public static final String K_ALPHA = "w_alpha";

    /** 背景色板，索引与设置页一致 */
    public static final int[] BGS = { 0x0D1117, 0x000000, 0x111A24, 0x1A1F26 };

    protected int layoutRes() { return R.layout.widget_board; }

    @Override
    public void onUpdate(final Context ctx, final AppWidgetManager mgr, final int[] ids) {
        paint(ctx, mgr, ids);
        final PendingResult pr = goAsync();
        new Thread(new Runnable() {
            public void run() {
                try {
                    if (fetchAndStore(ctx)) paint(ctx, mgr, ids);
                } finally {
                    pr.finish();
                }
            }
        }).start();
    }

    public static boolean fetchAndStore(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String url = sp.getString(K_URL, "");
        if (url == null || url.trim().isEmpty()) return false;
        HttpURLConnection conn = null;
        try {
            String u = url.trim();
            if (!u.startsWith("http")) return false;
            u = u + (u.contains("?") ? "&" : "?") + "t=" + System.currentTimeMillis();
            conn = (HttpURLConnection) new URL(u).openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", "shixing-widget");
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            JSONArray arr = new JSONArray(sb.toString());
            int wait = 0, block = 0, run = 0;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String s = o.optString("状态", "");
                if ("等我拍板".equals(s)) wait++;
                else if ("卡住".equals(s)) block++;
                else if ("在跑".equals(s)) run++;
            }
            String ts = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date());
            sp.edit().putInt(K_WAIT, wait).putInt(K_BLOCK, block)
              .putInt(K_RUN, run).putString(K_TS, ts).apply();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    public static int bgColor(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int idx = sp.getInt(K_BG, 0);
        int alpha = sp.getInt(K_ALPHA, 100);
        if (idx < 0 || idx >= BGS.length) idx = 0;
        int a = Math.round(255f * Math.max(0, Math.min(100, alpha)) / 100f);
        return Color.argb(a, Color.red(BGS[idx]), Color.green(BGS[idx]), Color.blue(BGS[idx]));
    }

    private void paint(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) mgr.updateAppWidget(id, build(ctx, layoutRes()));
    }

    public static RemoteViews build(Context ctx, int layout) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), layout);
        rv.setTextViewText(R.id.w_wait, String.valueOf(sp.getInt(K_WAIT, 0)));
        rv.setTextViewText(R.id.w_block, String.valueOf(sp.getInt(K_BLOCK, 0)));
        rv.setTextViewText(R.id.w_run, String.valueOf(sp.getInt(K_RUN, 0)));
        String ts = sp.getString(K_TS, "");
        rv.setTextViewText(R.id.w_ts, ts.isEmpty() ? "未同步" : ts);
        rv.setInt(R.id.w_root, "setBackgroundColor", bgColor(ctx));

        Intent i = new Intent(ctx, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.w_root, pi);
        return rv;
    }

    /** 样式改了以后，把所有尺寸的组件都刷一遍 */
    public static void refreshAll(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        Class<?>[] cs = { BoardWidget.class, Widget4x1.class, Widget4x2.class, Widget2x2.class };
        int[] layouts = { R.layout.widget_board, R.layout.widget_4x1, R.layout.widget_4x2, R.layout.widget_2x2 };
        for (int k = 0; k < cs.length; k++) {
            ComponentName cn = new ComponentName(ctx, cs[k]);
            int[] ids = mgr.getAppWidgetIds(cn);
            if (ids == null) continue;
            for (int id : ids) mgr.updateAppWidget(id, build(ctx, layouts[k]));
        }
    }
}
