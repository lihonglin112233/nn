package com.dorocar.car.vehicle;

import android.R;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import com.dorocar.car.ui.MainActivity;
import com.dorocar.car.vehicle.GwmRepository;

/* loaded from: classes.dex */
public class VehiclePollService extends Service {
    private static final String ACTION_START = "com.dorocar.car.vehicle.START";
    private static final String ACTION_STOP = "com.dorocar.car.vehicle.STOP";
    private static final String CHANNEL_ID = "doro_vehicle_poll";
    private static final int NOTIFICATION_ID = 2;
    private static final long SESSION_POLL_MS = 400;
    private static final long SESSION_WAIT_MS = 8000;
    private static final String TAG = "VehiclePollService";
    private GwmRepository.Listener listener;
    private GwmRepository repository;
    private boolean started;

    public static void start(Context context) {
        try {
            Intent action = new Intent(context, (Class<?>) VehiclePollService.class).setAction(ACTION_START);
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(action);
            } else {
                context.startService(action);
            }
        } catch (Throwable th) {
            Log.w(TAG, "启动轮询服务失败", th);
        }
    }

    public static void stop(Context context) {
        try {
            context.startService(new Intent(context, (Class<?>) VehiclePollService.class).setAction(ACTION_STOP));
        } catch (Throwable th) {
            Log.w(TAG, "停止轮询服务失败", th);
        }
    }

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        this.repository = GwmRepository.get(this);
        startForegroundWithNotice("正在同步车况");
        this.listener = new GwmRepository.Listener() { // from class: com.dorocar.car.vehicle.VehiclePollService.1
            @Override // com.dorocar.car.vehicle.GwmRepository.Listener
            public void onLoginStateChanged(boolean z) {
                if (!z) {
                    VehiclePollService.this.stopSelf();
                }
            }

            @Override // com.dorocar.car.vehicle.GwmRepository.Listener
            public void onStatusUpdated(VehicleStatus vehicleStatus, String str) {
                String str2;
                if (vehicleStatus != null) {
                    StatusAlertCenter.get().onStatusUpdated(VehiclePollService.this.getApplicationContext(), vehicleStatus);
                    if (vehicleStatus.fuelRange == null) {
                        str2 = "车况已更新";
                    } else {
                        str2 = "续航 " + Math.round(vehicleStatus.fuelRange.doubleValue()) + " km · " + vehicleStatus.securitySummary();
                    }
                    VehiclePollService.this.updateNotice(str2);
                }
            }

            @Override // com.dorocar.car.vehicle.GwmRepository.Listener
            public void onRefreshing(boolean z) {
            }

            @Override // com.dorocar.car.vehicle.GwmRepository.Listener
            public void onReauthRequired(String str) {
                VehiclePollService.this.updateNotice("登录已失效，请打开 App 重新登录");
            }
        };
        this.repository.addListener(this.listener);
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int i, int i2) {
        if (ACTION_STOP.equals(intent == null ? null : intent.getAction())) {
            stopSelf();
            return NOTIFICATION_ID;
        }
        if (!this.repository.isLoggedIn()) {
            if (this.repository.sessionState() == GwmRepository.SessionState.RESTORING) {
                awaitSessionThenStart();
                return NOTIFICATION_ID;
            }
            stopSelf();
            return NOTIFICATION_ID;
        }
        if (!this.repository.isPollEnabled()) {
            stopSelf();
            return NOTIFICATION_ID;
        }
        if (!this.started) {
            this.started = true;
            this.repository.startPolling();
            this.repository.refreshNow();
        }
        // 被系统回收后自动重建：START_STICKY 让系统在内存回收后重新拉起本服务，
        // 只要本地登录态仍在、轮询开关仍打开，就继续在后台同步车况，无需用户重新登录
        return START_STICKY;
    }

    private void awaitSessionThenStart() {
        new Thread(new Runnable() { // from class: com.dorocar.car.vehicle.VehiclePollService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                VehiclePollService.this.m56x6f7d28e0();
            }
        }, "session-wait").start();
    }

    /* renamed from: lambda$awaitSessionThenStart$1$com-dorocar-car-vehicle-VehiclePollService, reason: not valid java name */
    /* synthetic */ void m56x6f7d28e0() {
        final boolean waitForSession = waitForSession();
        new Handler(getMainLooper()).post(new Runnable() { // from class: com.dorocar.car.vehicle.VehiclePollService$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                VehiclePollService.this.m55xac90bf81(waitForSession);
            }
        });
    }

    /* renamed from: lambda$awaitSessionThenStart$0$com-dorocar-car-vehicle-VehiclePollService, reason: not valid java name */
    /* synthetic */ void m55xac90bf81(boolean z) {
        if (!z) {
            Log.i(TAG, "会话续期未完成，停止轮询服务");
            stopSelf();
        } else if (!this.repository.isPollEnabled()) {
            stopSelf();
        } else if (!this.started) {
            this.started = true;
            this.repository.startPolling();
            this.repository.refreshNow();
        }
    }

    private boolean waitForSession() {
        long currentTimeMillis = System.currentTimeMillis() + SESSION_WAIT_MS;
        while (System.currentTimeMillis() < currentTimeMillis) {
            try {
                Thread.sleep(SESSION_POLL_MS);
                if (this.repository.isLoggedIn()) {
                    return true;
                }
                if (this.repository.sessionState() != GwmRepository.SessionState.RESTORING) {
                    return false;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return this.repository.isLoggedIn();
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override // android.app.Service
    public void onDestroy() {
        super.onDestroy();
        if (this.repository != null) {
            this.repository.stopPolling();
            if (this.listener != null) {
                this.repository.removeListener(this.listener);
            }
        }
    }

    private void startForegroundWithNotice(String str) {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "车况同步", 1);
                notificationChannel.setDescription("在后台定时同步车辆状态");
                notificationChannel.setShowBadge(false);
                notificationChannel.setSound(null, null);
                NotificationManager notificationManager = (NotificationManager) getSystemService(NotificationManager.class);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(notificationChannel);
                }
            }
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, buildNotice(str), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(NOTIFICATION_ID, buildNotice(str));
            }
        } catch (Throwable th) {
            Log.w(TAG, "前台通知失败（不影响轮询）", th);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateNotice(String str) {
        try {
            NotificationManager notificationManager = (NotificationManager) getSystemService("notification");
            if (notificationManager != null) {
                notificationManager.notify(NOTIFICATION_ID, buildNotice(str));
            }
        } catch (Throwable th) {
        }
    }

    private Notification buildNotice(String str) {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= 26) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }
        return builder.setSmallIcon(R.drawable.ic_menu_mylocation).setContentTitle("Doro 车况同步").setContentText(str).setContentIntent(PendingIntent.getActivity(this, 1, new Intent(this, (Class<?>) MainActivity.class).putExtra("open_tab", "vehicle"), Build.VERSION.SDK_INT >= 23 ? 67108864 : 0)).setOngoing(true).setPriority(Build.VERSION.SDK_INT < 26 ? -2 : 0).build();
    }
}
