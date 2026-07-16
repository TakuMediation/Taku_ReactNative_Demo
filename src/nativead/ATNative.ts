import ATReactNativeBridge from '../specs/NativeATBridge';
import { ATLog } from '../utils/ATLog';
import { ATAdEvents } from '../events/ATAdEvents';
import { NativeCallback } from '../constants/Const';
import { toAdError } from '../types';
import type {
  ATAdInfo,
  AdError,
  ATAdEventPayload,
  ATAdListener,
  ATAdRequest,
  ATShowConfig,
  ATAdStatusInfo,
} from '../types';
import { NativeAd } from './NativeAd';

/** 加载期监听。 */
export interface ATNativeNetworkListener {
  onNativeAdLoaded?: () => void;
  onNativeAdLoadFail?: (error: AdError) => void;
}

/** 展示期监听（NativeAd.setNativeEventListener 用；含 video）。 */
export interface ATNativeEventListener {
  onNativeAdShow?: (info: ATAdInfo) => void;
  onNativeAdClicked?: (info: ATAdInfo) => void;
  onNativeAdClose?: (info: ATAdInfo) => void;
  onNativeAdVideoStart?: () => void;
  onNativeAdVideoEnd?: () => void;
  onNativeAdVideoProgress?: (progress: number) => void;
  onDeeplinkCallback?: (info: ATAdInfo, success: boolean) => void;
}

function asError(message?: string): AdError {
  return (
    toAdError(message) ?? { code: '', desc: '', fullErrorInfo: message ?? '' }
  );
}

/**
 * Native 加载器。一 placementId 一实例，持**唯一** ATAdEvents 订阅，
 * 按 callbackName 把加载期事件给 networkListener、展示期事件给 NativeAd 的 eventListener。
 */
export class ATNative {
  private networkListener?: ATNativeNetworkListener;
  private eventListener?: ATNativeEventListener;
  private adSourceListener?: (payload: ATAdEventPayload) => void;
  private multipleLoadedListener?: (info: ATAdInfo) => void;
  private readonly boundRoute: ATAdListener = (p) => this.route(p);

  constructor(
    private readonly placementId: string,
    listener?: ATNativeNetworkListener
  ) {
    this.networkListener = listener;
    ATAdEvents.setAdListener(placementId, this.boundRoute);
  }

  /** NativeAd 经此注册展示期监听（共用本实例的唯一订阅）。 */
  _setEventListener(l: ATNativeEventListener): void {
    this.eventListener = l;
  }

  private route(p: ATAdEventPayload): void {
    if (p.callbackName.indexOf('AdSource') >= 0) {
      this.adSourceListener?.(p);
      return;
    }
    if (p.callbackName === NativeCallback.multipleLoaded) {
      this.multipleLoadedListener?.(p.extraDic ?? {});
      return;
    }
    const info: ATAdInfo = p.extraDic ?? {};
    switch (p.callbackName) {
      case NativeCallback.loaded:
        this.networkListener?.onNativeAdLoaded?.();
        break;
      case NativeCallback.loadFail:
        this.networkListener?.onNativeAdLoadFail?.(asError(p.requestMessage));
        break;
      case NativeCallback.show:
        this.eventListener?.onNativeAdShow?.(info);
        break;
      case NativeCallback.click:
        this.eventListener?.onNativeAdClicked?.(info);
        break;
      case NativeCallback.close:
        this.eventListener?.onNativeAdClose?.(info);
        break;
      case NativeCallback.videoStart:
        this.eventListener?.onNativeAdVideoStart?.();
        break;
      case NativeCallback.videoEnd:
        this.eventListener?.onNativeAdVideoEnd?.();
        break;
      case NativeCallback.videoProgress:
        this.eventListener?.onNativeAdVideoProgress?.(
          Number((p.extraDic as { progress?: number })?.progress ?? 0)
        );
        break;
      case NativeCallback.deeplink:
        this.eventListener?.onDeeplinkCallback?.(
          info,
          p.isDeeplinkSuccess ?? false
        );
        break;
      default:
        break;
    }
  }

