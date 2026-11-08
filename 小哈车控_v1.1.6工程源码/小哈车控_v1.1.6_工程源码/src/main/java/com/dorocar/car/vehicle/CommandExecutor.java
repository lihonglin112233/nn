package com.dorocar.car.vehicle;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.dorocar.car.vehicle.CommandExecutor;
import com.dorocar.car.vehicle.CommandOutcome;
import com.dorocar.car.vehicle.EngineGuard;
import com.dorocar.car.vehicle.GwmRepository;
import com.dorocar.car.vehicle.HvacGuard;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/* loaded from: classes.dex */
public final class CommandExecutor {
    private static final String TAG = "CommandExecutor";
    private static final long VERIFY_INTERVAL_MS = 4000;
    private static final long VERIFY_TIMEOUT_MS = 48000;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final GwmRepository repo;

    public static final class Result {
        public final String message;
        public final boolean partial;
        public final List<CommandOutcome.Step> steps;
        public final boolean success;
        public final boolean verified;

        Result(boolean z, boolean z2, String str, List<CommandOutcome.Step> list, boolean z3) {
            this.success = z;
            this.partial = z2;
            this.message = str;
            this.steps = list;
            this.verified = z3;
        }
    }

    public interface Callback {
        void onProgress(String str);

        void onResult(Result result);

        /* renamed from: com.dorocar.car.vehicle.CommandExecutor$Callback$-CC, reason: invalid class name */
        public final /* synthetic */ class CC {
            public static void $default$onProgress(Callback _this, String str) {
            }
        }
    }

    public CommandExecutor(Context context, GwmRepository gwmRepository) {
        this.repo = gwmRepository;
    }

    public void run(String str, Map<String, Object> map, Callback callback) {
        run(str, map, 15, callback);
    }

    /* JADX WARN: Removed duplicated region for block: B:26:0x00cc  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void run(String str, Map<String, Object> map, int i, Callback callback) {
        Map<String, Object> acBodyOf;
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        ArrayList arrayList3 = new ArrayList();
        VehicleStatus currentStatus = this.repo.currentStatus();
        if (VehicleActions.needsEngine(str)) {
            HvacGuard.Verdict canTurnOn = HvacGuard.canTurnOn(currentStatus);
            if (!canTurnOn.allowed) {
                callback.onResult(new Result(false, false, canTurnOn.message, arrayList, false));
                return;
            }
        }
        if ("ENGINE_START".equals(str)) {
            EngineGuard.Verdict canStart = EngineGuard.canStart(currentStatus);
            if (!canStart.allowed) {
                callback.onResult(new Result(false, false, canStart.message, arrayList, false));
                return;
            }
        } else if ("ENGINE_STOP".equals(str)) {
            EngineGuard.Verdict canStop = EngineGuard.canStop(currentStatus);
            if (!canStop.allowed) {
                callback.onResult(new Result(false, false, canStop.message, arrayList, false));
                return;
            }
        }
        String controlType = VehicleActions.controlType(str);
        if (controlType == null) {
            callback.onResult(new Result(false, false, "暂不支持该动作", arrayList, false));
            return;
        }
        arrayList3.add(controlType);
        arrayList.add(VehicleActions.stepOf(str, tempOf(map), zoneOf(map)));
        if ("ENGINE_START".equals(str)) {
            acBodyOf = VehicleActions.bodyOf(str, i);
            if (map != null) {
                acBodyOf.putAll(map);
            }
            arrayList2.add(acBodyOf);
        } else {
            if (VehicleActions.AC_ON.equals(str)) {
                acBodyOf = VehicleActions.acBodyOf(tempOf(map));
                if (map != null) {
                    acBodyOf.putAll(map);
                }
                map = acBodyOf;
            }
            arrayList2.add(map);
            if (VehicleActions.LOCK.equals(str)) {
                ArrayList arrayList4 = new ArrayList();
                if (currentStatus != null) {
                    if (!allSideWindowsClosed(currentStatus)) {
                        arrayList3.add("WINDOW_CLOSE");
                        arrayList2.add(null);
                        arrayList.add(CommandOutcome.step("WINDOW_CLOSE", "关车窗", CommandOutcome.SIDEWINDOWS_CLOSED));
                        arrayList4.add("车窗");
                    }
                    if (Boolean.TRUE.equals(currentStatus.sunroof)) {
                        arrayList3.add(GwmClient.CMD_SKYLIGHT_CLOSE);
                        arrayList2.add(null);
                        arrayList.add(CommandOutcome.step(GwmClient.CMD_SKYLIGHT_CLOSE, "关天窗", CommandOutcome.SKYLIGHT_CLOSED));
                        arrayList4.add("天窗");
                    }
                } else {
                    arrayList3.add("WINDOW_CLOSE");
                    arrayList2.add(null);
                    arrayList.add(CommandOutcome.step("WINDOW_CLOSE", "关车窗", CommandOutcome.SIDEWINDOWS_CLOSED));
                    arrayList3.add(GwmClient.CMD_SKYLIGHT_CLOSE);
                    arrayList2.add(null);
                    arrayList.add(CommandOutcome.step(GwmClient.CMD_SKYLIGHT_CLOSE, "关天窗", CommandOutcome.SKYLIGHT_CLOSED));
                }
                if (!arrayList4.isEmpty() && currentStatus != null) {
                    callback.onProgress("检测到" + CommandExecutor$$ExternalSyntheticBackport0.m("、", arrayList4) + "未关闭，一并下发");
                }
            }
        }
        sendSequentially(arrayList3, arrayList2, arrayList, currentStatus, 0, callback);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendSequentially(List<String> list, List<Map<String, Object>> list2, List<CommandOutcome.Step> list3, VehicleStatus vehicleStatus, int i, Callback callback) {
        if (i >= list.size()) {
            verify(list3, vehicleStatus, callback);
            return;
        }
        String str = list.get(i);
        String labelOf = labelOf(list3, str, i);
        if (i > 0) {
            callback.onProgress("正在下发：" + labelOf + "…");
        }
        this.repo.sendCommand(str, list2.get(i), new AnonymousClass1(list, list2, list3, vehicleStatus, i, callback, labelOf));
    }

    /* renamed from: com.dorocar.car.vehicle.CommandExecutor$1, reason: invalid class name */
    class AnonymousClass1 implements GwmRepository.Callback<String> {
        final /* synthetic */ VehicleStatus val$before;
        final /* synthetic */ List val$bodies;
        final /* synthetic */ Callback val$cb;
        final /* synthetic */ int val$index;
        final /* synthetic */ String val$label;
        final /* synthetic */ List val$steps;
        final /* synthetic */ List val$types;

