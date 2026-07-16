import ATReactNativeBridge from '../specs/NativeATBridge';
import { ATLog } from '../utils/ATLog';
import { ATAdEvents } from '../events/ATAdEvents';
import { BannerCallback } from '../constants/Const';
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
 * Banner 监听器。Banner 自动刷新，有 AutoRefresh 回调。
 */
export interface ATBannerListener {
  onBannerAdLoaded?: () => void;
  onBannerAdLoadFail?: (error: AdError) => void;
  onBannerAdShow?: (info: ATAdInfo) => void;
  onBannerAdClicked?: (info: ATAdInfo) => void;
  onBannerAdClose?: (info: ATAdInfo) => void;
  onBannerAdAutoRefreshed?: (info: ATAdInfo) => void;
  onBannerAdAutoRefreshFail?: (error: AdError) => void;
  onDeeplinkCallback?: (info: ATAdInfo, success: boolean) => void;
  onDownloadConfirm?: (info: ATAdInfo) => void;
}

function asError(message?: string): AdError {
  return (
    toAdError(message) ?? { code: '', desc: '', fullErrorInfo: message ?? '' }
  );
}

/**
 * Banner 命令式 API。视图渲染走 Fabric `<ATBannerViewComponent>`；
 * 本类用于命令式控制（load/查询/destroy/监听）。实例内按 callbackName 分流。
 */
export class ATBannerView {
  private mainListener?: ATBannerListener;
  private adSourceListener?: (payload: ATAdEventPayload) => void;
  private multipleLoadedListener?: (info: ATAdInfo) => void;
  private placementId = '';

  setPlacementId(placementId: string): void {
    ATLog.call('ATBannerView', 'setPlacementId');
    if (this.placementId) {
      ATAdEvents.removeAdListener(this.placementId);
      ATAdEvents.removeRevenueListener(this.placementId);
    }
    this.placementId = placementId;
    ATAdEvents.setAdListener(placementId, (p) => this.route(p));
  }

  private route(p: ATAdEventPayload): void {
    if (p.callbackName.indexOf('AdSource') >= 0) {
      this.adSourceListener?.(p);
      return;
    }
    if (p.callbackName === BannerCallback.multipleLoaded) {
      this.multipleLoadedListener?.(p.extraDic ?? {});
      return;
    }
    if (this.mainListener) {
      dispatchBannerEvent(this.mainListener, p);
    }
  }

  /**
   * @param bannerSize 与 Fabric `width`/`height` 一致（pt/dp），避免 load 与 show 尺寸不一致导致空白。
   */
  loadAd(
    adRequest?: ATAdRequest,
    bannerSize?: { width: number; height: number }
  ): void {
    ATLog.call('ATBannerView', 'loadAd', this.placementId, adRequest, bannerSize);
    const w = bannerSize?.width ?? 320;
    const h = bannerSize?.height ?? 50;
    const extraDic: Record<string, unknown> = {
      size: { x: 0, y: 0, width: w, height: h },
    };
    if (adRequest) {
      extraDic.atAdRequest = adRequest;
    }
    ATReactNativeBridge.loadBannerAd(this.placementId, extraDic);
  }

  setShowConfig(config: ATShowConfig): void {
    ATLog.call('ATBannerView', 'setShowConfig');
    ATReactNativeBridge.setBannerShowConfig(
      this.placementId,
      config as unknown as Object
    );
  }

  setLocalExtra(map: Record<string, unknown>): void {
    ATLog.call('ATBannerView', 'setLocalExtra');
    ATReactNativeBridge.setBannerLocalExtra(this.placementId, map);
  }

  setTKExtra(map: Record<string, unknown>): void {
    ATLog.call('ATBannerView', 'setTKExtra');
    ATReactNativeBridge.setBannerTKExtra(this.placementId, map);
  }

  checkAdStatus(): Promise<ATAdStatusInfo> {
    ATLog.call('ATBannerView', 'checkAdStatus');
    return ATReactNativeBridge.checkBannerLoadStatus(
      this.placementId
    ) as Promise<ATAdStatusInfo>;
  }

  checkValidAdCaches(): Promise<ATAdInfo[]> {
    ATLog.call('ATBannerView', 'checkValidAdCaches');
    return ATReactNativeBridge.getBannerValidAds(this.placementId) as Promise<
      ATAdInfo[]
    >;
  }

  static entryAdScenario(
    placementId: string,
    scenarioId: string,
    tkExtra?: Record<string, unknown>
  ): void {
    ATLog.call('ATBannerView', 'entryAdScenario', placementId, scenarioId);
    ATReactNativeBridge.entryBannerScenario(
      placementId,
      scenarioId,
      tkExtra ?? {}
    );
  }

  setBannerAdListener(listener: ATBannerListener): void {
    this.mainListener = listener;
  }

  setAdRevenueListener(listener: (info: ATAdInfo) => void): void {
    ATAdEvents.setRevenueListener(this.placementId, listener);
  }

  setAdMultipleLoadedListener(listener: (info: ATAdInfo) => void): void {
    this.multipleLoadedListener = listener;
  }

  setAdSourceStatusListener(
    listener: (payload: ATAdEventPayload) => void
  ): void {
    this.adSourceListener = listener;
  }

  destroy(): void {
    ATLog.call('ATBannerView', 'destroy');
    ATReactNativeBridge.destroyBanner(this.placementId);
    if (this.placementId) {
      ATAdEvents.removeAdListener(this.placementId);
      ATAdEvents.removeRevenueListener(this.placementId);
    }
  }
}

export function dispatchBannerEvent(
  l: ATBannerListener,
  p: ATAdEventPayload
): void {
  const info: ATAdInfo = p.extraDic ?? {};
  switch (p.callbackName) {
    case BannerCallback.loaded:
      l.onBannerAdLoaded?.();
      break;
    case BannerCallback.loadFail:
      l.onBannerAdLoadFail?.(asError(p.requestMessage));
      break;
    case BannerCallback.show:
      l.onBannerAdShow?.(info);
      break;
    case BannerCallback.click:
      l.onBannerAdClicked?.(info);
      break;
    case BannerCallback.close:
      l.onBannerAdClose?.(info);
      break;
    case BannerCallback.refresh:
      l.onBannerAdAutoRefreshed?.(info);
      break;
    case BannerCallback.refreshFail:
      l.onBannerAdAutoRefreshFail?.(asError(p.requestMessage));
      break;
    case BannerCallback.deeplink:
      l.onDeeplinkCallback?.(info, p.isDeeplinkSuccess ?? false);
      break;
    case BannerCallback.downloadConfirm:
      l.onDownloadConfirm?.(info);
      break;
    default:
      break;
  }
}
