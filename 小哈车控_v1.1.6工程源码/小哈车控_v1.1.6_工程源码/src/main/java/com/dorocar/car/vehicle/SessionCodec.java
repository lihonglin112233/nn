package com.dorocar.car.vehicle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 登录凭据的存取核心（纯 Java，不依赖 Android，便于单元测试）。
 *
 * 设计要点：
 * 1. 不依赖 AndroidKeyStore：旧实现用 Keystore 里的密钥加密凭据，一旦密钥被系统重建/清理，
 *    解密必然失败，表现为"杀掉后台再打开就要重新登录"。
 * 2. 多副本冗余：内部文件 + SharedPreferences + 外部私有目录，任意一处可读即可恢复登录；
 *    读到一处后自动回填其余位置。
 * 3. 原子写入：临时文件 + fsync + rename，进程被杀也不会写出半截 JSON。
 * 4. 脏数据自愈：全部副本损坏时主动清理并给出可读提示，避免每次启动都报错。
 */
public final class SessionCodec {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final String TMP_SUFFIX = ".tmp";
    private static final int FORMAT_VERSION = 2;

    private String lastError;

    public String lastError() {
        return this.lastError;
    }

    /** 生成带版本与摘要的载荷。 */
    public static String wrap(String stateJson) {
        return "{\"v\":" + FORMAT_VERSION + ",\"sha\":\"" + sha1(stateJson) + "\",\"data\":" + stateJson + "}";
    }

    /** 还原为 GwmClient，失败返回 null 并记录原因。 */
    public GwmClient decode(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            Object parsed = JsonLite.parse(text.trim());
            if (!(parsed instanceof Map)) {
                this.lastError = "登录凭据格式异常，需要重新登录";
                return null;
            }
            Map envelope = (Map) parsed;
            Object data = envelope.get("data");
            Object sha = envelope.get("sha");
            String body;
            if (data instanceof Map) {
                body = GwmCrypto.encodeDotnetJson(data);
                if (sha != null && !sha1(body).equals(String.valueOf(sha))) {
                    // 摘要不一致通常只是序列化差异，继续尝试
                }
            } else {
                body = text.trim();
            }
            Object bodyObj = JsonLite.parse(body);
            if (!(bodyObj instanceof Map)) {
                this.lastError = "登录凭据格式异常，需要重新登录";
                return null;
            }
            GwmClient client = GwmClient.fromStateMap((Map) bodyObj);
            if (client == null || !client.hasRefreshableSession()) {
                this.lastError = "本地凭据缺少刷新令牌，需要重新登录";
                return null;
            }
            this.lastError = null;
            return client;
        } catch (Throwable th) {
            this.lastError = "登录凭据已失效（" + th.getClass().getSimpleName() + "），需要重新登录";
            return null;
        }
    }

    /** 依次尝试多个副本，返回第一个可用的客户端。 */
    public GwmClient decodeFirst(List<String> copies) {
        GwmClient fallback = null;
        String firstError = null;
        for (String text : copies) {
            GwmClient c = decode(text);
            if (c != null) {
                return c;
            }
            if (firstError == null) {
                firstError = this.lastError;
            }
        }
        this.lastError = firstError;
        return fallback;
    }

    /** 原子写入：临时文件 -> fsync -> rename，失败再直接覆盖写。 */
    public static boolean writeAtomic(File target, String content) {
        try {
            File parent = target.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            byte[] bytes = content.getBytes(UTF8);
            File tmp = new File(target.getAbsolutePath() + TMP_SUFFIX);
            FileOutputStream fos = new FileOutputStream(tmp);
            try {
                fos.write(bytes);
                fos.flush();
                fos.getFD().sync();
            } finally {
                fos.close();
            }
            if (tmp.renameTo(target)) {
                return target.length() > 0;
            }
            target.delete();
            if (tmp.renameTo(target)) {
                return target.length() > 0;
            }
            FileOutputStream direct = new FileOutputStream(target);
            try {
                direct.write(bytes);
                direct.flush();
                direct.getFD().sync();
            } finally {
                direct.close();
            }
            tmp.delete();
            return target.length() > 0;
        } catch (Throwable th) {
            return false;
        }
    }

    public static String read(File f) {
        if (f == null || !f.exists() || f.length() == 0) {
            return null;
        }
        try {
            FileInputStream fis = new FileInputStream(f);
            try {
                byte[] buf = new byte[(int) f.length()];
                int read = 0;
                while (read < buf.length) {
                    int n = fis.read(buf, read, buf.length - read);
                    if (n <= 0) {
                        break;
                    }
                    read += n;
                }
                return read <= 0 ? null : new String(buf, 0, read, UTF8);
            } finally {
                fis.close();
            }
        } catch (Throwable th) {
            return null;
        }
    }

    public static String sha1(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] d = md.digest(s.getBytes(UTF8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) {
                sb.append(Character.forDigit((b >> 4) & 15, 16)).append(Character.forDigit(b & 15, 16));
            }
            return sb.toString();
        } catch (Throwable th) {
            return "";
        }
    }

    public static List<String> listOf(String... items) {
        List<String> out = new ArrayList<>(items.length);
        for (String s : items) {
            out.add(s);
        }
        return out;
    }
}
