package com.dorocar.car.vehicle;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.OutputStream;
import java.util.List;

/**
 * 登录凭据持久化（Android 侧薄封装）。
 *
 * 旧实现把凭据用 AndroidKeyStore 中的 AES-GCM 密钥加密后存进 SharedPreferences，
 * 密钥或密文任一环节丢失（密钥库重建、厂商清理、备份恢复、GCM 校验失败）都会导致解密异常，
 * 表现就是"删掉后台再打开必须重新登录"。
 *
 * 现在：凭据以 JSON 写入三个相互独立的位置，任意一处可读即可恢复登录，并在读取成功后自动回填其余位置。
 *  - 私有文件 files/doro_gwm_session.json（主）
 *  - SharedPreferences doro_gwm_secure/payload（键名与旧版本保持一致，便于平滑升级）
 *  - 外部私有目录 Android/data/<pkg>/files/doro_gwm_session.json（兜底）
 * 写入与解析逻辑见 {@link SessionCodec}。
 */
public final class GwmSecureStore {
    private static final String TAG = "GwmSecureStore";
    private static final String PREFS = "doro_gwm_secure";
    private static final String PREF_PAYLOAD = "payload";
    private static final String FILE_NAME = "doro_gwm_session.json";

    private final Context appContext;
    private final SharedPreferences prefs;
    private final SessionCodec codec = new SessionCodec();

    public GwmSecureStore(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = this.appContext.getSharedPreferences(PREFS, 0);
    }

    public boolean save(GwmClient gwmClient) {
        if (gwmClient == null) {
            return false;
        }
        String json;
        try {
            json = GwmCrypto.encodeDotnetJson(gwmClient.toStateMap());
        } catch (Throwable th) {
            Log.e(TAG, "序列化凭据失败", th);
            return false;
        }
        if (json == null || json.length() < 2) {
            return false;
        }
        String payload = SessionCodec.wrap(json);
        boolean ok = false;
        try {
            ok |= SessionCodec.writeAtomic(internalFile(), payload);
        } catch (Throwable th) {
            Log.w(TAG, "写入内部文件失败", th);
        }
        try {
            ok |= this.prefs.edit().putString(PREF_PAYLOAD, payload).commit();
        } catch (Throwable th) {
            Log.w(TAG, "写入 SharedPreferences 失败", th);
        }
        try {
            File f = externalFile();
            if (f != null) {
                ok |= SessionCodec.writeAtomic(f, payload);
            }
        } catch (Throwable th) {
            Log.w(TAG, "写入外部私有目录失败", th);
        }
        if (!ok) {
            Log.e(TAG, "所有存储位置均写入失败，登录状态可能无法保持");
        }
        return ok;
    }

    public GwmClient load() {
        String fromFile = null;
        String fromPrefs = null;
        String fromExternal = null;
        try {
            fromFile = SessionCodec.read(internalFile());
        } catch (Throwable ignored) {
        }
        try {
            fromPrefs = this.prefs.getString(PREF_PAYLOAD, null);
        } catch (Throwable ignored) {
        }
        try {
            File f = externalFile();
            fromExternal = f == null ? null : SessionCodec.read(f);
        } catch (Throwable ignored) {
        }

        GwmClient client = this.codec.decodeFirst(SessionCodec.listOf(fromFile, fromPrefs, fromExternal));
        if (client != null) {
            // 有副本缺失时即时回填，降低下次启动再丢的风险
            if (fromFile == null || fromPrefs == null || fromExternal == null) {
                save(client);
            }
            return client;
        }
        if (hasCredentials()) {
            Log.w(TAG, "所有凭据副本均不可用：" + this.codec.lastError());
            clear();
        }
        return null;
    }

    public String lastLoadError() {
        return this.codec.lastError();
    }

    public void clear() {
        try {
            this.prefs.edit().remove(PREF_PAYLOAD).commit();
        } catch (Throwable ignored) {
        }
        try {
            internalFile().delete();
        } catch (Throwable ignored) {
        }
        try {
            File f = externalFile();
            if (f != null) {
                f.delete();
            }
        } catch (Throwable ignored) {
        }
    }

    public boolean hasCredentials() {
        try {
            if (this.prefs.contains(PREF_PAYLOAD)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            File f = internalFile();
            if (f.exists() && f.length() > 0) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            File f = externalFile();
            if (f != null && f.exists() && f.length() > 0) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public boolean exportPlaintext(OutputStream outputStream) {
        GwmClient client = load();
        if (client == null) {
            return false;
        }
        try {
            outputStream.write(GwmCrypto.encodeDotnetJson(client.toStateMap()).getBytes("UTF-8"));
            outputStream.flush();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private File internalFile() {
        return new File(this.appContext.getFilesDir(), FILE_NAME);
    }

    private File externalFile() {
        try {
            File dir = this.appContext.getExternalFilesDir(null);
            return dir == null ? null : new File(dir, FILE_NAME);
        } catch (Throwable th) {
            return null;
        }
    }
}
