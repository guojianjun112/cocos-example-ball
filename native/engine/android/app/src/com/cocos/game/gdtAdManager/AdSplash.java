package com.cocos.game.gdtAdManager;

import android.app.Activity;
import android.os.SystemClock;
import android.text.TextUtils;
import android.widget.FrameLayout;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.cocos.game.JsbBridgeCallback;
import com.qq.e.ads.splash.SplashAD;
import com.qq.e.ads.splash.SplashADListener;
import com.qq.e.comm.pi.IBidding;
import com.qq.e.comm.util.AdError;

import java.util.HashMap;

// 开屏广告
public class AdSplash {

    private static AdSplash instance;

    private AdMain m_mainInstance;

    private SplashAD ad;
    private long expireTimestamp;

    private AdMainCallBack m_adMainCallBack;

    // 获取&创建单例类
    public static AdSplash getInstance() {
        if (instance == null) {
            instance = new AdSplash();
            instance.m_mainInstance = AdMain.getInstance();
        }
        return instance;
    }

    public AdMainCallBack LoadAd(String id) {
        expireTimestamp = SystemClock.elapsedRealtime();

        // 加载开屏广告
        if (m_adMainCallBack == null) {
            m_adMainCallBack = new AdMainCallBack();
        }

        Activity activity = (Activity) m_mainInstance.getGameCtx();
        if (activity == null || TextUtils.isEmpty(id)) {
            m_mainInstance.DebugPrintE("%s : %s %s act == null || id == null", "开屏广告", id, "gdt");
            return m_adMainCallBack;
        }

        ad = new SplashAD(activity, id, new SplashADListener() {
            @Override
            public void onADDismissed() {
                // 广告关闭
                m_mainInstance.getMainView().post(() -> {
                    m_mainInstance.getMainView().removeAllViews();
                    // Optional: You might want to notify TypeScript to re-show banners if they were
                    // hidden
                });
                if (m_adMainCallBack.adLoadStatusCallBack != null) {
                    m_adMainCallBack.adLoadStatusCallBack.onSuccess(AdMainCallBack.LoadStatusType.RENDER, "2");
                }
            }

            @Override
            public void onNoAD(AdError adError) {
                // 广告加载或展示过程中出错
                m_mainInstance.DebugPrintE("%s : %s %s 加载错误, code:%s, msg:%s", "开屏广告", id, "gdt",
                        adError.getErrorCode(), adError.getErrorMsg());
                if (m_adMainCallBack.adLoadStatusCallBack != null) {
                    m_adMainCallBack.adLoadStatusCallBack.onError(AdMainCallBack.LoadStatusType.LOAD, null,
                            adError.getErrorCode(), adError.getErrorMsg());
                }
            }

            @Override
            public void onADPresent() {
                // 广告成功展
                m_mainInstance.DebugPrintE("广告成功展示");
            }

            @Override
            public void onADClicked() {
                // 广告被点击
                m_mainInstance.DebugPrintE("广告被点击");
            }

            @Override
            public void onADTick(long millisUntilFinished) {
                // 倒计时，返回广告还将被展示的剩余时间
                m_mainInstance.DebugPrintE("广告倒计时：%d", millisUntilFinished);
            }

            @Override
            public void onADExposure() {
                // 广告曝光
                m_mainInstance.DebugPrintE("广告曝光");
            }

            @Override
            public void onADLoaded(long expireTimestamp) {
                // 广告加载成功的回调，在fetchAdOnly的情况下，表示广告拉取成功可以显示了。
                // 广告需要在SystemClock.elapsedRealtime < expireTimestamp前展示，否则在showAd时会返回广告超时错误。
                AdSplash.getInstance().expireTimestamp = expireTimestamp;
                if (m_adMainCallBack.adLoadStatusCallBack != null) {
                    m_adMainCallBack.adLoadStatusCallBack.onSuccess(AdMainCallBack.LoadStatusType.RENDER, id);
                }
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put(IBidding.EXPECT_COST_PRICE, ad.getECPM());
                hashMap.put(IBidding.HIGHEST_LOSS_PRICE, Math.max(0, ad.getECPM() - 1));
                ad.sendWinNotification(hashMap);
            }
        });
        ad.fetchFullScreenAdOnly();
        m_mainInstance.DebugPrintE("广告加载中.....");
        return m_adMainCallBack;
    }

    // 显示开屏广告
    public void ShowAd(String id) {
        FrameLayout container = m_mainInstance.getMainView();
        if (container == null || ad == null || !ad.isValid()) {
            m_mainInstance.DebugPrintE("%s : %s %s container == null || ad == null || !isValid", "开屏广告", id, "gdt");
            return;
        }
        if (SystemClock.elapsedRealtime() < expireTimestamp) {
            ad.showFullScreenAd(container);

            JSONObject obj = new JSONObject();
            obj.put("adv_provider", "gdt");
            obj.put("adv_id", id);
            if (ad.getExtraInfo() != null) {
                Object rid = ad.getExtraInfo().get("request_id");
                String requestId = rid != null ? JSON.toJSONString(rid) : id;
                obj.put("adv_no", !TextUtils.isEmpty(requestId) ? requestId : id);
                obj.put("adv_ecpm", ad.getECPM());
            }
            obj.put("adv_event", "splashAd");
            JsbBridgeCallback.getInstance().sendToScript("showAd", JSON.toJSONString(obj));
        }
    }
}