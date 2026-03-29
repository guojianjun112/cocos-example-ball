
/**
 * Copyright (c) 2019 Xiamen Yaji Software Co.Ltd. All rights reserved.
 * Created by daisy on 2019/06/25.
 */
import { _decorator, Component, Node, instantiate, Prefab, sys } from "cc";
import { Constants } from "../data/constants";
import { Ball } from "./ball";
import { BoardManager } from "./board-manager";
import { CameraCtrl } from "./camera-ctrl";
import { UIManager } from "./ui-manager";
import { AudioManager } from "./audio-manager";
import { yr } from "../yr";
const { ccclass, property } = _decorator;

/**
 * @zh 游戏主控制脚本，负责游戏流程控制和全局状态管理
 */
@ccclass("Game")
export class Game extends Component {
    private static readonly AD_CONFIG_URL = "https://www.wxzj.online/api/Advertisement/selectYymd";
    private static readonly USER_STORAGE_KEY = "bqxxl_user";

    private splashRotateIndex: number = Math.floor(Math.random() * 2);
    private splashRotateTimerRunning: boolean = false;
    private adInitToken: string = "";
    private adInitRunning: boolean = false;
    private adInitReady: boolean = false;

    private startSplashRotation() {
        if (this.splashRotateTimerRunning) return;
        this.splashRotateTimerRunning = true;

        const runOnce = () => {
            const useGdt = this.splashRotateIndex % 2 == 0;
            this.splashRotateIndex++;
            if (useGdt) {
                console.log('request splash: gdt');
                yr.inst.createSplashAd({
                    provider: yr.inst.ads.gdt.provider,
                    unitId: yr.inst.ads.gdt.splash,
                    success: () => {
                        console.log('gdt 开屏请求成功');
                    },
                    fail: () => {
                        console.log('gdt 开屏请求失败');
                    },
                    showAd: (data: any) => {
                        console.log('gdt 开屏事件', data);
                    }
                });
            } else {
                console.log('request splash: sigmob');
                yr.inst.createSplashAd({
                    provider: yr.inst.ads.sigmob.provider,
                    unitId: yr.inst.ads.sigmob.splash,
                    userId: yr.inst.ads.sigmob.userId,
                    success: () => {
                        console.log('sigmob 开屏请求成功');
                    },
                    fail: () => {
                        console.log('sigmob 开屏请求失败');
                    },
                    showAd: (data: any) => {
                        console.log('sigmob 开屏事件', data);
                    }
                });
            }
        };

        runOnce();
        this.schedule(runOnce, yr.inst.ads.splashIntervalSec);
    }
    @property(Prefab)
    ballPref: Prefab = null!;
    @property(BoardManager)
    boardManager: BoardManager = null!;
    @property(CameraCtrl)
    cameraCtrl: CameraCtrl = null!;
    @property(UIManager)
    uiManager: UIManager = null!;
    @property(AudioManager)
    audioManager: AudioManager = null!;

     // There is no diamond in first board
    initFirstBoard = false;

    get ball(){
        return this._ball;
    }

    state = Constants.GAME_STATE.READY;
    score = 0;
    hasRevive = false;
    _ball: Ball = null!;
    __preload () {
        Constants.game = this;
    }

    onLoad(){
        if (!this.ballPref) {
            console.log('There is no ball!!');
            this.enabled = false;
            return;
        }

        const ball = instantiate(this.ballPref) as Node;
        // @ts-ignore
        ball.parent = this.node.parent;
        this._ball = ball.getComponent(Ball)!;
    }

    start(){
        this.node.on(Constants.GAME_EVENT.RESTART, this.gameStart, this);
        this.node.on(Constants.GAME_EVENT.REVIVE, this.gameRevive, this);

        void this.ensureAdProvidersReady();
    }

    onDestroy() {
        this.node.off(Constants.GAME_EVENT.RESTART, this.gameStart, this);
        this.node.off(Constants.GAME_EVENT.REVIVE, this.gameRevive, this);
        this.splashRotateTimerRunning = false;
        this.unscheduleAllCallbacks();
    }

    resetGame() {
        this.state = Constants.GAME_STATE.READY;
        this._ball.reset();
        this.cameraCtrl.reset();
        this.boardManager.reset();
        this.uiManager.showDialog(true);
    }

    async gameStart(){
        await this.ensureAdProvidersReady();
        this.audioManager.playSound();
        this.uiManager.showDialog(false);
        this.state = Constants.GAME_STATE.PLAYING;
        this.hasRevive = false;
        this.score = 0;

                // 鏄剧ず寮€灞忓箍鍛?
        this.startSplashRotation();
    }

