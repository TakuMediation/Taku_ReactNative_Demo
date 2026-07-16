import { Platform } from 'react-native';

/**
 * 对拍配置：appId/appKey/placement 
 *
 * iOS 与 Android 的 appId/appKey 不可互通——AnyThink 后台按平台发码。
 * 这里用 Platform.OS 区分，调用方写代码时直接 import sdkConfig，无需关心平台。
 */
const androidConfig = {
  appId: 'a5aa1f9deda26d',
  appKey: '4f7b9ac17decb9babec83aac078742c7',
  placements: {
    // All 激励位
    reward: 'b5b449fb3d89d7',
    // All 插屏位
    interstitial: 'b5baca53984692',
    // All 开屏位
    splash: 'b5bea7cc9a4497',
    // All Banner 位
    banner: 'b5baca4f74c3d8',
    // 自渲染广告位
    nativeSelfRender: 'b64dddc69ccb28',
    // 模版广告位
    nativeExpress: 'b64d9f97899aad',
  } as Record<string, string>,

  /**
   * 按网络区分的 placement（供网络选择下拉框使用）。
   * 现仅 All（缺各网络 placementId）；补 GDT/CSJ/Baidu 等位后在此扩，NetworkPicker 自动多选项。
   */
  networks: {
    reward: { All: 'b5b449fb3d89d7' },
    interstitial: { All: 'b5baca53984692' },
    splash: { All: 'b5bea7cc9a4497' },
    banner: { All: 'b5baca4f74c3d8' },
  } as Record<string, Record<string, string>>,

  rewardScenarioId: 'f5e5492eca9668',
  rewardShowCustomExt: 'RewardedShowCustomExt',
  interstitialScenarioId: 'f5e5492eca9668',
  interstitialShowCustomExt: 'InterstitialShowCustomExt',
  splashScenarioId: 'f5e5492eca9668',
  splashShowCustomExt: 'SplashShowCustomExt',
  bannerScenarioId: 'f5e5492eca9668',
  bannerShowCustomExt: 'BannerShowCustomExt',
  nativeScenarioId: 'f5e5492eca9668',
  nativeShowCustomExt: 'NativeShowCustomExt',
};

const iosConfig = {
  appId: 'a5b0e8491845b3',
  appKey: '7eae0567827cfe2b22874061763f30c9',
  placements: {
    // ADX 激励位（Adx(internal)）
    reward: 'b5b44a0f115321',
    // ADX 插屏位（Adx(internal)）
    interstitial: 'b5bacad8ea3036',
    // ADX 开屏位（Adx(internal)）
    splash: 'b5fa25036683d2',
    // ADX Banner 位（Adx(internal)）
    banner: 'b5bacad0803fd1',
    // Native 自渲染位（Adx(internal)）
    nativeSelfRender: 'b62fc5a95ae717',
    // Native 模板位（Adx-模板）b5bacac780e03b
    nativeExpress: 'b5bacac780e03b',
  } as Record<string, string>,

  networks: {
    reward: { All: 'b5b44a0f115321' },
    interstitial: { All: 'b5bacad8ea3036' },
    splash: { All: 'b5fa25036683d2' },
    banner: { All: 'b5bacad0803fd1' },
  } as Record<string, Record<string, string>>,

  rewardScenarioId: 'f5e54970dc84e6',
  rewardShowCustomExt: 'RewardedShowCustomExt',
  interstitialScenarioId: 'f5e54970dc84e6',
  interstitialShowCustomExt: 'InterstitialShowCustomExt',
  splashScenarioId: 'f5e54970dc84e6',
  splashShowCustomExt: 'SplashShowCustomExt',
  bannerScenarioId: 'f5e54970dc84e6',
  bannerShowCustomExt: 'BannerShowCustomExt',
  nativeScenarioId: 'f5e54970dc84e6',
  nativeShowCustomExt: 'NativeShowCustomExt',
};

export const sdkConfig = Platform.OS === 'ios' ? iosConfig : androidConfig;
