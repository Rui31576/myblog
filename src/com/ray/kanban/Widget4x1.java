package com.ray.kanban;

import android.content.Context;

public class Widget4x1 extends BoardWidget {
    @Override protected int layoutRes() { return R.layout.widget_4x1; }

    @Override public void onDeleted(Context ctx, int[] ids) {
        for (int id : ids) { /* 占位，后续可做清理 */ }
    }
}
