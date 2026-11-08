package com.dorocar.car.vehicle;

import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

/* loaded from: classes.dex */
public class GwmClient {
    private static final String AUTO_AI_DIRECT = "https://ti.gwm.com.cn:8443/tsp/ead";
    private static final String AUTO_AI_LOGIN_URL = "https://gapp-api.gwmapp-h.com/tsp/v1/proxy/navinfo/GW.M.APP_LOGIN";
    private static final String AUTO_AI_OPEN_COMMAND = "GW.M.SET_AND_OPEN_COMMAND";
    private static final String AUTO_AI_SEND_COMMON_COMMAND = "GW.M.SEND_COMMON_COMMAND";
    private static final String BEAN_TECH_BASE = "https://gw-app-gateway.gwmapp-h.com/";
    private static final String BT_LOGIN_PATH = "/app-api/api/v1.0/userAuth/loginSSOAccount";
    private static final String BT_RESULT_PATH = "/app-api/api/v1.0/vehicle/getRemoteCtrlResultT5";
    private static final String BT_SEND_PATH = "/app-api/api/v1.0/vehicle/T5/sendCmd";
    private static final String BT_STATUS_PATH = "/app-api/api/v3.0/vehicle/getLastStatus";
    private static final String BT_TIMELY_PATH = "/app-api/api/v3.0/vehicle/remote-ctrl/timely";
    public static final String CMD_DEFROST_BACK_START = "DEFROST_BACK_START";
    public static final String CMD_DEFROST_BACK_STOP = "DEFROST_BACK_STOP";
    public static final String CMD_DEFROST_FRONT_START = "DEFROST_FRONT_START";
    public static final String CMD_DEFROST_FRONT_STOP = "DEFROST_FRONT_STOP";
    public static final String CMD_ENGINE_START = "ENGINE_START";
    public static final String CMD_ENGINE_STOP = "ENGINE_STOP";
    public static final String CMD_FLASH = "FLASH";
    public static final String CMD_LOCK = "VEHICLE_LOCK";
    public static final String CMD_SEAT_HEAT_START = "SEAT_HEATING_START";
    public static final String CMD_SEAT_HEAT_STOP = "SEAT_HEATING_STOP";
    public static final String CMD_SEAT_VENT_START = "SEAT_VENTILATION_START";
    public static final String CMD_SEAT_VENT_STOP = "SEAT_VENTILATION_STOP";
    public static final String CMD_SKYLIGHT_CLOSE = "SKYLIGNT_CLOSE";
    public static final String CMD_STEER_HEAT_STOP = "STEERING_WHEEL_HEATLESS";
    public static final String CMD_UNLOCK = "VEHICLE_UNLOCK";
    public static final String CMD_WHISTLE = "WHISTLE";
    public static final String CMD_WINDOW_CLOSE = "WINDOW_CLOSE";
    private static final String DISCOVERY_URL = "https://gapp-api.gwmapp-h.com/gcar/v1/app/android/vehicle/query-vehicle-list";
    private static final String G_APP_BASE = "https://gapp-api.gwmapp-h.com/";
    private static final String PWD_LOGIN_URL = "https://gapp-api.gwmapp-h.com/api-guser/v5/user/pwd-login";
    private static final String REFRESH_URL = "https://gapp-api.gwmapp-h.com/api-guser/v5/token/refresh";
    private static final String SMS_LOGIN_URL = "https://gapp-api.gwmapp-h.com/api-guser/v5/user/sms-login";
    private static final String SMS_REQUEST_URL = "https://gapp-api.gwmapp-h.com/api-guser/v5/user/login-sms/send";
    public String autoTokenId;
    public String autoUserId;
    public String beanId;
    public String btAccessToken;
    public String btBeanId;
    public String btRefreshToken;
    public String gRefreshToken;
    public String gToken;
    public String ptToken;
    public String ssoToken;
    public String userId;
    public static final String CMD_WHISTLE_FLASH = "WHISTLE_FLASH";
    public static final String CMD_AC_START = "AIR_CONDITIONER_START";
    public static final String CMD_AC_STOP = "AIR_CONDITIONER_STOP";
    public static final String CMD_STEER_HEAT_START = "STEERING_WHEEL_HEATING";
    private static final List<String> TIMELY_COMMANDS = Arrays.asList("WHISTLE", "FLASH", CMD_WHISTLE_FLASH, CMD_AC_START, CMD_AC_STOP, CMD_STEER_HEAT_START);
    private static final List<String> TIMELY_WITH_BODY = Arrays.asList(CMD_AC_START);
    private static final Map<String, Integer> AUTO_AI_CMD_CODES = new LinkedHashMap();
    private final Map<String, String> platformCache = new ConcurrentHashMap();
    private final Object reLoginLock = new Object();
    private int timeoutMs = 20000;
    private final Set<String> t5BrokenVins = ConcurrentHashMap.newKeySet();
    public String phone = "";
    public String deviceId = GwmCrypto.randomDeviceId();

    static {
        AUTO_AI_CMD_CODES.put(CMD_UNLOCK, 1);
        AUTO_AI_CMD_CODES.put(CMD_LOCK, 2);
        AUTO_AI_CMD_CODES.put("WINDOW_CLOSE", 3);
        AUTO_AI_CMD_CODES.put(CMD_WHISTLE_FLASH, 5);
        AUTO_AI_CMD_CODES.put("ENGINE_START", 15);
        AUTO_AI_CMD_CODES.put("ENGINE_STOP", 16);
        AUTO_AI_CMD_CODES.put("WHISTLE", 19);
        AUTO_AI_CMD_CODES.put("FLASH", 20);
        AUTO_AI_CMD_CODES.put(CMD_SKYLIGHT_CLOSE, 28);
        AUTO_AI_CMD_CODES.put(CMD_AC_START, 6);
        AUTO_AI_CMD_CODES.put(CMD_AC_STOP, 7);
    }