        AnonymousClass1(List list, List list2, List list3, VehicleStatus vehicleStatus, int i, Callback callback, String str) {
            this.val$types = list;
            this.val$bodies = list2;
            this.val$steps = list3;
            this.val$before = vehicleStatus;
            this.val$index = i;
            this.val$cb = callback;
            this.val$label = str;
        }

        @Override // com.dorocar.car.vehicle.GwmRepository.Callback
        public void onSuccess(String str) {
            Handler handler = CommandExecutor.this.main;
            final List list = this.val$types;
            final List list2 = this.val$bodies;
            final List list3 = this.val$steps;
            final VehicleStatus vehicleStatus = this.val$before;
            final int i = this.val$index;
            final Callback callback = this.val$cb;
            handler.postDelayed(new Runnable() { // from class: com.dorocar.car.vehicle.CommandExecutor$1$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    CommandExecutor.AnonymousClass1.this.m35lambda$onSuccess$0$comdorocarcarvehicleCommandExecutor$1(list, list2, list3, vehicleStatus, i, callback);
                }
            }, 1200L);
        }

        /* renamed from: lambda$onSuccess$0$com-dorocar-car-vehicle-CommandExecutor$1, reason: not valid java name */
        /* synthetic */ void m35lambda$onSuccess$0$comdorocarcarvehicleCommandExecutor$1(List list, List list2, List list3, VehicleStatus vehicleStatus, int i, Callback callback) {
            CommandExecutor.this.sendSequentially(list, list2, list3, vehicleStatus, i + 1, callback);
        }

