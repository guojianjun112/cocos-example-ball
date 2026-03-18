package com.cocos.game.sigmobAdManager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.alibaba.fastjson2.JSONObject;
import com.cocos.game.JsbBridgeCallback;
import com.cocos.game.Logger;
import com.cocos.game.adManager.AdManager;
import com.cocos.game.gdtAdManager.AdMainCallBack;
import com.sigmob.windad.OnInitializationListener;
import com.sigmob.windad.OnStartListener;
import com.sigmob.windad.WindAdOptions;
import com.sigmob.windad.WindAds;
import com.sigmob.windad.WindAgeRestrictedUserStatus;
import com.sigmob.windad.WindConsentStatus;

public class AdMain {
    @SuppressLint("StaticFieldLeak")
    private static AdMain instance;
    private Context m_ctx = null;
    private final AdMainCallBack m_adMainCallBack = new AdMainCallBack();
    private boolean m_isInit = false;
    private boolean m_debugLog;
    private FrameLayout m_frameLayout;

    public static AdMain getInstance() {
        if (instance == null) {
            instance = new AdMain();
        }
        return instance;
    }

    public void setGameCtx(Context ctx) {
        this.m_ctx = ctx;
    }

    public Context getGameCtx() {
        return m_ctx;
    }

    public void setAdMainCallBack(AdMainCallBack.SDKInitCallBack sdkInitCallBack) {
        m_adMainCallBack.Handler(sdkInitCallBack);
    }

    public void setDebugLogEnable(boolean enable) {
        this.m_debugLog = enable;
    }

    public void DebugPrintE(String format, Object... args) {
        if (m_debugLog) {
            Logger.e("", String.format(format, args));
        }
    }

    public FrameLayout CreateAndGetFrameLayout() {
        if (m_frameLayout == null) {
            m_frameLayout = new FrameLayout(m_ctx);
        }
        return m_frameLayout;
    }

    public FrameLayout getMainView() {
        return m_frameLayout;
    }

    public FrameLayout.LayoutParams getLayoutFull() {
        FrameLayout.LayoutParams lytp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        lytp.gravity = Gravity.CENTER;
        return lytp;
    }

    public Point getScreen() {
        WindowManager wm = ((WindowManager) m_ctx.getSystemService(Context.WINDOW_SERVICE));
        Display display;
        if (wm != null) {
            display = wm.getDefaultDisplay();
        } else {
            DebugPrintE("getSystemService wm = null");
            return null;
        }

        Point screenSize = new Point();
        if (display != null) {
            display.getRealSize(screenSize);
        } else {
            DebugPrintE("getSystemService display = null");
            return null;
        }
        DebugPrintE("getScreenSize xy:" + screenSize.x + " " + screenSize.y);
        return screenSize;
    }

    public float px2dp(float px) {
        final float scale = m_ctx.getResources().getDisplayMetrics().density;
        return px / Math.max(1, scale) + 0.5f;
    }

    public float dp2px(float dp) {
        final float scale = m_ctx.getResources().getDisplayMetrics().density;
        return dp * scale + 0.5f;
    }

    private void applyPrivacyDefaults() {
        WindAds ads = WindAds.sharedAds();
        ads.setUserAge(18);
        ads.setAdult(true);
        ads.setPersonalizedAdvertisingOn(true);
        ads.setSensorStatus(true);
        ads.setIsAgeRestrictedUser(WindAgeRestrictedUserStatus.NO);
        ads.setUserGDPRConsentStatus(WindConsentStatus.ACCEPT);
    }

