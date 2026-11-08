package com.dorocar.car.vehicle;

/* loaded from: classes.dex */
public class GwmVehicle {
    public String brandName;
    public String modelName;
    public String nickname;
    public String platform;
    public String seriesName;
    public String vin;

    public String displayName() {
        return notEmpty(this.nickname) ? this.nickname : notEmpty(this.seriesName) ? this.seriesName : notEmpty(this.modelName) ? this.modelName : (this.vin == null || this.vin.length() < 6) ? "我的车辆" : "车辆 " + this.vin.substring(this.vin.length() - 6);
    }

    public String subtitle() {
        return subtitleWithPlatform(this.platform);
    }

    public String subtitleWithPlatform(String str) {
        StringBuilder sb = new StringBuilder();
        if (this.vin != null) {
            sb.append(maskVin(this.vin));
        }
        if (notEmpty(this.modelName) && !this.modelName.equals(this.nickname)) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(this.modelName);
        }
        if (!notEmpty(str)) {
            str = this.platform;
        }
        if (notEmpty(str)) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(str);
        }
        return sb.toString();
    }

    public static String maskVin(String str) {
        return str == null ? "" : str.length() <= 6 ? str : "VIN …" + str.substring(str.length() - 6);
    }

    private static boolean notEmpty(String str) {
        return (str == null || str.trim().isEmpty()) ? false : true;
    }

    public String toString() {
        return displayName() + "(" + this.vin + ")";
    }
}
