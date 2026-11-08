package com.dorocar.car.vehicle;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/* loaded from: classes.dex */
public final class GwmCrypto {
    public static final String AUTO_AI_CKEY = "ea49a50f914b8d38af1c84809d302683";
    private static final String AUTO_AI_PRIVATE_KEY = "dad377585f566b548c961a418dcec41a";
    public static final String BEAN_TECH_APP_KEY = "7863128529";
    private static final String BEAN_TECH_SECRET = "21382b32fea1d5fa03813d806d2dd64f";
    public static final String DEFAULT_NOTE_ID = "145765423214576567716671";
    public static final String G_APP_APP_ID = "GWM-APP-ANDROID-1100018";
    private static final char[] HEX_UPPER;
    public static final String OFFICIAL_USER_AGENT = "okhttp/4.2.2";
    private static final byte[] OPENSSL_MAGIC;
    private static final String SECRET_32 = "E3*138%pb=GcflmhmsaA4WU^J-f&0Ofe";
    private static final String SECRET_36 = "t8X_MybKFjp-Kg^mt99ALe-ArGzJE5mpCOra";
    public static final String SOURCE_APP_CODE = "2150";
    public static final String SOURCE_APP_VERSION = "2.2.0";
    public static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Map<Integer, String> ENVELOPE_PASSWORDS = new LinkedHashMap();

    private GwmCrypto() {
    }

    static {
        ENVELOPE_PASSWORDS.put(1, "Qin.1^0123456789abcdef0123456789abcdef0123456789abcdef012345cdef");
        ENVELOPE_PASSWORDS.put(2, "Gwn*9$0123456789abcdef0123456789abcdef0189abcdef0123456789abcdef");
        OPENSSL_MAGIC = "Salted__".getBytes(UTF8);
        HEX_UPPER = "0123456789ABCDEF".toCharArray();
    }

    public static String sha256Hex(String str) {
        return bytesToHex(sha256(str.getBytes(UTF8)));
    }

