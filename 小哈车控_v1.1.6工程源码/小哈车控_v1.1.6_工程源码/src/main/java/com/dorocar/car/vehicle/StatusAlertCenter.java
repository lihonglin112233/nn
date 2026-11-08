package com.dorocar.car.vehicle;

import android.R;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public final class StatusAlertCenter {
    static final int ALERT_NOTIFICATION_ID_BASE = 100;
    public static final String CHANNEL_ALERTS = "doro_status_alerts";
    private static final String TAG = "StatusAlert";
    private static volatile StatusAlertCenter instance;
    private Snapshot last;
    private UiSink uiSink;
    private int unreadCount;

    public interface UiSink {
        void onStatusAlert(Alert alert);
    }

    public static final class Alert {
        public final boolean good;
        public final String item;
        public final String text;

        Alert(String str, String str2, boolean z) {
            this.item = str;
            this.text = str2;
            this.good = z;
        }

        public String toString() {
            return this.text;
        }
    }

    private static final class Snapshot {
        Boolean acOn;
        Double batteryPercent;
        Boolean doorsLocked;
        Boolean engineRunning;
        Double fuelRange;
        Boolean trunkOpen;
        Boolean windowOpen;

        private Snapshot() {
        }
    }

    public static StatusAlertCenter get() {
        if (instance == null) {
            synchronized (StatusAlertCenter.class) {
                if (instance == null) {
                    instance = new StatusAlertCenter();
                }
            }
        }
        return instance;
    }

    StatusAlertCenter() {
    }

    public void setUiSink(UiSink uiSink) {
        this.uiSink = uiSink;
    }

    public int unreadCount() {
        return this.unreadCount;
    }

    public void clearUnread() {
        this.unreadCount = 0;
    }

    public List<Alert> onStatusUpdated(Context context, VehicleStatus vehicleStatus) {
        List<Alert> diff = diff(vehicleStatus);
        if (diff == null) {
            return new ArrayList();
        }
        for (Alert alert : diff) {
            this.unreadCount++;
            notifyAlert(context, alert);
            if (this.uiSink != null) {
                try {
                    this.uiSink.onStatusAlert(alert);
                } catch (Exception e) {
                    Log.w(TAG, "界面回调失败", e);
                }
            }
        }
        if (!diff.isEmpty()) {
            Log.i(TAG, "状态提醒 x" + diff.size());
        }
        return diff;
    }

    List<Alert> diff(VehicleStatus vehicleStatus) {
        int floor;
        int floor2;
        ArrayList arrayList = new ArrayList();
        if (vehicleStatus == null) {
            return new ArrayList();
        }
        Snapshot snapshotOf = snapshotOf(vehicleStatus);
        Snapshot snapshot = this.last;
        this.last = snapshotOf;
        if (snapshot == null) {
            return null;
        }
        if (changed(snapshot.doorsLocked, snapshotOf.doorsLocked)) {
            boolean equals = Boolean.TRUE.equals(snapshotOf.doorsLocked);
            arrayList.add(new Alert("门锁", equals ? "车辆已闭锁" : "车辆已解锁", equals));
        }
        if (changed(snapshot.windowOpen, snapshotOf.windowOpen)) {
            boolean equals2 = Boolean.TRUE.equals(snapshotOf.windowOpen);
            arrayList.add(new Alert("车窗", equals2 ? "检测到车窗被打开" : "车窗已全部关闭", !equals2));
        }
        if (changed(snapshot.trunkOpen, snapshotOf.trunkOpen)) {
            boolean equals3 = Boolean.TRUE.equals(snapshotOf.trunkOpen);
            arrayList.add(new Alert("后备箱", equals3 ? "后备箱已打开" : "后备箱已关闭", !equals3));
        }
        if (changed(snapshot.engineRunning, snapshotOf.engineRunning)) {
            boolean equals4 = Boolean.TRUE.equals(snapshotOf.engineRunning);
            arrayList.add(new Alert("发动机", equals4 ? "发动机已启动" : "发动机已熄火", equals4));
        }
        if (changed(snapshot.acOn, snapshotOf.acOn)) {
            boolean equals5 = Boolean.TRUE.equals(snapshotOf.acOn);
            arrayList.add(new Alert("空调", equals5 ? "空调已开启" : "空调已关闭", equals5));
        }
        if (snapshotOf.batteryPercent != null && snapshot.batteryPercent != null && (floor2 = (int) Math.floor(snapshotOf.batteryPercent.doubleValue() / 10.0d)) < ((int) Math.floor(snapshot.batteryPercent.doubleValue() / 10.0d)) && snapshotOf.batteryPercent.doubleValue() <= 30.0d) {
            arrayList.add(new Alert("电量", "电量已低于 " + ((floor2 + 1) * 10) + "%（当前 " + Math.round(snapshotOf.batteryPercent.doubleValue()) + "%）", false));
        }
        if (snapshotOf.fuelRange != null && snapshot.fuelRange != null && snapshotOf.fuelRange.doubleValue() < snapshot.fuelRange.doubleValue() && (floor = (int) Math.floor(snapshotOf.fuelRange.doubleValue() / 50.0d)) < ((int) Math.floor(snapshot.fuelRange.doubleValue() / 50.0d)) && snapshotOf.fuelRange.doubleValue() <= 100.0d) {
            arrayList.add(new Alert("续航", "续航不足 " + ((floor + 1) * 50) + " km（当前 " + Math.round(snapshotOf.fuelRange.doubleValue()) + " km）", false));
        }
        return arrayList;
    }

    private static boolean changed(Boolean bool, Boolean bool2) {
        return (bool == null || bool2 == null || bool.equals(bool2)) ? false : true;
    }

    private static Snapshot snapshotOf(VehicleStatus vehicleStatus) {
        Snapshot snapshot = new Snapshot();
        snapshot.doorsLocked = vehicleStatus.doorsLocked;
        snapshot.windowOpen = Boolean.valueOf(vehicleStatus.hasOpenWindow());
        snapshot.trunkOpen = vehicleStatus.doorTrunk;
        snapshot.engineRunning = Boolean.valueOf(vehicleStatus.isEngineRunning());
        snapshot.acOn = vehicleStatus.airConditioner;
        snapshot.batteryPercent = vehicleStatus.batteryPercent;
        snapshot.fuelRange = vehicleStatus.fuelRange;
        return snapshot;
    }

    private static void notifyAlert(Context context, Alert alert) {
        Notification.Builder builder;
        try {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
            if (notificationManager == null) {
                return;
            }
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ALERTS, "状态提醒", 3);
                notificationChannel.setDescription("车辆状态发生变化时提醒");
                notificationManager.createNotificationChannel(notificationChannel);
            }
            if (Build.VERSION.SDK_INT >= 26) {
                builder = new Notification.Builder(context, CHANNEL_ALERTS);
            } else {
                builder = new Notification.Builder(context);
            }
            Notification.Builder contentIntent = builder.setSmallIcon(R.drawable.ic_dialog_info).setContentTitle("状态提醒").setContentText(alert.item + "：" + alert.text).setAutoCancel(true).setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, Class.forName(context.getPackageName() + ".ui.MainActivity")), Build.VERSION.SDK_INT >= 23 ? 67108864 : 0));
            if (Build.VERSION.SDK_INT < 26) {
            }
            contentIntent.setPriority(0);
            notificationManager.notify(alert.item.hashCode() + ALERT_NOTIFICATION_ID_BASE, builder.build());
        } catch (Throwable th) {
            Log.w(TAG, "提醒通知失败", th);
        }
    }
}
