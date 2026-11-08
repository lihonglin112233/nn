package com.dorocar.car.vehicle;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.dorocar.car.vehicle.GwmClient;
import com.dorocar.car.vehicle.GwmRepository;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/* loaded from: classes.dex */
public final class GwmRepository {
    public static final int DEFAULT_INTERVAL = 60;
    private static final String KEY_INTERVAL = "poll_interval";
    private static final String KEY_POLL = "poll_enabled";
    private static final String KEY_REMOTE = "remote_enabled";
    private static final String KEY_VIN = "selected_vin";
    private static final int MAX_BACKOFF_MS = 600000;
    public static final int MIN_INTERVAL = 30;
    private static final String PREFS = "doro_gwm";
    private static final String TAG = "GwmRepository";
    private static volatile GwmRepository instance;
    private final Context appContext;
    private volatile GwmClient client;
    private int consecutiveFailures;
    private volatile boolean destroyed;
    private volatile String lastError;
    private volatile VehicleStatus lastStatus;
    private volatile long lastUpdateAt;
    private volatile List<GwmVehicle> lastVehicles;
    private boolean pollScheduled;
    private final SharedPreferences prefs;
    private volatile String sessionMessage;
    private volatile SessionState sessionState;
    private final GwmSecureStore store;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Listener> listeners = new CopyOnWriteArrayList();
    private final AtomicBoolean polling = new AtomicBoolean(false);
    private final Runnable pollTask = new Runnable() { // from class: com.dorocar.car.vehicle.GwmRepository$$ExternalSyntheticLambda2
        @Override // java.lang.Runnable
        public final void run() {
            GwmRepository.this.pollOnce();
        }
    };

    public interface Callback<T> {
        void onError(String str);

        void onSuccess(T t);
    }

    public interface Listener {
        void onLoginStateChanged(boolean z);

        void onReauthRequired(String str);

        void onRefreshing(boolean z);

        void onStatusUpdated(VehicleStatus vehicleStatus, String str);
    }

    public enum SessionState {
        LOGGED_OUT,
        RESTORING,
        ACTIVE,
        EXPIRED
    }

    public static GwmRepository get(Context context) {
        if (instance == null) {
            synchronized (GwmRepository.class) {
                if (instance == null) {
                    instance = new GwmRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private GwmRepository(Context context) {
        this.sessionState = SessionState.LOGGED_OUT;
        this.appContext = context;
        this.store = new GwmSecureStore(context);
        this.prefs = context.getSharedPreferences(PREFS, 0);
        this.client = this.store.load();
        String lastLoadError = this.store.lastLoadError();
        if (this.client != null) {
            this.sessionState = this.client.isLoggedIn() ? SessionState.ACTIVE : SessionState.RESTORING;
        } else {
            this.sessionState = SessionState.LOGGED_OUT;
            if (lastLoadError != null) {
                this.sessionMessage = lastLoadError;
            }
        }
        if (this.sessionState == SessionState.RESTORING) {
            this.executor.execute(new Runnable() { // from class: com.dorocar.car.vehicle.GwmRepository$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    GwmRepository.this.restoreSessionQuietly();
                }
            });
        } else if (this.sessionState == SessionState.ACTIVE) {
            // 冷启动即已登录时，后台静默刷新一次，确保 token 与本地持久化状态保持最新
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    refreshSavedSessionOnBoot();
                }
            });
        }
    }

    /**
     * 冷启动时本地已有完整凭据：先静默校验一次。
     * 成功：刷新 token 并回写；失败但仍有刷新令牌：尝试续期；
     * 都失败：只在真正的认证失败时标记过期，网络异常不能把用户踢下线。
     */
    private void refreshSavedSessionOnBoot() {
        GwmClient gwmClient = this.client;
        if (gwmClient == null) {
            return;
        }
        try {
            gwmClient.getVehicles();
            if (sessionInvalidated || this.client != gwmClient) {
                return; // 期间已退出登录，丢弃过期任务
            }
            persistQuietly(gwmClient);
            setSessionState(SessionState.ACTIVE, null);
        } catch (Throwable th) {
            Log.w(TAG, "冷启动校验会话失败：" + th);
            if (gwmClient.reviveSessionQuietly()) {
                persistQuietly(gwmClient);
                setSessionState(SessionState.ACTIVE, null);
                return;
            }
            // 仅在明确为认证类错误时才要求重新登录；网络异常保留本地登录态
            if (isAuthFailure(th)) {
                setSessionState(SessionState.EXPIRED, "登录已过期，需要重新验证");
                notifyLoginState();
                notifyReauth("登录已过期，请重新用短信验证码登录");
            }
        }
    }

