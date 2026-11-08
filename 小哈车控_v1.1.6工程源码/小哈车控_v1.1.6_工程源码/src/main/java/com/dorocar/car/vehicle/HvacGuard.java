package com.dorocar.car.vehicle;

import java.util.Locale;

/* loaded from: classes.dex */
public final class HvacGuard {
    private static final String NO_STATUS = "车况还没读到，无法确认车辆是否启动，暂时不能开空调";

    public static final class Verdict {
        public final boolean allowed;
        public final String message;

        Verdict(boolean z, String str) {
            this.allowed = z;
            this.message = str;
        }
    }

    private HvacGuard() {
    }

    public static boolean isVehicleStarted(VehicleStatus vehicleStatus) {
        return vehicleStatus != null && vehicleStatus.isEngineRunning();
    }

    public static Verdict canTurnOn(VehicleStatus vehicleStatus) {
        if (vehicleStatus == null) {
            return new Verdict(false, NO_STATUS);
        }
        return new Verdict(true, "可以远程开空调");
    }

    public static Verdict canTurnOff(VehicleStatus vehicleStatus) {
        return new Verdict(true, "可以关空调");
    }

    public static String hint(VehicleStatus vehicleStatus) {
        return vehicleStatus == null ? "车况未同步" : isVehicleStarted(vehicleStatus) ? "车辆已启动 · 可远程控制空调" : "车辆未启动 · 可远程开启空调（会一并启动发动机）";
    }

    public static boolean platformSupportsHvac(String str) {
        return true;
    }

    public static boolean platformSupportsAux(String str) {
        if (str == null || str.isEmpty()) {
            return true;
        }
        String lowerCase = str.toLowerCase(Locale.ROOT);
        return (lowerCase.contains("gtsp") || lowerCase.contains("navinfo")) ? false : true;
    }

    public static String unsupportedPlatformText(String str) {
        StringBuilder append = new StringBuilder().append("当前车机平台（");
        if (str == null) {
            str = "未知";
        }
        return append.append(str).append("）未开放除霜/方向盘加热指令，可改用开空调调节车内温度").toString();
    }
}
