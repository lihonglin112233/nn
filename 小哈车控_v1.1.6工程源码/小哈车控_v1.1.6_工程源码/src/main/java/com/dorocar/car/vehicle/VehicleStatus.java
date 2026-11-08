package com.dorocar.car.vehicle;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/* loaded from: classes.dex */
public final class VehicleStatus {
    public Boolean acAutoMode;
    public Double acTempSet;
    public String acquisitionTime;
    public Boolean airConditioner;
    public Boolean antitheft;
    public Double avgEnergyConsumption;
    public Double avgFuelConsumption;
    public Double batteryPercent;
    public Double batteryVoltage;
    public Boolean cabinClean;
    public Double cabinTemp;
    public Boolean doorFrontLeft;
    public Boolean doorFrontRight;
    public Boolean doorRearLeft;
    public Boolean doorRearRight;
    public Boolean doorTrunk;
    public Boolean doorsLocked;
    public String engineState;
    public Double evRange;
    public Boolean frontDefroster;
    public String fuelGauge;
    public Double fuelRange;
    public Double fuelVolume;
    public String gear;
    public String gpsSwitchOn;
    public Boolean highBeam;
    public Boolean hood;
    public Boolean lowBeam;
    public Double mileage;
    public Boolean oilAlarm;
    public String powerState;
    public Boolean rearDefroster;
    public Boolean seatHeatDriver;
    public Boolean seatHeatPassenger;
    public Boolean seatHeatRearLeft;
    public Boolean seatHeatRearRight;
    public Boolean seatVentDriver;
    public Boolean seatVentPassenger;
    public Boolean steerWheelHeat;
    public Boolean sunroof;
    public String tboxStatus;
    public Boolean tireAlarmFl;
    public Boolean tireAlarmFr;
    public Boolean tireAlarmRl;
    public Boolean tireAlarmRr;
    public Double tirePressureFl;
    public Double tirePressureFr;
    public Double tirePressureRl;
    public Double tirePressureRr;
    public Double tireTempFl;
    public Double tireTempFr;
    public Double tireTempRl;
    public Double tireTempRr;
    public Boolean windowFrontLeft;
    public Boolean windowFrontRight;
    public Boolean windowRearLeft;
    public Boolean windowRearRight;
    public Boolean windshieldHeat;

