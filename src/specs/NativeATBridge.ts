import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

/**
 * TopOn/AnyThink RN 桥 TurboModule 规格（Codegen 输入）。
 *
 * 下行（JS→Native）一律传**结构化对象**：Codegen 把 TS object marshal 为
 * Android `ReadableMap` / iOS `NSDictionary`，不让开发者传 JSON 字符串。
 * 复杂嵌套参数（filterSpec/atAdRequest/sharedPlacementConfig）在桥内
 * `ReadableMap→JSON字符串→BridgeJsonMapUtil.*FromJson`。
 *
 * 桥方法按功能分组：初始化/Core、各广告类型的命令式接口。
 */
export interface Spec extends TurboModule {
  // —— 初始化 / 版本 ——
  initAnyThinkSDK(appId: string, appKey: string): void;
  start(): void;
  getSDKVersionName(): string;

  // —— 日志 / 渠道 / 自定义参数 ——
  setLogEnabled(enabled: boolean): void;
  setChannelStr(channel: string): void;
  setCustomDataDic(customMap: Object): void;
  setPlacementCustomData(placementId: string, customMap: Object): void;

  // —— GDPR / 隐私 ——
  getGDPRLevel(): Promise<number>;
  setDataConsentSet(level: number): void;
  showGDPRConsentDialog(appId: string): void;

  // —— 过滤 / 策略 / 调试 ——
  putFilter(placementId: string, filterSpec: Object): void;
  removeFilters(): void;
  removeFilterWithPlacementId(placementId: string): void;
  setAdSourcePrivacyPolicy(policyJson: string): void;
  showDebuggerUI(debugKey: string): void;

  // —— ATSDK 隐私/合规补全（getPersonalizedAdStatus 原生无对应方法，故无桥方法）——
  showGDPRConsentSecondDialog(appId: string): void;
  showGdprAuth(): void;
  checkIsEuTraffic(): Promise<boolean>;
  isEUTraffic(): Promise<boolean>;
  deniedUploadDeviceInfo(deviceInfoKeys: Array<string>): void;
  setPersonalizedAdStatus(status: number): void;
  setFilterAdSourceIdList(
    placementId: string,
    adSourceIdList: Array<string>
  ): void;
  setFilterNetworkFirmIdList(
    placementId: string,
    networkFirmIdList: Array<string>
  ): void;
  setForbidNetworkFirmIdList(networkFirmIdList: Array<string>): void;
  setForbidShowNetworkFirmIdList(
    placementId: string,
    networkFirmIdList: Array<string>
  ): void;
  setAllowedShowNetworkFirmIdList(
    placementId: string,
    networkFirmIdList: Array<string>
  ): void;
  setRiskFilterNetworkFirmIdList(
    risk: number,
    networkFirmIdList: Array<string>
  ): void;
  getArea(): Promise<string>;
  isCnSDK(): Promise<boolean>;
  setLocation(location: Object): void;
  setDeviceInformationData(data: Object): void;
  integrationChecking(): void;
  isNetworkLogDebug(): Promise<boolean>;
  setChannelSource(channelFrom: number): void;
  setLocalStrategyAssetPath(path: string): void;
  setSharedPlacementConfig(config: Object): void;

  // —— 激励视频 ——
  // load/show 失败一律经 RewardedVideoCall 事件回传，不走 Promise.reject。
  // 未 init / 无 Activity 等"调用前置不满足"才 reject（SDK_NOT_INITIALIZED / ACTIVITY_NULL）。
  loadRewardedVideo(placementID: string, settings: Object): void;
  showRewardedVideo(placementID: string, scenario: string): Promise<void>;
  showRewardedVideoWithConfig(
    placementID: string,
    showConfig: Object
  ): Promise<void>;
  rewardedVideoReady(placementID: string): Promise<boolean>;
  checkRewardedVideoLoadStatus(placementID: string): Promise<Object>;
  getRewardedVideoValidAds(placementID: string): Promise<Object[]>;
  entryRewardedVideoScenario(
    placementID: string,
    scenario: string,
    tkExtra: Object
  ): void;
  setRewardedVideoLocalExtra(placementID: string, map: Object): void;
  setRewardedVideoTKExtra(placementID: string, map: Object): void;
  // Auto 加载（callbackName 与手动一致）
  addAutoLoadRewardedVideo(placementID: string, settings: Object): void;
  removeAutoLoadRewardedVideo(placementID: string): void;
  autoLoadRewardedVideoReady(placementID: string): boolean;
  showAutoLoadRewardedVideo(
    placementID: string,
    scenario: string
  ): Promise<void>;
  showAutoLoadRewardedVideoWithConfig(
    placementID: string,
    showConfig: Object
  ): Promise<void>;

