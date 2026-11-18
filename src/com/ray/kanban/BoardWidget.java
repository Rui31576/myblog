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
    public static final String K_FOCUS = "focus_title";

    /** 背景色板，索引与设置页一致 */
    public static final int[] BGS = { 0x0D1117, 0x000000, 0x12294A, 0x0E2E24, 0x2A1B3D, 0xF0761E };

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

    /** 4×1 横条：整条自绘 + 分区点击 + 版面轮换 */
    private static RemoteViews buildBar(Context ctx, int widgetId, int color, boolean light,
                                        SharedPreferences sp) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        android.os.Bundle o = (widgetId > 0) ? mgr.getAppWidgetOptions(widgetId) : null;
        float d = ctx.getResources().getDisplayMetrics().density;
        int wDp = 288, hDp = 60;
        if (o != null) {
            int w1 = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
            int w2 = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0);
            int h1 = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);
            int h2 = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0);
            // 必须用最大值：minHeight 是"能缩到多小"，用它会导致纵向被拉伸
            wDp = Math.max(w1, w2) > 0 ? Math.max(w1, w2) : 288;
            hDp = Math.max(h1, h2) > 0 ? Math.max(h1, h2) : 72;
        }
        double ratio = (double) wDp / Math.max(1, hDp);
        if (ratio < 3.0) { hDp = (int) Math.round(wDp / 3.0); }
        if (ratio > 6.0) { hDp = (int) Math.round(wDp / 6.0); }
        int wPx = (int) (wDp * d);
        int hPx = (int) (hDp * d);

        // 版面：按 30 分钟一档轮换，0 计数 / 1 焦点
        int mode = (int) ((System.currentTimeMillis() / (30L * 60L * 1000L)) % 2);
        String focus = sp.getString(K_FOCUS, "");

        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_4x1);
        try {
            rv.setImageViewBitmap(R.id.w_canvas, BarPainter.draw(ctx, wPx, hPx,
                    sp.getInt(K_WAIT, 0), sp.getInt(K_BLOCK, 0), sp.getInt(K_RUN, 0),
                    sp.getString(K_TS, ""), focus, color, light, mode));
        } catch (Throwable th) {
            // 绘制失败也不能让整块组件挂掉，退成纯色底
            try {
                android.graphics.Bitmap fb = android.graphics.Bitmap.createBitmap(8, 8,
                        android.graphics.Bitmap.Config.ARGB_8888);
                fb.eraseColor(color);
                rv.setImageViewBitmap(R.id.w_canvas, fb);
            } catch (Throwable ignored) { }
        }

        int flag = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        rv.setOnClickPendingIntent(R.id.z_wait, open(ctx, "wait", 11, flag));
        rv.setOnClickPendingIntent(R.id.z_block, open(ctx, "block", 12, flag));
        rv.setOnClickPendingIntent(R.id.z_run, open(ctx, "run", 13, flag));
        rv.setOnClickPendingIntent(R.id.z_more, open(ctx, "flip", 14, flag));
        return rv;
    }

    private static PendingIntent open(Context ctx, String filter, int code, int flag) {
        Intent i = new Intent(ctx, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra("filter", filter);
        return PendingIntent.getActivity(ctx, code, i, flag);
    }

    /** 背景是否属于亮色，需要反过来用深色字 */
    public static boolean isLight(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int idx = sp.getInt(K_BG, 0);
        int alpha = sp.getInt(K_ALPHA, 100);
        if (alpha < 45) return false;              // 太透，按深色处理
        if (idx < 0 || idx >= BGS.length) idx = 0;
        int c = BGS[idx];
        double lum = (0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c));
        return lum > 140;
    }

    /** 只给该布局里确实存在的标签上色，避免应用到不存在的控件导致组件加载失败 */
    private static void setLabelColor(RemoteViews rv, int layout, int c) {
        if (layout == R.layout.widget_4x1) return;
        if (layout == R.layout.widget_2x2) {
            rv.setTextColor(R.id.w_l0, c);
            return;
        }
        rv.setTextColor(R.id.w_l1, c);
        rv.setTextColor(R.id.w_l2, c);
        rv.setTextColor(R.id.w_l3, c);
    }

    private void paint(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) mgr.updateAppWidget(id, build(ctx, layoutRes(), id));
    }

    public static RemoteViews build(Context ctx, int layout, int widgetId) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), layout);
        rv.setTextViewText(R.id.w_wait, String.valueOf(sp.getInt(K_WAIT, 0)));
        rv.setTextViewText(R.id.w_block, String.valueOf(sp.getInt(K_BLOCK, 0)));
        rv.setTextViewText(R.id.w_run, String.valueOf(sp.getInt(K_RUN, 0)));
        String ts = sp.getString(K_TS, "");
        rv.setTextViewText(R.id.w_ts, ts.isEmpty() ? "未同步" : ts);
        int color = bgColor(ctx);
        boolean light = isLight(ctx);
        int fg = light ? 0xFF1A1208 : 0xFFFFFFFF;
        int lab = light ? 0xB31A1208 : 0xFF7C8A99;

        if (layout == R.layout.widget_4x1) {
            return buildBar(ctx, widgetId, color, light, sp);
        }
        android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(1, 1,
                android.graphics.Bitmap.Config.ARGB_8888);
        bmp.setPixel(0, 0, color);
        rv.setImageViewBitmap(R.id.w_bg, bmp);

        if (light) {
            rv.setTextColor(R.id.w_wait, 0xFF1A1208);
            rv.setTextColor(R.id.w_block, 0xFF1A1208);
            rv.setTextColor(R.id.w_run, 0xFF1A1208);
            rv.setTextColor(R.id.w_ts, 0x801A1208);
            setLabelColor(rv, layout, 0x991A1208);
        }

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
            for (int id : ids) mgr.updateAppWidget(id, build(ctx, layouts[k], id));
        }
    }
}
