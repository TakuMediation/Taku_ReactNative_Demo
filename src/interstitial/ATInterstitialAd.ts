import ATReactNativeBridge from '../specs/NativeATBridge';
import { ATLog } from '../utils/ATLog';
import { ATAdEvents } from '../events/ATAdEvents';
import { InterstitialCallback } from '../constants/Const';
import { toAdError } from '../types';
import type {
  ATAdInfo,
  AdError,
  ATAdEventPayload,
  ATAdRequest,
  ATShowConfig,
  ATAdStatusInfo,
} from '../types';

/**
 * 插屏监听器。无 reward/again，有 show + 视频播放回调。
 */
export interface ATInterstitialListener {
  onInterstitialAdLoaded?: () => void;
  onInterstitialAdLoadFail?: (error: AdError) => void;
  onInterstitialAdShow?: (info: ATAdInfo) => void;
  onInterstitialAdShowFailed?: (error: AdError) => void;
  onInterstitialAdClose?: (info: ATAdInfo) => void;
  onInterstitialAdClicked?: (info: ATAdInfo) => void;
  onInterstitialAdVideoStart?: (info: ATAdInfo) => void;
  onInterstitialAdVideoEnd?: (info: ATAdInfo) => void;
  onInterstitialAdVideoError?: (error: AdError) => void;
  onDeeplinkCallback?: (info: ATAdInfo, success: boolean) => void;
  onDownloadConfirm?: (info: ATAdInfo) => void;
  /** GDT 奖励式插屏任务回调。 */
  onInterstitialAdReward?: (info: ATAdInfo) => void;
}

function asError(message?: string): AdError {
  return (
    toAdError(message) ?? { code: '', desc: '', fullErrorInfo: message ?? '' }
  );
}

/**
 * 插屏广告（一 placementId 一实例）。
 * 实例内按 callbackName 把 AdSource/MultipleLoaded 分流到对应 listener。
 */
export class ATInterstitialAd {
  private mainListener?: ATInterstitialListener;
  private adSourceListener?: (payload: ATAdEventPayload) => void;
  private multipleLoadedListener?: (info: ATAdInfo) => void;
  private registered = false;

  constructor(private readonly placementId: string) {}

  private ensureRegistered(): void {
    if (this.registered) {
      return;
    }
    ATAdEvents.setAdListener(this.placementId, (p) => this.route(p));
    this.registered = true;
  }

  private route(p: ATAdEventPayload): void {
    if (p.callbackName.indexOf('AdSource') >= 0) {
      this.adSourceListener?.(p);
      return;
    }
    if (p.callbackName === InterstitialCallback.multipleLoaded) {
      this.multipleLoadedListener?.(p.extraDic ?? {});
      return;
    }
    if (this.mainListener) {
      dispatchInterstitialEvent(this.mainListener, p);
    }
  }

  load(adRequest?: ATAdRequest): void {
    ATLog.call('ATInterstitialAd', 'load', this.placementId, adRequest);
    const extraDic = adRequest ? { atAdRequest: adRequest } : {};
    ATReactNativeBridge.loadInterstitial(this.placementId, extraDic);
  }

  /** 展示（失败走 Event）。scenarioOrConfig 为 string 时走 scenario；为对象时走 showConfig。 */
  show(scenarioOrConfig?: string | ATShowConfig): void {
    ATLog.call('ATInterstitialAd', 'show', this.placementId, scenarioOrConfig);
    if (typeof scenarioOrConfig === 'string') {
      ATReactNativeBridge.showInterstitial(
        this.placementId,
        scenarioOrConfig
      ).catch(() => {});
      return;
    }
    if (scenarioOrConfig) {
      ATReactNativeBridge.showInterstitialWithConfig(
        this.placementId,
        scenarioOrConfig as unknown as Object
      ).catch(() => {});
    } else {
      ATReactNativeBridge.showInterstitial(this.placementId, '').catch(
        () => {}
      );
    }
  }

