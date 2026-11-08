package com.dorocar.car.vehicle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/* loaded from: classes.dex */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (!"android.intent.action.BOOT_COMPLETED".equals(action) && !"android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            return;
        }
        GwmRepository gwmRepository = GwmRepository.get(context.getApplicationContext());
        boolean isLoggedIn = gwmRepository.isLoggedIn();
        boolean isPollEnabled = gwmRepository.isPollEnabled();
        Log.i(TAG, "开机 " + action + " 登录=" + isLoggedIn + " 轮询开关=" + isPollEnabled);
        if (isLoggedIn && isPollEnabled) {
            VehiclePollService.start(context.getApplicationContext());
        }
    }
}
