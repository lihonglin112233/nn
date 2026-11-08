package com.dorocar.car.vehicle;

import com.dorocar.car.vehicle.CommandOutcome;
import java.util.HashMap;
import java.util.Map;

/* loaded from: classes.dex */
public final class VehicleActions {
    public static final String AC_OFF = "AC_OFF";
    public static final String AC_ON = "AC_ON";
    public static final String AC_TEMP_SET = "AC_TEMP_SET";
    public static final int DEFAULT_RUN_MINUTES = 15;
    public static final String DEFROST_FRONT = "DEFROST_FRONT";
    public static final String DEFROST_REAR = "DEFROST_REAR";
    public static final String ENGINE_START = "ENGINE_START";
    public static final String ENGINE_STOP = "ENGINE_STOP";
    public static final String FLASH = "FLASH";
    public static final String LOCK = "LOCK";
    public static final String STEER_HEAT = "STEER_HEAT";
    public static final String UNLOCK = "UNLOCK";
    public static final String WHISTLE = "WHISTLE";
    public static final String WINDOW_CLOSE = "WINDOW_CLOSE";
    private static final Map<String, String> CONTROL = new HashMap();
    private static final Map<String, String> LABELS = new HashMap();

    static {
        map(LOCK, GwmClient.CMD_LOCK, "锁车");
        map(UNLOCK, GwmClient.CMD_UNLOCK, "解锁");
        map("WINDOW_CLOSE", "WINDOW_CLOSE", "关车窗");
        map("WHISTLE", "WHISTLE", "鸣笛");
        map("FLASH", "FLASH", "闪灯");
        map(AC_ON, GwmClient.CMD_AC_START, "开空调");
        map(AC_OFF, GwmClient.CMD_AC_STOP, "关空调");
        map(DEFROST_FRONT, GwmClient.CMD_DEFROST_FRONT_START, "前除霜");
        map(DEFROST_REAR, GwmClient.CMD_DEFROST_BACK_START, "后除霜");
        map(STEER_HEAT, GwmClient.CMD_STEER_HEAT_START, "方向盘加热");
        map("ENGINE_START", "ENGINE_START", "远程启动");
        map("ENGINE_STOP", "ENGINE_STOP", "远程熄火");
        map(AC_TEMP_SET, GwmClient.CMD_AC_START, "调温");
    }

    private static void map(String str, String str2, String str3) {
        CONTROL.put(str, str2);
        LABELS.put(str, str3);
    }

    private VehicleActions() {
    }

    public static String controlType(String str) {
        return CONTROL.get(str);
    }

    public static String label(String str) {
        String str2 = LABELS.get(str);
        return str2 == null ? str : str2;
    }

    public static boolean needsEngine(String str) {
        return AC_ON.equals(str) || AC_TEMP_SET.equals(str) || DEFROST_FRONT.equals(str) || DEFROST_REAR.equals(str) || STEER_HEAT.equals(str);
    }

    public static CommandOutcome.Step stepOf(String str) {
        return stepOf(str, null, null);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static CommandOutcome.Step stepOf(String str, Float f, String str2) {
        char c;
        String label = label(str);
        String controlType = controlType(str);
        switch (str.hashCode()) {
            case -1848679223:
                if (str.equals("WINDOW_CLOSE")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -1787112636:
                if (str.equals(UNLOCK)) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case -1567043372:
                if (str.equals(AC_TEMP_SET)) {
                    c = '\n';
                    break;
                }
                c = 65535;
                break;
            case -1387459650:
                if (str.equals(STEER_HEAT)) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case -729353843:
                if (str.equals(DEFROST_FRONT)) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case -411343905:
                if (str.equals("ENGINE_STOP")) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case 2342187:
                if (str.equals(LOCK)) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 62118684:
                if (str.equals(AC_ON)) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 133227525:
                if (str.equals("ENGINE_START")) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case 1085195680:
                if (str.equals(DEFROST_REAR)) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case 1925679026:
                if (str.equals(AC_OFF)) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
            case 1:
                if (UNLOCK.equals(str)) {
                    return CommandOutcome.step(controlType, label, new CommandOutcome.Probe() { // from class: com.dorocar.car.vehicle.VehicleActions$$ExternalSyntheticLambda0
                        @Override // com.dorocar.car.vehicle.CommandOutcome.Probe
                        public final Boolean read(VehicleStatus vehicleStatus) {
                            return VehicleActions.lambda$stepOf$0(vehicleStatus);
                        }
                    });
                }
                return CommandOutcome.step(controlType, label, CommandOutcome.LOCKED);
            case 2:
                return CommandOutcome.step(controlType, label, CommandOutcome.SIDEWINDOWS_CLOSED);
            case 3:
                return CommandOutcome.step(controlType, label, CommandOutcome.AC_ON);
            case 4:
                return CommandOutcome.step(controlType, label, CommandOutcome.AC_OFF);
            case 5:
                return CommandOutcome.step(controlType, label, CommandOutcome.FRONT_DEFROST_ON);
            case 6:
                return CommandOutcome.step(controlType, label, CommandOutcome.REAR_DEFROST_ON);
            case 7:
                return CommandOutcome.step(controlType, label, CommandOutcome.STEER_HEAT_ON);
            case '\b':
                return CommandOutcome.step(controlType, label, CommandOutcome.ENGINE_RUNNING);
            case '\t':
                return CommandOutcome.step(controlType, label, CommandOutcome.ENGINE_OFF);
            case '\n':
                return CommandOutcome.step(controlType, TempControl.labelOf(f == null ? 24.0f : f.floatValue()), CommandOutcome.tempAt(f != null ? f.floatValue() : 24.0f));
            default:
                return CommandOutcome.step(controlType, label, null);
        }
    }

    static /* synthetic */ Boolean lambda$stepOf$0(VehicleStatus vehicleStatus) {
        if (vehicleStatus == null) {
            return null;
        }
        return Boolean.valueOf(!vehicleStatus.doorsLocked.booleanValue());
    }

    public static Map<String, Object> bodyOf(String str, int i) {
        if (!"ENGINE_START".equals(str)) {
            return null;
        }
        int max = Math.max(1, Math.min(30, i));
        HashMap hashMap = new HashMap();
        hashMap.put("operationTime", Integer.valueOf(max * 60));
        hashMap.put("runMinutes", Integer.valueOf(max));
        return hashMap;
    }

    public static Map<String, Object> acBodyOf(Float f) {
        return TempControl.bodiesOf(f == null ? 24.0f : f.floatValue(), TempControl.ZONE_BOTH);
    }
}