    public boolean isEngineRunning() {
        return "1".equals(this.engineState) || "2".equals(this.engineState);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public String engineText() {
        char c;
        if (this.engineState == null) {
            return "未知";
        }
        String str = this.engineState;
        switch (str.hashCode()) {
            case 48:
                if (str.equals("0")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 49:
                if (str.equals("1")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 50:
                if (str.equals("2")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
        }
        return "未知";
    }

    public boolean hasOpenDoor() {
        return isTrue(this.doorFrontLeft) || isTrue(this.doorFrontRight) || isTrue(this.doorRearLeft) || isTrue(this.doorRearRight) || isTrue(this.doorTrunk) || isTrue(this.hood);
    }

    public boolean hasOpenWindow() {
        return isTrue(this.windowFrontLeft) || isTrue(this.windowFrontRight) || isTrue(this.windowRearLeft) || isTrue(this.windowRearRight) || isTrue(this.sunroof);
    }

    public boolean hasTireAlarm() {
        return isTrue(this.tireAlarmFl) || isTrue(this.tireAlarmFr) || isTrue(this.tireAlarmRl) || isTrue(this.tireAlarmRr);
    }

    private static boolean isTrue(Boolean bool) {
        return Boolean.TRUE.equals(bool);
    }

    public static VehicleStatus parse(Map<String, Object> map) {
        Map<String, Object> asMap;
        VehicleStatus vehicleStatus = new VehicleStatus();
        if (map == null || map.isEmpty()) {
            return vehicleStatus;
        }
        Map map2 = (Map) GwmCrypto.normalizeKeys(map);
        Map asMap2 = asMap(map2.get("vehiclestatusinfo"));
        if (asMap2 == null) {
            asMap2 = new LinkedHashMap();
        }
        vehicleStatus.acquisitionTime = asString(first(map2.get("acquisitiontime"), asMap2.get("acquisitiontime")));
        vehicleStatus.gpsSwitchOn = asString(first(asMap2.get("gpsswitchon"), map2.get("gpsswitchon")));
        vehicleStatus.tboxStatus = asString(first(map2.get("tboxstatus"), asMap2.get("tboxstate"), asMap2.get("tboxstatus")));
        vehicleStatus.fuelGauge = asString(first(asMap2.get("oilqty"), map2.get("oilqty")));
        vehicleStatus.fuelVolume = parseValue(asMap2.get("remainoil"));
        vehicleStatus.fuelRange = parseValue(asMap2.get("premileage"));
        vehicleStatus.batteryPercent = parseValue(asMap2.get("remainelectricpercent"));
        vehicleStatus.mileage = parseValue(asMap2.get("mileage"));
        Map<String, Object> asMap3 = asMap(asMap2.get("charge"));
        if (asMap3 != null) {
            vehicleStatus.evRange = parseValue(asMap3.get("evcontnsdistance"));
        }
        vehicleStatus.avgFuelConsumption = parseValue(asMap2.get("avgfuelconse"));
        vehicleStatus.avgEnergyConsumption = parseValue(asMap2.get("avrgegycns"));
        vehicleStatus.batteryVoltage = parseValue(asMap2.get("bmspackvolt"));
        vehicleStatus.cabinTemp = parseValue(first(asMap2.get("cbntemp"), asMap2.get("incartemperature")));
        Double parseValue = parseValue(first(asMap2.get("actemp"), asMap2.get("settemp"), asMap2.get("hvactemp"), asMap2.get("drvtemp"), asMap2.get("drvtemptarget")));
        if (parseValue != null && parseValue.doubleValue() > 60.0d) {
            parseValue = Double.valueOf(parseValue.doubleValue() / 10.0d);
        }
        if (parseValue != null && (parseValue.doubleValue() < 10.0d || parseValue.doubleValue() > 40.0d)) {
            parseValue = null;
        }
        vehicleStatus.acTempSet = parseValue;
        vehicleStatus.engineState = asString(asMap2.get("enginests"));
        vehicleStatus.powerState = asString(asMap2.get("power"));
        vehicleStatus.gear = asString(first(asMap2.get("hcugearsts"), asMap2.get("gearsts")));
        vehicleStatus.doorsLocked = toBoolInverted(asMap2.get("escllocksts"));
        if (vehicleStatus.doorsLocked == null && (asMap = asMap(asMap2.get("door"))) != null) {
            vehicleStatus.doorsLocked = toBoolInverted(asMap.get("maindrvedoorlocksts"));
        }
        Map<String, Object> asMap4 = asMap(asMap2.get("door"));
        if (asMap4 != null) {
            vehicleStatus.doorFrontLeft = toBool(asMap4.get("maindrvedoorsts"));
            vehicleStatus.doorFrontRight = toBool(asMap4.get("vicedoorsts"));
            vehicleStatus.doorRearLeft = toBool(asMap4.get("lbdoorsts"));
            vehicleStatus.doorRearRight = toBool(asMap4.get("rbdoorsts"));
            vehicleStatus.doorTrunk = toBool(first(asMap4.get("backdoorsts"), asMap4.get("tailgateopenupsts")));
        }
        vehicleStatus.hood = toBool(asMap2.get("enginedoorsts"));
        Map<String, Object> asMap5 = asMap(asMap2.get("windows"));
        if (asMap5 != null) {
            vehicleStatus.windowFrontLeft = windowPosToBool(asMap5.get("lfwinposnsts"), "1");
            vehicleStatus.windowFrontRight = windowPosToBool(asMap5.get("rfwinposnsts"), "1");
            vehicleStatus.windowRearLeft = windowPosToBool(asMap5.get("lbwinposnsts"), "1");
            vehicleStatus.windowRearRight = windowPosToBool(asMap5.get("rbwinposnsts"), "1");
            vehicleStatus.sunroof = windowPosToBool(asMap5.get("skylightsts"), "3");
            vehicleStatus.windshieldHeat = toBool(asMap5.get("fwinheatsts"));
        }
        vehicleStatus.airConditioner = toBool(asMap2.get("airconditionsts"));
        vehicleStatus.acAutoMode = toBool(first(asMap2.get("airconditionautomodenasts"), asMap2.get("acautomodests")));
        vehicleStatus.frontDefroster = toBool(asMap2.get("frontfrost"));
        vehicleStatus.rearDefroster = toBool(asMap2.get("backfrost"));
        Map<String, Object> asMap6 = asMap(asMap2.get("lighting"));
        if (asMap6 != null) {
            vehicleStatus.lowBeam = toBool(asMap6.get("nearbeamsts"));
            vehicleStatus.highBeam = toBool(asMap6.get("farbeamsts"));
        }
        vehicleStatus.steerWheelHeat = toBool(first(asMap2.get("steerwheelheat"), asMap2.get("steerwheelheatdsts")));
        vehicleStatus.cabinClean = toBool(asMap2.get("cabinclean"));
        Map<String, Object> asMap7 = asMap(asMap2.get("seat"));
        if (asMap7 != null) {
            vehicleStatus.seatHeatDriver = toBool(asMap7.get("maindriverseatheatsts"));
            vehicleStatus.seatVentDriver = toBool(asMap7.get("maindriverseatventsts"));
            vehicleStatus.seatHeatPassenger = toBool(asMap7.get("viceseatheatsts"));
            vehicleStatus.seatVentPassenger = toBool(asMap7.get("viceseatventsts"));
            vehicleStatus.seatHeatRearLeft = toBool(asMap7.get("lbseatheatsts"));
            vehicleStatus.seatHeatRearRight = toBool(asMap7.get("rbseatheatsts"));
        }
        Map<String, Object> asMap8 = asMap(asMap2.get("tirepress"));
        if (asMap8 != null) {
            vehicleStatus.tirePressureFl = parseValue(asMap8.get("lftirepressval"));
            vehicleStatus.tirePressureFr = parseValue(asMap8.get("rftirepressval"));
            vehicleStatus.tirePressureRl = parseValue(asMap8.get("lbtirepressval"));
            vehicleStatus.tirePressureRr = parseValue(asMap8.get("rbtirepressval"));
            vehicleStatus.tireAlarmFl = toBool(first(asMap8.get("lftirepressindcrsts"), asMap8.get("lftirepresssts")));
            vehicleStatus.tireAlarmFr = toBool(first(asMap8.get("rftirepressindcrsts"), asMap8.get("rftirepresssts")));
            vehicleStatus.tireAlarmRl = toBool(first(asMap8.get("lbtirepressindcrsts"), asMap8.get("lbtirepresssts")));
            vehicleStatus.tireAlarmRr = toBool(first(asMap8.get("rbtirepressindcrsts"), asMap8.get("rbtirepresssts")));
        }
        Map<String, Object> asMap9 = asMap(asMap2.get("tiretemp"));
        if (asMap9 != null) {
            vehicleStatus.tireTempFl = parseValue(asMap9.get("lftiretempval"));
            vehicleStatus.tireTempFr = parseValue(asMap9.get("rftiretempval"));
            vehicleStatus.tireTempRl = parseValue(asMap9.get("lbtiretempval"));
            vehicleStatus.tireTempRr = parseValue(asMap9.get("rbtiretempval"));
        }
        vehicleStatus.antitheft = toBool(asMap2.get("vehicleantitheftstatus"));
        vehicleStatus.oilAlarm = toBool(asMap2.get("oilalarmsts"));
        return vehicleStatus;
    }

    static Map<String, Object> asMap(Object obj) {
        if (obj instanceof Map) {
            return (Map) obj;
        }
        return null;
    }

    static String asString(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            String trim = ((String) obj).trim();
            if (trim.isEmpty()) {
                return null;
            }
            return trim;
        }
        if (obj instanceof Double) {
            double doubleValue = ((Double) obj).doubleValue();
            if (doubleValue == Math.rint(doubleValue)) {
                return String.valueOf((long) doubleValue);
            }
        }
        return String.valueOf(obj);
    }

    static Object first(Object... objArr) {
        for (Object obj : objArr) {
            if (obj != null) {
                return obj;
            }
        }
        return null;
    }

    static Double parseValue(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return Double.valueOf(((Number) obj).doubleValue());
        }
        if (!(obj instanceof String)) {
            return null;
        }
        String trim = ((String) obj).trim();
        if (trim.isEmpty() || "--".equals(trim) || "null".equalsIgnoreCase(trim)) {
            return null;
        }
        int indexOf = trim.indexOf(44);
        if (indexOf >= 0) {
            trim = trim.substring(0, indexOf).trim();
        }
        try {
            return Double.valueOf(Double.parseDouble(trim));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static Boolean toBool(Object obj) {
        return toBool(obj, "1", "0");
    }

    static Boolean toBool(Object obj, String str, String str2) {
        String statusText = statusText(obj);
        if (statusText == null) {
            return null;
        }
        if (statusText.equals(str)) {
            return Boolean.TRUE;
        }
        if (statusText.equals(str2)) {
            return Boolean.FALSE;
        }
        return null;
    }

    static Boolean toBoolInverted(Object obj) {
        String statusText = statusText(obj);
        if (statusText == null) {
            return null;
        }
        if ("0".equals(statusText)) {
            return Boolean.TRUE;
        }
        if ("1".equals(statusText)) {
            return Boolean.FALSE;
        }
        return null;
    }

    static Boolean windowPosToBool(Object obj, String str) {
        String statusText = statusText(obj);
        if (statusText == null) {
            return null;
        }
        return statusText.equals(str) ? Boolean.FALSE : Boolean.TRUE;
    }

    private static String statusText(Object obj) {
        String trim;
        if (obj == null) {
            return null;
        }
        if (obj instanceof Boolean) {
            return ((Boolean) obj).booleanValue() ? "1" : "0";
        }
        if (obj instanceof Double) {
            double doubleValue = ((Double) obj).doubleValue();
            trim = doubleValue == Math.rint(doubleValue) ? String.valueOf((long) doubleValue) : String.valueOf(obj);
        } else if (obj instanceof Number) {
            trim = String.valueOf(((Number) obj).longValue());
        } else {
            trim = String.valueOf(obj).trim();
        }
        if (trim.isEmpty() || "--".equals(trim) || "null".equalsIgnoreCase(trim)) {
            return null;
        }
        return trim;
    }

    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append("引擎=").append(engineText()).append(" 续航=").append(this.fuelRange == null ? "-" : this.fuelRange + "km").append(" 油量=").append(this.fuelVolume == null ? "-" : this.fuelVolume + "L").append(" 里程=").append(this.mileage == null ? "-" : this.mileage + "km").append(" 车门锁=").append(this.doorsLocked != null ? this.doorsLocked.booleanValue() ? "已锁" : "未锁" : "-").append(" 车门开=").append(hasOpenDoor()).append(" 窗开=").append(hasOpenWindow());
        return sb.toString();
    }

    public String securitySummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.doorsLocked == null ? "门锁未知" : this.doorsLocked.booleanValue() ? "已锁车" : "未锁车");
        if (hasOpenDoor()) {
            sb.append(" · 有车门未关");
        }
        if (hasOpenWindow()) {
            sb.append(" · 有车窗未关");
        }
        if (hasTireAlarm()) {
            sb.append(" · 胎压异常");
        }
        return sb.toString();
    }

    public boolean isEmpty() {
        return this.engineState == null && this.mileage == null && this.fuelRange == null && this.doorsLocked == null && this.tirePressureFl == null;
    }

    private static boolean nonEmpty(List<?> list) {
        return (list == null || list.isEmpty()) ? false : true;
    }
}