    private static boolean isAuthFailure(Throwable th) {
        if (!(th instanceof GwmClient.AuthException)) {
            return false;
        }
        String msg = th.getMessage() == null ? "" : th.getMessage();
        return msg.contains("会话") || msg.contains("-101") || msg.contains("认证") || msg.contains("token")
                || msg.contains("login") || msg.contains("401");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void restoreSessionQuietly() {
        GwmClient gwmClient = this.client;
        if (gwmClient == null) {
            setSessionState(SessionState.LOGGED_OUT, null);
            notifyLoginState();
            return;
        }
        if (gwmClient.isLoggedIn()) {
            setSessionState(SessionState.ACTIVE, null);
            notifyLoginState();
            startPolling();
        } else if (!gwmClient.hasRefreshableSession()) {
            setSessionState(SessionState.EXPIRED, "登录凭据不完整，需要重新验证");
            notifyLoginState();
            notifyReauth("登录凭据不完整，请重新用短信验证码登录");
        } else {
            if (gwmClient.reviveSessionQuietly()) {
                persistQuietly(gwmClient);
                setSessionState(SessionState.ACTIVE, null);
                notifyLoginState();
                startPolling();
                return;
            }
            setSessionState(SessionState.EXPIRED, "登录已过期，需要重新验证一次");
            notifyLoginState();
            notifyReauth("登录已过期，请重新用短信验证码登录");
        }
    }

    private void setSessionState(SessionState sessionState, String str) {
        this.sessionState = sessionState;
        this.sessionMessage = str;
    }

    private void markSessionActiveIfNeeded() {
        if (this.sessionState != SessionState.ACTIVE && isLoggedIn()) {
            setSessionState(SessionState.ACTIVE, null);
            notifyLoginState();
        }
    }

    public SessionState sessionState() {
        return this.sessionState;
    }

    public String sessionMessage() {
        return this.sessionMessage;
    }

    public boolean isSessionUsable() {
        return this.sessionState == SessionState.ACTIVE && isLoggedIn();
    }

    public void addListener(Listener listener) {
        if (listener == null || this.listeners.contains(listener)) {
            return;
        }
        this.listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        this.listeners.remove(listener);
    }

    private void notifyLoginState() {
        final boolean isLoggedIn = isLoggedIn();
        this.main.post(new Runnable() { // from class: com.dorocar.car.vehicle.GwmRepository$$ExternalSyntheticLambda20
            @Override // java.lang.Runnable
            public final void run() {
                GwmRepository.this.m41lambda$notifyLoginState$0$comdorocarcarvehicleGwmRepository(isLoggedIn);
            }
        });
    }

    /* renamed from: lambda$notifyLoginState$0$com-dorocar-car-vehicle-GwmRepository, reason: not valid java name */
    /* synthetic */ void m41lambda$notifyLoginState$0$comdorocarcarvehicleGwmRepository(boolean z) {
        Iterator<Listener> it = this.listeners.iterator();
        while (it.hasNext()) {
            it.next().onLoginStateChanged(z);
        }
    }

    private void notifyStatus(final VehicleStatus vehicleStatus, final String str) {
        this.main.post(new Runnable() { // from class: com.dorocar.car.vehicle.GwmRepository$$ExternalSyntheticLambda21
            @Override // java.lang.Runnable
            public final void run() {
                GwmRepository.this.m44lambda$notifyStatus$1$comdorocarcarvehicleGwmRepository(vehicleStatus, str);
            }
        });
    }

    /* renamed from: lambda$notifyStatus$1$com-dorocar-car-vehicle-GwmRepository, reason: not valid java name */
    /* synthetic */ void m44lambda$notifyStatus$1$comdorocarcarvehicleGwmRepository(VehicleStatus vehicleStatus, String str) {
        Iterator<Listener> it = this.listeners.iterator();
        while (it.hasNext()) {
            it.next().onStatusUpdated(vehicleStatus, str);
        }
    }

    private void notifyRefreshing(final boolean z) {
        this.main.post(new Runnable() { // from class: com.dorocar.car.vehicle.GwmRepository$$ExternalSyntheticLambda14
            @Override // java.lang.Runnable
            public final void run() {
                GwmRepository.this.m43lambda$notifyRefreshing$2$comdorocarcarvehicleGwmRepository(z);
            }
        });
    }

    /* renamed from: lambda$notifyRefreshing$2$com-dorocar-car-vehicle-GwmRepository, reason: not valid java name */
    /* synthetic */ void m43lambda$notifyRefreshing$2$comdorocarcarvehicleGwmRepository(boolean z) {
        Iterator<Listener> it = this.listeners.iterator();
        while (it.hasNext()) {
            it.next().onRefreshing(z);
        }
    }

    private void notifyReauth(final String str) {
        this.main.post(new Runnable() { // from class: com.dorocar.car.vehicle.GwmRepository$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                GwmRepository.this.m42lambda$notifyReauth$3$comdorocarcarvehicleGwmRepository(str);
            }
        });
    }