    private getTokenFromStorage() {
        try {
            const raw = sys.localStorage.getItem(Game.USER_STORAGE_KEY) || "{}";
            const user = JSON.parse(raw);
            return user?.token || "";
        } catch (e) {
            return "";
        }
    }

    private async ensureAdProvidersReady() {
        const token = this.getTokenFromStorage();
        if (this.adInitReady && this.adInitToken === token) {
            return;
        }
        if (this.adInitRunning) {
            return;
        }
        this.adInitRunning = true;
        try {
            yr.inst.init();
            await this.syncSplashConfigFromServer(token);

            console.log('initAd params gdt', JSON.stringify({ provider: yr.inst.ads.gdt.provider, appId: yr.inst.ads.gdt.appId }));
            yr.inst.initAd({
                provider: yr.inst.ads.gdt.provider,
                appId: yr.inst.ads.gdt.appId,
                success: () => {
                    console.log('gdt SDK 初始化成功');
                },
                fail: () => {
                    console.log('gdt SDK 初始化失败');
                }
            });

            console.log('initAd params sigmob', JSON.stringify({ provider: yr.inst.ads.sigmob.provider, appId: yr.inst.ads.sigmob.appId, appKey: yr.inst.ads.sigmob.appKey }));
            yr.inst.initAd({
                provider: yr.inst.ads.sigmob.provider,
                appId: yr.inst.ads.sigmob.appId,
                appKey: yr.inst.ads.sigmob.appKey,
                success: () => {
                    console.log('sigmob SDK 初始化成功');
                },
                fail: () => {
                    console.log('sigmob SDK 初始化失败');
                }
            });

            this.adInitReady = true;
            this.adInitToken = token;
        } finally {
            this.adInitRunning = false;
        }
    }

    private async syncSplashConfigFromServer(token: string) {
        if (!token) {
            console.log("ad config: token empty, use local default config");
            return;
        }

        try {
            const response = await fetch(Game.AD_CONFIG_URL, {
                method: "GET",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": token,
                },
            });
            if (!response.ok) {
                console.log("ad config: request failed", response.status);
                return;
            }

            const resp = await response.json();
            if (resp?.code !== 1 || !Array.isArray(resp?.data)) {
                console.log("ad config: invalid response", JSON.stringify(resp));
                return;
            }

            const gdtSplashCandidates = resp.data.filter((item: any) => {
                return String(item?.as || "").toLowerCase() === "gdt"
                    && Number(item?.ggid) === 2
                    && Number(item?.type) === 4
                    && !!item?.appid
                    && !!item?.rewarded;
            });

            const preferredAppId = String(yr.inst.ads.gdt.appId || "");
            const gdtSplash = gdtSplashCandidates.find((item: any) => String(item?.appid) === preferredAppId)
                || gdtSplashCandidates[0];

            if (!gdtSplash) {
                console.log("ad config: gdt splash config not found, keep local config");
                return;
            }

            yr.inst.ads.gdt.appId = String(gdtSplash.appid);
            yr.inst.ads.gdt.splash = String(gdtSplash.rewarded);
            console.log("ad config: applied remote gdt splash", JSON.stringify({
                id: gdtSplash.id,
                appId: yr.inst.ads.gdt.appId,
                splash: yr.inst.ads.gdt.splash,
                candidates: gdtSplashCandidates.length,
            }));
        } catch (e: any) {
            console.log("ad config: fetch failed", e?.message || e);
        }
    }

    gameDie(){
        this.audioManager.playSound(false);
        this.state = Constants.GAME_STATE.PAUSE;

        if (!this.hasRevive) {
            this.node.emit(Constants.GAME_EVENT.DYING, ()=>{
                this.gameOver();
            });
        } else {
            this.gameOver();
        }
    }

    gameOver() {
        this.state = Constants.GAME_STATE.OVER;
        this.audioManager.playSound(false);

        this.resetGame();
    }

    gameRevive(){
        this.hasRevive = true;
        this.state = Constants.GAME_STATE.READY;
        this.ball.revive();
        this.scheduleOnce(() => {
            this.audioManager.playSound();
            this.state = Constants.GAME_STATE.PLAYING;
        }, 1);
    }

    addScore(score: number){
        this.score += score;
        this.node.emit(Constants.GAME_EVENT.ADDSCORE, this.score);
    }
}



