import ATReactNativeBridge from '../specs/NativeATBridge';
import { ATLog } from '../utils/ATLog';
import { ATAdEvents } from '../events/ATAdEvents';
import { SplashCallback } from '../constants/Const';
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
 * 开屏监听器。load 失败/超时经此上报（不走 Promise.reject）。
 */
export interface ATSplashListener {
  /** 加载成功；isTimeout 表示是否为超时后兜底返回。 */
  onSplashAdLoaded?: (isTimeout: boolean) => void;
  onSplashAdLoadFail?: (error: AdError) => void;
  onSplashAdTimeout?: () => void;
  onSplashAdShow?: (info: ATAdInfo) => void;
  onSplashAdShowFailed?: (error: AdError) => void;
  onSplashAdClick?: (info: ATAdInfo) => void;
  onSplashAdClose?: (info: ATAdInfo) => void;
  onDeeplinkCallback?: (info: ATAdInfo, success: boolean) => void;
  onDownloadConfirm?: (info: ATAdInfo) => void;
}

/** 开屏 show 配置 = ATShowConfig + 桥内容器高度（heightRatio 优先；都不传则全屏）。 */
export type SplashShowConfig = ATShowConfig & {
  // heightRatio?: number;
  // heightPx?: number;
};

function asError(message?: string): AdError {
  return (
    toAdError(message) ?? { code: '', desc: '', fullErrorInfo: message ?? '' }
  );
}

/**
 * 开屏广告（一 placementId 一实例）。show 渲染进容器（高度可配，通过 heightRatio 设置）。
 */
export class ATSplashAd {
  private mainListener?: ATSplashListener;
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
    if (p.callbackName === SplashCallback.multipleLoaded) {
      this.multipleLoadedListener?.(p.extraDic ?? {});
      return;
    }
    if (this.mainListener) {
      dispatchSplashEvent(this.mainListener, p);
    }
  }

  /** 加载（adRequest 底价/渠道；fetchAdTimeout 毫秒；失败/超时走 Event）。 */
  load(adRequest?: ATAdRequest, fetchAdTimeout?: number): void {
    ATLog.call(
      'ATSplashAd',
      'load',
      this.placementId,
      adRequest,
      fetchAdTimeout
    );
    const extraDic: Record<string, unknown> = adRequest
      ? { atAdRequest: adRequest }
      : {};
    if (fetchAdTimeout != null) {
      extraDic.fetchAdTimeout = fetchAdTimeout;
    }
    ATReactNativeBridge.loadSplash(this.placementId, extraDic);
  }

  /** 展示。scenarioOrConfig 为 string 时走 scenario；为对象时走 showConfig（可含 heightRatio/heightPx）。 */
  show(scenarioOrConfig?: string | SplashShowConfig): void {
    ATLog.call('ATSplashAd', 'show', this.placementId, scenarioOrConfig);
    if (typeof scenarioOrConfig === 'string') {
      ATReactNativeBridge.showSplash(this.placementId, scenarioOrConfig).catch(
        () => {}
      );
      return;
    }
    if (scenarioOrConfig) {
      ATReactNativeBridge.showSplashWithConfig(
        this.placementId,
        scenarioOrConfig as unknown as Object
      ).catch(() => {});
    } else {
      ATReactNativeBridge.showSplash(this.placementId, '').catch(() => {});
    }
  }

  isAdReady(): Promise<boolean> {
    ATLog.call('ATSplashAd', 'isAdReady');
    return ATReactNativeBridge.splashReady(this.placementId);
  }

  checkAdStatus(): Promise<ATAdStatusInfo> {
    ATLog.call('ATSplashAd', 'checkAdStatus');
    return ATReactNativeBridge.checkSplashLoadStatus(
      this.placementId
    ) as Promise<ATAdStatusInfo>;
  }

  checkValidAdCaches(): Promise<ATAdInfo[]> {
    ATLog.call('ATSplashAd', 'checkValidAdCaches');
    return ATReactNativeBridge.getSplashValidAds(this.placementId) as Promise<
      ATAdInfo[]
    >;
  }

  static entryAdScenario(
    placementId: string,
    scenarioId: string,
    tkExtra?: Record<string, unknown>
  ): void {
    ATLog.call('ATSplashAd', 'entryAdScenario', placementId, scenarioId);
    ATReactNativeBridge.entrySplashScenario(
      placementId,
      scenarioId,
      tkExtra ?? {}
    );
  }

  setLocalExtra(map: Record<string, unknown>): void {
    ATLog.call('ATSplashAd', 'setLocalExtra');
    ATReactNativeBridge.setSplashLocalExtra(this.placementId, map);
  }

  setTKExtra(map: Record<string, unknown>): void {
    ATLog.call('ATSplashAd', 'setTKExtra');
    ATReactNativeBridge.setSplashTKExtra(this.placementId, map);
  }

  setAdListener(listener: ATSplashListener): void {
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

export function dispatchSplashEvent(
  l: ATSplashListener,
  p: ATAdEventPayload
): void {
  const info: ATAdInfo = p.extraDic ?? {};
  switch (p.callbackName) {
    case SplashCallback.loaded:
      l.onSplashAdLoaded?.(p.isTimeout ?? false);
      break;
    case SplashCallback.loadFail:
      l.onSplashAdLoadFail?.(asError(p.requestMessage));
      break;
    case SplashCallback.timeout:
      l.onSplashAdTimeout?.();
      break;
    case SplashCallback.show:
      l.onSplashAdShow?.(info);
      break;
    case SplashCallback.showFailed:
      l.onSplashAdShowFailed?.(asError(p.requestMessage));
      break;
    case SplashCallback.click:
      l.onSplashAdClick?.(info);
      break;
    case SplashCallback.close:
      l.onSplashAdClose?.(info);
      break;
    case SplashCallback.deeplink:
      l.onDeeplinkCallback?.(info, p.isDeeplinkSuccess ?? false);
      break;
    case SplashCallback.downloadConfirm:
      l.onDownloadConfirm?.(info);
      break;
    default:
      break;
  }
}