    /* renamed from: lambda$notifyReauth$3$com-dorocar-car-vehicle-GwmRepository, reason: not valid java name */
    /* synthetic */ void m42lambda$notifyReauth$3$comdorocarcarvehicleGwmRepository(String str) {
        Iterator<Listener> it = this.listeners.iterator();
        while (it.hasNext()) {
            it.next().onReauthRequired(str);
        }
    }

    public boolean isLoggedIn() {
        GwmClient gwmClient = this.client;
        return gwmClient != null && gwmClient.isLoggedIn();
    }

    public boolean hasAccount() {
        return this.client != null;
    }

    public String phone() {
        GwmClient gwmClient = this.client;
        return gwmClient == null ? "" : gwmClient.phone;
    }

    public String selectedVin() {
        return this.prefs.getString(KEY_VIN, null);
    }

    public void setSelectedVin(String str) {
        this.prefs.edit().putString(KEY_VIN, str).apply();
    }

    public GwmVehicle selectedVehicle() {
        String selectedVin = selectedVin();
        if (selectedVin == null || this.lastVehicles == null) {
            return null;
        }
        for (GwmVehicle gwmVehicle : this.lastVehicles) {
            if (selectedVin.equalsIgnoreCase(gwmVehicle.vin)) {
                return gwmVehicle;
            }
        }
        return null;
    }

    public List<GwmVehicle> cachedVehicles() {
        return this.lastVehicles;
    }

    public boolean isPollEnabled() {
        return this.prefs.getBoolean(KEY_POLL, true);
    }

    public void setPollEnabled(boolean z) {
        this.prefs.edit().putBoolean(KEY_POLL, z).apply();
        if (z) {
            startPolling();
        } else {
            stopPolling();
        }
    }

    public int pollIntervalSeconds() {
        return this.prefs.getInt(KEY_INTERVAL, 60);
    }

    public void setPollIntervalSeconds(int i) {
        this.prefs.edit().putInt(KEY_INTERVAL, Math.max(30, Math.min(3600, i))).apply();
    }

    public boolean isRemoteEnabled() {
        return this.prefs.getBoolean(KEY_REMOTE, true);
    }

    public void setRemoteEnabled(boolean z) {
        this.prefs.edit().putBoolean(KEY_REMOTE, z).apply();
    }

    public VehicleStatus lastStatus() {
        return this.lastStatus;
    }

    public VehicleStatus currentStatus() {
        return this.lastStatus;
    }

    public String platformOf(String str) {
        GwmClient gwmClient = this.client;
        if (gwmClient == null) {
            return "";
        }
        if (str == null) {
            try {
                str = selectedVin();
            } catch (Exception e) {
                return "";
            }
        }
        return gwmClient.cachedPlatform(str);
    }

