package com.ray.kanban;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

/** 4×1 横条自绘：整条画成一张位图，交给 RemoteViews 显示。 */
public class BarPainter {

    public static final int MODE_COUNTS = 0;   // 三段计数
    public static final int MODE_FOCUS  = 1;   // 焦点：最要紧那件

    public static Bitmap draw(Context ctx, int wPx, int hPx, int wait, int block, int run,
                              String ts, String focusTitle, int bgColor, boolean light, int mode) {
        if (wPx <= 0) wPx = 900;
        if (hPx <= 0) hPx = 170;
        if (wPx < 200) wPx = 200;
        if (hPx < 80) hPx = 80;
        // 位图总面积上限：RemoteViews 对位图内存有硬限制，超了会整块加载失败
        long area = (long) wPx * hPx;
        long MAX = 420000L;
        if (area > MAX) {
            double k = Math.sqrt((double) MAX / area);
            wPx = (int) (wPx * k);
            hPx = (int) (hPx * k);
        }

        Bitmap bmp = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        // 圆角裁剪 + 底色
        RectF all = new RectF(0, 0, wPx, hPx);
        float r = hPx * 0.26f;
        c.drawColor(Color.TRANSPARENT);
        p.setColor(bgColor);
        c.drawRoundRect(all, r, r, p);

        float d = ctx.getResources().getDisplayMetrics().density;
        int fgMain = light ? 0xFF1A1208 : 0xFFEEF4FA;
        int fgDim  = light ? 0xB31A1208 : 0xFF7C8A99;
        int sep    = light ? 0x331A1208 : 0x22FFFFFF;

        // 顶部一道细高光
        p.setColor(light ? 0x22FFFFFF : 0x14FFFFFF);
        c.drawRoundRect(new RectF(r, 0, wPx - r, Math.max(2f, hPx * 0.045f)), 4, 4, p);

        float timeW = hPx * 1.25f;
        float body = wPx - timeW;
        float seg = body / 3f;

        if (mode == MODE_COUNTS) {
            int[] vals = { wait, block, run };
            int[] cols = light ? new int[]{ fgMain, fgMain, fgMain }
                               : new int[]{ 0xFFFFAB2E, 0xFFFF4D3D, 0xFF2EE6A0 };
            String[] labs = { "拍板", "卡住", "在跑" };

            Paint num = new Paint(Paint.ANTI_ALIAS_FLAG);
            num.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            num.setTextAlign(Paint.Align.CENTER);

            Paint lab = new Paint(Paint.ANTI_ALIAS_FLAG);
            lab.setColor(fgDim);
            lab.setTextAlign(Paint.Align.CENTER);

            for (int i = 0; i < 3; i++) {
                float cx = seg * i + seg / 2f;
                // 数字：占高度的 52%
                num.setColor(cols[i]);
                num.setTextSize(hPx * 0.46f);
                c.drawText(String.valueOf(vals[i]), cx, hPx * 0.62f, num);
                // 标签
                lab.setTextSize(hPx * 0.17f);
                c.drawText(labs[i], cx, hPx * 0.88f, lab);
                // 分隔线
                if (i > 0) {
                    p.setColor(sep);
                    c.drawRect(seg * i, hPx * 0.22f, seg * i + Math.max(1f, d * 0.5f), hPx * 0.78f, p);
                }
            }
        } else {
            // 焦点模式：一句话 + 一个数
            Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
            t.setColor(fgMain);
            t.setTextSize(hPx * 0.30f);
            String title = focusTitle == null ? "" : focusTitle;
            float maxW = body - hPx * 0.30f;
            while (t.measureText(title) > maxW && title.length() > 0) {
                title = title.substring(0, title.length() - 1);
            }
            if (!title.equals(focusTitle) && title.length() > 1) {
                title = title.substring(0, title.length() - 1) + "…";
            }
            c.drawText(title, hPx * 0.22f, hPx * 0.44f, t);

            Paint sub = new Paint(Paint.ANTI_ALIAS_FLAG);
            sub.setColor(fgDim);
            sub.setTextSize(hPx * 0.20f);
            c.drawText("还压着 " + (wait + block) + " 件", hPx * 0.22f, hPx * 0.78f, sub);
        }

        // 右侧时间
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(fgDim);
        tp.setTextSize(hPx * 0.17f);
        tp.setTextAlign(Paint.Align.RIGHT);
        c.drawText(ts == null ? "" : ts, wPx - hPx * 0.16f, hPx * 0.60f, tp);

        // 右侧竖向分隔
        p.setColor(sep);
        c.drawRect(body, hPx * 0.22f, body + Math.max(1f, d * 0.5f), hPx * 0.78f, p);

        return bmp;
    }
}
