package com.anythink.reactnative.bridge;

import com.anythink.reactnative.NativeATBridgeSpec;
import com.anythink.reactnative.event.ATReactNativeEventEmitter;
import com.anythink.reactnative.init.ATInitManager;
import com.anythink.reactnative.reward.ATRewardVideoManager;
import com.anythink.reactnative.interstitial.ATInterstitialManager;
import com.anythink.reactnative.splash.ATSplashManager;
import com.anythink.reactnative.banner.ATBannerManager;
import com.anythink.reactnative.nativead.ATNativeManager;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

/**
 * TopOn/AnyThink RN 桥的 TurboModule 入口。
 *
 * 仅做转发：把 Codegen `NativeATBridgeSpec` 的方法路由到对应 *Manager。
 * 不写业务逻辑。回调统一经 {@link ATReactNativeEventEmitter}。
 */
public class ATReactNativeBridgeModule extends NativeATBridgeSpec {

  public static final String NAME = NativeATBridgeSpec.NAME;

  public ATReactNativeBridgeModule(ReactApplicationContext reactContext) {
    super(reactContext);
    // 注入 context：Manager 取 Context/Activity，EventEmitter 取 RCTDeviceEventEmitter
    ATInitManager.getInstance().setReactContext(reactContext);
    ATReactNativeEventEmitter.getInstance().setReactContext(reactContext);
    ATRewardVideoManager.getInstance().setReactContext(reactContext);
    ATInterstitialManager.getInstance().setReactContext(reactContext);
    ATSplashManager.getInstance().setReactContext(reactContext);
    ATBannerManager.getInstance().setReactContext(reactContext);
    ATNativeManager.getInstance().setReactContext(reactContext);
  }

  // —— 初始化 / 版本 ——

  @Override
  public void initAnyThinkSDK(String appId, String appKey) {
    ATInitManager.getInstance().initAnyThinkSDK(appId, appKey);
  }

  @Override
  public void start() {
    ATInitManager.getInstance().start();
  }

  @Override
  public String getSDKVersionName() {
    return ATInitManager.getInstance().getSDKVersionName();
  }

  // —— 日志 / 渠道 / 自定义参数 ——

  @Override
  public void setLogEnabled(boolean enabled) {
    ATInitManager.getInstance().setLogEnabled(enabled);
  }

  @Override
  public void setChannelStr(String channel) {
    ATInitManager.getInstance().setChannelStr(channel);
  }

  @Override
  public void setCustomDataDic(ReadableMap customMap) {
    ATInitManager.getInstance().setCustomDataDic(customMap);
  }

  @Override
  public void setPlacementCustomData(String placementId, ReadableMap customMap) {
    ATInitManager.getInstance().setPlacementCustomData(placementId, customMap);
  }

  // —— GDPR / 隐私 ——

  @Override
  public void getGDPRLevel(Promise promise) {
    ATInitManager.getInstance().getGDPRLevel(promise);
  }

  @Override
  public void setDataConsentSet(double level) {
    ATInitManager.getInstance().setDataConsentSet((int) level);
  }

  @Override
  public void showGDPRConsentDialog(String appId) {
    ATInitManager.getInstance().showGDPRConsentDialog(appId);
  }

  // —— 过滤 / 策略 / 调试 ——

  @Override
  public void putFilter(String placementId, ReadableMap filterSpec) {
    ATInitManager.getInstance().putFilter(placementId, filterSpec);
  }

  @Override
  public void removeFilters() {
    ATInitManager.getInstance().removeFilters();
  }

  @Override
  public void removeFilterWithPlacementId(String placementId) {
    ATInitManager.getInstance().removeFilterWithPlacementId(placementId);
  }

  @Override
  public void setAdSourcePrivacyPolicy(String policyJson) {
    ATInitManager.getInstance().setAdSourcePrivacyPolicy(policyJson);
  }

  @Override
  public void showDebuggerUI(String debugKey) {
    ATInitManager.getInstance().showDebuggerUI(debugKey);
  }

  // —— ATSDK 补全 ——

  @Override
  public void showGDPRConsentSecondDialog(String appId) {
    ATInitManager.getInstance().showGDPRConsentSecondDialog(appId);
  }

  @Override
  public void showGdprAuth() {
    ATInitManager.getInstance().showGdprAuth();
  }

  @Override
  public void checkIsEuTraffic(Promise promise) {
    ATInitManager.getInstance().checkIsEuTraffic(promise);
  }

  @Override
  public void isEUTraffic(Promise promise) {
    ATInitManager.getInstance().isEUTraffic(promise);
  }

