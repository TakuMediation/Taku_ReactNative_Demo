import type { RefObject } from 'react';
import { findNodeHandle } from 'react-native';
import type { View } from 'react-native';
import ATReactNativeBridge from '../specs/NativeATBridge';
import { ATAdEvents } from '../events/ATAdEvents';
import type { ATAdInfo } from '../types';
import type { ATNative, ATNativeEventListener } from './ATNative';
import type { ATNativePrepareInfo, ATAdMaterial } from './types';

/**
 * 已加载的 Native 广告实例（= 安卓 NativeAd 对象，可在列表里多条共存）。
 * `adId` 是跨桥的对象引用（原生生成、用户无感）：素材/绑定/生命周期/销毁均按 adId 定位到具体那条。
 * 用户把本对象传给 `<ATNativeAdView ad={ad}>` 渲染；点击/曝光事件按 placementID#adId 分流。
 */
export class NativeAd {
  constructor(
    readonly placementId: string,
    /** 跨桥对象引用（来自原生 getNativeAd）；用户不读不传。 */
    readonly adId: string,
    private readonly owner: ATNative,
    /** offer 是否为模板(express)：true 用模板布局（SDK 画），false 自渲染（开发者摆 AssetView）。
     * 同一个位会混返两种，必须据此分流——express 进自渲染布局会得到空壳。 */
    readonly isExpress: boolean = false
  ) {}

  prepare(prepareInfo: ATNativePrepareInfo): void {
    ATReactNativeBridge.prepareNativeAd(
      this.placementId,
      prepareInfo as unknown as Object
    );
  }

  /** 用容器 ref 自动填 parent（findNodeHandle→viewTag），再 prepare。 */
  renderAdContainer(
    containerRef: RefObject<View | null>,
    prepareInfo: ATNativePrepareInfo
  ): void {
    const parentTag = containerRef.current
      ? findNodeHandle(containerRef.current)
      : undefined;
    this.prepare({
      ...prepareInfo,
      parent: parentTag ?? prepareInfo.parent,
    });
  }

  setNativeEventListener(listener: ATNativeEventListener): void {
    this.owner._setEventListener(listener);
  }

  setAdRevenueListener(listener: (info: ATAdInfo) => void): void {
    ATAdEvents.setRevenueListener(this.placementId, listener);
  }

  getAdMaterial(): Promise<ATAdMaterial> {
    return ATReactNativeBridge.getNativeAdMaterial(
      this.placementId,
      this.adId
    ) as Promise<ATAdMaterial>;
  }

  getAdInfo(): Promise<ATAdInfo> {
    return ATReactNativeBridge.checkNativeLoadStatus(
      this.placementId
    ) as Promise<ATAdInfo>;
  }

  /** 生命周期：恢复（前台且可见时调；暂停/恢复视频、曝光计时由广告源决定）。 */
  onResume(): void {
    ATReactNativeBridge.nativeAdOnResume(this.placementId, this.adId);
  }

  /** 生命周期：暂停（切后台/滑出可视区时调）。 */
  onPause(): void {
    ATReactNativeBridge.nativeAdOnPause(this.placementId, this.adId);
  }

  /** 销毁本条广告（只销毁这一条，不影响同位其他条/共享的 ATNative 加载器）。 */
  destroy(): void {
    ATReactNativeBridge.destroyNativeAd(this.placementId, this.adId);
    this.owner._releaseAd(this.adId);
  }
}
