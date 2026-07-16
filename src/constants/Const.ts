/**
 * RN 侧常量字典（与原生常量保持一致）。
 *
 * 本文件只包含 **RN 实际使用的子集**，可随 API 实际使用情况增补/调整；
 * 但凡与原生 **同名**的 callName / callbackName / key，字符串值必须一致，防拼写漂移。
 */

/** 8 个固定 callName（各广告类型的回调通道名）。 */
export const CallName = {
  InitCallName: 'InitCallName',
  RewardedVideoCall: 'RewardedVideoCall',
  InterstitialCall: 'InterstitialCall',
  BannerCall: 'BannerCall',
  NativeCall: 'NativeCall',
  SplashCall: 'SplashCall',
  DownloadCall: 'DownloadCall',
  CommonADCall: 'CommonADCall',
} as const;

/** 统一 payload 根字段（对应 Const.java Const.CallbackKey）。 */
export const CallbackKey = {
  callbackName: 'callbackName',
  placementID: 'placementID',
  extraDic: 'extraDic',
  requestMessage: 'requestMessage',
  isDeeplinkSuccess: 'isDeeplinkSuccess',
  isTimeout: 'isTimeout',
  adId: 'adId',
} as const;

/** Init 通道 callbackName（对应 Const.java InitCallback）。 */
export const InitCallback = {
  location: 'location',
  consentSet: 'consentSet',
  consentDismiss: 'consentDismiss',
  // RN 专用：init 完成回调（便于 JS 侧感知初始化结果）
  sdkInitSuccess: 'sdkInitSuccess',
  sdkInitFail: 'sdkInitFail',
} as const;

/** 激励视频 callbackName（对应 Const.java RewardVideoCallback；值必须一致）。 */
export const RewardVideoCallback = {
  loaded: 'rewardedVideoDidFinishLoading',
  loadFail: 'rewardedVideoDidFailToLoad',
  playStart: 'rewardedVideoDidStartPlaying',
  playEnd: 'rewardedVideoDidEndPlaying',
  playFail: 'rewardedVideoDidFailToPlay',
  close: 'rewardedVideoDidClose',
  click: 'rewardedVideoDidClick',
  reward: 'rewardedVideoDidRewardSuccess',
  deeplink: 'rewardedVideoDidDeepLink',
  againPlayStart: 'rewardedVideoDidAgainStartPlaying',
  againPlayEnd: 'rewardedVideoDidAgainEndPlaying',
  againPlayFail: 'rewardedVideoDidAgainFailToPlay',
  againClick: 'rewardedVideoDidAgainClick',
  againReward: 'rewardedVideoDidAgainRewardSuccess',
  rewardFailed: 'rewardedVideoDidRewardFailed',
  againRewardFailed: 'rewardedVideoDidAgainRewardFailed',
  downloadConfirm: 'rewardedVideoDidDownloadConfirm',
  multipleLoaded: 'rewardedVideoDidMultipleLoaded',
  adSourceBiddingAttempt: 'rewardedVideoDidAdSourceBiddingAttempt',
  adSourceBiddingFilled: 'rewardedVideoDidAdSourceBiddingFilled',
  adSourceBiddingFail: 'rewardedVideoDidAdSourceBiddingFail',
  adSourceAttempt: 'rewardedVideoDidAdSourceAttempt',
  adSourceLoadFilled: 'rewardedVideoDidAdSourceLoadFilled',
  adSourceLoadFail: 'rewardedVideoDidAdSourceLoadFail',
} as const;

/** 插屏 callbackName（对应 Const.java InterstitialCallback；值必须一致）。无 reward/again，多 show/showFail。 */
export const InterstitialCallback = {
  loaded: 'interstitialAdDidFinishLoading',
  loadFail: 'interstitialAdFailToLoadAD',
  playStart: 'interstitialAdDidStartPlaying',
  playEnd: 'interstitialAdDidEndPlaying',
  playFail: 'interstitialDidFailToPlayVideo',
  close: 'interstitialAdDidClose',
  click: 'interstitialAdDidClick',
  show: 'interstitialDidShowSucceed',
  showFail: 'interstitialFailedToShow',
  deeplink: 'interstitialAdDidDeepLink',
  downloadConfirm: 'interstitialAdDidDownloadConfirm',
  multipleLoaded: 'interstitialAdDidMultipleLoaded',
  adSourceBiddingAttempt: 'interstitialAdDidAdSourceBiddingAttempt',
  adSourceBiddingFilled: 'interstitialAdDidAdSourceBiddingFilled',
  adSourceBiddingFail: 'interstitialAdDidAdSourceBiddingFail',
  adSourceAttempt: 'interstitialAdDidAdSourceAttempt',
  adSourceLoadFilled: 'interstitialAdDidAdSourceLoadFilled',
  adSourceLoadFail: 'interstitialAdDidAdSourceLoadFail',
} as const;

