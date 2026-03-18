package com.cocos.game;

import android.content.Context;
import android.text.TextUtils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.cocos.lib.CocosActivity;
import com.cocos.lib.CocosHelper;
import com.cocos.lib.CocosJavascriptJavaBridge;
import com.cocos.lib.JsbBridge;

public class JsbBridgeCallback {
    private JsbBridgeCallback() {
    }

    private static final class InstanceHolder {
        private static final JsbBridgeCallback mInstance = new JsbBridgeCallback();
    }

    public static JsbBridgeCallback getInstance() {
        return InstanceHolder.mInstance;
    }

    public void onScript(String req, String arg) {
        if (mCallback != null) mCallback.onScript(req, arg);
    }

    public void sendToScript(String event, String data) {
        JSONObject obj = new JSONObject();
        obj.put("event", event);
        obj.put("data", data);
        String resp = JSON.toJSONString(obj);
        Logger.d("", String.format("Android: app sendToScript %s", resp));

        JsbBridge.sendToScript(resp);

        CocosHelper.runOnGameThread(() -> {
            String value = String.format("window.onNative(%s,'%s')", obj, "");
            CocosJavascriptJavaBridge.evalString(value);
        });
    }

    protected String getOaid(Context context) {
        // Simplified OAID implementation
        return "";
    }

    public interface ICallback {
        void onScript(String req, String arg);
    }

    private CocosActivity mContext;
    private ICallback mCallback;

    public void init(CocosActivity context) {
        mContext = context;

        mCallback = (req, arg) -> {
            Logger.d("", String.format("Android: app onScript %s", req));
            Logger.d("", String.format("Android: app onScript arg %s", arg));
            context.runOnUiThread(() -> {
                JSONObject requ = JSON.parseObject(req);
                String event = requ.getString("event");
                JSONObject data = requ.getJSONObject("data");
                if (TextUtils.equals(event, "initAd") ||
                        TextUtils.equals(event, "feedAd") ||
                        TextUtils.equals(event, "splashAd") ||
                        TextUtils.equals(event, "bannerAd") ||
                        TextUtils.equals(event, "interstitialAd") ||
                        TextUtils.equals(event, "rewardedVideoAd") ||
                        TextUtils.equals(event, "drawAd")) {
                    String provider = data.getString("provider");
                    Logger.d("", "Android: provider=" + provider + ", event=" + event + ", data=" + data);
                    if (TextUtils.equals(provider, "gdt")) {
                        com.cocos.game.gdtAdManager.AdMain.getInstance().handlerAd(context, event, data);
                    } else if (TextUtils.equals(provider, "sigmob")) {
                        com.cocos.game.sigmobAdManager.AdMain.getInstance().handlerAd(context, event, data);
                    }
                }
            });
        };
    }
}
