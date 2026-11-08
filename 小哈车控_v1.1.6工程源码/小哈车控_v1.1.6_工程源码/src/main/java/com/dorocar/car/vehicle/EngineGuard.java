package com.dorocar.car.vehicle;

import java.util.Locale;

/* loaded from: classes.dex */
public final class EngineGuard {
    public static final double MIN_BATTERY_PERCENT = 15.0d;
    public static final double MIN_BATTERY_VOLTAGE = 11.6d;

    public static final class Verdict {
        public final boolean allowed;
        public final String message;
        public final boolean needsConfirm;

        Verdict(boolean z, boolean z2, String str) {
            this.allowed = z;
            this.needsConfirm = z2;
            this.message = str;
        }
    }

    private EngineGuard() {
    }

    public static Verdict canStart(VehicleStatus vehicleStatus) {
        if (vehicleStatus == null) {
            return new Verdict(false, false, "车况还没读到，无法确认车辆状态，暂时不能远程启动");
        }
        if (vehicleStatus.engineState == null) {
            return new Verdict(false, false, "该车型没有上报启动状态，无法远程启动");
        }
        if (vehicleStatus.isEngineRunning()) {
            return new Verdict(false, false, "车辆已在" + vehicleStatus.engineText() + "，无需重复启动");
        }
        if (vehicleStatus.hasOpenDoor()) {
            return new Verdict(true, true, "有车门或后备箱未关闭，远程启动前请确认车内无人、周围安全");
        }
        if (vehicleStatus.batteryPercent != null && vehicleStatus.batteryPercent.doubleValue() < 15.0d) {
            return new Verdict(false, false, String.format(Locale.ROOT, "电量仅剩 %.0f%%，低于 %.0f%%，不建议远程启动", vehicleStatus.batteryPercent, Double.valueOf(15.0d)));
        }
        if (vehicleStatus.batteryVoltage != null && vehicleStatus.batteryVoltage.doubleValue() < 11.6d) {
            return new Verdict(false, false, String.format(Locale.ROOT, "蓄电池电压仅 %.1fV，可能无法启动", vehicleStatus.batteryVoltage));
        }
        return new Verdict(true, false, "可以远程启动");
    }

    public static Verdict canStop(VehicleStatus vehicleStatus) {
        if (vehicleStatus == null) {
            return new Verdict(true, false, "可以远程熄火");
        }
        if (vehicleStatus.engineState != null && !vehicleStatus.isEngineRunning()) {
            return new Verdict(false, false, "车辆当前已" + vehicleStatus.engineText() + "，无需熄火");
        }
        return new Verdict(true, false, "可以远程熄火");
    }

    public static String hint(VehicleStatus vehicleStatus) {
        if (vehicleStatus == null) {
            return "车况未同步";
        }
        if (vehicleStatus.engineState == null) {
            return "该车型不上报启动状态";
        }
        if (vehicleStatus.isEngineRunning()) {
            return vehicleStatus.engineText() + " · 可远程熄火";
        }
        return "已" + vehicleStatus.engineText() + " · 可远程启动";
    }
}