    public static String md5Hex(String str) {
        try {
            return bytesToHex(MessageDigest.getInstance("MD5").digest(str.getBytes(UTF8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 不可用", e);
        }
    }

    static byte[] sha256(byte[] bArr) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bArr);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    static byte[] md5(byte[] bArr) {
        try {
            return MessageDigest.getInstance("MD5").digest(bArr);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 不可用", e);
        }
    }

    public static String bytesToHex(byte[] bArr) {
        StringBuilder sb = new StringBuilder(bArr.length * 2);
        for (byte b : bArr) {
            sb.append(Character.forDigit((b >> 4) & 15, 16));
            sb.append(Character.forDigit(b & 15, 16));
        }
        return sb.toString();
    }

    static String toHexUpper(byte[] bArr) {
        char[] cArr = new char[bArr.length * 2];
        for (int i = 0; i < bArr.length; i++) {
            int i2 = bArr[i] & 255;
            int i3 = i * 2;
            cArr[i3] = HEX_UPPER[i2 >>> 4];
            cArr[i3 + 1] = HEX_UPPER[i2 & 15];
        }
        return new String(cArr);
    }

    public static String encodeDotnetJson(Object obj) {
        StringBuilder sb = new StringBuilder(128);
        writeDotnetJson(sb, obj, 0);
        return sb.toString();
    }

    private static void writeDotnetJson(StringBuilder sb, Object obj, int i) {
        if (i > 32) {
            throw new IllegalArgumentException("json_depth_invalid");
        }
        if (obj == null) {
            sb.append("null");
            return;
        }
        if (obj instanceof Boolean) {
            sb.append(((Boolean) obj).booleanValue() ? "true" : "false");
            return;
        }
        if ((obj instanceof Double) || (obj instanceof Float)) {
            double doubleValue = ((Number) obj).doubleValue();
            if (doubleValue == Math.rint(doubleValue) && !Double.isInfinite(doubleValue)) {
                sb.append((long) doubleValue);
                return;
            } else {
                sb.append(doubleValue);
                return;
            }
        }
        if (obj instanceof Number) {
            sb.append(obj.toString());
            return;
        }
        if (obj instanceof CharSequence) {
            writeDotnetString(sb, obj.toString());
            return;
        }
        if (obj instanceof Map) {
            sb.append('{');
            boolean z = true;
            for (Map.Entry entry : ((Map<?, ?>) obj).entrySet()) {
                if (!z) {
                    sb.append(',');
                }
                writeDotnetString(sb, String.valueOf(entry.getKey()));
                sb.append(':');
                writeDotnetJson(sb, entry.getValue(), i + 1);
                z = false;
            }
            sb.append('}');
            return;
        }
        if (obj instanceof Iterable) {
            sb.append('[');
            boolean z2 = true;
            for (Object obj2 : (Iterable) obj) {
                if (!z2) {
                    sb.append(',');
                }
                writeDotnetJson(sb, obj2, i + 1);
                z2 = false;
            }
            sb.append(']');
            return;
        }
        if (obj.getClass().isArray()) {
            sb.append('[');
            int length = Array.getLength(obj);
            for (int i2 = 0; i2 < length; i2++) {
                if (i2 > 0) {
                    sb.append(',');
                }
                writeDotnetJson(sb, Array.get(obj, i2), i + 1);
            }
            sb.append(']');
            return;
        }
        throw new IllegalArgumentException("json_value_invalid: " + obj.getClass());
    }

    private static boolean isHtmlSensitive(char c) {
        return c == '\"' || c == '&' || c == '\'' || c == '+' || c == '<' || c == '>' || c == '`';
    }

    static void writeDotnetString(StringBuilder sb, String str) {
        int i;
        sb.append('\"');
        int i2 = 0;
        while (i2 < str.length()) {
            char charAt = str.charAt(i2);
            switch (charAt) {
                case '\b':
                    sb.append("\\b");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                default:
                    if (charAt >= ' ' && charAt <= '~' && !isHtmlSensitive(charAt)) {
                        sb.append(charAt);
                        break;
                    } else if (Character.isHighSurrogate(charAt) && (i = i2 + 1) < str.length() && Character.isLowSurrogate(str.charAt(i))) {
                        appendUnicode(sb, charAt);
                        appendUnicode(sb, str.charAt(i));
                        i2 = i;
                        break;
                    } else {
                        appendUnicode(sb, charAt);
                    }
            }
            i2++;
        }
        sb.append('\"');
    }

    private static void appendUnicode(StringBuilder sb, int i) {
        char[] charArray = "0123456789ABCDEF".toCharArray();
        sb.append('\\').append('u').append(charArray[(i >> 12) & 15]).append(charArray[(i >> 8) & 15]).append(charArray[(i >> 4) & 15]).append(charArray[i & 15]);
    }

    static byte[][] deriveOpenSslKeyIv(String str, byte[] bArr) {
        byte[] bytes = str.getBytes(UTF8);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(48);
        byte[] bArr2 = new byte[0];
        while (byteArrayOutputStream.size() < 48) {
            byte[] bArr3 = new byte[bArr2.length + bytes.length + bArr.length];
            System.arraycopy(bArr2, 0, bArr3, 0, bArr2.length);
            System.arraycopy(bytes, 0, bArr3, bArr2.length, bytes.length);
            System.arraycopy(bArr, 0, bArr3, bArr2.length + bytes.length, bArr.length);
            bArr2 = md5(bArr3);
            byteArrayOutputStream.write(bArr2, 0, bArr2.length);
        }
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        byte[] bArr4 = new byte[32];
        byte[] bArr5 = new byte[16];
        System.arraycopy(byteArray, 0, bArr4, 0, 32);
        System.arraycopy(byteArray, 32, bArr5, 0, 16);
        return new byte[][]{bArr4, bArr5};
    }

    private static Cipher aesCbc(int i, byte[] bArr, byte[] bArr2) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(i, new SecretKeySpec(bArr, "AES"), new IvParameterSpec(bArr2));
            return cipher;
        } catch (Exception e) {
            throw new IllegalStateException("AES 不可用", e);
        }
    }

    public static String encryptEnvelope(String str, int i, byte[] bArr) {
        String str2 = ENVELOPE_PASSWORDS.get(Integer.valueOf(i));
        if (str2 == null) {
            throw new IllegalArgumentException("未知 keyId: " + i);
        }
        byte[][] deriveOpenSslKeyIv = deriveOpenSslKeyIv(str2, bArr);
        try {
            byte[] doFinal = aesCbc(1, deriveOpenSslKeyIv[0], deriveOpenSslKeyIv[1]).doFinal(str.getBytes(UTF8));
            byte[] bArr2 = new byte[OPENSSL_MAGIC.length + 8 + doFinal.length];
            System.arraycopy(OPENSSL_MAGIC, 0, bArr2, 0, 8);
            System.arraycopy(bArr, 0, bArr2, 8, 8);
            System.arraycopy(doFinal, 0, bArr2, 16, doFinal.length);
            return "G_A(" + Base64Codec.encode(bArr2) + "," + i + ")";
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            throw new IllegalStateException("信封加密失败", e);
        }
    }

