import ATReactNativeBridge from '../specs/NativeATBridge';
import { ATAdEvents } from '../events/ATAdEvents';
import { dispatchInterstitialEvent } from './ATInterstitialAd';
import type { ATInterstitialListener } from './ATInterstitialAd';
import type { ATShowConfig } from '../types';
import { ATLog } from '../utils/ATLog';

type AutoLoadBridge = typeof ATReactNativeBridge & {
  showAutoLoadInterstitialWithConfig?: (
    placementID: string,
    showConfig: Object
  ) => Promise<void>;
};

/**
 * 插屏 AutoLoad（静态入口；复用同一事件通道，callbackName 与手动 `ATInterstitialAd` 完全一致）。
 */
export class ATInterstitialAutoAd {
  static setAdListener(
    placementID: string,
    listener: ATInterstitialListener
  ): void {
    ATAdEvents.setAdListener(placementID, (p) =>
      dispatchInterstitialEvent(listener, p)
    );
  }

  static removeAdListener(placementID: string): void {
    ATAdEvents.removeAdListener(placementID);
  }

  static addPlacement(
    placementID: string,
    settings: Record<string, unknown> = {}
  ): void {
    ATReactNativeBridge.addAutoLoadInterstitial(placementID, settings);
  }

  static removePlacement(placementID: string): void {
    ATReactNativeBridge.removeAutoLoadInterstitial(placementID);
  }

  static isAdReady(placementID: string): boolean {
    return ATReactNativeBridge.autoLoadInterstitialReady(placementID);
  }

  /**
   * 展示 AutoLoad 插屏。
   * - 无参 / string：走 scenario 参数（showAutoLoadInterstitial）
   * - ATShowConfig 对象：走 showConfig（showAutoLoadInterstitialWithConfig）
   */
  static show(
    placementID: string,
    scenarioOrConfig?: string | ATShowConfig
  ): Promise<void> {
    if (typeof scenarioOrConfig === 'string') {
      return ATReactNativeBridge.showAutoLoadInterstitial(
        placementID,
        scenarioOrConfig
      );
    }
    if (scenarioOrConfig) {
      const bridge = ATReactNativeBridge as AutoLoadBridge;
      const showWithConfig = bridge.showAutoLoadInterstitialWithConfig;
      if (typeof showWithConfig === 'function') {
        return showWithConfig.call(
          bridge,
          placementID,
          scenarioOrConfig as unknown as Object
        );
      }
      ATLog.call(
        'ATInterstitialAutoAd',
        'show(showConfig fallback)',
        'showAutoLoadInterstitialWithConfig unavailable; using JSON scenario'
      );
      return ATReactNativeBridge.showAutoLoadInterstitial(
        placementID,
        JSON.stringify(scenarioOrConfig)
      );
    }
    return ATReactNativeBridge.showAutoLoadInterstitial(placementID, '');
  }
}
