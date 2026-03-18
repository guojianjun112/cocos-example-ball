import { native, sys } from 'cc';
import { ads as adConfig } from './config/ad-config';

interface Callback {
  success?: Function | null | undefined;
  showAd?: Function | null | undefined;
  onProgress?: Function | null | undefined;
  rewarded?: Function | null | undefined;
  fail?: Function | null | undefined;
}

// 3.8.x
export class yr {
  private constructor() { }

  private static _inst?: yr | null | undefined = null;
  public static get inst(): yr {
    if (this._inst == null) this._inst = new yr();
    return this._inst;
  }

  public ads: any = adConfig;

  private callback: Map<string, Callback | null | undefined> = new Map();

  private isAndroid() {
    return sys.isNative && sys.os == sys.OS.ANDROID;
  }

  private isIos() {
    return sys.isNative && sys.os == sys.OS.IOS;
  }

  private bridge: 'native' | 'window' = 'window';
  public init() {
    console.log('sys.isNative', sys.isNative);
    console.log('sys.os', sys.os, JSON.stringify(sys.OS));
    console.log('sys.platform', sys.platform, JSON.stringify(sys.Platform));
    console.log('ads config', JSON.stringify(this.ads));

    if (this.isAndroid() || this.isIos()) {
      native.bridge.onNative = (res: string, arg?: string | null | undefined) => {
        if (this.bridge == 'native') this.onNative(res, arg);
      };

      window['onNative'] = (res: any, arg?: string | null | undefined) => {
        if (this.bridge == 'window') this.onNative(JSON.stringify(res), arg);
      };
    }
  }

  private onNative(res: string, _arg?: string | null | undefined) {
    console.log('yr onNative', res);
    let resp = JSON.parse(res);
    let { event, data } = resp;
    console.log('yr onNative event', event, 'data', data);

    if (event == 'showAd') {
      let cb = this.getCallback(event);
      if (data && cb?.showAd) cb.showAd(data);
    } else {
      if (data && this.getCallback(event)?.success) {
        this.getCallback(event).success(data);
      }
      else if (this.getCallback(event)?.fail) {
        this.getCallback(event).fail(data);
      }
    }
  }

  public setCallback(event: string, callback: Callback) {
    this.callback.set(event, callback);
  }
  private getCallback(event: string) {
    return this.callback.has(event) ? this.callback.get(event) : null;
  }

  public sendToNative(event: string, data?: any) {
    this.setCallback(event, { success: data?.success, showAd: data?.showAd, onProgress: data?.onProgress, rewarded: data?.rewarded, fail: data?.fail });

    let obj: Object = { event: event, data: data };
    let req = JSON.stringify(obj);
    console.log('yr sendToNative', req);

    if (this.bridge == 'native') {
      native.bridge.sendToNative(req, '');
    }

    if (this.bridge == 'window') {
      if (this.isAndroid()) {
        let typeVoid = 'V';
        let typeString = 'Ljava/lang/String;';
        let className = 'com/cocos/game/AppActivity';
        let methodName = 'sendToNative';
        let methodSignature = `(${typeString}${typeString})${typeVoid}`;
        native.reflection.callStaticMethod(className, methodName, methodSignature, req, '');
      } else if (this.isIos()) {
        let className = 'AppController';
        let methodName = 'sendToNative:andArg:';
        native.reflection.callStaticMethod(className, methodName, req, '');
      }
    }
  }

  /**
   * 鍒濆鍖栧箍鍛?
   * {
   *   provider: 'gdt', // app 蹇呭～
   *   appId: '',
   * }
   */
  public initAd(data?: any) {
    if (this.isAndroid() || this.isIos()) {
      this.sendToNative('initAd', data);
    } else {
      if (data?.success) data.success();
    }
  }

  /**
   * 寮€灞忓箍鍛?
   * {
   *   provider: 'gdt', // app 蹇呭～
   *   unitId: '',
   * }
   */
  public createSplashAd(data?: any) {
    if (this.isAndroid() || this.isIos()) {
      this.sendToNative('splashAd', data);
    } else {
      if (data?.success) data.success();
    }
  }
}



