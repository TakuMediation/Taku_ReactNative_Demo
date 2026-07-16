package com.anythink.reactnative.bridge;


import com.anythink.reactnative.event.ATReactNativeEventEmitter;
import com.anythink.reactnative.init.ATInitManager;
import com.anythink.reactnative.reward.ATRewardVideoManager;
import com.anythink.reactnative.interstitial.ATInterstitialManager;
import com.anythink.reactnative.splash.ATSplashManager;
import com.anythink.reactnative.banner.ATBannerManager;
import com.anythink.reactnative.nativead.ATNativeManager;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;

/**
 * 旧架构（Paper）桥入口 —— 与 newarch 的 {@link NativeATBridgeSpec} 壳逐方法等价，
 * 仅暴露方式不同：extends ReactContextBaseJavaModule + @ReactMethod（newarch 是 extends Spec + @Override）。
 * 方法体完全相同（转发同一 *Manager 单例）。AI 逐方法语义翻译，校验靠 scripts/verify-arch-parity.mjs。
 * 同步返回方法标 @ReactMethod(isBlockingSynchronousMethod = true)。
 */
public class ATReactNativeBridgeModule extends ReactContextBaseJavaModule {

  public static final String NAME = "ATReactNativeBridge";

  public ATReactNativeBridgeModule(ReactApplicationContext reactContext) {
    super(reactContext);
    ATInitManager.getInstance().setReactContext(reactContext);
    ATReactNativeEventEmitter.getInstance().setReactContext(reactContext);
    ATRewardVideoManager.getInstance().setReactContext(reactContext);
    ATInterstitialManager.getInstance().setReactContext(reactContext);
    ATSplashManager.getInstance().setReactContext(reactContext);
    ATBannerManager.getInstance().setReactContext(reactContext);
    ATNativeManager.getInstance().setReactContext(reactContext);
  }

  @Override
  public String getName() {
    return NAME;
  }

  // —— 初始化 / 版本 ——

  @ReactMethod
  public void initAnyThinkSDK(String appId, String appKey) {
    ATInitManager.getInstance().initAnyThinkSDK(appId, appKey);
  }

  @ReactMethod
  public void start() {
    ATInitManager.getInstance().start();
  }

  @ReactMethod(isBlockingSynchronousMethod = true)
  public String getSDKVersionName() {
    return ATInitManager.getInstance().getSDKVersionName();
  }

  // —— 日志 / 渠道 / 自定义参数 ——

  @ReactMethod
  public void setLogEnabled(boolean enabled) {
    ATInitManager.getInstance().setLogEnabled(enabled);
  }

  @ReactMethod
  public void setChannelStr(String channel) {
    ATInitManager.getInstance().setChannelStr(channel);
  }

  @ReactMethod
  public void setCustomDataDic(ReadableMap customMap) {
    ATInitManager.getInstance().setCustomDataDic(customMap);
  }

  @ReactMethod
  public void setPlacementCustomData(String placementId, ReadableMap customMap) {
    ATInitManager.getInstance().setPlacementCustomData(placementId, customMap);
  }

  // —— GDPR / 隐私 ——

  @ReactMethod
  public void getGDPRLevel(Promise promise) {
    ATInitManager.getInstance().getGDPRLevel(promise);
  }

  @ReactMethod
  public void setDataConsentSet(double level) {
    ATInitManager.getInstance().setDataConsentSet((int) level);
  }

  @ReactMethod
  public void showGDPRConsentDialog(String appId) {
    ATInitManager.getInstance().showGDPRConsentDialog(appId);
  }

  // —— 过滤 / 策略 / 调试 ——

  @ReactMethod
  public void putFilter(String placementId, ReadableMap filterSpec) {
    ATInitManager.getInstance().putFilter(placementId, filterSpec);
  }

  @ReactMethod
  public void removeFilters() {
    ATInitManager.getInstance().removeFilters();
  }

  @ReactMethod
  public void removeFilterWithPlacementId(String placementId) {
    ATInitManager.getInstance().removeFilterWithPlacementId(placementId);
  }

  @ReactMethod
  public void setAdSourcePrivacyPolicy(String policyJson) {
    ATInitManager.getInstance().setAdSourcePrivacyPolicy(policyJson);
  }

  @ReactMethod
  public void showDebuggerUI(String debugKey) {
    ATInitManager.getInstance().showDebuggerUI(debugKey);
  }

  // —— ATSDK 补全 ——

  @ReactMethod
  public void showGDPRConsentSecondDialog(String appId) {
    ATInitManager.getInstance().showGDPRConsentSecondDialog(appId);
  }

  @ReactMethod
  public void showGdprAuth() {
    ATInitManager.getInstance().showGdprAuth();
  }

  @ReactMethod
  public void checkIsEuTraffic(Promise promise) {
    ATInitManager.getInstance().checkIsEuTraffic(promise);
  }