    public String lastError() {
        return this.lastError;
    }

    public long lastUpdateAt() {
        return this.lastUpdateAt;
    }

    public void requestSmsCode(final String str, final Callback<Void> callback) {
        this.executor.execute(new Runnable() { // from class: com.dorocar.car.vehicle.GwmRepository$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                GwmRepository.this.m48lambda$requestSmsCode$4$comdorocarcarvehicleGwmRepository(str, callback);
            }
        });
    }

    /* renamed from: lambda$requestSmsCode$4$com-dorocar-car-vehicle-GwmRepository, reason: not valid java name */
    /* synthetic */ void m48lambda$requestSmsCode$4$comdorocarcarvehicleGwmRepository(String str, Callback callback) {
        try {
            GwmClient gwmClient = this.client != null ? this.client : new GwmClient();
            gwmClient.requestSmsCode(str);
            this.client = gwmClient;
            sessionInvalidated = false;
            succeed(callback, null);
        } catch (Exception e) {
            fail(callback, friendly(e));
        }
    }

    public void loginSms(final String str, final String str2, final Callback<List<GwmVehicle>> callback) {
        this.executor.execute(new Runnable() { // from class: com.dorocar.car.vehicle.GwmRepository$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                GwmRepository.this.m39lambda$loginSms$5$comdorocarcarvehicleGwmRepository(str, str2, callback);
            }
        });
    }

    /* renamed from: lambda$loginSms$5$com-dorocar-car-vehicle-GwmRepository, reason: not valid java name */
    /* synthetic */ void m39lambda$loginSms$5$comdorocarcarvehicleGwmRepository(String str, String str2, Callback callback) {
        try {
            GwmClient gwmClient = this.client != null ? this.client : new GwmClient();
            gwmClient.phone = str == null ? "" : str.trim();
            List<GwmVehicle> loginSms = gwmClient.loginSms(str2);
            this.client = gwmClient;
            sessionInvalidated = false;
            this.lastVehicles = Collections.unmodifiableList(new ArrayList(loginSms));
            if (loginSms.size() == 1) {
                setSelectedVin(loginSms.get(0).vin);
            }
            this.store.save(gwmClient);
            this.consecutiveFailures = 0;
            setSessionState(SessionState.ACTIVE, null);
            notifyLoginState();
            startPolling();
            succeed(callback, loginSms);
        } catch (Exception e) {
            fail(callback, friendly(e));
        }
    }

    public void loginPassword(final String str, final String str2, final Callback<List<GwmVehicle>> callback) {
        this.executor.execute(new Runnable() { // from class: com.dorocar.car.vehicle.GwmRepository$$ExternalSyntheticLambda18
            @Override // java.lang.Runnable
            public final void run() {
                GwmRepository.this.m38lambda$loginPassword$6$comdorocarcarvehicleGwmRepository(str, str2, callback);
            }
        });
    }

    /* renamed from: lambda$loginPassword$6$com-dorocar-car-vehicle-GwmRepository, reason: not valid java name */
    /* synthetic */ void m38lambda$loginPassword$6$comdorocarcarvehicleGwmRepository(String str, String str2, Callback callback) {
        try {
            GwmClient gwmClient = this.client != null ? this.client : new GwmClient();
            List<GwmVehicle> loginPassword = gwmClient.loginPassword(str, str2);
            this.client = gwmClient;
            sessionInvalidated = false;
            this.lastVehicles = Collections.unmodifiableList(new ArrayList(loginPassword));
            if (loginPassword.size() == 1) {
                setSelectedVin(loginPassword.get(0).vin);
            }
            this.store.save(gwmClient);
            this.consecutiveFailures = 0;
            setSessionState(SessionState.ACTIVE, null);
            notifyLoginState();
            startPolling();
            succeed(callback, loginPassword);
        } catch (Exception e) {
            fail(callback, friendly(e));
        }
    }

    public void verifySession(final Callback<Void> callback) {
        this.executor.execute(new Runnable() { // from class: com.dorocar.car.vehicle.GwmRepository$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                GwmRepository.this.m54lambda$verifySession$7$comdorocarcarvehicleGwmRepository(callback);
            }
        });
    }

    /* renamed from: lambda$verifySession$7$com-dorocar-car-vehicle-GwmRepository, reason: not valid java name */
    /* synthetic */ void m54lambda$verifySession$7$comdorocarcarvehicleGwmRepository(Callback callback) {
        GwmClient gwmClient = this.client;
        if (gwmClient == null || !gwmClient.isLoggedIn()) {
            fail(callback, "未登录");
            return;
        }
        try {
            gwmClient.getVehicles();
            persistQuietly(gwmClient);
            succeed(callback, null);
        } catch (Exception e) {
            try {
                gwmClient.recoverSession();
                persistQuietly(gwmClient);
                succeed(callback, null);
            } catch (Exception e2) {
                fail(callback, friendly(e2));
            }
        }
    }

    /** 退出后置位：防止队列中尚未执行的旧任务把已清理的会话再写回/恢复 */
    private volatile boolean sessionInvalidated = false;

    public void logout() {
        stopPolling();
        // 同步立即清理：不再排队等后台线程。否则若队列前面积压了网络请求
        // （超时最长 20 秒）或指令重试（最长约 60 秒等待），界面会一直停在"已登录"。
        sessionInvalidated = true;
        try {
            this.store.clear();
        } catch (Throwable th) {
            Log.w(TAG, "清理本地凭据失败", th);
        }
        this.client = null;
        this.lastStatus = null;
        this.lastError = null;
        this.lastUpdateAt = 0L;
        this.lastVehicles = null;
        this.prefs.edit().remove(KEY_VIN).apply();
        setSessionState(SessionState.LOGGED_OUT, null);
        notifyLoginState();
        // 兜底：后台再清一遍，防止与并发写入竞争产生残留
        this.executor.execute(new Runnable() {
            @Override
            public final void run() {
                try {
                    GwmRepository.this.store.clear();
                } catch (Throwable ignored) {
                }
            }
        });
    }

    /* renamed from: lambda$logout$8$com-dorocar-car-vehicle-GwmRepository, reason: not valid java name */
    /* synthetic */ void m40lambda$logout$8$comdorocarcarvehicleGwmRepository() {
        this.store.clear();
        this.client = null;
        setSessionState(SessionState.LOGGED_OUT, null);
        this.lastStatus = null;
        this.lastError = null;
        this.lastUpdateAt = 0L;
        this.lastVehicles = null;
        this.prefs.edit().remove(KEY_VIN).apply();
        notifyLoginState();
    }

    public void fetchVehicles(final Callback<List<GwmVehicle>> callback) {
        this.executor.execute(new Runnable() { // from class: com.dorocar.car.vehicle.GwmRepository$$ExternalSyntheticLambda15
            @Override // java.lang.Runnable
            public final void run() {
                GwmRepository.this.m37lambda$fetchVehicles$9$comdorocarcarvehicleGwmRepository(callback);
            }
        });
    }

    /* renamed from: lambda$fetchVehicles$9$com-dorocar-car-vehicle-GwmRepository, reason: not valid java name */
    /* synthetic */ void m37lambda$fetchVehicles$9$comdorocarcarvehicleGwmRepository(Callback callback) {
        GwmClient gwmClient = this.client;
        if (gwmClient == null) {
            fail(callback, "未登录");
            return;
        }
        try {
            List<GwmVehicle> vehicles = gwmClient.getVehicles();
            this.lastVehicles = Collections.unmodifiableList(new ArrayList(vehicles));
            persistQuietly(gwmClient);
            succeed(callback, vehicles);
        } catch (Exception e) {
            fail(callback, friendly(e));
        }
    }

    /* renamed from: lambda$refreshNow$10$com-dorocar-car-vehicle-GwmRepository, reason: not valid java name */
    /* synthetic */ void m47lambda$refreshNow$10$comdorocarcarvehicleGwmRepository() {
        refreshLocked(true);
    }

    public void refreshNow() {
        this.executor.execute(new Runnable() { // from class: com.dorocar.car.vehicle.GwmRepository$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                GwmRepository.this.m47lambda$refreshNow$10$comdorocarcarvehicleGwmRepository();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void pollOnce() {
        if (this.destroyed) {
            this.pollScheduled = false;
            return;
        }
        if (!isPollEnabled() || !isLoggedIn()) {
            this.pollScheduled = false;
            return;
        }
        if (!this.polling.compareAndSet(false, true)) {
            this.main.postDelayed(this.pollTask, 5000L);
            return;
        }
        try {
            this.executor.execute(new Runnable() { // from class: com.dorocar.car.vehicle.GwmRepository$$ExternalSyntheticLambda11
                @Override // java.lang.Runnable
                public final void run() {
                    GwmRepository.this.m45lambda$pollOnce$11$comdorocarcarvehicleGwmRepository();
                }
            });
        } catch (Exception e) {
            this.polling.set(false);
            this.pollScheduled = false;
        }
    }

    /* renamed from: lambda$pollOnce$11$com-dorocar-car-vehicle-GwmRepository, reason: not valid java name */
    /* synthetic */ void m45lambda$pollOnce$11$comdorocarcarvehicleGwmRepository() {
        try {
            refreshLocked(false);
        } finally {
            this.polling.set(false);
            scheduleNext();
        }
    }

    private void refreshLocked(boolean z) {
        String selectedVin = selectedVin();
        GwmClient gwmClient = this.client;
        if (gwmClient == null || !gwmClient.isLoggedIn()) {
            if (z) {
                notifyStatus(null, "未登录");
                return;
            }
            return;
        }
        if (selectedVin == null || selectedVin.isEmpty()) {
            try {
                List<GwmVehicle> vehicles = gwmClient.getVehicles();
                this.lastVehicles = Collections.unmodifiableList(new ArrayList(vehicles));
                if (vehicles.isEmpty()) {
                    notifyStatus(null, "账号下没有车辆");
                    return;
                } else {
                    selectedVin = vehicles.get(0).vin;
                    setSelectedVin(selectedVin);
                }
            } catch (Exception e) {
                notifyStatus(null, friendly(e));
                return;
            }
        }
        notifyRefreshing(true);
        try {
            try {
                VehicleStatus parsedStatus = gwmClient.getParsedStatus(selectedVin);
                this.lastStatus = parsedStatus;
                this.lastError = null;
                this.lastUpdateAt = System.currentTimeMillis();
                this.consecutiveFailures = 0;
                persistQuietly(gwmClient);
                markSessionActiveIfNeeded();
                notifyStatus(parsedStatus, null);
            } catch (Throwable th) {
                notifyRefreshing(false);
                throw th;
            }
        } catch (GwmClient.AuthException e2) {
            try {
                gwmClient.recoverSession();
                persistQuietly(gwmClient);
                VehicleStatus parsedStatus2 = gwmClient.getParsedStatus(selectedVin);
                this.lastStatus = parsedStatus2;
                this.lastError = null;
                this.lastUpdateAt = System.currentTimeMillis();
                this.consecutiveFailures = 0;
                notifyStatus(parsedStatus2, null);
            } catch (Exception e3) {
                this.consecutiveFailures++;
                String friendly = friendly(e3);
                this.lastError = friendly;
                notifyStatus(this.lastStatus, friendly);
                if (this.consecutiveFailures >= 3) {
                    if (gwmClient.reviveSessionQuietly()) {
                        persistQuietly(gwmClient);
                        this.consecutiveFailures = 0;
                        setSessionState(SessionState.ACTIVE, null);
                        notifyRefreshing(false);
                        return;
                    }
                    setSessionState(SessionState.EXPIRED, "登录已过期，需要重新验证");
                    notifyReauth(friendly);
                    stopPolling();
                }
            }
        } catch (GwmClient.RiskControlException e4) {
            this.consecutiveFailures++;
            this.lastError = friendly(e4);
            notifyStatus(this.lastStatus, this.lastError);
        } catch (Exception e5) {
            this.consecutiveFailures++;
            this.lastError = friendly(e5);
            notifyStatus(this.lastStatus, this.lastError);
        }
        notifyRefreshing(false);
    }

    public void sendCommand(final String str, final Map<String, Object> map, final Callback<String> callback) {
        this.executor.execute(new Runnable() { // from class: com.dorocar.car.vehicle.GwmRepository$$ExternalSyntheticLambda16
            @Override // java.lang.Runnable
            public final void run() {
                GwmRepository.this.m51lambda$sendCommand$14$comdorocarcarvehicleGwmRepository(callback, str, map);
            }
        });
    }

    /* renamed from: lambda$sendCommand$14$com-dorocar-car-vehicle-GwmRepository, reason: not valid java name */
    /* synthetic */ void m51lambda$sendCommand$14$comdorocarcarvehicleGwmRepository(Callback callback, String str, Map map) {
        String selectedVin = selectedVin();
        GwmClient gwmClient = this.client;
        if (gwmClient == null || !gwmClient.isLoggedIn()) {
            fail(callback, "请先登录");
            return;
        }
        if (selectedVin == null || selectedVin.isEmpty()) {
            fail(callback, "请先选择车辆");
            return;
        }
        try {
            String sendCommand = gwmClient.sendCommand(selectedVin, str, map);
            persistQuietly(gwmClient);
            succeed(callback, sendCommand);
            this.main.postDelayed(new Runnable() { // from class: com.dorocar.car.vehicle.GwmRepository$$ExternalSyntheticLambda19
                @Override // java.lang.Runnable
                public final void run() {
                    GwmRepository.this.m50lambda$sendCommand$13$comdorocarcarvehicleGwmRepository();
                }
            }, 6000L);
        } catch (Exception e) {
            fail(callback, friendly(e));
        }
    }

    /* renamed from: lambda$sendCommand$12$com-dorocar-car-vehicle-GwmRepository, reason: not valid java name */
    /* synthetic */ void m49lambda$sendCommand$12$comdorocarcarvehicleGwmRepository() {
        refreshLocked(false);
    }

    /* renamed from: lambda$sendCommand$13$com-dorocar-car-vehicle-GwmRepository, reason: not valid java name */
    /* synthetic */ void m50lambda$sendCommand$13$comdorocarcarvehicleGwmRepository() {
        this.executor.execute(new Runnable() { // from class: com.dorocar.car.vehicle.GwmRepository$$ExternalSyntheticLambda13
            @Override // java.lang.Runnable
            public final void run() {
                GwmRepository.this.m49lambda$sendCommand$12$comdorocarcarvehicleGwmRepository();
            }
        });
    }

    public void queryCommandResult(final String str, final String str2, final Callback<Map<String, Object>> callback) {
        this.executor.execute(new Runnable() { // from class: com.dorocar.car.vehicle.GwmRepository$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                GwmRepository.this.m46x7ce75847(callback, str, str2);
            }
        });
    }

    /* renamed from: lambda$queryCommandResult$15$com-dorocar-car-vehicle-GwmRepository, reason: not valid java name */
    /* synthetic */ void m46x7ce75847(Callback callback, String str, String str2) {
        GwmClient gwmClient = this.client;
        if (gwmClient == null || !gwmClient.isLoggedIn()) {
            fail(callback, "请先登录");
            return;
        }
        if (str == null || str.isEmpty()) {
            str = selectedVin();
        }
        if (str == null || str.isEmpty()) {
            fail(callback, "请先选择车辆");
            return;
        }
        try {
            Object commandResult = gwmClient.getCommandResult(str, str2);
            Map<String, Object> asStringKeyMap = commandResult instanceof Map ? asStringKeyMap(commandResult) : null;
            persistQuietly(gwmClient);
            succeed(callback, asStringKeyMap);
        } catch (Exception e) {
            fail(callback, friendly(e));
        }
    }

    private static Map<String, Object> asStringKeyMap(Object obj) {
        try {
            return (Map) obj;
        } catch (ClassCastException e) {
            return null;
        }
    }

    public void startPolling() {
        this.main.post(new Runnable() { // from class: com.dorocar.car.vehicle.GwmRepository$$ExternalSyntheticLambda12
            @Override // java.lang.Runnable
            public final void run() {
                GwmRepository.this.m52lambda$startPolling$16$comdorocarcarvehicleGwmRepository();
            }
        });
    }

    /* renamed from: lambda$startPolling$16$com-dorocar-car-vehicle-GwmRepository, reason: not valid java name */
    /* synthetic */ void m52lambda$startPolling$16$comdorocarcarvehicleGwmRepository() {
        if (!this.destroyed && isPollEnabled() && isLoggedIn() && !this.pollScheduled) {
            this.pollScheduled = true;
            this.main.removeCallbacks(this.pollTask);
            this.main.postDelayed(this.pollTask, 1500L);
        }
    }

    public void stopPolling() {
        this.main.post(new Runnable() { // from class: com.dorocar.car.vehicle.GwmRepository$$ExternalSyntheticLambda9
            @Override // java.lang.Runnable
            public final void run() {
                GwmRepository.this.m53lambda$stopPolling$17$comdorocarcarvehicleGwmRepository();
            }
        });
    }

    /* renamed from: lambda$stopPolling$17$com-dorocar-car-vehicle-GwmRepository, reason: not valid java name */
    /* synthetic */ void m53lambda$stopPolling$17$comdorocarcarvehicleGwmRepository() {
        this.pollScheduled = false;
        this.main.removeCallbacks(this.pollTask);
    }

    private void scheduleNext() {
        if (this.destroyed || !isPollEnabled() || !isLoggedIn()) {
            this.pollScheduled = false;
            return;
        }
        long pollIntervalSeconds = pollIntervalSeconds() * 1000;
        if (this.consecutiveFailures > 0) {
            pollIntervalSeconds = Math.min(600000L, pollIntervalSeconds * (1 << Math.min(this.consecutiveFailures, 6)));
        }
        this.pollScheduled = true;
        this.main.removeCallbacks(this.pollTask);
        this.main.postDelayed(this.pollTask, pollIntervalSeconds);
    }

    private void persistQuietly(GwmClient gwmClient) {
        // 退出登录后，队列里残留的旧任务不得把已清理的会话再写回磁盘
        if (gwmClient == null || sessionInvalidated || this.client != gwmClient) {
            return;
        }
        try {
            this.store.save(gwmClient);
        } catch (Throwable th) {
            Log.w(TAG, "凭据保存失败", th);
        }
    }

    private <T> void succeed(final Callback<T> callback, final T t) {
        if (callback == null) {
            return;
        }
        this.main.post(new Runnable() { // from class: com.dorocar.car.vehicle.GwmRepository$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                callback.onSuccess(t);
            }
        });
    }

    private void fail(final Callback<?> callback, final String str) {
        if (callback == null) {
            return;
        }
        this.main.post(new Runnable() { // from class: com.dorocar.car.vehicle.GwmRepository$$ExternalSyntheticLambda17
            @Override // java.lang.Runnable
            public final void run() {
                callback.onError(str);
            }
        });
    }

    private static String friendly(Exception exc) {
        if (exc instanceof GwmClient.RiskControlException) {
            return exc.getMessage();
        }
        if (exc instanceof GwmClient.AuthException) {
            String message = exc.getMessage();
            return (message == null || !message.contains("1013")) ? message == null ? "认证失败" : message : "触发风控，请在官方 APP 完成验证后重试";
        }
        if (exc instanceof GwmClient.ConnectionException) {
            return "网络异常：" + exc.getMessage();
        }
        if (exc instanceof GwmClient.SchemaException) {
            return "数据异常：" + exc.getMessage();
        }
        if (exc instanceof UnknownHostException) {
            return "无法连接服务器，请检查网络";
        }
        if (exc instanceof SocketTimeoutException) {
            return "连接超时，请稍后重试";
        }
        String message2 = exc.getMessage();
        return (message2 == null || message2.isEmpty()) ? exc.getClass().getSimpleName() : message2;
    }

    public void shutdown() {
        this.destroyed = true;
        stopPolling();
    }
}
