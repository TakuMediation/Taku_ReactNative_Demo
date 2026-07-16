import { ATSDK } from './core/ATSDK';

export { ATSDK };
export { ATAdEvents } from './events/ATAdEvents';
export { ATRewardVideoAd } from './rewardvideo/ATRewardVideoAd';
export type { ATRewardVideoListener } from './rewardvideo/ATRewardVideoAd';
export { ATRewardVideoAutoAd } from './rewardvideo/ATRewardVideoAutoAd';
export { ATInterstitialAd } from './interstitial/ATInterstitialAd';
export type { ATInterstitialListener } from './interstitial/ATInterstitialAd';
export { ATInterstitialAutoAd } from './interstitial/ATInterstitialAutoAd';
export { ATSplashAd } from './splash/ATSplashAd';
export type { ATSplashListener, SplashShowConfig } from './splash/ATSplashAd';
export { ATBannerView } from './banner/ATBannerView';
export type { ATBannerListener } from './banner/ATBannerView';
export { ATBannerViewComponent } from './banner/ATBannerViewComponent';
export type { ATBannerViewProps } from './banner/ATBannerViewComponent';
export { ATNative } from './nativead/ATNative';
export type {
  ATNativeNetworkListener,
  ATNativeEventListener,
} from './nativead/ATNative';
export { NativeAd } from './nativead/NativeAd';
export { ATNativeAdView } from './nativead/ATNativeAdView';
export type { ATNativeAdViewProps } from './nativead/ATNativeAdView';
// 自渲染 AssetView 组件：放进 <ATNativeAdView> 内，从 Context 取素材自渲染
export {
  ATNativeTitleView,
  ATNativeDescView,
  ATNativeCtaView,
  ATNativeIconView,
  ATNativeMainImageView,
  ATNativeMediaView,
  ATNativeCloseView,
} from './nativead/ATNativeAssetViews';
export type { ATNativePrepareInfo, ATAdMaterial } from './nativead/types';
export {
  CallName,
  CallbackKey,
  InitCallback,
  RewardVideoCallback,
  InterstitialCallback,
  SplashCallback,
  BannerCallback,
  NativeCallback,
  BridgeError,
} from './constants/Const';
export type {
  ATAdInfo,
  AdError,
  ATAdEventPayload,
  ATAdListener,
} from './types';

/**
 * 获取版本号方法。
 */
export function getSDKVersionName(): string {
  return ATSDK.getSDKVersionName();
}