  @ReactMethod
  public void isEUTraffic(Promise promise) {
    ATInitManager.getInstance().isEUTraffic(promise);
  }

  @ReactMethod
  public void deniedUploadDeviceInfo(ReadableArray deviceInfoKeys) {
    ATInitManager.getInstance().deniedUploadDeviceInfo(deviceInfoKeys);
  }

  @ReactMethod
  public void setPersonalizedAdStatus(double status) {
    ATInitManager.getInstance().setPersonalizedAdStatus((int) status);
  }

  @ReactMethod
  public void setFilterAdSourceIdList(String placementId, ReadableArray list) {
    ATInitManager.getInstance().setFilterAdSourceIdList(placementId, list);
  }

  @ReactMethod
  public void setFilterNetworkFirmIdList(String placementId, ReadableArray list) {
    ATInitManager.getInstance().setFilterNetworkFirmIdList(placementId, list);
  }

  @ReactMethod
  public void setForbidNetworkFirmIdList(ReadableArray list) {
    ATInitManager.getInstance().setForbidNetworkFirmIdList(list);
  }

  @ReactMethod
  public void setForbidShowNetworkFirmIdList(String placementId, ReadableArray list) {
    ATInitManager.getInstance().setForbidShowNetworkFirmIdList(placementId, list);
  }

  @ReactMethod
  public void setAllowedShowNetworkFirmIdList(String placementId, ReadableArray list) {
    ATInitManager.getInstance().setAllowedShowNetworkFirmIdList(placementId, list);
  }

  @ReactMethod
  public void setRiskFilterNetworkFirmIdList(double risk, ReadableArray list) {
    ATInitManager.getInstance().setRiskFilterNetworkFirmIdList((int) risk, list);
  }

  @ReactMethod
  public void getArea(Promise promise) {
    ATInitManager.getInstance().getArea(promise);
  }

  @ReactMethod
  public void isCnSDK(Promise promise) {
    ATInitManager.getInstance().isCnSDK(promise);
  }

  @ReactMethod
  public void setLocation(ReadableMap location) {
    ATInitManager.getInstance().setLocation(location);
  }

  @ReactMethod
  public void setDeviceInformationData(ReadableMap data) {
    ATInitManager.getInstance().setDeviceInformationData(data);
  }

  @ReactMethod
  public void integrationChecking() {
    ATInitManager.getInstance().integrationChecking();
  }

  @ReactMethod
  public void isNetworkLogDebug(Promise promise) {
    ATInitManager.getInstance().isNetworkLogDebug(promise);
  }

  @ReactMethod
  public void setChannelSource(double channelFrom) {
    ATInitManager.getInstance().setChannelSource((int) channelFrom);
  }

  @ReactMethod
  public void setLocalStrategyAssetPath(String path) {
    ATInitManager.getInstance().setLocalStrategyAssetPath(path);
  }

  @ReactMethod
  public void setSharedPlacementConfig(ReadableMap config) {
    ATInitManager.getInstance().setSharedPlacementConfig(config);
  }

  // —— 激励视频 ——

  @ReactMethod
  public void loadRewardedVideo(String placementID, ReadableMap settings) {
    ATRewardVideoManager.getInstance().load(placementID, settings);
  }

  @ReactMethod
  public void showRewardedVideo(String placementID, String scenario, Promise promise) {
    ATRewardVideoManager.getInstance().show(placementID, scenario, promise);
  }

  @ReactMethod
  public void showRewardedVideoWithConfig(String placementID, ReadableMap showConfig, Promise promise) {
    ATRewardVideoManager.getInstance().showWithConfig(placementID, showConfig, promise);
  }

  @ReactMethod
  public void rewardedVideoReady(String placementID, Promise promise) {
    promise.resolve(ATRewardVideoManager.getInstance().isAdReady(placementID));
  }

  @ReactMethod
  public void checkRewardedVideoLoadStatus(String placementID, Promise promise) {
    promise.resolve(ATRewardVideoManager.getInstance().checkAdStatus(placementID));
  }

  @ReactMethod
  public void getRewardedVideoValidAds(String placementID, Promise promise) {
    ATRewardVideoManager.getInstance().getValidAds(placementID, promise);
  }

  @ReactMethod
  public void entryRewardedVideoScenario(String placementID, String scenario, ReadableMap tkExtra) {
    ATRewardVideoManager.getInstance().entryScenario(placementID, scenario, tkExtra);
  }

  @ReactMethod
  public void setRewardedVideoLocalExtra(String placementID, ReadableMap map) {
    ATRewardVideoManager.getInstance().setLocalExtra(placementID, map);
  }

  @ReactMethod
  public void setRewardedVideoTKExtra(String placementID, ReadableMap map) {
    ATRewardVideoManager.getInstance().setTKExtra(placementID, map);
  }