    public static String encryptEnvelope(String str, int i) {
        byte[] bArr = new byte[8];
        new SecureRandom().nextBytes(bArr);
        return encryptEnvelope(str, i, bArr);
    }

    public static String decryptEnvelope(String str) {
        if (str == null) {
            return null;
        }
        if (!str.startsWith("G_A(") || !str.endsWith(")")) {
            return str;
        }
        int lastIndexOf = str.lastIndexOf(44);
        if (lastIndexOf <= 4) {
            throw new IllegalStateException("G_A 信封格式错误");
        }
        try {
            int parseInt = Integer.parseInt(str.substring(lastIndexOf + 1, str.length() - 1).trim());
            String str2 = ENVELOPE_PASSWORDS.get(Integer.valueOf(parseInt));
            if (str2 == null) {
                throw new IllegalStateException("未知 keyId: " + parseInt);
            }
            byte[] decode = Base64Codec.decode(str.substring(4, lastIndexOf).replaceAll("\\s+", ""));
            if (decode.length < 32 || decode.length % 16 != 0) {
                throw new IllegalStateException("G_A 载荷格式错误");
            }
            for (int i = 0; i < 8; i++) {
                if (decode[i] != OPENSSL_MAGIC[i]) {
                    throw new IllegalStateException("G_A 载荷格式错误");
                }
            }
            byte[] bArr = new byte[8];
            System.arraycopy(decode, 8, bArr, 0, 8);
            int length = decode.length - 16;
            byte[] bArr2 = new byte[length];
            System.arraycopy(decode, 16, bArr2, 0, length);
            byte[][] deriveOpenSslKeyIv = deriveOpenSslKeyIv(str2, bArr);
            try {
                return new String(aesCbc(2, deriveOpenSslKeyIv[0], deriveOpenSslKeyIv[1]).doFinal(bArr2), UTF8);
            } catch (BadPaddingException | IllegalBlockSizeException e) {
                throw new IllegalStateException("G_A 解密失败", e);
            }
        } catch (NumberFormatException e2) {
            throw new IllegalStateException("G_A keyId 非法", e2);
        }
    }

    static String deriveSecret(String str, String str2, String str3) {
        long j;
        String str4;
        try {
            j = Long.parseLong(str.trim());
        } catch (Exception e) {
            j = 0;
        }
        int i = (int) ((j % 100000) % 32);
        long j2 = j & 1;
        String str5 = SECRET_36;
        if (j2 == 1) {
            StringBuilder sb = new StringBuilder(36);
            for (int i2 = 0; i2 < 6; i2++) {
                for (int i3 = 0; i3 < 6; i3++) {
                    sb.append(SECRET_36.charAt((i3 * 6) + i2));
                }
            }
            str5 = sb.toString();
        }
        String substring = (str5 + str5).substring(i, i + 6);
        int i4 = (int) (j % 8);
        if (str2 == null) {
            str2 = "";
        }
        int i5 = i4 + 6;
        if (str2.length() < i5) {
            str4 = "";
        } else {
            str4 = str2.substring(i4, i5);
        }
        String trim = str3 == null ? "" : str3.trim();
        return SECRET_32 + substring + str4 + (trim.length() > 9 ? trim.substring(3, 9) : "");
    }

    public static String defaultSign(String str, String str2, String str3, Map<String, String> map) {
        String str4 = get(map, "Timestamp");
        String str5 = get(map, "Authorization");
        String str6 = get(map, "DeviceId");
        StringBuilder sb = new StringBuilder(512);
        sb.append(str.toUpperCase()).append(str2);
        String[] strArr = {"AppId", "Authorization", "DeviceId", "NoteId", "SourceApp", "SourceAppVer", "SourceType", "Timestamp"};
        for (int i = 0; i < 8; i++) {
            String str7 = strArr[i];
            sb.append(str7.toLowerCase()).append(':').append(get(map, str7));
        }
        if (!"GET".equalsIgnoreCase(str)) {
            sb.append("json=").append(str3 == null ? "" : str3);
        }
        sb.append(deriveSecret(str4, str6, str5));
        return sha256Hex(sb.toString());
    }