  // —— 插屏 ——
  // load/show 失败一律经 InterstitialCall 事件回传；未 init / 无 Activity 才 reject。
  loadInterstitial(placementID: string, settings: Object): void;
  showInterstitial(placementID: string, scenario: string): Promise<void>;
  showInterstitialWithConfig(
    placementID: string,
    showConfig: Object
  ): Promise<void>;
  interstitialReady(placementID: string): Promise<boolean>;
  checkInterstitialLoadStatus(placementID: string): Promise<Object>;
  getInterstitialValidAds(placementID: string): Promise<Object[]>;
  entryInterstitialScenario(
    placementID: string,
    scenario: string,
    tkExtra: Object
  ): void;
  setInterstitialLocalExtra(placementID: string, map: Object): void;
  setInterstitialTKExtra(placementID: string, map: Object): void;
  addAutoLoadInterstitial(placementID: string, settings: Object): void;
  removeAutoLoadInterstitial(placementID: string): void;
  autoLoadInterstitialReady(placementID: string): boolean;
  showAutoLoadInterstitial(
    placementID: string,
    scenario: string
  ): Promise<void>;
  showAutoLoadInterstitialWithConfig(
    placementID: string,
    showConfig: Object
  ): Promise<void>;

  // —— 开屏 ——
  // load 含 fetchAdTimeout；show 进桥内容器（showConfig 含 heightRatio/heightPx）。
  // load 失败/超时只走 SplashCall 事件；未 init / 无 Activity 才 reject。开屏无 Auto / 无 checkValidAdCaches。
  loadSplash(placementID: string, settings: Object): void;
  showSplash(placementID: string, scenario: string): Promise<void>;
  showSplashWithConfig(placementID: string, showConfig: Object): Promise<void>;
  splashReady(placementID: string): Promise<boolean>;
  checkSplashLoadStatus(placementID: string): Promise<Object>;
  getSplashValidAds(placementID: string): Promise<Object[]>;
  entrySplashScenario(
    placementID: string,
    scenario: string,
    tkExtra: Object
  ): void;
  setSplashLocalExtra(placementID: string, map: Object): void;
  setSplashTKExtra(placementID: string, map: Object): void;

  // —— Banner（命令式部分，视图走 Fabric 组件 ATBannerViewNativeComponent）——
  loadBannerAd(placementID: string, settings: Object): void;
  checkBannerLoadStatus(placementID: string): Promise<Object>;
  getBannerValidAds(placementID: string): Promise<Object[]>;
  entryBannerScenario(
    placementID: string,
    scenario: string,
    tkExtra: Object
  ): void;
  setBannerShowConfig(placementID: string, map: Object): void;
  setBannerLocalExtra(placementID: string, map: Object): void;
  setBannerTKExtra(placementID: string, map: Object): void;
  destroyBanner(placementID: string): void;

  // —— Native 信息流（两类 ATNative/NativeAd，视图走 Fabric ATNativeAdViewNativeComponent）——
  loadNativeAd(placementID: string, settings: Object): void;
  getNativeAd(placementID: string): Promise<Object | null>;
  prepareNativeAd(placementID: string, prepareInfo: Object): void;
  checkNativeLoadStatus(placementID: string): Promise<Object>;
  getNativeValidAds(placementID: string): Promise<Object[]>;
  getNativeAdMaterial(placementID: string, adId: string): Promise<Object>;
  entryNativeScenario(
    placementID: string,
    scenario: string,
    tkExtra: Object
  ): void;
  setNativeShowConfig(placementID: string, map: Object): void;
  setNativeLocalExtra(placementID: string, map: Object): void;
  setNativeTKExtra(placementID: string, map: Object): void;
  destroyNativeAd(placementID: string, adId: string): void;
  nativeAdOnResume(placementID: string, adId: string): void;
  nativeAdOnPause(placementID: string, adId: string): void;

  // —— NativeEventEmitter 必需（不可省）——
  // 8 个 callName 由 JS 侧 Const.ts 提供（与原生常量保持一致），无需原生 getConstants。
  addListener(eventName: string): void;
  removeListeners(count: number): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>('ATReactNativeBridge');
