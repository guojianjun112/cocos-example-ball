package com.cocos.game.adManager;

import android.annotation.SuppressLint;

public class AdManager {
    @SuppressLint("StaticFieldLeak")
    private static AdManager instance;

    public static AdManager getInstance() {
        if (instance == null) {
            instance = new AdManager();
        }
        return instance;
    }

    // 0 未展示，1 已展示
    private int splashState = 0;
    private int feedState = 0;
    private int bannerState = 0;
    private int rewardState = 0;
    private int interstitialState = 0;
    private int drawState = 0;

    public void reset() {
        splashState = 0;
        feedState = 0;
        bannerState = 0;
        rewardState = 0;
        interstitialState = 0;
        drawState = 0;
    }

    public int getSplashState() {
        return splashState;
    }

    public void setSplashState(int splashState) {
        this.splashState = splashState;
    }

    public int getFeedState() {
        return feedState;
    }

    public void setFeedState(int feedState) {
        this.feedState = feedState;
    }

    public int getBannerState() {
        return bannerState;
    }

    public void setBannerState(int bannerState) {
        this.bannerState = bannerState;
    }

    public int getRewardState() {
        return rewardState;
    }

    public void setRewardState(int rewardState) {
        this.rewardState = rewardState;
    }

    public int getInterstitialState() {
        return interstitialState;
    }

    public void setInterstitialState(int interstitialState) {
        this.interstitialState = interstitialState;
    }

    public int getDrawState() {
        return drawState;
    }

    public void setDrawState(int drawState) {
        this.drawState = drawState;
    }
}