  isAdReady(): Promise<boolean> {
    ATLog.call('ATInterstitialAd', 'isAdReady');
    return ATReactNativeBridge.interstitialReady(this.placementId);
  }

  checkAdStatus(): Promise<ATAdStatusInfo> {
    ATLog.call('ATInterstitialAd', 'checkAdStatus');
    return ATReactNativeBridge.checkInterstitialLoadStatus(
      this.placementId
    ) as Promise<ATAdStatusInfo>;
  }

  checkValidAdCaches(): Promise<ATAdInfo[]> {
    ATLog.call('ATInterstitialAd', 'checkValidAdCaches');
    return ATReactNativeBridge.getInterstitialValidAds(
      this.placementId
    ) as Promise<ATAdInfo[]>;
  }

  static entryAdScenario(
    placementId: string,
    scenarioId: string,
    tkExtra?: Record<string, unknown>
  ): void {
    ATLog.call('ATInterstitialAd', 'entryAdScenario', placementId, scenarioId);
    ATReactNativeBridge.entryInterstitialScenario(
      placementId,
      scenarioId,
      tkExtra ?? {}
    );
  }

  setLocalExtra(map: Record<string, unknown>): void {
    ATLog.call('ATInterstitialAd', 'setLocalExtra');
    ATReactNativeBridge.setInterstitialLocalExtra(this.placementId, map);
  }

  setTKExtra(map: Record<string, unknown>): void {
    ATLog.call('ATInterstitialAd', 'setTKExtra');
    ATReactNativeBridge.setInterstitialTKExtra(this.placementId, map);
  }

  setAdListener(listener: ATInterstitialListener): void {
    this.mainListener = listener;
    this.ensureRegistered();
  }

  setAdRevenueListener(listener: (info: ATAdInfo) => void): void {
    ATAdEvents.setRevenueListener(this.placementId, listener);
  }

  setAdMultipleLoadedListener(listener: (info: ATAdInfo) => void): void {
    this.multipleLoadedListener = listener;
    this.ensureRegistered();
  }

  setAdSourceStatusListener(
    listener: (payload: ATAdEventPayload) => void
  ): void {
    this.adSourceListener = listener;
    this.ensureRegistered();
  }

  removeAdListener(): void {
    ATAdEvents.removeAdListener(this.placementId);
    ATAdEvents.removeRevenueListener(this.placementId);
    this.registered = false;
  }
}

export function dispatchInterstitialEvent(
  l: ATInterstitialListener,
  p: ATAdEventPayload
): void {
  const info: ATAdInfo = p.extraDic ?? {};
  switch (p.callbackName) {
    case InterstitialCallback.loaded:
      l.onInterstitialAdLoaded?.();
      break;
    case InterstitialCallback.loadFail:
      l.onInterstitialAdLoadFail?.(asError(p.requestMessage));
      break;
    case InterstitialCallback.show:
      l.onInterstitialAdShow?.(info);
      break;
    case InterstitialCallback.showFail:
      l.onInterstitialAdShowFailed?.(asError(p.requestMessage));
      break;
    case InterstitialCallback.close:
      l.onInterstitialAdClose?.(info);
      break;
    case InterstitialCallback.click:
      l.onInterstitialAdClicked?.(info);
      break;
    case InterstitialCallback.playStart:
      l.onInterstitialAdVideoStart?.(info);
      break;
    case InterstitialCallback.playEnd:
      l.onInterstitialAdVideoEnd?.(info);
      break;
    case InterstitialCallback.playFail:
      l.onInterstitialAdVideoError?.(asError(p.requestMessage));
      break;
    case InterstitialCallback.deeplink:
      l.onDeeplinkCallback?.(info, p.isDeeplinkSuccess ?? false);
      break;
    case InterstitialCallback.downloadConfirm:
      l.onDownloadConfirm?.(info);
      break;
    default:
      break;
  }
}