/** 开屏 callbackName（对应 Const.java SplashCallback；值必须一致）。特有 timeout/showFailed，无 video/reward/again。 */
export const SplashCallback = {
  loaded: 'splashDidFinishLoading',
  loadFail: 'splashDidFailToLoad',
  timeout: 'splashDidTimeout',
  show: 'splashDidShowSuccess',
  showFailed: 'splashDidShowFailed',
  close: 'splashDidClose',
  click: 'splashDidClick',
  deeplink: 'splashDidDeepLink',
  downloadConfirm: 'splashDidDownloadConfirm',
  multipleLoaded: 'splashDidMultipleLoaded',
  adSourceBiddingAttempt: 'splashDidAdSourceBiddingAttempt',
  adSourceBiddingFilled: 'splashDidAdSourceBiddingFilled',
  adSourceBiddingFail: 'splashDidAdSourceBiddingFail',
  adSourceAttempt: 'splashDidAdSourceAttempt',
  adSourceLoadFilled: 'splashDidAdSourceLoadFilled',
  adSourceLoadFail: 'splashDidAdSourceLoadFail',
} as const;

/** Banner callbackName（对应 Const.java BannerCallback；值必须一致）。特有 close=TapClose、refresh/refreshFail=AutoRefresh。 */
export const BannerCallback = {
  loaded: 'bannerAdDidFinishLoading',
  loadFail: 'bannerAdFailToLoadAD',
  close: 'bannerAdTapCloseButton',
  click: 'bannerAdDidClick',
  show: 'bannerAdDidShowSucceed',
  refresh: 'bannerAdAutoRefreshSucceed',
  refreshFail: 'bannerAdAutoRefreshFail',
  deeplink: 'bannerAdDidDeepLink',
  downloadConfirm: 'bannerAdDidDownloadConfirm',
  multipleLoaded: 'bannerAdDidMultipleLoaded',
  adSourceBiddingAttempt: 'bannerAdDidAdSourceBiddingAttempt',
  adSourceBiddingFilled: 'bannerAdDidAdSourceBiddingFilled',
  adSourceBiddingFail: 'bannerAdDidAdSourceBiddingFail',
  adSourceAttempt: 'bannerAdDidAdSourceAttempt',
  adSourceLoadFilled: 'bannerAdDidAdSourceLoadFilled',
  adSourceLoadFail: 'bannerAdDidAdSourceLoadFail',
} as const;

/** Native callbackName（对应 Const.java NativeCallback；值必须一致）。含 video Start/End/Progress + TapClose。 */
export const NativeCallback = {
  loaded: 'nativeAdDidFinishLoading',
  loadFail: 'nativeAdFailToLoadAD',
  close: 'nativeAdDidTapCloseButton',
  click: 'nativeAdDidClick',
  show: 'nativeAdDidShowNativeAd',
  videoStart: 'nativeAdDidStartPlayingVideo',
  videoEnd: 'nativeAdDidEndPlayingVideo',
  videoProgress: 'NativeVideoProgress',
  deeplink: 'nativeAdDidDeepLink',
  downloadConfirm: 'nativeAdDidDownloadConfirm',
  multipleLoaded: 'nativeAdDidMultipleLoaded',
  adSourceBiddingAttempt: 'nativeAdDidAdSourceBiddingAttempt',
  adSourceBiddingFilled: 'nativeAdDidAdSourceBiddingFilled',
  adSourceBiddingFail: 'nativeAdDidAdSourceBiddingFail',
  adSourceAttempt: 'nativeAdDidAdSourceAttempt',
  adSourceLoadFilled: 'nativeAdDidAdSourceLoadFilled',
  adSourceLoadFail: 'nativeAdDidAdSourceLoadFail',
} as const;

/** 通用广告回调（callName: CommonADCall）。 */
export const CommonCallback = {
  adShowRevenue: 'adShowRevenueCallbackKey',
} as const;

/** 下载进度 callbackName（callName: DownloadCall）。 */
export const DownloadCallback = {
  start: 'downloadStart',
  update: 'downloadUpdate',
  pause: 'downloadPause',
  finished: 'downloadFinished',
  failed: 'downloadFailed',
  installed: 'downloadInstalled',
} as const;

/**
 * 桥接错误码（TurboModule reject 用；非广告 load/show 失败——后者只走 Event）。
 * 同步存在于 Android `Const.BridgeError`（见 utils/Const.java）。
 */
export const BridgeError = {
  SDK_NOT_INITIALIZED: 'SDK_NOT_INITIALIZED',
  INVALID_PLACEMENT: 'INVALID_PLACEMENT',
  ACTIVITY_NULL: 'ACTIVITY_NULL',
  INVALID_ARGUMENT: 'INVALID_ARGUMENT',
} as const;

export type CallNameType = (typeof CallName)[keyof typeof CallName];