  @ReactMethod
  public void addAutoLoadRewardedVideo(String placementID, ReadableMap settings) {
    ATRewardVideoManager.getInstance().addAutoLoad(placementID, settings);
  }

  @ReactMethod
  public void removeAutoLoadRewardedVideo(String placementID) {
    ATRewardVideoManager.getInstance().removeAutoLoad(placementID);
  }

  @ReactMethod(isBlockingSynchronousMethod = true)
  public boolean autoLoadRewardedVideoReady(String placementID) {
    return ATRewardVideoManager.getInstance().autoLoadReady(placementID);
  }

  @ReactMethod
  public void showAutoLoadRewardedVideo(String placementID, String scenario, Promise promise) {
    ATRewardVideoManager.getInstance().showAutoLoad(placementID, scenario, promise);
  }

  @ReactMethod
  public void showAutoLoadRewardedVideoWithConfig(String placementID, ReadableMap showConfig, Promise promise) {
    ATRewardVideoManager.getInstance().showAutoLoadWithConfig(placementID, showConfig, promise);
  }

  // —— 插屏 ——

  @ReactMethod
  public void loadInterstitial(String placementID, ReadableMap settings) {
    ATInterstitialManager.getInstance().load(placementID, settings);
  }

  @ReactMethod
  public void showInterstitial(String placementID, String scenario, Promise promise) {
    ATInterstitialManager.getInstance().show(placementID, scenario, promise);
  }

  @ReactMethod
  public void showInterstitialWithConfig(String placementID, ReadableMap showConfig, Promise promise) {
    ATInterstitialManager.getInstance().showWithConfig(placementID, showConfig, promise);
  }

  @ReactMethod
  public void interstitialReady(String placementID, Promise promise) {
    promise.resolve(ATInterstitialManager.getInstance().isAdReady(placementID));
  }

  @ReactMethod
  public void checkInterstitialLoadStatus(String placementID, Promise promise) {
    promise.resolve(ATInterstitialManager.getInstance().checkAdStatus(placementID));
  }

  @ReactMethod
  public void getInterstitialValidAds(String placementID, Promise promise) {
    ATInterstitialManager.getInstance().getValidAds(placementID, promise);
  }

  @ReactMethod
  public void entryInterstitialScenario(String placementID, String scenario, ReadableMap tkExtra) {
    ATInterstitialManager.getInstance().entryScenario(placementID, scenario, tkExtra);
  }

  @ReactMethod
  public void setInterstitialLocalExtra(String placementID, ReadableMap map) {
    ATInterstitialManager.getInstance().setLocalExtra(placementID, map);
  }

  @ReactMethod
  public void setInterstitialTKExtra(String placementID, ReadableMap map) {
    ATInterstitialManager.getInstance().setTKExtra(placementID, map);
  }

  @ReactMethod
  public void addAutoLoadInterstitial(String placementID, ReadableMap settings) {
    ATInterstitialManager.getInstance().addAutoLoad(placementID, settings);
  }

  @ReactMethod
  public void removeAutoLoadInterstitial(String placementID) {
    ATInterstitialManager.getInstance().removeAutoLoad(placementID);
  }

  @ReactMethod(isBlockingSynchronousMethod = true)
  public boolean autoLoadInterstitialReady(String placementID) {
    return ATInterstitialManager.getInstance().autoLoadReady(placementID);
  }

  @ReactMethod
  public void showAutoLoadInterstitial(String placementID, String scenario, Promise promise) {
    ATInterstitialManager.getInstance().showAutoLoad(placementID, scenario, promise);
  }

  @ReactMethod
  public void showAutoLoadInterstitialWithConfig(String placementID, ReadableMap showConfig, Promise promise) {
    ATInterstitialManager.getInstance().showAutoLoadWithConfig(placementID, showConfig, promise);
  }

  // —— 开屏 ——

  @ReactMethod
  public void loadSplash(String placementID, ReadableMap settings) {
    ATSplashManager.getInstance().load(placementID, settings);
  }

  @ReactMethod
  public void showSplash(String placementID, String scenario, Promise promise) {
    ATSplashManager.getInstance().show(placementID, scenario, promise);
  }

  @ReactMethod
  public void showSplashWithConfig(String placementID, ReadableMap showConfig, Promise promise) {
    ATSplashManager.getInstance().showWithConfig(placementID, showConfig, promise);
  }

  @ReactMethod
  public void splashReady(String placementID, Promise promise) {
    promise.resolve(ATSplashManager.getInstance().isAdReady(placementID));
  }

  @ReactMethod
  public void checkSplashLoadStatus(String placementID, Promise promise) {
    promise.resolve(ATSplashManager.getInstance().checkAdStatus(placementID));
  }

  @ReactMethod
  public void getSplashValidAds(String placementID, Promise promise) {
    ATSplashManager.getInstance().getValidAds(placementID, promise);
  }

