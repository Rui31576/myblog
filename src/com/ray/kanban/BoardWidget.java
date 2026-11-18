package com.ray.kanban;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

    @Override
    public void onUpdate(final Context ctx, final AppWidgetManager mgr, final int[] ids) {
        // 先用缓存立刻渲染一次，避免白屏
        paint(ctx, mgr, ids);
        // 再异步拉数据
        final PendingResult pr = goAsync();
        new Thread(new Runnable() {
            public void run() {
                try {
                    if (fetchAndStore(ctx)) {
                        paint(ctx, mgr, ids);
                    }
                } finally {
                    pr.finish();
                }
            }
        }).start();
    }

    /** 拉数据源并写入 SharedPreferences，成功返回 true。 */
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
            conn.setRequestProperty("User-Agent", "kanban-widget");
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

    private static void paint(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) {
            mgr.updateAppWidget(id, build(ctx));
        }
    }

    public static RemoteViews build(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_board);
        rv.setTextViewText(R.id.w_wait, String.valueOf(sp.getInt(K_WAIT, 0)));
        rv.setTextViewText(R.id.w_block, String.valueOf(sp.getInt(K_BLOCK, 0)));
        rv.setTextViewText(R.id.w_run, String.valueOf(sp.getInt(K_RUN, 0)));
        String ts = sp.getString(K_TS, "");
        rv.setTextViewText(R.id.w_ts, ts.isEmpty() ? "未同步" : ts);


        Intent i = new Intent(ctx, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.w_root, pi);
        return rv;
    }

    public static void refreshAll(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        ComponentName cn = new ComponentName(ctx, BoardWidget.class);
        int[] ids = mgr.getAppWidgetIds(cn);
        if (ids == null || ids.length == 0) return;
        RemoteViews rv = build(ctx);
        for (int id : ids) mgr.updateAppWidget(id, rv);
    }
}
