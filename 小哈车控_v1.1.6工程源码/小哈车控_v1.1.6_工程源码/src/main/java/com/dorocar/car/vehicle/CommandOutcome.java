package com.dorocar.car.vehicle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public final class CommandOutcome {
    public static final Probe LOCKED = new Probe() { // from class: com.dorocar.car.vehicle.CommandOutcome$$ExternalSyntheticLambda1
        @Override // com.dorocar.car.vehicle.CommandOutcome.Probe
        public final Boolean read(VehicleStatus vehicleStatus) {
            return CommandOutcome.lambda$static$0(vehicleStatus);
        }
    };
    public static final Probe WINDOWS_CLOSED = new Probe() { // from class: com.dorocar.car.vehicle.CommandOutcome$$ExternalSyntheticLambda6
        @Override // com.dorocar.car.vehicle.CommandOutcome.Probe
        public final Boolean read(VehicleStatus vehicleStatus) {
            return CommandOutcome.lambda$static$1(vehicleStatus);
        }
    };
    public static final Probe SIDEWINDOWS_CLOSED = new Probe() { // from class: com.dorocar.car.vehicle.CommandOutcome$$ExternalSyntheticLambda7
        @Override // com.dorocar.car.vehicle.CommandOutcome.Probe
        public final Boolean read(VehicleStatus vehicleStatus) {
            return CommandOutcome.lambda$static$2(vehicleStatus);
        }
    };
    public static final Probe SKYLIGHT_CLOSED = new Probe() { // from class: com.dorocar.car.vehicle.CommandOutcome$$ExternalSyntheticLambda8
        @Override // com.dorocar.car.vehicle.CommandOutcome.Probe
        public final Boolean read(VehicleStatus vehicleStatus) {
            return CommandOutcome.lambda$static$3(vehicleStatus);
        }
    };
    public static final Probe ENGINE_RUNNING = new Probe() { // from class: com.dorocar.car.vehicle.CommandOutcome$$ExternalSyntheticLambda9
        @Override // com.dorocar.car.vehicle.CommandOutcome.Probe
        public final Boolean read(VehicleStatus vehicleStatus) {
            return CommandOutcome.lambda$static$4(vehicleStatus);
        }
    };
    public static final Probe ENGINE_OFF = new Probe() { // from class: com.dorocar.car.vehicle.CommandOutcome$$ExternalSyntheticLambda10
        @Override // com.dorocar.car.vehicle.CommandOutcome.Probe
        public final Boolean read(VehicleStatus vehicleStatus) {
            return CommandOutcome.lambda$static$5(vehicleStatus);
        }
    };
    public static final Probe AC_ON = new Probe() { // from class: com.dorocar.car.vehicle.CommandOutcome$$ExternalSyntheticLambda11
        @Override // com.dorocar.car.vehicle.CommandOutcome.Probe
        public final Boolean read(VehicleStatus vehicleStatus) {
            return CommandOutcome.lambda$static$6(vehicleStatus);
        }
    };
    public static final Probe AC_OFF = new Probe() { // from class: com.dorocar.car.vehicle.CommandOutcome$$ExternalSyntheticLambda12
        @Override // com.dorocar.car.vehicle.CommandOutcome.Probe
        public final Boolean read(VehicleStatus vehicleStatus) {
            return CommandOutcome.lambda$static$7(vehicleStatus);
        }
    };
    public static final Probe AC_AUTO = new Probe() { // from class: com.dorocar.car.vehicle.CommandOutcome$$ExternalSyntheticLambda13
        @Override // com.dorocar.car.vehicle.CommandOutcome.Probe
        public final Boolean read(VehicleStatus vehicleStatus) {
            return CommandOutcome.lambda$static$8(vehicleStatus);
        }
    };
    public static final Probe FRONT_DEFROST_ON = new Probe() { // from class: com.dorocar.car.vehicle.CommandOutcome$$ExternalSyntheticLambda14
        @Override // com.dorocar.car.vehicle.CommandOutcome.Probe
        public final Boolean read(VehicleStatus vehicleStatus) {
            return CommandOutcome.lambda$static$9(vehicleStatus);
        }
    };
    public static final Probe REAR_DEFROST_ON = new Probe() { // from class: com.dorocar.car.vehicle.CommandOutcome$$ExternalSyntheticLambda2
        @Override // com.dorocar.car.vehicle.CommandOutcome.Probe
        public final Boolean read(VehicleStatus vehicleStatus) {
            return CommandOutcome.lambda$static$10(vehicleStatus);
        }
    };
    public static final Probe STEER_HEAT_ON = new Probe() { // from class: com.dorocar.car.vehicle.CommandOutcome$$ExternalSyntheticLambda3
        @Override // com.dorocar.car.vehicle.CommandOutcome.Probe
        public final Boolean read(VehicleStatus vehicleStatus) {
            return CommandOutcome.lambda$static$11(vehicleStatus);
        }
    };
    public static final Probe AC_TEMP_KNOWN = new Probe() { // from class: com.dorocar.car.vehicle.CommandOutcome$$ExternalSyntheticLambda4
        @Override // com.dorocar.car.vehicle.CommandOutcome.Probe
        public final Boolean read(VehicleStatus vehicleStatus) {
            return CommandOutcome.lambda$static$13(vehicleStatus);
        }
    };
    public static final Probe TRUNK_OPEN = new Probe() { // from class: com.dorocar.car.vehicle.CommandOutcome$$ExternalSyntheticLambda5
        @Override // com.dorocar.car.vehicle.CommandOutcome.Probe
        public final Boolean read(VehicleStatus vehicleStatus) {
            return CommandOutcome.lambda$static$14(vehicleStatus);
        }
    };

    public interface Probe {
        Boolean read(VehicleStatus vehicleStatus);
    }

    public enum State {
        DONE,
        PENDING,
        UNVERIFIABLE
    }

    public static final class Step {
        public final String controlType;
        public final String label;
        public final Probe probe;

        Step(String str, String str2, Probe probe) {
            this.controlType = str;
            this.label = str2;
            this.probe = probe;
        }
    }

    static /* synthetic */ Boolean lambda$static$0(VehicleStatus vehicleStatus) {
        if (vehicleStatus == null) {
            return null;
        }
        return vehicleStatus.doorsLocked;
    }

    static /* synthetic */ Boolean lambda$static$1(VehicleStatus vehicleStatus) {
        if (vehicleStatus == null) {
            return null;
        }
        return Boolean.valueOf(!vehicleStatus.hasOpenWindow());
    }

    static /* synthetic */ Boolean lambda$static$2(VehicleStatus vehicleStatus) {
        if (vehicleStatus == null) {
            return null;
        }
        if (vehicleStatus.windowFrontLeft == null && vehicleStatus.windowFrontRight == null && vehicleStatus.windowRearLeft == null && vehicleStatus.windowRearRight == null) {
            return null;
        }
        return Boolean.valueOf((isTrue(vehicleStatus.windowFrontLeft) || isTrue(vehicleStatus.windowFrontRight) || isTrue(vehicleStatus.windowRearLeft) || isTrue(vehicleStatus.windowRearRight)) ? false : true);
    }

    static /* synthetic */ Boolean lambda$static$3(VehicleStatus vehicleStatus) {
        if (vehicleStatus == null || vehicleStatus.sunroof == null) {
            return null;
        }
        return Boolean.valueOf(!isTrue(vehicleStatus.sunroof));
    }

    static /* synthetic */ Boolean lambda$static$4(VehicleStatus vehicleStatus) {
        if (vehicleStatus == null) {
            return null;
        }
        return Boolean.valueOf(vehicleStatus.isEngineRunning());
    }

    static /* synthetic */ Boolean lambda$static$5(VehicleStatus vehicleStatus) {
        if (vehicleStatus == null) {
            return null;
        }
        return Boolean.valueOf(!vehicleStatus.isEngineRunning());
    }

    static /* synthetic */ Boolean lambda$static$6(VehicleStatus vehicleStatus) {
        if (vehicleStatus == null) {
            return null;
        }
        return vehicleStatus.airConditioner;
    }

    static /* synthetic */ Boolean lambda$static$7(VehicleStatus vehicleStatus) {
        if (vehicleStatus == null || vehicleStatus.airConditioner == null) {
            return null;
        }
        return Boolean.valueOf(!vehicleStatus.airConditioner.booleanValue());
    }

    static /* synthetic */ Boolean lambda$static$8(VehicleStatus vehicleStatus) {
        if (vehicleStatus == null) {
            return null;
        }
        return vehicleStatus.acAutoMode;
    }

    static /* synthetic */ Boolean lambda$static$9(VehicleStatus vehicleStatus) {
        if (vehicleStatus == null) {
            return null;
        }
        return vehicleStatus.frontDefroster;
    }

    static /* synthetic */ Boolean lambda$static$10(VehicleStatus vehicleStatus) {
        if (vehicleStatus == null) {
            return null;
        }
        return vehicleStatus.rearDefroster;
    }

    static /* synthetic */ Boolean lambda$static$11(VehicleStatus vehicleStatus) {
        if (vehicleStatus == null) {
            return null;
        }
        return vehicleStatus.steerWheelHeat;
    }

    public static Probe tempAt(float f) {
        final float normalize = TempControl.normalize(f);
        return new Probe() { // from class: com.dorocar.car.vehicle.CommandOutcome$$ExternalSyntheticLambda0
            @Override // com.dorocar.car.vehicle.CommandOutcome.Probe
            public final Boolean read(VehicleStatus vehicleStatus) {
                return CommandOutcome.lambda$tempAt$12(normalize, vehicleStatus);
            }
        };
    }

    static /* synthetic */ Boolean lambda$tempAt$12(float f, VehicleStatus vehicleStatus) {
        if (vehicleStatus == null || vehicleStatus.acTempSet == null) {
            return null;
        }
        return Boolean.valueOf(Math.abs(vehicleStatus.acTempSet.doubleValue() - ((double) f)) <= 0.5009999871253967d);
    }

    static /* synthetic */ Boolean lambda$static$13(VehicleStatus vehicleStatus) {
        if (vehicleStatus == null || vehicleStatus.acTempSet == null) {
            return null;
        }
        return Boolean.TRUE;
    }

    static /* synthetic */ Boolean lambda$static$14(VehicleStatus vehicleStatus) {
        if (vehicleStatus == null) {
            return null;
        }
        return vehicleStatus.doorTrunk;
    }

    private static boolean isTrue(Boolean bool) {
        return Boolean.TRUE.equals(bool);
    }

    public static Step step(String str, String str2, Probe probe) {
        return new Step(str, str2, probe);
    }

    public static State judge(Step step, VehicleStatus vehicleStatus) {
        if (step == null || step.probe == null) {
            return State.UNVERIFIABLE;
        }
        Boolean read = step.probe.read(vehicleStatus);
        return read == null ? State.UNVERIFIABLE : read.booleanValue() ? State.DONE : State.PENDING;
    }

    public static State judgeAll(List<Step> list, VehicleStatus vehicleStatus) {
        Iterator<Step> it = list.iterator();
        boolean z = false;
        boolean z2 = false;
        while (it.hasNext()) {
            State judge = judge(it.next(), vehicleStatus);
            if (judge == State.PENDING) {
                z = true;
            }
            if (judge == State.DONE) {
                z2 = true;
            }
        }
        return z ? State.PENDING : z2 ? State.DONE : State.UNVERIFIABLE;
    }

    public static List<Step> pending(List<Step> list, VehicleStatus vehicleStatus) {
        ArrayList arrayList = new ArrayList();
        for (Step step : list) {
            if (judge(step, vehicleStatus) == State.PENDING) {
                arrayList.add(step);
            }
        }
        return arrayList;
    }

    public static List<Step> done(List<Step> list, VehicleStatus vehicleStatus) {
        ArrayList arrayList = new ArrayList();
        for (Step step : list) {
            if (judge(step, vehicleStatus) == State.DONE) {
                arrayList.add(step);
            }
        }
        return arrayList;
    }

    public static String feedback(String str, List<Step> list, VehicleStatus vehicleStatus, boolean z) {
        if (list == null || list.isEmpty()) {
            return str + "：已下发";
        }
        if (judgeAll(list, vehicleStatus) == State.DONE) {
            return str + "成功" + detail(list, vehicleStatus);
        }
        if (vehicleStatus == null) {
            return str + "：车况暂时读不到";
        }
        List<Step> pending = pending(list, vehicleStatus);
        List<Step> done = done(list, vehicleStatus);
        State judge = judge(list.get(0), vehicleStatus);
        boolean z2 = judge == State.DONE;
        boolean z3 = judge == State.UNVERIFIABLE;
        if (!z2 || done.isEmpty()) {
            if (z3 && !z) {
                return str + "已下发（该车型不回读此项，无法确认是否生效）";
            }
            if (z) {
                return str + "未生效：车况没有变化，可能车没收到指令";
            }
            return str + "已下发，车况还没变化，等待车端执行…";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(str).append("部分完成：");
        sb.append(labels(done)).append("已完成");
        if (!pending.isEmpty()) {
            sb.append("，但").append(labels(pending)).append("还没完成");
        }
        return sb.toString();
    }

    private static String detail(List<Step> list, VehicleStatus vehicleStatus) {
        List<Step> done = done(list, vehicleStatus);
        return done.size() <= 1 ? "" : "（" + labels(done) + "）";
    }

    private static String labels(List<Step> list) {
        StringBuilder sb = new StringBuilder();
        for (Step step : list) {
            if (sb.length() > 0) {
                sb.append("、");
            }
            sb.append(step.label);
        }
        return sb.toString();
    }

    public static String sideEffects(List<Step> list, VehicleStatus vehicleStatus, VehicleStatus vehicleStatus2) {
        if (list == null || vehicleStatus == null || vehicleStatus2 == null) {
            return "";
        }
        ArrayList arrayList = new ArrayList();
        for (int i = 1; i < list.size(); i++) {
            Step step = list.get(i);
            if (step.probe != null) {
                Boolean read = step.probe.read(vehicleStatus);
                Boolean read2 = step.probe.read(vehicleStatus2);
                if (read2 != null && !Boolean.TRUE.equals(read) && Boolean.TRUE.equals(read2)) {
                    arrayList.add(step.label);
                }
            }
        }
        if (arrayList.isEmpty()) {
            return "";
        }
        return "，另外还完成了" + CommandExecutor$$ExternalSyntheticBackport0.m("、", arrayList);
    }

    public static List<Step> lockChain(String str, String str2, String str3, String str4, boolean z, boolean z2) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(step(null, "锁车", LOCKED));
        if (z) {
            arrayList.add(step(str, str2, SIDEWINDOWS_CLOSED));
        }
        if (z2) {
            arrayList.add(step(str3, str4, SKYLIGHT_CLOSED));
        }
        return arrayList;
    }
}