  @Override
  public void deniedUploadDeviceInfo(com.facebook.react.bridge.ReadableArray deviceInfoKeys) {
    ATInitManager.getInstance().deniedUploadDeviceInfo(deviceInfoKeys);
  }

  @Override
  public void setPersonalizedAdStatus(double status) {
    ATInitManager.getInstance().setPersonalizedAdStatus((int) status);
  }

  @Override
  public void setFilterAdSourceIdList(String placementId, com.facebook.react.bridge.ReadableArray list) {
    ATInitManager.getInstance().setFilterAdSourceIdList(placementId, list);
  }

  @Override
  public void setFilterNetworkFirmIdList(String placementId, com.facebook.react.bridge.ReadableArray list) {
    ATInitManager.getInstance().setFilterNetworkFirmIdList(placementId, list);
  }

  @Override
  public void setForbidNetworkFirmIdList(com.facebook.react.bridge.ReadableArray list) {
    ATInitManager.getInstance().setForbidNetworkFirmIdList(list);
  }

  @Override
  public void setForbidShowNetworkFirmIdList(String placementId, com.facebook.react.bridge.ReadableArray list) {
    ATInitManager.getInstance().setForbidShowNetworkFirmIdList(placementId, list);
  }

  @Override
  public void setAllowedShowNetworkFirmIdList(String placementId, com.facebook.react.bridge.ReadableArray list) {
    ATInitManager.getInstance().setAllowedShowNetworkFirmIdList(placementId, list);
  }

  @Override
  public void setRiskFilterNetworkFirmIdList(double risk, com.facebook.react.bridge.ReadableArray list) {
    ATInitManager.getInstance().setRiskFilterNetworkFirmIdList((int) risk, list);
  }

  @Override
  public void getArea(Promise promise) {
    ATInitManager.getInstance().getArea(promise);
  }

  @Override
  public void isCnSDK(Promise promise) {
    ATInitManager.getInstance().isCnSDK(promise);
  }

  @Override
  public void setLocation(ReadableMap location) {
    ATInitManager.getInstance().setLocation(location);
  }

  @Override
  public void setDeviceInformationData(ReadableMap data) {
    ATInitManager.getInstance().setDeviceInformationData(data);
  }

  @Override
  public void integrationChecking() {
    ATInitManager.getInstance().integrationChecking();
  }

  @Override
  public void isNetworkLogDebug(Promise promise) {
    ATInitManager.getInstance().isNetworkLogDebug(promise);
  }

  @Override
  public void setChannelSource(double channelFrom) {
    ATInitManager.getInstance().setChannelSource((int) channelFrom);
  }

  @Override
  public void setLocalStrategyAssetPath(String path) {
    ATInitManager.getInstance().setLocalStrategyAssetPath(path);
  }

  @Override
  public void setSharedPlacementConfig(ReadableMap config) {
    ATInitManager.getInstance().setSharedPlacementConfig(config);
  }

  // —— 激励视频 ——

  @Override
  public void loadRewardedVideo(String placementID, ReadableMap settings) {
    ATRewardVideoManager.getInstance().load(placementID, settings);
  }

  @Override
  public void showRewardedVideo(String placementID, String scenario, Promise promise) {
    ATRewardVideoManager.getInstance().show(placementID, scenario, promise);
  }

  @Override
  public void showRewardedVideoWithConfig(String placementID, ReadableMap showConfig, Promise promise) {
    ATRewardVideoManager.getInstance().showWithConfig(placementID, showConfig, promise);
  }

  @Override
  public void rewardedVideoReady(String placementID, Promise promise) {
    promise.resolve(ATRewardVideoManager.getInstance().isAdReady(placementID));
  }

  @Override
  public void checkRewardedVideoLoadStatus(String placementID, Promise promise) {
    promise.resolve(ATRewardVideoManager.getInstance().checkAdStatus(placementID));
  }

  @Override
  public void getRewardedVideoValidAds(String placementID, Promise promise) {
    ATRewardVideoManager.getInstance().getValidAds(placementID, promise);
  }

  @Override
  public void entryRewardedVideoScenario(String placementID, String scenario, ReadableMap tkExtra) {
    ATRewardVideoManager.getInstance().entryScenario(placementID, scenario, tkExtra);
  }

  @Override
  public void setRewardedVideoLocalExtra(String placementID, ReadableMap map) {
    ATRewardVideoManager.getInstance().setLocalExtra(placementID, map);
  }

  @Override
  public void setRewardedVideoTKExtra(String placementID, ReadableMap map) {
    ATRewardVideoManager.getInstance().setTKExtra(placementID, map);
  }

