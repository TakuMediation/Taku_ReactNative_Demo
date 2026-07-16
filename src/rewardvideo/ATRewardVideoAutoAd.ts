import ATReactNativeBridge from '../specs/NativeATBridge';
import { ATAdEvents } from '../events/ATAdEvents';
import { dispatchRewardEvent } from './ATRewardVideoAd';
import type { ATRewardVideoListener } from './ATRewardVideoAd';
import type { ATShowConfig } from '../types';
import { ATLog } from '../utils/ATLog';

type AutoLoadBridge = typeof ATReactNativeBridge & {
  showAutoLoadRewardedVideoWithConfig?: (
    placementID: string,
    showConfig: Object
  ) => Promise<void>;
};

/**
 * 激励视频 AutoLoad（静态入口；复用同一事件通道，callbackName 与手动 `ATRewardVideoAd` 完全一致）。
 */
export class ATRewardVideoAutoAd {
  static setAdListener(
    placementID: string,
    listener: ATRewardVideoListener
  ): void {
    ATAdEvents.setAdListener(placementID, (p) =>
      dispatchRewardEvent(listener, p)
    );
  }

  static removeAdListener(placementID: string): void {
    ATAdEvents.removeAdListener(placementID);
  }

  static addPlacement(
    placementID: string,
    settings: Record<string, unknown> = {}
  ): void {
    ATReactNativeBridge.addAutoLoadRewardedVideo(placementID, settings);
  }

  static removePlacement(placementID: string): void {
    ATReactNativeBridge.removeAutoLoadRewardedVideo(placementID);
  }

  static isAdReady(placementID: string): boolean {
    return ATReactNativeBridge.autoLoadRewardedVideoReady(placementID);
  }

  /**
   * 展示 AutoLoad 激励。
   * - 无参 / string：走 scenario 参数（showAutoLoadRewardedVideo）
   * - ATShowConfig 对象：走 showConfig（showAutoLoadRewardedVideoWithConfig）
   */
  static show(
    placementID: string,
    scenarioOrConfig?: string | ATShowConfig
  ): Promise<void> {
    if (typeof scenarioOrConfig === 'string') {
      return ATReactNativeBridge.showAutoLoadRewardedVideo(
        placementID,
        scenarioOrConfig
      );
    }
    if (scenarioOrConfig) {
      const bridge = ATReactNativeBridge as AutoLoadBridge;
      const showWithConfig = bridge.showAutoLoadRewardedVideoWithConfig;
      if (typeof showWithConfig === 'function') {
        return showWithConfig.call(
          bridge,
          placementID,
          scenarioOrConfig as unknown as Object
        );
      }
      // 旧版原生未编进 showAutoLoadRewardedVideoWithConfig 时，经 scenario JSON 透传 showConfig。
      ATLog.call(
        'ATRewardVideoAutoAd',
        'show(showConfig fallback)',
        'showAutoLoadRewardedVideoWithConfig unavailable; using JSON scenario'
      );
      return ATReactNativeBridge.showAutoLoadRewardedVideo(
        placementID,
        JSON.stringify(scenarioOrConfig)
      );
    }
    return ATReactNativeBridge.showAutoLoadRewardedVideo(placementID, '');
  }
}