    public static class AuthException extends Exception {
        public AuthException(String str) {
            super(str);
        }
    }

    public static class ConnectionException extends Exception {
        public ConnectionException(String str) {
            super(str);
        }
    }

    public static class RiskControlException extends Exception {
        public RiskControlException(String str) {
            super(str);
        }
    }

    public static class SchemaException extends Exception {
        public SchemaException(String str) {
            super(str);
        }
    }

    /** 车端上一条远控还在执行中（-114），需等待后重试 */
    public static class BusyException extends Exception {
        public BusyException(String str) {
            super(str);
        }
    }

    public void setTimeoutSeconds(int i) {
        this.timeoutMs = Math.max(5, i) * 1000;
    }

    public Map<String, Object> toStateMap() {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        linkedHashMap.put("phone", this.phone);
        linkedHashMap.put("device_id", this.deviceId);
        linkedHashMap.put("g_token", this.gToken);
        linkedHashMap.put("g_refresh_token", this.gRefreshToken);
        linkedHashMap.put("sso_token", this.ssoToken);
        linkedHashMap.put("pt_token", this.ptToken);
        linkedHashMap.put("user_id", this.userId);
        linkedHashMap.put("bean_id", this.beanId);
        linkedHashMap.put("bt_access_token", this.btAccessToken);
        linkedHashMap.put("bt_refresh_token", this.btRefreshToken);
        linkedHashMap.put("bt_bean_id", this.btBeanId);
        linkedHashMap.put("auto_token_id", this.autoTokenId);
        linkedHashMap.put("auto_user_id", this.autoUserId);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : this.platformCache.entrySet()) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(entry.getKey()).append('=').append(entry.getValue() == null ? "" : entry.getValue());
        }
        linkedHashMap.put("platforms", sb.toString());
        StringBuilder sb2 = new StringBuilder();
        for (String str : this.t5BrokenVins) {
            if (sb2.length() > 0) {
                sb2.append(';');
            }
            sb2.append(str);
        }
        linkedHashMap.put("t5broken", sb2.toString());
        return linkedHashMap;
    }

    public static GwmClient fromStateMap(Map<String, Object> map) {
        GwmClient gwmClient = new GwmClient();
        if (map == null) {
            return gwmClient;
        }
        gwmClient.phone = str(map.get("phone"), "");
        String str = str(map.get("device_id"), "");
        if (str.isEmpty()) {
            str = GwmCrypto.randomDeviceId();
        }
        gwmClient.deviceId = str;
        gwmClient.gToken = strOrNull(map.get("g_token"));
        gwmClient.gRefreshToken = strOrNull(map.get("g_refresh_token"));
        gwmClient.ssoToken = strOrNull(map.get("sso_token"));
        gwmClient.ptToken = strOrNull(map.get("pt_token"));
        gwmClient.userId = strOrNull(map.get("user_id"));
        gwmClient.beanId = strOrNull(map.get("bean_id"));
        gwmClient.btAccessToken = strOrNull(map.get("bt_access_token"));
        gwmClient.btRefreshToken = strOrNull(map.get("bt_refresh_token"));
        gwmClient.btBeanId = strOrNull(map.get("bt_bean_id"));
        gwmClient.autoTokenId = strOrNull(map.get("auto_token_id"));
        gwmClient.autoUserId = strOrNull(map.get("auto_user_id"));
        for (String str2 : str(map.get("platforms"), "").split(";")) {
            int indexOf = str2.indexOf(61);
            if (indexOf > 0) {
                gwmClient.platformCache.put(str2.substring(0, indexOf), str2.substring(indexOf + 1));
            }
        }
        for (String str3 : str(map.get("t5broken"), "").split(";")) {
            if (!str3.isEmpty()) {
                gwmClient.t5BrokenVins.add(str3);
                gwmClient.platformCache.put(str3, "gtsp");
            }
        }
        return gwmClient;
    }

    private static String str(Object obj, String str) {
        return obj == null ? str : String.valueOf(obj);
    }

    private static String strOrNull(Object obj) {
        if (obj == null) {
            return null;
        }
        String valueOf = String.valueOf(obj);
        if (valueOf.isEmpty() || "null".equals(valueOf)) {
            return null;
        }
        return valueOf;
    }

    public boolean hasGApp() {
        return notEmpty(this.gToken) && notEmpty(this.gRefreshToken) && notEmpty(this.userId);
    }

    public boolean isLoggedIn() {
        return hasGApp() && notEmpty(this.beanId) && notEmpty(this.btAccessToken) && notEmpty(this.autoTokenId) && notEmpty(this.autoUserId);
    }

    public boolean hasRefreshableSession() {
        return hasGApp();
    }

    public boolean reviveSessionQuietly() {
        if (!hasGApp()) {
            return false;
        }
        // 已有完整会话时直接可用，不要重复登录覆盖本地 token
        if (isLoggedIn()) {
            return true;
        }
        // 先用刷新令牌换取新的 gToken，再重新初始化各通道
        try {
            refresh();
        } catch (Exception e) {
            Log.w("GwmClient", "刷新令牌失败，尝试直接恢复通道：" + e);
        }
        try {
            initializeServices();
            if (isLoggedIn()) {
                return true;
            }
        } catch (Exception e) {
            Log.w("GwmClient", "静默恢复失败，需要重新登录：" + e);
        }
        return isLoggedIn();
    }

    private static boolean notEmpty(String str) {
        return (str == null || str.isEmpty()) ? false : true;
    }

    public void requestSmsCode(String str) throws Exception {
        this.phone = str == null ? "" : str.trim();
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        linkedHashMap.put("phone", this.phone);
        linkedHashMap.put("flag", "LOGIN");
        gAppPost("request_verification", SMS_REQUEST_URL, linkedHashMap, true);
    }

    public List<GwmVehicle> loginSms(String str) throws Exception {
        String trim = str == null ? "" : str.trim();
        if (trim.isEmpty()) {
            throw new AuthException("验证码不能为空");
        }
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        linkedHashMap.put("code", trim);
        linkedHashMap.put("phone", this.phone);
        linkedHashMap.put("deviceToken", "");
        applyGAppData(asMap(gAppPost("login", SMS_LOGIN_URL, linkedHashMap, true)));
        initializeServices();
        return getVehicles();
    }

    public List<GwmVehicle> loginPassword(String str, String str2) throws Exception {
        this.phone = str == null ? "" : str.trim();
        if (str2 == null) {
            str2 = "";
        }
        if (this.phone.isEmpty()) {
            throw new AuthException("手机号不能为空");
        }
        if (str2.isEmpty()) {
            throw new AuthException("密码不能为空");
        }
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        linkedHashMap.put("phone", this.phone);
        linkedHashMap.put("deviceId", this.deviceId);
        linkedHashMap.put("password", str2);
        linkedHashMap.put("deviceToken", "");
        applyGAppData(asMap(gAppPost("login_pwd", PWD_LOGIN_URL, linkedHashMap, true)));
        initializeServices();
        return getVehicles();
    }

    public void refresh() throws Exception {
        if (!hasGApp()) {
            throw new AuthException("没有可刷新的登录状态，请重新短信登录");
        }
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        linkedHashMap.put(BridgeConst.EXTRA_TOKEN, this.gToken);
        linkedHashMap.put("refreshToken", this.gRefreshToken);
        applyGAppData(asMap(gAppPost("refresh_token", REFRESH_URL, linkedHashMap, true)));
        initializeServices();
    }

    public void recoverSession() throws Exception {
        synchronized (this.reLoginLock) {
            try {
                try {
                    initializeServices();
                } catch (Throwable th) {
                    throw th;
                }
            } catch (AuthException e) {
                refresh();
            } catch (SchemaException e2) {
                refresh();
            }
        }
    }

    public List<GwmVehicle> getVehicles() throws Exception {
        Object ciProp;
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        linkedHashMap.put("vehicleVersion", 13);
        Object gAppPost = gAppPost("acquire_vehicles", DISCOVERY_URL, linkedHashMap, false);
        Object ciProp2 = ciProp(gAppPost, "acquireVehiclesList");
        if (ciProp2 != null) {
            gAppPost = ciProp2;
        }
        if (!(gAppPost instanceof List)) {
            throw new SchemaException("车辆列表格式异常");
        }
        ArrayList arrayList = new ArrayList();
        Iterator it = ((List) gAppPost).iterator();
        while (it.hasNext()) {
            Map<String, Object> asMap = asMap(it.next());
            if (asMap != null && (ciProp = ciProp(asMap, "vin")) != null && !String.valueOf(ciProp).trim().isEmpty()) {
                String upperCase = String.valueOf(ciProp).trim().toUpperCase(Locale.ROOT);
                String ciString = ciString(asMap, "belongPlatform");
                GwmVehicle gwmVehicle = new GwmVehicle();
                gwmVehicle.vin = upperCase;
                gwmVehicle.modelName = ciString(asMap, "modelName");
                gwmVehicle.seriesName = ciString(asMap, "appShowSeriesName");
                gwmVehicle.nickname = ciString(asMap, "vehicleNick");
                gwmVehicle.brandName = ciString(asMap, "brandName");
                gwmVehicle.platform = ciString == null ? "" : ciString.trim();
                arrayList.add(gwmVehicle);
                if (!this.t5BrokenVins.contains(upperCase)) {
                    this.platformCache.put(upperCase, gwmVehicle.platform.toLowerCase(Locale.ROOT));
                } else {
                    this.platformCache.put(upperCase, "gtsp");
                }
            }
        }
        return arrayList;
    }

    public Map<String, Object> getStatus(String str) throws Exception {
        String upperCase = str == null ? "" : str.trim().toUpperCase(Locale.ROOT);
        if (!isLoggedIn()) {
            throw new AuthException("未完成登录");
        }
        Map<String, Object> asMap = asMap(decodeGAppResponse(http("GET", beanTechUrl(BT_STATUS_PATH) + "?vin=" + urlEncode(upperCase), beanTechHeaders("GET", BT_STATUS_PATH, GwmCrypto.randomNonce(), String.valueOf(System.currentTimeMillis()), "vin=" + upperCase, upperCase), null), "get_last_status"));
        return asMap == null ? new LinkedHashMap() : asMap;
    }

    public VehicleStatus getParsedStatus(String str) throws Exception {
        return VehicleStatus.parse(getStatus(str));
    }

    public String cachedPlatform(String str) {
        String str2 = this.platformCache.get(str == null ? "" : str.trim().toUpperCase(Locale.ROOT));
        return str2 == null ? "" : str2;
    }

    public String getPlatform(String str) throws Exception {
        String upperCase = str == null ? "" : str.trim().toUpperCase(Locale.ROOT);
        if (!this.platformCache.containsKey(upperCase)) {
            getVehicles();
        }
        String str2 = this.platformCache.get(upperCase);
        return str2 == null ? "" : str2;
    }

    public String sendCommand(String str, String str2, Map<String, Object> map) throws Exception {
        /* 车端执行一条远控通常需要 10~30 秒，期间再下发会返回 -114。
         * 这里自动等待重试：最多 5 次尝试，共约等待 60 秒，覆盖车端最长执行窗口。 */
        int attempt = 0;
        while (true) {
            try {
                return sendCommandInner(str, str2, map);
            } catch (BusyException e) {
                attempt++;
                if (attempt >= 5) {
                    throw e;
                }
                try {
                    Thread.sleep(attempt == 1 ? 12000L : 15000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            } catch (AuthException e) {
                String message = e.getMessage() == null ? "" : e.getMessage();
                if (!message.contains("会话") && !message.contains("-101") && !message.contains("认证失败")) {
                    throw e;
                }
                recoverSession();
                return sendCommandInner(str, str2, map);
            }
        }
    }

    private String sendCommandInner(String str, String str2, Map<String, Object> map) throws Exception {
        String str3;
        Map<String, Object> map2;
        int i;
        String upperCase = str == null ? "" : str.trim().toUpperCase(Locale.ROOT);
        if (!isLoggedIn()) {
            throw new AuthException("未完成登录");
        }
        try {
            str3 = getPlatform(upperCase);
        } catch (Exception e) {
            str3 = "";
        }
        if ("beantech".equals(str3) && !this.t5BrokenVins.contains(upperCase)) {
            try {
                return sendBeanTechCommand(upperCase, str2, map);
            } catch (AuthException e2) {
                if ((e2.getMessage() != null ? e2.getMessage() : "").contains("550002")) {
                    this.t5BrokenVins.add(upperCase);
                    this.platformCache.put(upperCase, "gtsp");
                } else {
                    throw e2;
                }
            }
        }
        Integer num = AUTO_AI_CMD_CODES.get(str2);
        /* 修正：AutoAI（gtsp/ti.gwm.com.cn）协议中 cmdCode 17/18 实为后背门开启/关闭，
         * 空调开/关实为 6/7（与中国区官方 App 行为一致，经社区实测验证）：
         *  - 开空调：fn=GW.M.SET_AND_OPEN_COMMAND，cmdCode=6，airParams={runTime,temperature}
         *  - 关空调：fn=GW.M.SEND_COMMON_COMMAND，cmdCode=7，无参数 */
        boolean isAcStart = CMD_AC_START.equals(str2);
        boolean isAcStop = CMD_AC_STOP.equals(str2);
        if (isAcStart) {
            num = 6;
        } else if (isAcStop) {
            num = 7;
        }
        if (num == null) {
            StringBuilder append = new StringBuilder().append("平台 ");
            if (str3.isEmpty()) {
                str3 = "未知";
            }
            throw new AuthException(append.append(str3).append(" 暂不支持该指令：").append(str2).append("（gtsp 平台支持 锁车/解锁/关全车窗/鸣笛/闪灯/鸣笛闪灯/空调开/空调关/远程启动/熄火/关天窗）").toString());
        }
        String str4 = ("ENGINE_START".equals(str2) || isAcStart) ? AUTO_AI_OPEN_COMMAND : AUTO_AI_SEND_COMMON_COMMAND;
        if ("ENGINE_START".equals(str2)) {
            if (map != null && (map.get("operationTime") instanceof Number)) {
                i = Math.max(1, ((Number) map.get("operationTime")).intValue() / 60);
            } else {
                i = 15;
            }
            LinkedHashMap linkedHashMap = new LinkedHashMap();
            linkedHashMap.put("runTime", Integer.valueOf(i));
            map2 = new LinkedHashMap<>();
            map2.put("engineParams", linkedHashMap);
        } else {
            if (!isAcStart) {
                map2 = null;
            } else {
                map2 = new LinkedHashMap<>();
                Object obj = map == null ? null : map.get("operationTime");
                map2.put("operationTime", Integer.valueOf(obj instanceof Number ? ((Number) obj).intValue() : TempControl.DEFAULT_DURATION_SECONDS));
                Object obj2 = map != null ? map.get("temperature") : null;
                if (obj2 instanceof Number) {
                    map2.put("temperature", Integer.valueOf(((Number) obj2).intValue()));
                }
            }
        }
        if (isAcStart) {
            // SET_AND_OPEN_COMMAND 使用 airParams 携带运行时长与温度
            Object operationTime = map2.remove("operationTime");
            LinkedHashMap airParams = new LinkedHashMap();
            airParams.put("runTime", Math.max(1, ((Number) operationTime).intValue() / 60));
            Object temperature = map2.remove("temperature");
            if (temperature instanceof Number) {
                airParams.put("temperature", Integer.valueOf(((Number) temperature).intValue()));
            }
            map2 = new LinkedHashMap<>();
            map2.put("airParams", airParams);
        }
        return sendAutoAiCommand(upperCase, num.intValue(), str4, map2);
    }

    private String sendBeanTechCommand(String str, String str2, Map<String, Object> map) throws Exception {
        String randomSequence = GwmCrypto.randomSequence();
        boolean contains = TIMELY_COMMANDS.contains(str2);
        boolean z = !contains || TIMELY_WITH_BODY.contains(str2);
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        if (z) {
            linkedHashMap.put("cmdBody", map);
        }
        linkedHashMap.put("controlType", str2);
        LinkedHashMap linkedHashMap2 = new LinkedHashMap();
        linkedHashMap2.put("commands", Collections.singletonList(linkedHashMap));
        linkedHashMap2.put("sendType", 0);
        linkedHashMap2.put("seqNo", randomSequence);
        linkedHashMap2.put("vin", str);
        if (!contains) {
            linkedHashMap2.put("isSaveConfig", null);
        }
        String encodeDotnetJson = GwmCrypto.encodeDotnetJson(linkedHashMap2);
        String str3 = contains ? BT_TIMELY_PATH : BT_SEND_PATH;
        Map<String, String> beanTechHeaders = beanTechHeaders("POST", str3, GwmCrypto.randomNonce(), String.valueOf(System.currentTimeMillis()), "json=" + encodeDotnetJson, str);
        decodeGAppResponse(http("POST", beanTechUrl(str3), beanTechHeaders, encodeDotnetJson), "send_cmd");
        return randomSequence;
    }

    private String sendAutoAiCommand(String str, int i, String str2, Map<String, Object> map) throws Exception {
        String valueOf = String.valueOf(System.currentTimeMillis());
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        linkedHashMap.put("flag", 1);
        linkedHashMap.put("signStr", GwmCrypto.md5Hex(str + (this.autoTokenId == null ? "" : this.autoTokenId)));
        linkedHashMap.put("userId", this.autoUserId);
        linkedHashMap.put("userType", "0");
        linkedHashMap.put("vin", str);
        linkedHashMap.put("cmdCode", Integer.valueOf(i));
        if (map != null) {
            linkedHashMap.putAll(map);
        }
        LinkedHashMap linkedHashMap2 = new LinkedHashMap();
        linkedHashMap2.put("brandType", "gwm");
        linkedHashMap2.put("cVer", GwmCrypto.SOURCE_APP_VERSION);
        linkedHashMap2.put("fn", str2);
        linkedHashMap2.put("fv", "0202");
        linkedHashMap2.put("mobileId", this.deviceId);
        linkedHashMap2.put("osType", "Android");
        linkedHashMap2.put("osVer", "");
        linkedHashMap2.put("rs", "2");
        linkedHashMap2.put(BridgeConst.EXTRA_TS, GwmCrypto.chinaTimestamp());
        linkedHashMap2.put("tk", this.autoTokenId);
        linkedHashMap2.put("v", "1.0");
        LinkedHashMap linkedHashMap3 = new LinkedHashMap();
        linkedHashMap3.put("body", linkedHashMap);
        linkedHashMap3.put("header", linkedHashMap2);
        String str3 = "https://ti.gwm.com.cn:8443/tsp/ead?p=" + urlEncodeFull(GwmCrypto.encodeDotnetJson(linkedHashMap3));
        LinkedHashMap linkedHashMap4 = new LinkedHashMap();
        linkedHashMap4.put("v", "1.0");
        linkedHashMap4.put("cid", this.deviceId);
        linkedHashMap4.put("client", "phone");
        linkedHashMap4.put("sign", GwmCrypto.autoAiSign(valueOf));
        linkedHashMap4.put("time", valueOf);
        linkedHashMap4.put("ckey", GwmCrypto.AUTO_AI_CKEY);
        linkedHashMap4.put("protocolVer", "2.1.2");
        linkedHashMap4.put("token", this.autoTokenId != null ? this.autoTokenId : "");
        linkedHashMap4.put("brandType", "GWM");
        linkedHashMap4.put("protocolVer", "2.1.2");
        linkedHashMap4.put(BridgeConst.EXTRA_TOKEN, this.autoTokenId);
        linkedHashMap4.put("brandType", "GWM");
        linkedHashMap4.put("Accept-Encoding", "gzip");
        linkedHashMap4.put("User-Agent", GwmCrypto.OFFICIAL_USER_AGENT);
        Object ciProp = ciProp(decodeAutoAiResponse(http("GET", str3, linkedHashMap4, null), "send_cmd"), "transactionId");
        if (ciProp == null) {
            throw new SchemaException("AutoAI 远控响应缺少 transactionId");
        }
        return valueToStr(ciProp);
    }

    public Object getCommandResult(String str, String str2) throws Exception {
        String upperCase = str == null ? "" : str.trim().toUpperCase(Locale.ROOT);
        if (!isLoggedIn()) {
            throw new AuthException("未完成登录");
        }
        return decodeGAppResponse(http("GET", beanTechUrl(BT_RESULT_PATH) + "?seqNo=" + urlEncode(str2), beanTechHeaders("GET", BT_RESULT_PATH, GwmCrypto.randomNonce(), String.valueOf(System.currentTimeMillis()), "seqno=" + str2, upperCase), null), "get_cmd_result");
    }

    private void applyGAppData(Map<String, Object> map) throws SchemaException {
        if (map == null) {
            throw new SchemaException("G-App 响应格式异常");
        }
        Object or = or(ciProp(map, "gToken"), ciProp(map, BridgeConst.EXTRA_TOKEN));
        Object or2 = or(ciProp(map, "gRefreshToken"), ciProp(map, "refreshToken"));
        Object ciProp = ciProp(map, "ssoToken");
        Object ciProp2 = ciProp(map, "ptToken");
        Object ciProp3 = ciProp(map, "userId");
        Object ciProp4 = ciProp(map, "beanId");
        if (or != null && or2 != null && ciProp3 != null) {
            this.gToken = valueToStr(or);
            this.gRefreshToken = valueToStr(or2);
            this.userId = valueToStr(ciProp3);
        }
        if (ciProp != null) {
            this.ssoToken = valueToStr(ciProp);
        }
        if (ciProp2 != null) {
            this.ptToken = valueToStr(ciProp2);
        }
        if (ciProp4 != null) {
            this.beanId = valueToStr(ciProp4);
        }
        if (!hasGApp()) {
            throw new SchemaException("G-App 登录响应缺少必需字段（gToken/gRefreshToken/userId）");
        }
    }

    private void initializeServices() throws Exception {
        if (this.ssoToken == null && this.ptToken == null) {
            throw new SchemaException("缺少 ssoToken，无法登录 BeanTech");
        }
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        linkedHashMap.put("appType", 0);
        linkedHashMap.put("deviceId", this.deviceId);
        linkedHashMap.put("phone", this.phone);
        linkedHashMap.put("ssoId", this.userId);
        linkedHashMap.put("ssoToken", this.ssoToken != null ? this.ssoToken : this.ptToken);
        String encodeDotnetJson = GwmCrypto.encodeDotnetJson(linkedHashMap);
        String valueOf = String.valueOf(System.currentTimeMillis());
        String randomNonce = GwmCrypto.randomNonce();
        Map<String, String> beanTechBaseHeaders = beanTechBaseHeaders(randomNonce, valueOf);
        beanTechBaseHeaders.put("bt-auth-sign", GwmCrypto.beanTechSign("POST", BT_LOGIN_PATH, randomNonce, valueOf, "json=" + encodeDotnetJson));
        if (this.beanId != null) {
            beanTechBaseHeaders.put("beanId", this.beanId);
        }
        beanTechBaseHeaders.put("Content-Type", "application/json; charset=UTF-8");
        Map<String, Object> asMap = asMap(decodeGAppResponse(http("POST", beanTechUrl(BT_LOGIN_PATH), beanTechBaseHeaders, encodeDotnetJson), "initialize_bean_tech"));
        if (asMap == null) {
            throw new SchemaException("BeanTech 登录响应格式异常");
        }
        Object ciProp = ciProp(asMap, "accessToken");
        if (ciProp == null) {
            throw new SchemaException("BeanTech 登录响应缺少 accessToken");
        }
        this.btAccessToken = String.valueOf(ciProp);
        Object ciProp2 = ciProp(asMap, "refreshToken");
        if (ciProp2 != null) {
            this.btRefreshToken = String.valueOf(ciProp2);
        }
        Object ciProp3 = ciProp(asMap, "beanId");
        this.btBeanId = ciProp3 != null ? String.valueOf(ciProp3) : this.beanId;
        if (this.ssoToken == null) {
            throw new SchemaException("缺少 ssoToken，无法登录 AutoAI");
        }
        String valueOf2 = String.valueOf(System.currentTimeMillis());
        LinkedHashMap linkedHashMap2 = new LinkedHashMap();
        linkedHashMap2.put("appType", 0);
        linkedHashMap2.put("phone", this.phone);
        linkedHashMap2.put("pushId", "0");
        linkedHashMap2.put("pushKey", "0");
        linkedHashMap2.put("ssoid", this.userId);
        linkedHashMap2.put("ssoTk", this.ssoToken);
        LinkedHashMap linkedHashMap3 = new LinkedHashMap();
        linkedHashMap3.put("brandType", "gwm");
        linkedHashMap3.put("cVer", GwmCrypto.SOURCE_APP_VERSION);
        linkedHashMap3.put("fn", "GW.M.APP_LOGIN");
        linkedHashMap3.put("fv", "0202");
        linkedHashMap3.put("mobileId", this.deviceId);
        linkedHashMap3.put("osType", "Android");
        linkedHashMap3.put("osVer", "");
        linkedHashMap3.put("rs", "2");
        linkedHashMap3.put(BridgeConst.EXTRA_TS, GwmCrypto.chinaTimestamp());
        linkedHashMap3.put("tk", "");
        linkedHashMap3.put("v", "1.0");
        LinkedHashMap linkedHashMap4 = new LinkedHashMap();
        linkedHashMap4.put("body", linkedHashMap2);
        linkedHashMap4.put("header", linkedHashMap3);
        String str = "https://gapp-api.gwmapp-h.com/tsp/v1/proxy/navinfo/GW.M.APP_LOGIN?p=" + urlEncodeFull(GwmCrypto.encodeDotnetJson(linkedHashMap4));
        LinkedHashMap linkedHashMap5 = new LinkedHashMap();
        linkedHashMap5.put("v", "1.0");
        linkedHashMap5.put("cid", this.deviceId);
        linkedHashMap5.put("client", "phone");
        linkedHashMap5.put("sign", GwmCrypto.autoAiSign(valueOf2));
        linkedHashMap5.put("time", valueOf2);
        linkedHashMap5.put("ckey", GwmCrypto.AUTO_AI_CKEY);
        linkedHashMap5.put("protocolVer", "2.1.2");
        linkedHashMap5.put("brandType", "GWM");
        linkedHashMap5.put("Accept-Encoding", "gzip");
        linkedHashMap5.put("User-Agent", GwmCrypto.OFFICIAL_USER_AGENT);
        Object decodeAutoAiResponse = decodeAutoAiResponse(http("GET", str, linkedHashMap5, null), "initialize_auto_ai");
        Object ciProp4 = ciProp(decodeAutoAiResponse, "tokenId");
        Object ciProp5 = ciProp(decodeAutoAiResponse, "userId");
        if (ciProp4 == null || ciProp5 == null) {
            throw new SchemaException("AutoAI 登录响应缺少 tokenId/userId");
        }
        this.autoTokenId = String.valueOf(ciProp4);
        this.autoUserId = String.valueOf(ciProp5);
    }

    private Object gAppPost(String str, String str2, Map<String, Object> map, boolean z) throws Exception {
        String str3;
        String encodeDotnetJson = GwmCrypto.encodeDotnetJson(map);
        if (z) {
            encodeDotnetJson = GwmCrypto.encryptEnvelope(encodeDotnetJson, 1);
        }
        String valueOf = String.valueOf((System.currentTimeMillis() / 1000) * 1000);
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        linkedHashMap.put("Authorization", this.btAccessToken == null ? "" : this.btAccessToken);
        linkedHashMap.put("SourceApp", "GWM");
        linkedHashMap.put("SourceType", "ANDROID");
        linkedHashMap.put("SourceAppVer", GwmCrypto.SOURCE_APP_VERSION);
        linkedHashMap.put("Timestamp", valueOf);
        linkedHashMap.put("DeviceId", this.deviceId);
        linkedHashMap.put("AppId", GwmCrypto.G_APP_APP_ID);
        String str4 = encodeDotnetJson;
        linkedHashMap.put("NoteId", GwmCrypto.DEFAULT_NOTE_ID);
        LinkedHashMap linkedHashMap2 = new LinkedHashMap();
        if (this.gToken != null) {
            str3 = GwmCrypto.DEFAULT_NOTE_ID;
            linkedHashMap2.put("G-TOKEN", this.gToken);
        } else {
            str3 = GwmCrypto.DEFAULT_NOTE_ID;
        }
        linkedHashMap2.put("Authorization", this.btAccessToken == null ? "" : this.btAccessToken);
        if (this.userId != null) {
            linkedHashMap2.put("ssoId", this.userId);
        }
        linkedHashMap2.put("SourceApp", "GWM");
        linkedHashMap2.put("SourceType", "ANDROID");
        linkedHashMap2.put("SourceAppVer", GwmCrypto.SOURCE_APP_VERSION);
        linkedHashMap2.put("SourceAppCode", GwmCrypto.SOURCE_APP_CODE);
        linkedHashMap2.put("Timestamp", valueOf);
        linkedHashMap2.put("DeviceId", this.deviceId);
        linkedHashMap2.put("AppId", GwmCrypto.G_APP_APP_ID);
        if (this.beanId != null) {
            linkedHashMap2.put("beanId", this.beanId);
        }
        linkedHashMap2.put("NoteId", str3);
        linkedHashMap2.put("Sign", GwmCrypto.defaultSign("POST", str2, str4, linkedHashMap));
        linkedHashMap2.put("Accept-Encoding", "gzip");
        linkedHashMap2.put("User-Agent", GwmCrypto.OFFICIAL_USER_AGENT);
        linkedHashMap2.put("Content-Type", "application/json; charset=UTF-8");
        return decodeGAppResponse(http("POST", str2, linkedHashMap2, str4), str);
    }

    private static String beanTechUrl(String str) {
        if (str.startsWith("/")) {
            str = str.substring(1);
        }
        return BEAN_TECH_BASE + str;
    }

    private Map<String, String> beanTechBaseHeaders(String str, String str2) {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        linkedHashMap.put("securityToken", "");
        linkedHashMap.put("tenantId", "1");
        linkedHashMap.put("terminal", "GW_APP_GWM");
        linkedHashMap.put("enterPriseId", "CC01");
        linkedHashMap.put("operatorRole", "0");
        linkedHashMap.put("rs", "2");
        linkedHashMap.put("sys", "app-android");
        linkedHashMap.put("cVer", GwmCrypto.SOURCE_APP_VERSION);
        linkedHashMap.put("appId", "097a7099af30d960");
        linkedHashMap.put("brand", "10");
        linkedHashMap.put("bt-auth-appkey", GwmCrypto.BEAN_TECH_APP_KEY);
        linkedHashMap.put("bt-auth-nonce", str);
        linkedHashMap.put("bt-auth-timestamp", str2);
        linkedHashMap.put("Accept-Encoding", "gzip");
        linkedHashMap.put("User-Agent", GwmCrypto.OFFICIAL_USER_AGENT);
        return linkedHashMap;
    }

    private Map<String, String> beanTechHeaders(String str, String str2, String str3, String str4, String str5, String str6) throws AuthException {
        String str7 = this.btBeanId != null ? this.btBeanId : this.beanId;
        if (!notEmpty(this.btAccessToken) || !notEmpty(str7) || !notEmpty(this.autoTokenId)) {
            throw new AuthException("BeanTech 会话不完整，请重新登录");
        }
        LinkedHashMap<String, String> h = new LinkedHashMap<>();
        h.put("securityToken", "");
        h.put("beanId", str7);
        h.put("tenantId", "1");
        h.put("terminal", "GW_APP_GWM");
        h.put("tokenId", this.autoTokenId);
        h.put("accessToken", this.btAccessToken);
        h.put("enterPriseId", "CC01");
        h.put("operatorRole", "0");
        h.put("rs", "2");
        h.put("sys", "app-android");
        h.put("vin", str6);
        h.put("cVer", GwmCrypto.SOURCE_APP_VERSION);
        h.put("appId", "097a7099af30d960");
        h.put("brand", "10");
        h.put("bt-auth-sign", GwmCrypto.beanTechSign(str, str2, str3, str4, str5));
        h.put("bt-auth-nonce", str3);
        h.put("bt-auth-appkey", GwmCrypto.BEAN_TECH_APP_KEY);
        h.put("bt-auth-timestamp", str4);
        h.put("Content-Type", "application/json; charset=UTF-8");
        h.put("Connection", "Keep-Alive");
        h.put("Accept-Encoding", "gzip");
        h.put("User-Agent", GwmCrypto.OFFICIAL_USER_AGENT);
        return h;
    }

    private Object decodeGAppResponse(HttpResult httpResult, String str) throws Exception {
        checkHttp(httpResult, str);
        Object asMap = asMap(JsonLite.parse(httpResult.body));
        if (asMap == null) {
            throw new SchemaException(str + ": 响应不是 JSON 对象");
        }
        String ciString = ciString(asMap, "code");
        if (ciString != null && !ciString.isEmpty() && !"0".equals(ciString) && !"000000".equals(ciString) && !"200".equals(ciString)) {
            if ("1013".equals(ciString)) {
                throw new RiskControlException("触发风控（1013），请在官方 APP 完成验证后再试");
            }
            String ciString2 = ciString(asMap, "description");
            if (ciString2 == null) {
                ciString2 = ciString(asMap, "msg");
            }
            if (ciString2 == null || ciString2.isEmpty()) {
                ciString2 = "响应: " + summarizeBody(httpResult.body);
            }
            throw new AuthException((str + ": API 返回错误码 " + ciString + " " + ciString2).trim());
        }
        Object ciProp = ciProp(asMap, "data");
        if (ciProp != null) {
            asMap = ciProp;
        }
        if (!(asMap instanceof String)) {
            return asMap;
        }
        String str2 = (String) asMap;
        if (str2.startsWith("G_A(")) {
            try {
                return JsonLite.parse(GwmCrypto.decryptEnvelope(str2));
            } catch (Exception e) {
                throw new SchemaException(str + ": G_A 信封解密失败");
            }
        }
        return asMap;
    }

    private Object decodeAutoAiResponse(HttpResult httpResult, String str) throws Exception {
        checkHttp(httpResult, str);
        Map<String, Object> asMap = asMap(JsonLite.parse(httpResult.body));
        if (asMap == null || ciProp(asMap, "header") == null) {
            return decodeGAppResponse(httpResult, str);
        }
        Map<String, Object> asMap2 = asMap(ciProp(asMap, "header"));
        String ciString = asMap2 == null ? null : ciString(asMap2, "c");
        if (ciString != null && !ciString.isEmpty() && !"0".equals(ciString)) {
            if ("1013".equals(ciString)) {
                throw new RiskControlException("触发风控（1013），请在官方 APP 完成验证后再试");
            }
            String ciString2 = asMap2 == null ? "" : ciString(asMap2, "m");
            if ("-114".equals(ciString)) {
                throw new BusyException(str + ": 上一条远控还在执行中（-114 " + ciString2 + "）。车端执行一条远控通常需要 10~30 秒，正在自动等待重试…");
            }
            throw new AuthException((str + ": AutoAI 返回错误码 " + ciString + (ciString2 != null ? " " + ciString2 : "")).trim());
        }
        Object ciProp = ciProp(asMap, "body");
        return ciProp == null ? asMap : ciProp;
    }

    private void checkHttp(HttpResult httpResult, String str) throws Exception {
        if (httpResult.code == 401 || httpResult.code == 403) {
            throw new AuthException(str + ": 认证失败（HTTP " + httpResult.code + "）");
        }
        if (httpResult.code < 200 || httpResult.code > 299) {
            throw new ConnectionException(str + ": HTTP " + httpResult.code);
        }
    }

    private static String summarizeBody(String str) {
        if (str == null) {
            return "(空)";
        }
        String trim = str.replaceAll("\\s+", " ").trim();
        return trim.length() > 160 ? trim.substring(0, 160) + "…" : trim;
    }

    private static final class HttpResult {
        final String body;
        final int code;

        HttpResult(int i, String str) {
            this.code = i;
            this.body = str;
        }
    }

    private HttpResult http(String str, String str2, Map<String, String> map, String str3) throws IOException {
        try {
            HttpURLConnection httpURLConnection2 = (HttpURLConnection) new URL(str2).openConnection();
            try {
                httpURLConnection2.setRequestMethod(str);
                httpURLConnection2.setConnectTimeout(this.timeoutMs);
                httpURLConnection2.setReadTimeout(this.timeoutMs);
                httpURLConnection2.setInstanceFollowRedirects(false);
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    httpURLConnection2.setRequestProperty(entry.getKey(), entry.getValue());
                }
                if (str3 != null) {
                    httpURLConnection2.setDoOutput(true);
                    byte[] bytes = str3.getBytes(GwmCrypto.UTF8);
                    httpURLConnection2.setFixedLengthStreamingMode(bytes.length);
                    OutputStream outputStream = httpURLConnection2.getOutputStream();
                    try {
                        outputStream.write(bytes);
                        outputStream.close();
                    } catch (Throwable th) {
                        outputStream.close();
                        throw th;
                    }
                }
                int responseCode = httpURLConnection2.getResponseCode();
                HttpResult httpResult = new HttpResult(responseCode, readAll(responseCode >= 400 ? httpURLConnection2.getErrorStream() : httpURLConnection2.getInputStream(), httpURLConnection2.getContentEncoding()));
                if (httpURLConnection2 != null) {
                    httpURLConnection2.disconnect();
                }
                return httpResult;
            } catch (Throwable th2) {
                if (httpURLConnection2 != null) {
                    httpURLConnection2.disconnect();
                }
                throw th2;
            }
        } catch (Throwable th3) {
            throw new RuntimeException(th3);
        }
    }

    private static String readAll(InputStream inputStream, String str) throws IOException {
        if (inputStream == null) {
            return "";
        }
        if (str != null && str.toLowerCase(Locale.ROOT).contains("gzip")) {
            inputStream = new GZIPInputStream(inputStream);
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] bArr = new byte[8192];
        while (true) {
            int read = inputStream.read(bArr);
            if (read <= 0) {
                inputStream.close();
                return new String(byteArrayOutputStream.toByteArray(), GwmCrypto.UTF8);
            }
            byteArrayOutputStream.write(bArr, 0, read);
        }
    }

    private static String urlEncode(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            return str;
        }
    }

    private static String urlEncodeFull(String str) {
        return urlEncode(str).replace("*", "%2A").replace("%7E", "~");
    }

    static Map<String, Object> asMap(Object obj) {
        if (obj instanceof Map) {
            return (Map) obj;
        }
        return null;
    }

    static Object or(Object obj, Object obj2) {
        return obj != null ? obj : obj2;
    }

    static Object or(Object obj, Object obj2, Object obj3) {
        return obj != null ? obj : obj2 != null ? obj2 : obj3;
    }

    static Object ciProp(Object obj, String str) {
        Map<String, Object> asMap = asMap(obj);
        if (asMap == null) {
            return null;
        }
        String lowerCase = str.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Object> entry : asMap.entrySet()) {
            if (entry.getKey() != null && entry.getKey().toLowerCase(Locale.ROOT).equals(lowerCase)) {
                return entry.getValue();
            }
        }
        return null;
    }

    static String valueToStr(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Double) {
            double doubleValue = ((Double) obj).doubleValue();
            if (doubleValue == Math.rint(doubleValue) && !Double.isInfinite(doubleValue) && Math.abs(doubleValue) < 9.0E15d) {
                return String.valueOf((long) doubleValue);
            }
        }
        return String.valueOf(obj);
    }

    static String ciString(Object obj, String str) {
        Object ciProp = ciProp(obj, str);
        if (ciProp == null) {
            return null;
        }
        String valueToStr = valueToStr(ciProp);
        if (valueToStr.isEmpty()) {
            return null;
        }
        return valueToStr;
    }
}
