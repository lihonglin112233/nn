package com.dorocar.car.ui;

import android.content.Context;
import android.os.Build;

/* loaded from: classes.dex */
public final class UiCompat {
    private UiCompat() {
    }

    public static int getColor(Context context, int i) {
        if (Build.VERSION.SDK_INT >= 23) {
            return context.getResources().getColor(i, context.getTheme());
        }
        return context.getResources().getColor(i);
    }
}