    public void SDK_Init(String appId, String appKey) {
        if (m_isInit) {
            Log.i("AdInit", "[sigmob] SDK already initialized");
            return;
        }
        if (m_ctx == null) {
            DebugPrintE("Android: sigmob init failed, game context not set");
            return;
        }
        if (TextUtils.isEmpty(appId) || TextUtils.isEmpty(appKey)) {
            DebugPrintE("Android: sigmob init failed, appId or appKey empty");
            return;
        }

        Log.i("AdInit", "[sigmob] SDK_Init begin, appId=" + appId);
        applyPrivacyDefaults();

        WindAdOptions windAdOptions = new WindAdOptions(appId, appKey);
        WindAds ads = WindAds.sharedAds();
        ads.init(m_ctx.getApplicationContext(), windAdOptions, new OnInitializationListener() {
            @Override
            public void onInitializationSuccess() {
                Log.i("AdInit", "[sigmob] onInitializationSuccess");
                DebugPrintE("Android: sigmob init success");
                m_isInit = true;
                if (m_adMainCallBack.sdkInitCallBack != null) {
                    m_adMainCallBack.sdkInitCallBack.onSuccess();
                }

                Activity activity = (Activity) m_ctx;
                activity.runOnUiThread(() -> activity.addContentView(CreateAndGetFrameLayout(), getLayoutFull()));
            }

            @Override
            public void onInitializationFail(String error) {
                Log.e("AdInit", "[sigmob] onInitializationFail: " + error);
                DebugPrintE("Android: sigmob init failed, error: %s", error);
                m_isInit = false;
                if (m_adMainCallBack.sdkInitCallBack != null) {
                    m_adMainCallBack.sdkInitCallBack.onError(0, error);
                }
            }
        });

        ads.start(new OnStartListener() {
            @Override
            public void onStartSuccess() {
                Log.i("AdInit", "[sigmob] onStartSuccess");
                DebugPrintE("Android: sigmob start success");
            }

            @Override
            public void onStartFail(String error) {
                Log.e("AdInit", "[sigmob] onStartFail: " + error);
                DebugPrintE("Android: sigmob start failed, error: %s", error);
            }
        });
    }

    public void handlerAd(Context context, String event, JSONObject data) {
        Log.i("AdInit", "[sigmob] handlerAd event=" + event + ", data=" + data);
        if (TextUtils.equals(event, "initAd")) {
            String appId = data.getString("appId");
            String appKey = data.getString("appKey");
            initAdHandler(context, event, appId, appKey);
        } else if (TextUtils.equals(event, "splashAd")) {
            String unitId = data.getString("unitId");
            String userId = data.getString("userId");
            loadAdHandler(AdSplash.getInstance().LoadAd(unitId, userId), event, (s) -> AdSplash.getInstance().ShowAd(s));
        }
    }

    private void initAdHandler(Context context, String event, String appId, String appKey) {
        Log.i("AdInit", "[sigmob] initAdHandler called, appId=" + appId + ", event=" + event);

        AdMain adManager = AdMain.getInstance();
        adManager.setDebugLogEnable(true);
        adManager.setGameCtx(context);
        adManager.setAdMainCallBack(new AdMainCallBack.SDKInitCallBack() {
            @Override
            public void onSuccess() {
                Log.i("AdInit", "[sigmob] SDK init SUCCESS, sending event=" + event);
                JsbBridgeCallback.getInstance().sendToScript(event, "1");
            }

            @Override
            public void onError(int i, String e) {
                Log.i("AdInit", "[sigmob] SDK init FAILED, code=" + i + ", msg=" + e);
                JsbBridgeCallback.getInstance().sendToScript(event, "");
            }
        });

        adManager.SDK_Init(appId, appKey);
        Log.i("AdInit", "[sigmob] SDK_Init called, waiting for callback...");
    }

    private void loadAdHandler(AdMainCallBack callBack, String event, ShowAd showAd) {
        callBack.Handler(new AdMainCallBack.AdLoadStatusCallBack() {
            @Override
            public void onSuccess(AdMainCallBack.LoadStatusType type, Object obj) {
                if (type == AdMainCallBack.LoadStatusType.RENDER) {
                    String str = String.valueOf(obj);
                    if (!TextUtils.equals("2", str)) {
                        if (TextUtils.equals(event, "splashAd")) {
                            if (AdManager.getInstance().getSplashState() == 1) return;
                            AdManager.getInstance().setSplashState(1);
                        }
                        showAd.showAd(str);
                    }
                    JsbBridgeCallback.getInstance().sendToScript(event, obj == null ? "1" : String.valueOf(obj));
                }
            }

            @Override
            public void onError(AdMainCallBack.LoadStatusType type, Object obj, int i, String e) {
                if (type == AdMainCallBack.LoadStatusType.LOAD || type == AdMainCallBack.LoadStatusType.RENDER) {
                    JsbBridgeCallback.getInstance().sendToScript(event, "");
                }
            }
        });
    }

    public interface ShowAd {
        void showAd(String data);
    }
}
