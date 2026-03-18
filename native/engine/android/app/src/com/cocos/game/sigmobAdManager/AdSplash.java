package com.cocos.game.sigmobAdManager;

import android.app.Activity;
import android.text.TextUtils;
import android.widget.FrameLayout;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.cocos.game.JsbBridgeCallback;
import com.cocos.game.adManager.AdManager;
import com.cocos.game.gdtAdManager.AdMainCallBack;
import com.sigmob.windad.Splash.WindSplashAD;
import com.sigmob.windad.Splash.WindSplashADListener;
import com.sigmob.windad.Splash.WindSplashAdRequest;
import com.sigmob.windad.WindAdError;

import java.util.HashMap;
import java.util.Map;

public class AdSplash {

    private static AdSplash instance;

    private AdMain m_mainInstance;
    private WindSplashAD splashAd;
    private AdMainCallBack m_adMainCallBack;

    public static AdSplash getInstance() {
        if (instance == null) {
            instance = new AdSplash();
            instance.m_mainInstance = AdMain.getInstance();
        }
        return instance;
    }

    public AdMainCallBack LoadAd(String placementId, String userId) {
        if (m_adMainCallBack == null) {
            m_adMainCallBack = new AdMainCallBack();
        }

        Activity activity = (Activity) m_mainInstance.getGameCtx();
        if (activity == null || TextUtils.isEmpty(placementId)) {
            m_mainInstance.DebugPrintE("%s : %s %s act == null || placementId == null", "splashAd", placementId, "sigmob");
            return m_adMainCallBack;
        }

        android.util.Log.i("SigmobSplash", "LoadAd placementId=" + placementId + ", userId=" + userId);
        Map<String, Object> options = new HashMap<>();
        if (!TextUtils.isEmpty(userId)) {
            options.put("user_id", userId);
        }

        WindSplashAdRequest splashAdRequest = new WindSplashAdRequest(placementId, userId, options);
        splashAd = new WindSplashAD(splashAdRequest, new WindSplashADListener() {
            @Override
            public void onSplashAdShow(String placementId) {
                android.util.Log.i("SigmobSplash", "onSplashAdShow placementId=" + placementId);
            }

            @Override
            public void onSplashAdLoadSuccess(String placementId) {
                android.util.Log.i("SigmobSplash", "onSplashAdLoadSuccess placementId=" + placementId);
                if (m_adMainCallBack.adLoadStatusCallBack != null) {
                    m_adMainCallBack.adLoadStatusCallBack.onSuccess(AdMainCallBack.LoadStatusType.RENDER, placementId);
                }
            }

            @Override
            public void onSplashAdLoadFail(WindAdError error, String placementId) {
                android.util.Log.e("SigmobSplash", "onSplashAdLoadFail placementId=" + placementId + ", error=" + error);
                if (m_adMainCallBack.adLoadStatusCallBack != null) {
                    m_adMainCallBack.adLoadStatusCallBack.onError(AdMainCallBack.LoadStatusType.LOAD, null, 0, error.toString());
                }
            }

            @Override
            public void onSplashAdShowError(WindAdError error, String placementId) {
                android.util.Log.e("SigmobSplash", "onSplashAdShowError placementId=" + placementId + ", error=" + error);
                if (m_adMainCallBack.adLoadStatusCallBack != null) {
                    m_adMainCallBack.adLoadStatusCallBack.onError(AdMainCallBack.LoadStatusType.RENDER, null, 0, error.toString());
                }
            }

            @Override
            public void onSplashAdClick(String placementId) {
                android.util.Log.i("SigmobSplash", "onSplashAdClick placementId=" + placementId);
            }

            @Override
            public void onSplashAdClose(String placementId) {
                android.util.Log.i("SigmobSplash", "onSplashAdClose placementId=" + placementId);
                if (splashAd != null) {
                    splashAd.destroy();
                }
                AdManager.getInstance().setSplashState(0);
                FrameLayout container = m_mainInstance.getMainView();
                if (container != null) {
                    container.removeAllViews();
                }
                if (m_adMainCallBack.adLoadStatusCallBack != null) {
                    m_adMainCallBack.adLoadStatusCallBack.onSuccess(AdMainCallBack.LoadStatusType.RENDER, "2");
                }
            }

            @Override
            public void onSplashAdSkip(String s) {
                android.util.Log.i("SigmobSplash", "onSplashAdSkip");
            }
        });

        android.util.Log.i("SigmobSplash", "loadAd()");
        splashAd.loadAd();
        return m_adMainCallBack;
    }

    public void ShowAd(String placementId) {
        FrameLayout container = m_mainInstance.getMainView();
        if (container == null || splashAd == null) {
            m_mainInstance.DebugPrintE("%s : %s %s container == null || ad == null", "splashAd", placementId, "sigmob");
            return;
        }

        android.util.Log.i("SigmobSplash", "show() placementId=" + placementId);
        splashAd.show(container);

        JSONObject obj = new JSONObject();
        obj.put("adv_provider", "sigmob");
        obj.put("adv_id", placementId);
        obj.put("adv_event", "splashAd");
        JsbBridgeCallback.getInstance().sendToScript("showAd", JSON.toJSONString(obj));
    }
}