  @Override
  public void addAutoLoadRewardedVideo(String placementID, ReadableMap settings) {
    ATRewardVideoManager.getInstance().addAutoLoad(placementID, settings);
  }

  @Override
  public void removeAutoLoadRewardedVideo(String placementID) {
    ATRewardVideoManager.getInstance().removeAutoLoad(placementID);
  }

  @Override
  public boolean autoLoadRewardedVideoReady(String placementID) {
    return ATRewardVideoManager.getInstance().autoLoadReady(placementID);
  }

  @Override
  public void showAutoLoadRewardedVideo(String placementID, String scenario, Promise promise) {
    ATRewardVideoManager.getInstance().showAutoLoad(placementID, scenario, promise);
  }

  @Override
  public void showAutoLoadRewardedVideoWithConfig(String placementID, ReadableMap showConfig, Promise promise) {
    ATRewardVideoManager.getInstance().showAutoLoadWithConfig(placementID, showConfig, promise);
  }

  // —— 插屏 ——

  @Override
  public void loadInterstitial(String placementID, ReadableMap settings) {
    ATInterstitialManager.getInstance().load(placementID, settings);
  }

  @Override
  public void showInterstitial(String placementID, String scenario, Promise promise) {
    ATInterstitialManager.getInstance().show(placementID, scenario, promise);
  }

  @Override
  public void showInterstitialWithConfig(String placementID, ReadableMap showConfig, Promise promise) {
    ATInterstitialManager.getInstance().showWithConfig(placementID, showConfig, promise);
  }

  @Override
  public void interstitialReady(String placementID, Promise promise) {
    promise.resolve(ATInterstitialManager.getInstance().isAdReady(placementID));
  }

  @Override
  public void checkInterstitialLoadStatus(String placementID, Promise promise) {
    promise.resolve(ATInterstitialManager.getInstance().checkAdStatus(placementID));
  }

  @Override
  public void getInterstitialValidAds(String placementID, Promise promise) {
    ATInterstitialManager.getInstance().getValidAds(placementID, promise);
  }

  @Override
  public void entryInterstitialScenario(String placementID, String scenario, ReadableMap tkExtra) {
    ATInterstitialManager.getInstance().entryScenario(placementID, scenario, tkExtra);
  }

  @Override
  public void setInterstitialLocalExtra(String placementID, ReadableMap map) {
    ATInterstitialManager.getInstance().setLocalExtra(placementID, map);
  }

  @Override
  public void setInterstitialTKExtra(String placementID, ReadableMap map) {
    ATInterstitialManager.getInstance().setTKExtra(placementID, map);
  }

  @Override
  public void addAutoLoadInterstitial(String placementID, ReadableMap settings) {
    ATInterstitialManager.getInstance().addAutoLoad(placementID, settings);
  }

  @Override
  public void removeAutoLoadInterstitial(String placementID) {
    ATInterstitialManager.getInstance().removeAutoLoad(placementID);
  }

  @Override
  public boolean autoLoadInterstitialReady(String placementID) {
    return ATInterstitialManager.getInstance().autoLoadReady(placementID);
  }

  @Override
  public void showAutoLoadInterstitial(String placementID, String scenario, Promise promise) {
    ATInterstitialManager.getInstance().showAutoLoad(placementID, scenario, promise);
  }

  @Override
  public void showAutoLoadInterstitialWithConfig(String placementID, ReadableMap showConfig, Promise promise) {
    ATInterstitialManager.getInstance().showAutoLoadWithConfig(placementID, showConfig, promise);
  }

  // —— 开屏 ——

  @Override
  public void loadSplash(String placementID, ReadableMap settings) {
    ATSplashManager.getInstance().load(placementID, settings);
  }

  @Override
  public void showSplash(String placementID, String scenario, Promise promise) {
    ATSplashManager.getInstance().show(placementID, scenario, promise);
  }

  @Override
  public void showSplashWithConfig(String placementID, ReadableMap showConfig, Promise promise) {
    ATSplashManager.getInstance().showWithConfig(placementID, showConfig, promise);
  }

  @Override
  public void splashReady(String placementID, Promise promise) {
    promise.resolve(ATSplashManager.getInstance().isAdReady(placementID));
  }

  @Override
  public void checkSplashLoadStatus(String placementID, Promise promise) {
    promise.resolve(ATSplashManager.getInstance().checkAdStatus(placementID));
  }

  @Override
  public void getSplashValidAds(String placementID, Promise promise) {
    ATSplashManager.getInstance().getValidAds(placementID, promise);
  }

