import ATReactNativeBridge from '../specs/NativeATBridge';
import { ATLog } from '../utils/ATLog';
import { ATAdEvents } from '../events/ATAdEvents';
import { RewardVideoCallback } from '../constants/Const';
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
 * 激励监听器（含主流程与各广告源的回调）。
 * load/show 失败经此上报（不走 Promise.reject）。
 */
export interface ATRewardVideoListener {
  onRewardedVideoAdLoaded?: () => void;
  onRewardedVideoAdFailed?: (error: AdError) => void;
  onRewardedVideoAdPlayStart?: (info: ATAdInfo) => void;
  onRewardedVideoAdPlayEnd?: (info: ATAdInfo) => void;
  onRewardedVideoAdPlayFailed?: (error: AdError, info?: ATAdInfo) => void;
  onRewardedVideoAdClosed?: (info: ATAdInfo) => void;
  onRewardedVideoAdPlayClicked?: (info: ATAdInfo) => void;
  onReward?: (info: ATAdInfo) => void;
  onRewardFailed?: (info: ATAdInfo) => void;
  onDeeplinkCallback?: (info: ATAdInfo, success: boolean) => void;
  onDownloadConfirm?: (info: ATAdInfo) => void;
  onRewardedVideoAdAgainPlayStart?: (info: ATAdInfo) => void;
  onRewardedVideoAdAgainPlayEnd?: (info: ATAdInfo) => void;
  onRewardedVideoAdAgainPlayFailed?: (error: AdError, info: ATAdInfo) => void;
  onRewardedVideoAdAgainPlayClicked?: (info: ATAdInfo) => void;
  onAgainReward?: (info: ATAdInfo) => void;
  onAgainRewardFailed?: (info: ATAdInfo) => void;
}

function asError(message?: string): AdError {
  return (
    toAdError(message) ?? { code: '', desc: '', fullErrorInfo: message ?? '' }
  );
}

/**
 * 激励视频广告（一 placementId 一实例）。
 * 回调经唯一 ATReactNativeEventEmitter → ATAdEvents 按 placementID 分发到此实例；
 * 实例内按 callbackName 把 AdSource/MultipleLoaded 分流到对应 listener（ATAdEvents 不增机制）。
 */