    private static String get(Map<String, String> map, String str) {
        String str2;
        return (map == null || (str2 = map.get(str)) == null) ? "" : str2;
    }

    public static String javaUrlEncode(String str) {
        StringBuilder sb = new StringBuilder(str.length() * 2);
        char[] charArray = "0123456789ABCDEF".toCharArray();
        for (byte b : str.getBytes(UTF8)) {
            int i = b & 255;
            char c = (char) i;
            if (((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || ((c >= '0' && c <= '9') || c == '-' || c == '_' || c == '.' || c == '*')) && i < 128) {
                sb.append(c);
            } else if (i == 32) {
                sb.append('+');
            } else {
                sb.append('%').append(charArray[i >> 4]).append(charArray[i & 15]);
            }
        }
        return sb.toString();
    }

    public static String beanTechSign(String str, String str2, String str3, String str4, String str5) {
        StringBuilder sb = new StringBuilder("/");
        boolean z = true;
        for (String str6 : str2.split("/")) {
            if (!str6.isEmpty()) {
                if (!z) {
                    sb.append('/');
                }
                sb.append(urlDecode(str6));
                z = false;
            }
        }
        return sha256Hex(javaUrlEncode(str.toUpperCase() + ((Object) sb) + ("bt-auth-appkey:7863128529bt-auth-nonce:" + str3 + "bt-auth-timestamp:" + str4) + str5 + BEAN_TECH_SECRET).replace("+", "").replace("%20", "").replace("%0A", "").replace("%09", "").replace("%0D", ""));
    }

    static String urlDecode(String str) {
        int i;
        if (str.indexOf(37) < 0) {
            return str;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(str.length());
        int i2 = 0;
        while (i2 < str.length()) {
            char charAt = str.charAt(i2);
            if (charAt == '%' && (i = i2 + 2) < str.length()) {
                int i3 = i2 + 1;
                if (isHex(str.charAt(i3)) && isHex(str.charAt(i))) {
                    i2 += 3;
                    byteArrayOutputStream.write(Integer.parseInt(str.substring(i3, i2), 16));
                }
            }
            byte[] bytes = String.valueOf(charAt).getBytes(UTF8);
            byteArrayOutputStream.write(bytes, 0, bytes.length);
            i2++;
        }
        return new String(byteArrayOutputStream.toByteArray(), UTF8);
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    public static String autoAiSign(String str) {
        String str2 = "SIGN_BODY=[]&SIGN_TIME=" + str;
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec("C_KEY=ea49a50f914b8d38af1c84809d302683&API_KEY=dad377585f566b548c961a418dcec41a".getBytes(UTF8), "HmacSHA1"));
            return Base64Codec.encode(mac.doFinal(str2.getBytes(UTF8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 不可用", e);
        }
    }

    public static String chinaTimestamp() {
        long currentTimeMillis = System.currentTimeMillis();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        return simpleDateFormat.format(new Date(currentTimeMillis)) + String.format(Locale.US, "%03d", Long.valueOf(currentTimeMillis % 1000));
    }

    public static String randomNonce() {
        byte[] bArr = new byte[16];
        new SecureRandom().nextBytes(bArr);
        return sha256Hex(toHexUpper(bArr)).substring(0, 16);
    }

    public static String randomSequence() {
        byte[] bArr = new byte[16];
        new SecureRandom().nextBytes(bArr);
        return bytesToHex(bArr) + (new SecureRandom().nextInt(9000) + 1000);
    }

    public static String randomDeviceId() {
        byte[] bArr = new byte[16];
        new SecureRandom().nextBytes(bArr);
        return toHexUpper(bArr);
    }

    public static Object normalizeKeys(Object obj) {
        if (obj instanceof Map) {
            LinkedHashMap linkedHashMap = new LinkedHashMap();
            for (Map.Entry entry : ((Map<?, ?>) obj).entrySet()) {
                linkedHashMap.put(String.valueOf(entry.getKey()).toLowerCase(Locale.ROOT), normalizeKeys(entry.getValue()));
            }
            return linkedHashMap;
        }
        if (obj instanceof List) {
            List list = (List) obj;
            ArrayList arrayList = new ArrayList(list.size());
            Iterator it = list.iterator();
            while (it.hasNext()) {
                arrayList.add(normalizeKeys(it.next()));
            }
            return arrayList;
        }
        return obj;
    }
}