        @Override // com.dorocar.car.vehicle.GwmRepository.Callback
        public void onError(String str) {
            if (this.val$index == 0) {
                this.val$cb.onResult(new Result(false, false, ((CommandOutcome.Step) this.val$steps.get(0)).label + "下发失败：" + str, this.val$steps, false));
                return;
            }
            Log.w(CommandExecutor.TAG, this.val$label + " 下发失败：" + str);
            this.val$cb.onProgress(this.val$label + "下发失败，继续校验其他项");
            Handler handler = CommandExecutor.this.main;
            final List list = this.val$types;
            final List list2 = this.val$bodies;
            final List list3 = this.val$steps;
            final VehicleStatus vehicleStatus = this.val$before;
            final int i = this.val$index;
            final Callback callback = this.val$cb;
            handler.postDelayed(new Runnable() { // from class: com.dorocar.car.vehicle.CommandExecutor$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    CommandExecutor.AnonymousClass1.this.m34lambda$onError$1$comdorocarcarvehicleCommandExecutor$1(list, list2, list3, vehicleStatus, i, callback);
                }
            }, 800L);
        }

        /* renamed from: lambda$onError$1$com-dorocar-car-vehicle-CommandExecutor$1, reason: not valid java name */
        /* synthetic */ void m34lambda$onError$1$comdorocarcarvehicleCommandExecutor$1(List list, List list2, List list3, VehicleStatus vehicleStatus, int i, Callback callback) {
            CommandExecutor.this.sendSequentially(list, list2, list3, vehicleStatus, i + 1, callback);
        }
    }

    private static String labelOf(List<CommandOutcome.Step> list, String str, int i) {
        for (CommandOutcome.Step step : list) {
            if (str.equals(step.controlType)) {
                return step.label;
            }
        }
        return i < list.size() ? list.get(i).label : str;
    }

    private void verify(List<CommandOutcome.Step> list, VehicleStatus vehicleStatus, Callback callback) {
        tick(list, vehicleStatus, callback, System.currentTimeMillis(), 0);
    }

    private void tick(final List<CommandOutcome.Step> list, final VehicleStatus vehicleStatus, final Callback callback, final long j, final int i) {
        this.repo.refreshNow();
        this.main.postDelayed(new Runnable() { // from class: com.dorocar.car.vehicle.CommandExecutor$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                CommandExecutor.this.m33lambda$tick$0$comdorocarcarvehicleCommandExecutor(list, j, vehicleStatus, callback, i);
            }
        }, VERIFY_INTERVAL_MS);
    }

    /* renamed from: lambda$tick$0$com-dorocar-car-vehicle-CommandExecutor, reason: not valid java name */
    /* synthetic */ void m33lambda$tick$0$comdorocarcarvehicleCommandExecutor(List list, long j, VehicleStatus vehicleStatus, Callback callback, int i) {
        String str;
        VehicleStatus currentStatus = this.repo.currentStatus();
        CommandOutcome.State judgeAll = CommandOutcome.judgeAll(list, currentStatus);
        long currentTimeMillis = System.currentTimeMillis() - j;
        String str2 = ((CommandOutcome.Step) list.get(0)).label;
        if (judgeAll == CommandOutcome.State.DONE) {
            callback.onResult(new Result(true, false, str2 + "成功" + CommandOutcome.sideEffects(list, vehicleStatus, currentStatus), list, true));
            return;
        }
        if (judgeAll != CommandOutcome.State.UNVERIFIABLE || i < 2) {
            if (currentTimeMillis > VERIFY_TIMEOUT_MS) {
                callback.onResult(new Result(false, !CommandOutcome.done(list, currentStatus).isEmpty(), CommandOutcome.feedback(str2, list, currentStatus, true), list, false));
                return;
            } else {
                tick(list, vehicleStatus, callback, j, i + 1);
                return;
            }
        }
        if (isTempStep((CommandOutcome.Step) list.get(0))) {
            str = "（该车型不回读设定温度，无法确认是否生效）";
        } else {
            str = "（该车型无状态可校验）";
        }
        callback.onResult(new Result(true, false, str2 + "：指令已下发" + str, list, false));
    }

    private static Float tempOf(Map<String, Object> map) {
        Object obj = map == null ? null : map.get("temperature");
        if (obj instanceof Number) {
            return Float.valueOf(((Number) obj).floatValue());
        }
        return null;
    }

    private static String zoneOf(Map<String, Object> map) {
        Object obj = map == null ? null : map.get("zone");
        if (obj instanceof String) {
            return (String) obj;
        }
        return null;
    }

    private static boolean isTempStep(CommandOutcome.Step step) {
        return (step == null || step.label == null || !step.label.startsWith("调温至")) ? false : true;
    }

    private static boolean allSideWindowsClosed(VehicleStatus vehicleStatus) {
        return Boolean.TRUE.equals(CommandOutcome.SIDEWINDOWS_CLOSED.read(vehicleStatus));
    }
}
