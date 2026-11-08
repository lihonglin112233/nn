package com.dorocar.car.vehicle;

import android.content.Context;
import android.content.SharedPreferences;

/* loaded from: classes.dex */
public final class DoroRemoteConfig {
    private static final String KEY_ALLOW = "wxid_whitelist";
    private static final String KEY_TOKEN = "bridge_token";
    private static final String PREF = "doro_remote_cmd";

    private DoroRemoteConfig() {
    }

    public static boolean hasToken(Context context) {
        String string = prefs(context).getString(KEY_TOKEN, "");
        return (string == null || string.trim().isEmpty()) ? false : true;
    }

    public static String token(Context context) {
        String string = prefs(context).getString(KEY_TOKEN, "");
        return string == null ? "" : string.trim();
    }

    public static void setToken(Context context, String str) {
        prefs(context).edit().putString(KEY_TOKEN, str == null ? "" : str.trim()).apply();
    }

    public static boolean tokenMatches(Context context, String str) {
        String str2 = token(context);
        if (str2.isEmpty() || str == null || str2.length() != str.length()) {
            return false;
        }
        int i = 0;
        for (int i2 = 0; i2 < str2.length(); i2++) {
            i |= str2.charAt(i2) ^ str.charAt(i2);
        }
        return i == 0;
    }

    public static boolean allows(Context context, String str) {
        String string;
        if (str == null) {
            return false;
        }
        String trim = str.trim();
        if (trim.isEmpty() || (string = prefs(context).getString(KEY_ALLOW, "")) == null || string.trim().isEmpty()) {
            return false;
        }
        for (String str2 : string.split("[,，;；\\s]+")) {
            if (str2.trim().equalsIgnoreCase(trim)) {
                return true;
            }
        }
        return false;
    }

    public static void setAllow(Context context, String str) {
        prefs(context).edit().putString(KEY_ALLOW, str == null ? "" : str.trim()).apply();
    }

    public static String allow(Context context) {
        String string = prefs(context).getString(KEY_ALLOW, "");
        return string == null ? "" : string;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF, 0);
    }
}
