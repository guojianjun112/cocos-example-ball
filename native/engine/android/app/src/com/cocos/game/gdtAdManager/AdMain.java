package com.cocos.game.gdtAdManager;

import android.annotation.SuppressLint;
import android.util.Log;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.text.TextUtils;
import android.view.Display;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.alibaba.fastjson2.JSONObject;
import com.cocos.game.JsbBridgeCallback;
import com.cocos.game.Logger;
import com.cocos.game.adManager.AdManager;
import com.qq.e.comm.managers.GDTAdSdk;

public class AdMain {
    @SuppressLint("StaticFieldLeak")
    private static AdMain instance;
    private Context m_ctx = null;
    private final AdMainCallBack m_adMainCallBack = new AdMainCallBack();
    private boolean m_isInit = false; // 是否初始化
    private boolean m_debugLog;
    private FrameLayout m_frameLayout;

    // 获取&创建单例类
    public static AdMain getInstance() {
        if (instance == null) {
            instance = new AdMain();
        }
        return instance;
    }

    /***
     * 设置游戏主类上下文
     * @param ctx 主类上下文
     */
    public void setGameCtx(Context ctx) {
        this.m_ctx = ctx;
    }

    public Context getGameCtx() {
        return m_ctx;
    }

    public void setAdMainCallBack(AdMainCallBack.SDKInitCallBack sdkInitCallBack) {
        m_adMainCallBack.Handler(sdkInitCallBack);
    }

    /***
     * 是否开启 调试日志输出
     * @param enable 调试输出开关
     */
    public void setDebugLogEnable(boolean enable) {
        this.m_debugLog = enable;
    }

    public void DebugPrintE(String format, Object... args) {
        if (m_debugLog) {
            Logger.e("", String.format(format, args));
        }
    }

    /***
     * 创建一个FrameLayout布局
     * @return 拿到这个布局
     */
    public FrameLayout CreateAndGetFrameLayout() {
        if (m_frameLayout == null) {
            m_frameLayout = new FrameLayout(m_ctx);
        }
        return m_frameLayout;
    }

    public FrameLayout getMainView() {
        return m_frameLayout;
    }

    /***
     * 获取一个撑满全屏的布局参数
     * @return 返回此参数
     */
    public FrameLayout.LayoutParams getLayoutFull() {
        FrameLayout.LayoutParams lytp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        lytp.gravity = Gravity.CENTER;
        return lytp;
    }

    /***
     * 获取屏幕真实宽高
     * @return 二维对象
     */
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
        DebugPrintE("getscreenSize xy:" + screenSize.x + " " + screenSize.y);
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

    /***
     * SDK 初始化
     */
    public void SDK_Init(String appId) {
        if (m_isInit) {
            return;
        }
        if (m_ctx == null) {
            DebugPrintE("Android: gdt 加载SDK失败,游戏上下文未设置");
            return;
        }
        if (TextUtils.isEmpty(appId)) {
            DebugPrintE("Android: gdt 加载SDK失败,appId为空");
            return;
        }
        GDTAdSdk.initWithoutStart(m_ctx, appId);
        SDK_StartLoad();
    }

    public void SDK_StartLoad() {
        // 开始加载
        GDTAdSdk.start(new GDTAdSdk.OnStartListener() {
            @Override
            public void onStartSuccess() {
                // 初始化成功
                if (m_isInit) return;
                DebugPrintE("Android: gdt 加载SDK成功");
                m_isInit = true;
                // 在初始化成功回调之后进行广告加载
                if (m_adMainCallBack.sdkInitCallBack != null) {
                    m_adMainCallBack.sdkInitCallBack.onSuccess();
                }
                // 添加容器到游戏上下文Activity
                Activity activity = (Activity) m_ctx;
                activity.runOnUiThread(() -> activity.addContentView(CreateAndGetFrameLayout(), getLayoutFull()));
            }

            @Override
            public void onStartFailed(Exception e) {
                // 初始化失败
                DebugPrintE("Android: gdt 加载SDK错误 msg: %s", e.getMessage());
                m_isInit = false;
                if (m_adMainCallBack.sdkInitCallBack != null) {
                    m_adMainCallBack.sdkInitCallBack.onError(0, e.getMessage());
                }
            }
        });
    }

    public void handlerAd(Context context, String event, JSONObject data) {
        if (TextUtils.equals(event, "initAd")) {
            String appId = data.getString("appId");
            initAdHandler(context, event, appId);
        } else if (TextUtils.equals(event, "splashAd")) {
            String unitId = data.getString("unitId");
            loadAdHandler(AdSplash.getInstance().LoadAd(unitId), event, (s) -> AdSplash.getInstance().ShowAd(s));
        }
    }

    private void initAdHandler(Context context, String event, String appId) {
        Log.i("AdInit", "[gdt] initAdHandler called, appId=" + appId + ", event=" + event);

        AdMain adManager = AdMain.getInstance();
        adManager.setDebugLogEnable(true);
        adManager.setGameCtx(context);
        adManager.setAdMainCallBack(new AdMainCallBack.SDKInitCallBack() {
            @Override
            public void onSuccess() {
                Log.i("AdInit", "[gdt] SDK init SUCCESS, sending event=" + event);

                JsbBridgeCallback.getInstance().sendToScript(event, "1");
            }

            @Override
            public void onError(int i, String e) {
                Log.i("AdInit", "[gdt] SDK init FAILED, code=" + i + ", msg=" + e);

                JsbBridgeCallback.getInstance().sendToScript(event, "");
            }
        });
        adManager.SDK_Init(appId);
        Log.i("AdInit", "[gdt] SDK_Init called, waiting for callback...");
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