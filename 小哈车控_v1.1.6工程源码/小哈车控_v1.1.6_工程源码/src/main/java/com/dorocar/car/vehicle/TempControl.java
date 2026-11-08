package com.dorocar.car.vehicle;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/* loaded from: classes.dex */
public final class TempControl {
    private static final int ALLOW_START_ENG = 1;
    public static final float DEFAULT_C = 24.0f;
    public static final int DEFAULT_DURATION_SECONDS = 600;
    private static final String KEY_TEMP = "temperature";
    public static final float MAX_C = 31.0f;
    public static final float MIN_C = 17.0f;
    public static final float STEP_C = 0.5f;
    public static final String ZONE_BOTH = "BOTH";
    public static final String ZONE_DRIVER = "DRIVER";
    public static final String ZONE_PASSENGER = "PASSENGER";

    private TempControl() {
    }

    public static boolean inRange(float f) {
        return f >= 16.999f && f <= 31.001f;
    }

    public static float normalize(float f) {
        return Math.round(Math.max(17.0f, Math.min(31.0f, Math.round(f / 0.5f) * 0.5f)) * 10.0f) / 10.0f;
    }

    public static float up(float f) {
        return normalize(normalize(f) + 0.5f);
    }

    public static float down(float f) {
        return normalize(normalize(f) - 0.5f);
    }

    public static boolean atMax(float f) {
        return normalize(f) >= 31.0f;
    }

    public static boolean atMin(float f) {
        return normalize(f) <= 17.0f;
    }

    public static final class Verdict {
        public final boolean allowed;
        public final String message;

        Verdict(boolean z, String str) {
            this.allowed = z;
            this.message = str;
        }
    }

    public static Verdict canSet(VehicleStatus vehicleStatus, float f) {
        if (!inRange(f)) {
            return new Verdict(false, String.format(Locale.ROOT, "温度超出可调范围（%.0f~%.0f℃）", Float.valueOf(17.0f), Float.valueOf(31.0f)));
        }
        return new Verdict(HvacGuard.canTurnOn(vehicleStatus).allowed, HvacGuard.canTurnOn(vehicleStatus).message);
    }

    public static Map<String, Object> bodiesOf(float f, String str) {
        int round = Math.round(normalize(f));
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        linkedHashMap.put("allowStartEng", Integer.valueOf(ALLOW_START_ENG));
        linkedHashMap.put("operationTime", Integer.valueOf(DEFAULT_DURATION_SECONDS));
        linkedHashMap.put(KEY_TEMP, Integer.valueOf(round));
        return linkedHashMap;
    }

    public static String labelOf(float f) {
        return "调温至 " + format(f);
    }

    public static String format(float f) {
        return format(f, false);
    }

    public static String format(float f, boolean z) {
        float normalize = normalize(f);
        boolean z2 = Math.abs(normalize - ((float) Math.round(normalize))) < 0.001f;
        if (z) {
            return String.format(Locale.ROOT, "%.0f°F", Float.valueOf(((normalize * 9.0f) / 5.0f) + 32.0f));
        }
        return String.format(Locale.ROOT, z2 ? "%.0f°C" : "%.1f°C", Float.valueOf(normalize));
    }

    public static String zoneLabel(String str) {
        return ZONE_DRIVER.equals(str) ? "主驾" : ZONE_PASSENGER.equals(str) ? "副驾" : "全车";
    }

    public static final class Response {
        public final String note;
        public final boolean verifiable;

        Response(boolean z, String str) {
            this.verifiable = z;
            this.note = str;
        }
    }

    public static Response responseOf(VehicleStatus vehicleStatus) {
        if (vehicleStatus == null) {
            return new Response(false, "车况还没拉到，无法验证温度");
        }
        if (vehicleStatus.acTempSet == null) {
            return new Response(false, "该车型不上报空调设定温度，只能确认已下发");
        }
        return new Response(true, "可验证");
    }
}