  @Override
  public void entrySplashScenario(String placementID, String scenario, ReadableMap tkExtra) {
    ATSplashManager.getInstance().entryScenario(placementID, scenario, tkExtra);
  }

  @Override
  public void setSplashLocalExtra(String placementID, ReadableMap map) {
    ATSplashManager.getInstance().setLocalExtra(placementID, map);
  }

  @Override
  public void setSplashTKExtra(String placementID, ReadableMap map) {
    ATSplashManager.getInstance().setTKExtra(placementID, map);
  }

  // —— Banner（命令式部分，视图走 Fabric ATBannerViewManager）——

  @Override
  public void loadBannerAd(String placementID, ReadableMap settings) {
    ATBannerManager.getInstance().load(placementID, settings);
  }

  @Override
  public void checkBannerLoadStatus(String placementID, Promise promise) {
    promise.resolve(ATBannerManager.getInstance().checkAdStatus(placementID));
  }

  @Override
  public void getBannerValidAds(String placementID, Promise promise) {
    ATBannerManager.getInstance().getValidAds(placementID, promise);
  }

  @Override
  public void entryBannerScenario(String placementID, String scenario, ReadableMap tkExtra) {
    ATBannerManager.getInstance().entryScenario(placementID, scenario, tkExtra);
  }

  @Override
  public void setBannerShowConfig(String placementID, ReadableMap map) {
    ATBannerManager.getInstance().setShowConfig(placementID, map);
  }

  @Override
  public void setBannerLocalExtra(String placementID, ReadableMap map) {
    ATBannerManager.getInstance().setLocalExtra(placementID, map);
  }

  @Override
  public void setBannerTKExtra(String placementID, ReadableMap map) {
    ATBannerManager.getInstance().setTKExtra(placementID, map);
  }

  @Override
  public void destroyBanner(String placementID) {
    ATBannerManager.getInstance().destroy(placementID);
  }

  // —— Native 信息流（两类 ATNative/NativeAd，视图走 Fabric ATNativeAdViewManager）——

  @Override
  public void loadNativeAd(String placementID, ReadableMap settings) {
    ATNativeManager.getInstance().load(placementID, settings);
  }

  @Override
  public void getNativeAd(String placementID, Promise promise) {
    ATNativeManager.getInstance().getNativeAd(placementID, promise);
  }

  @Override
  public void prepareNativeAd(String placementID, ReadableMap prepareInfo) {
    ATNativeManager.getInstance().prepare(placementID, prepareInfo);
  }

  @Override
  public void checkNativeLoadStatus(String placementID, Promise promise) {
    promise.resolve(ATNativeManager.getInstance().checkAdStatus(placementID));
  }

  @Override
  public void getNativeValidAds(String placementID, Promise promise) {
    ATNativeManager.getInstance().getValidAds(placementID, promise);
  }

  @Override
  public void getNativeAdMaterial(String placementID, String adId, Promise promise) {
    ATNativeManager.getInstance().getAdMaterial(placementID, adId, promise);
  }

  @Override
  public void entryNativeScenario(String placementID, String scenario, ReadableMap tkExtra) {
    ATNativeManager.getInstance().entryScenario(placementID, scenario, tkExtra);
  }

  @Override
  public void setNativeShowConfig(String placementID, ReadableMap map) {
    ATNativeManager.getInstance().setShowConfig(placementID, map);
  }

  @Override
  public void setNativeLocalExtra(String placementID, ReadableMap map) {
    ATNativeManager.getInstance().setLocalExtra(placementID, map);
  }

  @Override
  public void setNativeTKExtra(String placementID, ReadableMap map) {
    ATNativeManager.getInstance().setTKExtra(placementID, map);
  }

  @Override
  public void destroyNativeAd(String placementID, String adId) {
    if (adId == null || adId.isEmpty()) {
      ATNativeManager.getInstance().destroyAll(placementID);
    } else {
      ATNativeManager.getInstance().destroy(placementID, adId);
    }
  }

  @Override
  public void nativeAdOnResume(String placementID, String adId) {
    ATNativeManager.getInstance().nativeAdOnResume(placementID, adId);
  }

  @Override
  public void nativeAdOnPause(String placementID, String adId) {
    ATNativeManager.getInstance().nativeAdOnPause(placementID, adId);
  }

  // —— NativeEventEmitter 必需 ——

  @Override
  public void addListener(String eventName) {
    // NativeEventEmitter 要求；事件由 ATReactNativeEventEmitter 主动 emit，无需在此记录
  }

  @Override
  public void removeListeners(double count) {
    // 同上
  }
}
