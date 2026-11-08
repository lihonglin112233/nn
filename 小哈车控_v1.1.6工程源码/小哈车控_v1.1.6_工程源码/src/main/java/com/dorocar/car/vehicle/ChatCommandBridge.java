package com.dorocar.car.vehicle;

import android.content.Context;
import android.util.Log;
import com.dorocar.car.vehicle.CommandExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* loaded from: classes.dex */
public final class ChatCommandBridge {
    private static final String TAG = "ChatCommandBridge";
    private static final Pattern TEMP_PATTERN = Pattern.compile("(\\d{2})\\s*度?");

    public interface Listener {
        void onResult(boolean z, String str);
    }

    private ChatCommandBridge() {
    }

    public static boolean handle(Context context, String str) {
        return handle(context, str, null);
    }

    public static boolean handle(Context context, String str, final Listener listener) {
        if (str == null) {
            notifyResult(listener, false, "空内容");
            return false;
        }
        final Parsed parse = parse(str);
        if (parse == null) {
            Log.i(TAG, "未命中任何车控关键词: " + str);
            notifyResult(listener, false, "未识别到车控指令：" + str);
            return false;
        }
        GwmRepository gwmRepository = GwmRepository.get(context);
        if (!gwmRepository.isLoggedIn()) {
            Log.w(TAG, "未登录，忽略指令: " + parse.action);
            notifyResult(listener, false, "小哈车控未登录");
            return false;
        }
        CommandExecutor commandExecutor = new CommandExecutor(context, gwmRepository);
        CommandExecutor.Callback callback = new CommandExecutor.Callback() { // from class: com.dorocar.car.vehicle.ChatCommandBridge.1
            @Override // com.dorocar.car.vehicle.CommandExecutor.Callback
            public /* synthetic */ void onProgress(String str2) {
                CommandExecutor.Callback.CC.$default$onProgress(this, str2);
            }

            @Override // com.dorocar.car.vehicle.CommandExecutor.Callback
            public void onResult(CommandExecutor.Result result) {
                Log.i(ChatCommandBridge.TAG, "指令 " + parse.action + " 结果: " + result.message);
                ChatCommandBridge.notifyResult(listener, result.success || result.partial, result.message);
            }
        };
        if ("ENGINE_START".equals(parse.action)) {
            commandExecutor.run(parse.action, null, parse.minutes, callback);
        } else if (VehicleActions.AC_ON.equals(parse.action)) {
            commandExecutor.run(parse.action, TempControl.bodiesOf(24.0f, TempControl.ZONE_BOTH), callback);
        } else if (VehicleActions.AC_TEMP_SET.equals(parse.action) && parse.temp != null) {
            commandExecutor.run(parse.action, TempControl.bodiesOf(parse.temp.floatValue(), TempControl.ZONE_BOTH), callback);
        } else {
            commandExecutor.run(parse.action, null, callback);
        }
        Log.i(TAG, "已提交: " + parse.action + (parse.temp == null ? "" : " @" + parse.temp + "℃"));
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void notifyResult(Listener listener, boolean z, String str) {
        if (listener == null) {
            return;
        }
        try {
            listener.onResult(z, str);
        } catch (Throwable th) {
            Log.w(TAG, "回调执行结果失败", th);
        }
    }

    static final class Parsed {
        final String action;
        final int minutes;
        final Float temp;

        Parsed(String str, Float f, int i) {
            this.action = str;
            this.temp = f;
            this.minutes = i;
        }
    }

    static Parsed parse(String str) {
        String trim = str.replace("\n", " ").trim();
        if (trim.isEmpty()) {
            return null;
        }
        if (hit(trim, "关车窗", "关窗", "车窗关上", "车窗关闭")) {
            return new Parsed("WINDOW_CLOSE", null, 0);
        }
        if (hit(trim, "远程熄火", "熄火")) {
            return new Parsed("ENGINE_STOP", null, 0);
        }
        if (hit(trim, "远程启动", "启动发动机", "启动")) {
            return new Parsed("ENGINE_START", null, 15);
        }
        if (hit(trim, "关空调")) {
            return new Parsed(VehicleActions.AC_OFF, null, 0);
        }
        if (hit(trim, "开空调")) {
            Float temp = temp(trim);
            if (temp == null) {
                return new Parsed(VehicleActions.AC_ON, null, 0);
            }
            return new Parsed(VehicleActions.AC_TEMP_SET, temp, 0);
        }
        if (hit(trim, "前除霜")) {
            return new Parsed(VehicleActions.DEFROST_FRONT, null, 0);
        }
        if (hit(trim, "后除霜")) {
            return new Parsed(VehicleActions.DEFROST_REAR, null, 0);
        }
        if (hit(trim, "方向盘加热")) {
            return new Parsed(VehicleActions.STEER_HEAT, null, 0);
        }
        if (hit(trim, "鸣笛", "找车", "按喇叭")) {
            return new Parsed("WHISTLE", null, 0);
        }
        if (hit(trim, "闪灯", "闪一下")) {
            return new Parsed("FLASH", null, 0);
        }
        if (hit(trim, "解锁", "开门")) {
            return new Parsed(VehicleActions.UNLOCK, null, 0);
        }
        if (hit(trim, "锁车", "锁门", "上锁")) {
            return new Parsed(VehicleActions.LOCK, null, 0);
        }
        Float temp2 = temp(trim);
        if (temp2 != null) {
            return new Parsed(VehicleActions.AC_TEMP_SET, temp2, 0);
        }
        return null;
    }

    private static Float temp(String str) {
        Matcher matcher = TEMP_PATTERN.matcher(str);
        while (matcher.find()) {
            try {
                int parseInt = Integer.parseInt(matcher.group(1));
                if (parseInt >= 16 && parseInt <= 32) {
                    return Float.valueOf(parseInt);
                }
            } catch (NumberFormatException e) {
            }
        }
        return null;
    }

    private static boolean hit(String str, String... strArr) {
        for (String str2 : strArr) {
            if (str.contains(str2)) {
                return true;
            }
        }
        return false;
    }
}