  makeAdRequest(
    adRequest?: ATAdRequest,
    extra?: Record<string, unknown>
  ): void {
    ATLog.call('ATNative', 'makeAdRequest', this.placementId, adRequest, extra);
    ATReactNativeBridge.loadNativeAd(this.placementId, {
      ...(adRequest ? { atAdRequest: adRequest } : {}),
      ...(extra ?? {}),
    });
  }

  /**
   * 从缓存池消费一条广告，返回带 adId 的 NativeAd 对象（= 安卓 getNativeAd()）。
   * 列表里多次调用各取一条不同的；池空返回 null（调用方决定 makeAdRequest 补货）。
   */
  async getNativeAd(_showConfig?: ATShowConfig): Promise<NativeAd | null> {
    const res = (await ATReactNativeBridge.getNativeAd(this.placementId)) as {
      hasAd?: boolean;
      adId?: string;
      isExpress?: boolean;
    } | null;
    if (!res) {
      return null;
    }
    const adId = res.adId ?? '';
    return new NativeAd(this.placementId, adId, this, res.isExpress === true);
  }

  checkAdStatus(): Promise<ATAdStatusInfo> {
    ATLog.call('ATNative', 'checkAdStatus');
    return ATReactNativeBridge.checkNativeLoadStatus(
      this.placementId
    ) as Promise<ATAdStatusInfo>;
  }

  checkValidAdCaches(): Promise<ATAdInfo[]> {
    ATLog.call('ATNative', 'checkValidAdCaches');
    return ATReactNativeBridge.getNativeValidAds(this.placementId) as Promise<
      ATAdInfo[]
    >;
  }

  static entryAdScenario(
    placementId: string,
    scenarioId: string,
    tkExtra?: Record<string, unknown>
  ): void {
    ATLog.call('ATNative', 'entryAdScenario', placementId, scenarioId);
    ATReactNativeBridge.entryNativeScenario(
      placementId,
      scenarioId,
      tkExtra ?? {}
    );
  }

  setShowConfig(config: ATShowConfig): void {
    ATLog.call('ATNative', 'setShowConfig');
    ATReactNativeBridge.setNativeShowConfig(
      this.placementId,
      config as unknown as Object
    );
  }

  setLocalExtra(map: Record<string, unknown>): void {
    ATLog.call('ATNative', 'setLocalExtra');
    ATReactNativeBridge.setNativeLocalExtra(this.placementId, map);
  }

  setTKExtra(map: Record<string, unknown>): void {
    ATLog.call('ATNative', 'setTKExtra');
    ATReactNativeBridge.setNativeTKExtra(this.placementId, map);
  }

  setAdListener(listener: ATNativeNetworkListener): void {
    this.networkListener = listener;
  }

  setAdMultipleLoadedListener(listener: (info: ATAdInfo) => void): void {
    this.multipleLoadedListener = listener;
  }

  setAdSourceStatusListener(
    listener: (payload: ATAdEventPayload) => void
  ): void {
    this.adSourceListener = listener;
  }

  /** 单条广告销毁时回收其视图级订阅（由 {@link NativeAd.destroy} 调；不影响本加载器的 placementID 订阅）。 */
  _releaseAd(adId: string): void {
    if (adId) {
      ATAdEvents.removeAdInstanceListener(this.placementId, adId);
    }
  }

  /** 销毁本 placement 缓存（含已 load 未 show 的条）；不影响其他 placement。 */
  destroy(): void {
    ATLog.call('ATNative', 'destroy');
    ATReactNativeBridge.destroyNativeAd(this.placementId, '');
  }

  /** 释放本加载器的 placementID 订阅（整体不再用时调）。 */
  dispose(): void {
    // 传 boundRoute：仅当当前占位监听还是本实例时才删，避免误删同 placementId 另一存活实例的监听
    // （单条页/列表页共用位时，旧实例 dispose 不能清掉新实例的注册）。
    ATAdEvents.removeAdListener(this.placementId, this.boundRoute);
    ATAdEvents.removeRevenueListener(this.placementId);
  }
}
