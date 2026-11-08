package com.dorocar.car.vehicle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.dorocar.car.vehicle.ChatCommandBridge;

/* loaded from: classes.dex */
public class BridgeBus extends BroadcastReceiver {
    private static final String TAG = "DoroBridge";

    @Override // android.content.BroadcastReceiver
    public void onReceive(final Context context, Intent intent) {
        String action;
        if (intent == null || (action = intent.getAction()) == null) {
            return;
        }
        if (BridgeConst.ACTION_SYNC.equals(action)) {
            handleSync(context, intent);
            return;
        }
        if (BridgeConst.ACTION_IN.equals(action)) {
            if (!DoroRemoteConfig.tokenMatches(context, intent.getStringExtra(BridgeConst.EXTRA_TOKEN))) {
                Log.w(TAG, "token 不匹配，丢弃");
                return;
            }
            final String stringExtra = intent.getStringExtra(BridgeConst.EXTRA_TALKER);
            if (!DoroRemoteConfig.allows(context, stringExtra)) {
                Log.w(TAG, "talker 不在白名单: " + stringExtra);
                reply(context, stringExtra, "发送者不在白名单", false);
            } else {
                String stringExtra2 = intent.getStringExtra(BridgeConst.EXTRA_CONTENT);
                Log.i(TAG, "收到指令 talker=" + mask(stringExtra) + " content=" + stringExtra2);
                ChatCommandBridge.handle(context, stringExtra2, new ChatCommandBridge.Listener() { // from class: com.dorocar.car.vehicle.BridgeBus.1
                    @Override // com.dorocar.car.vehicle.ChatCommandBridge.Listener
                    public void onResult(boolean z, String str) {
                        BridgeBus.this.reply(context, stringExtra, str, z);
                    }
                });
            }
        }
    }

    private void handleSync(Context context, Intent intent) {
        String stringExtra = intent.getStringExtra(BridgeConst.EXTRA_TOKEN);
        String stringExtra2 = intent.getStringExtra(BridgeConst.EXTRA_ALLOW);
        if (stringExtra == null || stringExtra.trim().isEmpty()) {
            Log.w(TAG, "SYNC 缺 token，忽略");
            return;
        }
        if (DoroRemoteConfig.hasToken(context) && !DoroRemoteConfig.tokenMatches(context, stringExtra)) {
            Log.w(TAG, "SYNC token 与本地不一致，拒绝（防止别人乱改）");
            return;
        }
        if (!DoroRemoteConfig.hasToken(context)) {
            Log.i(TAG, "首次接收 token，已保存");
            DoroRemoteConfig.setToken(context, stringExtra);
        }
        if (stringExtra2 != null) {
            DoroRemoteConfig.setAllow(context, stringExtra2);
            Log.i(TAG, "白名单已同步: " + stringExtra2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reply(Context context, String str, String str2, boolean z) {
        if (str == null || str.isEmpty()) {
            return;
        }
        try {
            Intent intent = new Intent(BridgeConst.ACTION_OUT);
            intent.setPackage(BridgeConst.PKG_WECHAT);
            intent.putExtra(BridgeConst.EXTRA_TOKEN, DoroRemoteConfig.token(context));
            intent.putExtra(BridgeConst.EXTRA_TALKER, str);
            intent.putExtra(BridgeConst.EXTRA_RESULT, str2 == null ? "" : str2);
            intent.putExtra(BridgeConst.EXTRA_OK, z);
            context.sendBroadcast(intent);
            StringBuilder append = new StringBuilder().append("回执已投递: ");
            if (str2 == null) {
                str2 = "";
            }
            Log.i(TAG, append.append(str2).toString());
        } catch (Throwable th) {
            Log.w(TAG, "回执投递失败", th);
        }
    }

    private static String mask(String str) {
        return str == null ? "null" : str.length() <= 6 ? str : str.substring(0, 3) + "***" + str.substring(str.length() - 2);
    }
}