export class ATRewardVideoAd {
  private mainListener?: ATRewardVideoListener;
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
    if (p.callbackName === RewardVideoCallback.multipleLoaded) {
      this.multipleLoadedListener?.(p.extraDic ?? {});
      return;
    }
    if (this.mainListener) {
      dispatchRewardEvent(this.mainListener, p);
    }
  }

  /** 加载（adRequest 可含 atAdRequest 底价/渠道；失败走 onRewardedVideoAdFailed）。 */
  load(adRequest?: ATAdRequest): void {
    ATLog.call('ATRewardVideoAd', 'load', this.placementId, adRequest);
    const extraDic = adRequest ? { atAdRequest: adRequest } : {};
    ATReactNativeBridge.loadRewardedVideo(this.placementId, extraDic);
  }

  /** 展示（失败走 Event）。scenarioOrConfig 为 string 时走 scenario 参数；为对象时走 showConfig。 */
  show(scenarioOrConfig?: string | ATShowConfig): void {
    ATLog.call('ATRewardVideoAd', 'show', this.placementId, scenarioOrConfig);
    if (typeof scenarioOrConfig === 'string') {
      ATReactNativeBridge.showRewardedVideo(
        this.placementId,
        scenarioOrConfig
      ).catch(() => {});
      return;
    }
    if (scenarioOrConfig) {
      ATReactNativeBridge.showRewardedVideoWithConfig(
        this.placementId,
        scenarioOrConfig as unknown as Object
      ).catch(() => {});
    } else {
      ATReactNativeBridge.showRewardedVideo(this.placementId, '').catch(
        () => {}
      );
    }
  }

  isAdReady(): Promise<boolean> {
    ATLog.call('ATRewardVideoAd', 'isAdReady'); 
    return ATReactNativeBridge.rewardedVideoReady(this.placementId);
  }

  checkAdStatus(): Promise<ATAdStatusInfo> {
    ATLog.call('ATRewardVideoAd', 'checkAdStatus'); 
    return ATReactNativeBridge.checkRewardedVideoLoadStatus(
      this.placementId
    ) as Promise<ATAdStatusInfo>;
  }

  checkValidAdCaches(): Promise<ATAdInfo[]> {
    ATLog.call('ATRewardVideoAd', 'checkValidAdCaches'); 
    return ATReactNativeBridge.getRewardedVideoValidAds(
      this.placementId
    ) as Promise<ATAdInfo[]>;
  }

  static entryAdScenario(
    placementId: string,
    scenarioId: string,
    tkExtra?: Record<string, unknown>
  ): void {
    ATLog.call('ATRewardVideoAd', 'entryAdScenario', placementId, scenarioId); 
    ATReactNativeBridge.entryRewardedVideoScenario(
      placementId,
      scenarioId,
      tkExtra ?? {}
    );
  }

  setLocalExtra(map: Record<string, unknown>): void {
    ATLog.call('ATRewardVideoAd', 'setLocalExtra'); 
    ATReactNativeBridge.setRewardedVideoLocalExtra(this.placementId, map);
  }

  setTKExtra(map: Record<string, unknown>): void {
    ATLog.call('ATRewardVideoAd', 'setTKExtra'); 
    ATReactNativeBridge.setRewardedVideoTKExtra(this.placementId, map);
  }

  setAdListener(listener: ATRewardVideoListener): void {
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

export function dispatchRewardEvent(
  l: ATRewardVideoListener,
  p: ATAdEventPayload
): void {
  const info: ATAdInfo = p.extraDic ?? {};
  switch (p.callbackName) {
    case RewardVideoCallback.loaded:
      l.onRewardedVideoAdLoaded?.();
      break;
    case RewardVideoCallback.loadFail:
      l.onRewardedVideoAdFailed?.(asError(p.requestMessage));
      break;
    case RewardVideoCallback.playStart:
      l.onRewardedVideoAdPlayStart?.(info);
      break;
    case RewardVideoCallback.playEnd:
      l.onRewardedVideoAdPlayEnd?.(info);
      break;
    case RewardVideoCallback.playFail:
      l.onRewardedVideoAdPlayFailed?.(asError(p.requestMessage), info);
      break;
    case RewardVideoCallback.close:
      l.onRewardedVideoAdClosed?.(info);
      break;
    case RewardVideoCallback.click:
      l.onRewardedVideoAdPlayClicked?.(info);
      break;
    case RewardVideoCallback.reward:
      l.onReward?.(info);
      break;
    case RewardVideoCallback.deeplink:
      l.onDeeplinkCallback?.(info, p.isDeeplinkSuccess ?? false);
      break;
    case RewardVideoCallback.againPlayStart:
      l.onRewardedVideoAdAgainPlayStart?.(info);
      break;
    case RewardVideoCallback.againPlayEnd:
      l.onRewardedVideoAdAgainPlayEnd?.(info);
      break;
    case RewardVideoCallback.againPlayFail:
      l.onRewardedVideoAdAgainPlayFailed?.(asError(p.requestMessage), info);
      break;
    case RewardVideoCallback.againClick:
      l.onRewardedVideoAdAgainPlayClicked?.(info);
      break;
    case RewardVideoCallback.againReward:
      l.onAgainReward?.(info);
      break;
    case RewardVideoCallback.rewardFailed:
      l.onRewardFailed?.(info);
      break;
    case RewardVideoCallback.againRewardFailed:
      l.onAgainRewardFailed?.(info);
      break;
    case RewardVideoCallback.downloadConfirm:
      l.onDownloadConfirm?.(info);
      break;
    default:
      break;
  }
}