  @ReactMethod
  public void entrySplashScenario(String placementID, String scenario, ReadableMap tkExtra) {
    ATSplashManager.getInstance().entryScenario(placementID, scenario, tkExtra);
  }

  @ReactMethod
  public void setSplashLocalExtra(String placementID, ReadableMap map) {
    ATSplashManager.getInstance().setLocalExtra(placementID, map);
  }

  @ReactMethod
  public void setSplashTKExtra(String placementID, ReadableMap map) {
    ATSplashManager.getInstance().setTKExtra(placementID, map);
  }

  // —— Banner（命令式部分，视图走 ViewManager）——

  @ReactMethod
  public void loadBannerAd(String placementID, ReadableMap settings) {
    ATBannerManager.getInstance().load(placementID, settings);
  }

  @ReactMethod
  public void checkBannerLoadStatus(String placementID, Promise promise) {
    promise.resolve(ATBannerManager.getInstance().checkAdStatus(placementID));
  }

  @ReactMethod
  public void getBannerValidAds(String placementID, Promise promise) {
    ATBannerManager.getInstance().getValidAds(placementID, promise);
  }

  @ReactMethod
  public void entryBannerScenario(String placementID, String scenario, ReadableMap tkExtra) {
    ATBannerManager.getInstance().entryScenario(placementID, scenario, tkExtra);
  }

  @ReactMethod
  public void setBannerShowConfig(String placementID, ReadableMap map) {
    ATBannerManager.getInstance().setShowConfig(placementID, map);
  }

  @ReactMethod
  public void setBannerLocalExtra(String placementID, ReadableMap map) {
    ATBannerManager.getInstance().setLocalExtra(placementID, map);
  }

  @ReactMethod
  public void setBannerTKExtra(String placementID, ReadableMap map) {
    ATBannerManager.getInstance().setTKExtra(placementID, map);
  }

  @ReactMethod
  public void destroyBanner(String placementID) {
    ATBannerManager.getInstance().destroy(placementID);
  }

  // —— Native 信息流（视图走 ViewManager）——

  @ReactMethod
  public void loadNativeAd(String placementID, ReadableMap settings) {
    ATNativeManager.getInstance().load(placementID, settings);
  }

  @ReactMethod
  public void getNativeAd(String placementID, Promise promise) {
    ATNativeManager.getInstance().getNativeAd(placementID, promise);
  }

  @ReactMethod
  public void prepareNativeAd(String placementID, ReadableMap prepareInfo) {
    ATNativeManager.getInstance().prepare(placementID, prepareInfo);
  }

  @ReactMethod
  public void checkNativeLoadStatus(String placementID, Promise promise) {
    promise.resolve(ATNativeManager.getInstance().checkAdStatus(placementID));
  }

  @ReactMethod
  public void getNativeValidAds(String placementID, Promise promise) {
    ATNativeManager.getInstance().getValidAds(placementID, promise);
  }

  @ReactMethod
  public void getNativeAdMaterial(String placementID, String adId, Promise promise) {
    ATNativeManager.getInstance().getAdMaterial(placementID, adId, promise);
  }

  @ReactMethod
  public void entryNativeScenario(String placementID, String scenario, ReadableMap tkExtra) {
    ATNativeManager.getInstance().entryScenario(placementID, scenario, tkExtra);
  }

  @ReactMethod
  public void setNativeShowConfig(String placementID, ReadableMap map) {
    ATNativeManager.getInstance().setShowConfig(placementID, map);
  }

  @ReactMethod
  public void setNativeLocalExtra(String placementID, ReadableMap map) {
    ATNativeManager.getInstance().setLocalExtra(placementID, map);
  }

  @ReactMethod
  public void setNativeTKExtra(String placementID, ReadableMap map) {
    ATNativeManager.getInstance().setTKExtra(placementID, map);
  }

  @ReactMethod
  public void destroyNativeAd(String placementID, String adId) {
    if (adId == null || adId.isEmpty()) {
      ATNativeManager.getInstance().destroyAll(placementID);
    } else {
      ATNativeManager.getInstance().destroy(placementID, adId);
    }
  }

  @ReactMethod
  public void nativeAdOnResume(String placementID, String adId) {
    ATNativeManager.getInstance().nativeAdOnResume(placementID, adId);
  }

  @ReactMethod
  public void nativeAdOnPause(String placementID, String adId) {
    ATNativeManager.getInstance().nativeAdOnPause(placementID, adId);
  }

  // —— NativeEventEmitter 必需 ——

  @ReactMethod
  public void addListener(String eventName) {
    // NativeEventEmitter 要求；事件由 ATReactNativeEventEmitter 主动 emit，无需在此记录
  }

  @ReactMethod
  public void removeListeners(double count) {
    // 同上
  }
}
