#import "ATReactNativeBridge.h"
#import "ATConfiguration.h"
#import "ATInitManager.h"
#import "ATRewardVideoManager.h"
#import "ATInterstitialManager.h"
#import "ATSplashAdManager.h"
#import "ATBannerAdManager.h"
#import "ATBannerTool.h"
#import "ATNativeManager.h"
#import "ATPlatformNativeManager.h"
#import "ATNativeTool.h"
#import "ATCommonTool.h"
// RN 0.85：RCTCallableJSModules 类和协议属性都声明在 RCTBridgeModule.h 内；
// 该头通过 RNATBridgeSpec.h 已传递引入，下面只需 RCTEventDispatcher（fallback 用）。
#import <React/RCTBridgeModule.h>
#import <React/RCTEventDispatcher.h>
#ifdef RCT_NEW_ARCH_ENABLED
#import <React/RCTComponentViewProtocol.h>
#import <React/RCTComponentViewFactory.h>
#endif

// Fabric ComponentView 注册要点（RN 0.85，仅新架构）：
#ifdef RCT_NEW_ARCH_ENABLED
//   - RN 0.85 的 RCTFabricComponentsProvider(name) 是个硬编码 map，**只查 RN 内置组件**；
//     不再像旧版那样用 dlsym 找 `<ComponentName>Cls`。
//   - 第三方组件必须显式调用 [RCTComponentViewFactory registerComponentViewClass:] 才能被
//     Fabric runtime 找到，否则 JS 端 <ATBannerView>/<ATNativeADView> 会报 "Unimplemented component"。
//   - 同时仍要解决静态库 dead-strip 问题：在 init 里 (void) 一下两个 Cls() 函数，
//     强制链接器把 ATRNBannerView.o / ATRNNativeAdView.o 保留进 final binary。
extern "C" Class<RCTComponentViewProtocol> ATBannerViewCls(void);
extern "C" Class<RCTComponentViewProtocol> ATNativeADViewCls(void);
#endif

/**
 * RN→iOS 桥入口（双架构：旧架构 RCTBridgeModule + 新架构 TurboModule Spec）。
 *
 * 业务转发：
 * - init 系列          → ATInitManager 类方法
 * - 激励 / 插屏 / 开屏 / Banner / Native → 经 Bridge 内 lazy ivar 持单例 Manager，转发实例方法
 *
 * 事件回到 JS（onAdLoaded / onAdFailed / 等）由各 Delegate 通过
 * `[SendEventManager sendMethod:arguments:result:]`（ATSendSignalManager 垫片）
 * 转发到 [ATReactNativeEventEmitter sharedInstance] → sink → callableJSModules；
 * Bridge 自己不直接发事件，仅在每个 spec 方法第一行打印 `iOS: <method> <args>` 日志。
 */
@interface ATReactNativeBridge () {
    ATRewardVideoManager *_rewardMgr;
    ATInterstitialManager *_interstitialMgr;
    ATSplashAdManager *_splashMgr;
    ATBannerAdManager *_bannerMgr;
    ATNativeManager *_nativeMgr;           // 自渲染（self-render）
    ATPlatformNativeManager *_platformNativeMgr;  // 模板（Express）；按 settings.isNativeShowType=NO 分发
    // loadNativeAd 时记录每个 placement 的广告类型（YES=Express模板，NO=自渲染）。
    // 供 getNativeAdMaterial 查表返回 isExpress，而不需要消费 ATAdManager 的 offer pool。
    NSMutableDictionary<NSString *, NSNumber *> *_nativeExpressMap;
    // adId 自增计数器（主线程访问，无需加锁）
    NSUInteger _nativeAdIdCounter;
}
@end

@implementation ATReactNativeBridge

RCT_EXPORT_MODULE();

// RCTBridgeModule 协议里 bridge / callableJSModules 是 @optional property，
// 必须显式 @synthesize 才能让 RN runtime 注入实例。
@synthesize bridge = _bridge;
@synthesize callableJSModules = _callableJSModules;

#pragma mark - Threading（与 Flutter MethodChannel 主线程行为对齐；md §5.1 / phase-2e Task7）

// AnyThinkSDK 大量 API（GDPR consent dialog、Banner / Native view 操作、UI controller 取 root 等）
// 强制主线程。requiresMainQueueSetup 让 RN 在主线程实例化模块；
// methodQueue 把所有 spec 方法调度切到主队列，避免单方法各自 dispatch_async 切线。
+ (BOOL)requiresMainQueueSetup {
    return YES;
}

- (dispatch_queue_t)methodQueue {
    return dispatch_get_main_queue();
}

#pragma mark - Lifecycle / EventSink 注册

- (instancetype)init {
    if ((self = [super init])) {
        // 把自己注册成 EventEmitter 的下行通道。Delegate 调
        // [SendEventManager sendMethod:arguments:result:]
        //   → ATSendSignalManager 垫片
        //   → [ATReactNativeEventEmitter sharedInstance]
        //   → sink (本实例) → RCTCallableJSModules → JS
        [[ATReactNativeEventEmitter sharedInstance] setSink:self];

        _nativeExpressMap = [NSMutableDictionary dictionary];
        _nativeAdIdCounter = 0;

        // 显式把 Fabric ComponentView 注册到工厂（RN 0.85 不走 dlsym，详见顶部注释）。
        // 这两次 (void)Cls() 同时起到强引用作用，防 ATRNBannerView.o / ATRNNativeAdView.o 被 dead-strip。
#ifdef RCT_NEW_ARCH_ENABLED
        Class bannerCls = ATBannerViewCls();
        Class nativeCls = ATNativeADViewCls();
        RCTComponentViewFactory *factory = [RCTComponentViewFactory currentComponentViewFactory];
        if (bannerCls) {
            [factory registerComponentViewClass:bannerCls];
            ATLog(@"ATReactNativeBridge.init registered Fabric component ATBannerView (%@)", bannerCls);
        }
        if (nativeCls) {
            [factory registerComponentViewClass:nativeCls];
            ATLog(@"ATReactNativeBridge.init registered Fabric component ATNativeADView (%@)", nativeCls);
        }
#endif
    }
    return self;
}

#pragma mark - ATReactNativeEventSink

- (void)emitEventName:(NSString *)name body:(id)body {
    ATLog(@"emitEventName name=%@ body=%@", name, body);
    // 新架构推荐：通过 callableJSModules 调 JS 端 RCTDeviceEventEmitter.emit
    if (_callableJSModules != nil) {
        [_callableJSModules invokeModule:@"RCTDeviceEventEmitter"
                                  method:@"emit"
                                withArgs:@[name, body ?: @{}]];
        return;
    }
    // 降级：旧架构 / bridge 还在的情况
    if (_bridge != nil) {
        [_bridge.eventDispatcher sendDeviceEventWithName:name body:body];
        return;
    }
    ATLog(@"emitEventName no JS transport (callableJSModules & bridge both nil), drop event=%@", name);
}

#pragma mark - Lazy single-instance Managers

- (ATRewardVideoManager *)rewardMgr {
    if (!_rewardMgr) _rewardMgr = [[ATRewardVideoManager alloc] init];
    return _rewardMgr;
}

- (ATInterstitialManager *)interstitialMgr {
    if (!_interstitialMgr) _interstitialMgr = [[ATInterstitialManager alloc] init];
    return _interstitialMgr;
}

- (ATSplashAdManager *)splashMgr {
    if (!_splashMgr) _splashMgr = [[ATSplashAdManager alloc] init];
    return _splashMgr;
}

- (ATBannerAdManager *)bannerMgr {
    if (!_bannerMgr) _bannerMgr = [[ATBannerAdManager alloc] init];
    return _bannerMgr;
}

- (ATNativeManager *)nativeMgr {
    // 用 sharedManager 共享同一实例——它同时承担 SDK 加载 delegate（loadADWithPlacementID:delegate:）
    // 和展示 delegate（ATNativeADConfiguration.delegate）的角色。
    // 不共用同一实例会导致 UIComponent 走 attachIfNeeded 时 config.delegate=nil → 展示/点击回调丢失。
    if (!_nativeMgr) _nativeMgr = [ATNativeManager sharedManager];
    return _nativeMgr;
}

- (ATPlatformNativeManager *)platformNativeMgr {
    if (!_platformNativeMgr) _platformNativeMgr = [[ATPlatformNativeManager alloc] init];
    return _platformNativeMgr;
}

#pragma mark - 初始化 / 版本 / SDK lifecycle

RCT_EXPORT_METHOD(initAnyThinkSDK:(NSString *)appId appKey:(NSString *)appKey)
{
    ATLog(@"initAnyThinkSDK appId=%@", appId);
    [ATInitManager initAnyThinkSDKAppID:appId
                              appKeyStr:appKey
                           requestError:^(NSError * _Nullable error) {
        if (error) {
            ATLog(@"initAnyThinkSDK error=%@", error);
        }
    }];
}

RCT_EXPORT_METHOD(start)
{
    ATLog(@"start (noop, init 已含 startWithAppID)");
}

RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD(getSDKVersionName)
{
    NSString *v = [ATInitManager getSDKVersionName] ?: @"";
    ATLog(@"getSDKVersionName version=%@", v);
    return v;
}

#pragma mark - 日志 / 渠道 / 自定义参数

RCT_EXPORT_METHOD(setLogEnabled:(BOOL)enabled)
{
    ATLog(@"setLogEnabled enabled=%d", enabled);
    [ATInitManager setLogEnabled:enabled];
}

RCT_EXPORT_METHOD(setChannelStr:(NSString *)channel)
{
    ATLog(@"setChannelStr channel=%@", channel);
    [ATInitManager setChannelStr:channel];
}

RCT_EXPORT_METHOD(setCustomDataDic:(NSDictionary *)customMap)
{
    ATLog(@"setCustomDataDic customMap=%@", customMap);
    [ATInitManager setCustomDataDic:customMap];
}

RCT_EXPORT_METHOD(setPlacementCustomData:(NSString *)placementId customMap:(NSDictionary *)customMap)
{
    ATLog(@"setPlacementCustomData placementId=%@ customMap=%@", placementId, customMap);
    [ATInitManager setPlacementCustomData:customMap placementIDStr:placementId];
}

#pragma mark - GDPR / 隐私

RCT_EXPORT_METHOD(getGDPRLevel:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
    ATLog(@"getGDPRLevel");
    NSString *s = [ATInitManager getGDPRLevel];
    // 与 RN 端约定数字映射：0=Unknown, 1=Personalized, 2=Nonpersonalized
    NSNumber *n = [s isEqualToString:@"ATDataConsentSetPersonalized"] ? @1
                : [s isEqualToString:@"ATDataConsentSetNonpersonalized"] ? @2
                : @0;
    resolve(n);
}

RCT_EXPORT_METHOD(setDataConsentSet:(double)level)
{
    ATLog(@"setDataConsentSet level=%f", level);
    NSString *s = nil;
    if (level == 1) {
        s = @"ATDataConsentSetPersonalized";
    } else if (level == 2) {
        s = @"ATDataConsentSetNonpersonalized";
    } else {
        s = @"ATDataConsentSetUnknown";
    }
    [ATInitManager setDataConsentSet:s];
}

RCT_EXPORT_METHOD(showGDPRConsentDialog:(NSString *)appId)
{
    ATLog(@"showGDPRConsentDialog appId=%@", appId);
    [ATInitManager showGDPRConsentDialogWithAppId:appId];
}

#pragma mark - 过滤 / 策略 / 调试

RCT_EXPORT_METHOD(putFilter:(NSString *)placementId filterSpec:(NSDictionary *)filterSpec)
{
    ATLog(@"putFilter placementId=%@ filterSpec=%@", placementId, filterSpec);
    NSString *json = nil;
    if ([filterSpec isKindOfClass:[NSDictionary class]] && filterSpec.count > 0) {
        NSError *e = nil;
        NSData *data = [NSJSONSerialization dataWithJSONObject:filterSpec options:0 error:&e];
        if (!e && data) {
            json = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
        }
    }
    [ATInitManager putFilter:placementId filterJson:json];
}

RCT_EXPORT_METHOD(removeFilters)
{
    ATLog(@"removeFilters");
    [ATInitManager removeFilters];
}

RCT_EXPORT_METHOD(removeFilterWithPlacementId:(NSString *)placementId)
{
    ATLog(@"removeFilterWithPlacementId placementId=%@", placementId);
    [ATInitManager removeFilterWithPlacementId:placementId];
}

RCT_EXPORT_METHOD(setAdSourcePrivacyPolicy:(NSString *)policyJson)
{
    ATLog(@"setAdSourcePrivacyPolicy policyJson=%@", policyJson);
    [ATInitManager setAdSourcePrivacyPolicy:policyJson];
}

RCT_EXPORT_METHOD(showDebuggerUI:(NSString *)debugKey)
{
    ATLog(@"showDebuggerUI");
    [ATInitManager showDebuggerUI:debugKey];
}

#pragma mark - GDPR 扩展 / SDK 隐私策略

RCT_EXPORT_METHOD(showGDPRConsentSecondDialog:(NSString *)appId)
{
    ATLog(@"showGDPRConsentSecondDialog appId=%@", appId);
    [ATInitManager showGDPRConsentSecondDialogWithAppId:appId];
}

RCT_EXPORT_METHOD(showGdprAuth)
{
    ATLog(@"showGdprAuth");
    [ATInitManager showGDPRAuth];
}

RCT_EXPORT_METHOD(checkIsEuTraffic:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
    ATLog(@"checkIsEuTraffic");
    [ATInitManager checkIsEuTrafficWithAppId:nil completion:^(BOOL isEuTraffic) {
        resolve(@(isEuTraffic));
        ATLog(@"checkIsEuTraffic: %d", isEuTraffic);
    }];
}

RCT_EXPORT_METHOD(isEUTraffic:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
    ATLog(@"isEUTraffic");
    resolve(@([ATInitManager isEUTraffic]));
}

RCT_EXPORT_METHOD(deniedUploadDeviceInfo:(NSArray *)deviceInfoKeys)
{
    ATLog(@"deniedUploadDeviceInfo deviceInfoKeys=%@", deviceInfoKeys);
    [ATInitManager setDeniedUploadInfoArray:deviceInfoKeys ?: @[]];
}

RCT_EXPORT_METHOD(setPersonalizedAdStatus:(double)status)
{
    ATLog(@"setPersonalizedAdStatus status=%f", status);
    [ATInitManager setPersonalizedAdStatus:(NSInteger)status];
}

RCT_EXPORT_METHOD(setFilterAdSourceIdList:(NSString *)placementId adSourceIdList:(NSArray *)list)
{
    ATLog(@"setFilterAdSourceIdList placementId=%@ adSourceIdList=%@", placementId, list);
    [ATInitManager setFilterAdSourceIdList:placementId list:list ?: @[]];
}

RCT_EXPORT_METHOD(setFilterNetworkFirmIdList:(NSString *)placementId networkFirmIdList:(NSArray *)list)
{
    ATLog(@"setFilterNetworkFirmIdList placementId=%@ networkFirmIdList=%@", placementId, list);
    [ATInitManager setFilterNetworkFirmIdList:placementId list:list ?: @[]];
}

RCT_EXPORT_METHOD(setForbidNetworkFirmIdList:(NSArray *)list)
{
    ATLog(@"setForbidNetworkFirmIdList networkFirmIdList=%@", list);
    [ATInitManager setForbidNetworkFirmIdList:list ?: @[]];
}

RCT_EXPORT_METHOD(setForbidShowNetworkFirmIdList:(NSString *)placementId networkFirmIdList:(NSArray *)list)
{
    ATLog(@"setForbidShowNetworkFirmIdList placementId=%@ networkFirmIdList=%@", placementId, list);
    [ATInitManager setForbidShowNetworkFirmIdList:placementId list:list ?: @[]];
}

RCT_EXPORT_METHOD(setAllowedShowNetworkFirmIdList:(NSString *)placementId networkFirmIdList:(NSArray *)list)
{
    ATLog(@"setAllowedShowNetworkFirmIdList placementId=%@ networkFirmIdList=%@", placementId, list);
    [ATInitManager setAllowedShowNetworkFirmIdList:placementId list:list ?: @[]];
}

RCT_EXPORT_METHOD(setRiskFilterNetworkFirmIdList:(double)risk networkFirmIdList:(NSArray *)list)
{
    ATLog(@"setRiskFilterNetworkFirmIdList risk=%f networkFirmIdList=%@", risk, list);
    [ATInitManager setRiskFilterNetworkFirmIdList:(NSInteger)risk list:list ?: @[]];
}

RCT_EXPORT_METHOD(getArea:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
    ATLog(@"getArea");
    [ATInitManager getAreaWithCompletion:^(NSString *areaCode) {
        resolve(areaCode ?: @"");
    }];
}

RCT_EXPORT_METHOD(isCnSDK:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
    ATLog(@"isCnSDK");
    resolve(@([ATInitManager isCnSDK]));
}

RCT_EXPORT_METHOD(setLocation:(NSDictionary *)location)
{
    ATLog(@"setLocation location=%@", location);
    [ATInitManager setLocationWithDictionary:location ?: @{}];
}

RCT_EXPORT_METHOD(setDeviceInformationData:(NSDictionary *)data)
{
    ATLog(@"setDeviceInformationData data=%@", data);
    [ATInitManager setDeviceInformationData:data ?: @{}];
}

RCT_EXPORT_METHOD(integrationChecking)
{
    ATLog(@"integrationChecking");
    [ATInitManager integrationChecking];
}

RCT_EXPORT_METHOD(isNetworkLogDebug:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
    ATLog(@"isNetworkLogDebug");
    resolve(@([ATInitManager isNetworkLogDebug]));
}

RCT_EXPORT_METHOD(setChannelSource:(double)channelFrom)
{
    ATLog(@"setChannelSource channelFrom=%f", channelFrom);
    [ATInitManager setChannelSource:(NSInteger)channelFrom];
}

RCT_EXPORT_METHOD(setLocalStrategyAssetPath:(NSString *)path)
{
    ATLog(@"setLocalStrategyAssetPath path=%@", path);
    [ATInitManager setPresetPlacementConfigPath:path];
}

RCT_EXPORT_METHOD(setSharedPlacementConfig:(NSDictionary *)config)
{
    ATLog(@"setSharedPlacementConfig config=%@", config);
    [ATInitManager setSharedPlacementConfig:config ?: @{}];
}

#pragma mark - 激励视频
// 注意移植坑：播放失败回调用 rewardedVideoDidFailToPlay（在 ATRewardVideoDelegate 内），勿照抄 FailToLoad。

RCT_EXPORT_METHOD(loadRewardedVideo:(NSString *)placementID settings:(NSDictionary *)settings)
{
    ATLog(@"loadRewardedVideo placementID=%@ settings=%@", placementID, settings);
    [self.rewardMgr loadRewardedVideo:placementID extraDic:settings ?: @{}];
}

RCT_EXPORT_METHOD(showRewardedVideo:(NSString *)placementID
                 scenario:(NSString *)scenario
                  resolve:(RCTPromiseResolveBlock)resolve
                   reject:(RCTPromiseRejectBlock)reject)
{
    ATLog(@"showRewardedVideo placementID=%@ scenario=%@", placementID, scenario);
    if (scenario.length > 0) {
        [self.rewardMgr showRewardedVideo:placementID sceneID:scenario];
    } else {
        [self.rewardMgr showRewardedVideo:placementID];
    }
    resolve(nil);
}

RCT_EXPORT_METHOD(showRewardedVideoWithConfig:(NSString *)placementID
                         showConfig:(NSDictionary *)showConfig
                            resolve:(RCTPromiseResolveBlock)resolve
                             reject:(RCTPromiseRejectBlock)reject)
{
    ATLog(@"showRewardedVideoWithConfig placementID=%@ showConfig=%@", placementID, showConfig);
    NSString *scene = showConfig[@"Scenario"] ?: showConfig[@"scenarioId"] ?: @"";
    NSString *ext = showConfig[@"showCustomExt"] ?: showConfig[@"customExt"] ?: @"";
    [self.rewardMgr showRewardedVideoWithShowConfig:placementID sceneID:scene showCustomExt:ext];
    resolve(nil);
}

RCT_EXPORT_METHOD(rewardedVideoReady:(NSString *)placementID
                   resolve:(RCTPromiseResolveBlock)resolve
                    reject:(RCTPromiseRejectBlock)reject)
{
    ATLog(@"rewardedVideoReady placementID=%@", placementID);
    resolve(@([self.rewardMgr rewardedVideoReady:placementID]));
}

RCT_EXPORT_METHOD(checkRewardedVideoLoadStatus:(NSString *)placementID
                             resolve:(RCTPromiseResolveBlock)resolve
                              reject:(RCTPromiseRejectBlock)reject)
{
    ATLog(@"checkRewardedVideoLoadStatus placementID=%@", placementID);
    NSDictionary *status = [self.rewardMgr checkRewardedVideoLoadStatus:placementID];
    resolve(status ?: @{@"isLoading": @(NO), @"isReady": @(NO)});
}

RCT_EXPORT_METHOD(getRewardedVideoValidAds:(NSString *)placementID
                         resolve:(RCTPromiseResolveBlock)resolve
                          reject:(RCTPromiseRejectBlock)reject)
{
    ATLog(@"getRewardedVideoValidAds placementID=%@", placementID);
    NSString *json = [self.rewardMgr getRewardedVideoValidAds:placementID];
    id parsed = nil;
    if (json.length > 0) {
        parsed = [NSJSONSerialization JSONObjectWithData:[json dataUsingEncoding:NSUTF8StringEncoding]
                                                 options:0 error:nil];
    }
    resolve(parsed ?: @[]);
}

RCT_EXPORT_METHOD(entryRewardedVideoScenario:(NSString *)placementID scenario:(NSString *)scenario tkExtra:(NSDictionary *)tkExtra)
{
    ATLog(@"entryRewardedVideoScenario placementID=%@ scenario=%@ tkExtra=%@", placementID, scenario, tkExtra);
    [self.rewardMgr entryScenarioWithPlacementID:placementID sceneID:scenario];
}

RCT_EXPORT_METHOD(setRewardedVideoLocalExtra:(NSString *)placementID map:(NSDictionary *)map)
{
    ATLog(@"setRewardedVideoLocalExtra placementID=%@ map=%@ (待 Manager 补充接口)", placementID, map);
}

RCT_EXPORT_METHOD(setRewardedVideoTKExtra:(NSString *)placementID map:(NSDictionary *)map)
{
    ATLog(@"setRewardedVideoTKExtra placementID=%@ map=%@ (待 Manager 补充接口)", placementID, map);
}

RCT_EXPORT_METHOD(addAutoLoadRewardedVideo:(NSString *)placementID settings:(NSDictionary *)settings)
{
    ATLog(@"addAutoLoadRewardedVideo placementID=%@ settings=%@", placementID, settings);
    [self.rewardMgr autoLoadRewardedVideo:placementID];
    if ([settings isKindOfClass:[NSDictionary class]] && settings.count > 0) {
        [self.rewardMgr autoLoadRewardedVideoSetLocalExtra:placementID extraDic:settings];
    }
}

RCT_EXPORT_METHOD(removeAutoLoadRewardedVideo:(NSString *)placementID)
{
    ATLog(@"removeAutoLoadRewardedVideo placementID=%@", placementID);
    [self.rewardMgr cancelAutoLoadRewardedVideo:placementID];
}

RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD(autoLoadRewardedVideoReady:(NSString *)placementID)
{
    ATLog(@"autoLoadRewardedVideoReady placementID=%@", placementID);
    return @([self.rewardMgr rewardedVideoReady:placementID]);
}

RCT_EXPORT_METHOD(showAutoLoadRewardedVideo:(NSString *)placementID
                         scenario:(NSString *)scenario
                          resolve:(RCTPromiseResolveBlock)resolve
                           reject:(RCTPromiseRejectBlock)reject)
{
    ATLog(@"showAutoLoadRewardedVideo placementID=%@ scenario=%@", placementID, scenario);
    NSDictionary *configDict = [ATCommonTool dictionaryWithJsonString:scenario];
    if ([configDict isKindOfClass:[NSDictionary class]] && configDict.count > 0) {
        NSString *scene = configDict[@"scenarioId"] ?: configDict[@"Scenario"] ?: @"";
        NSString *ext = configDict[@"showCustomExt"] ?: configDict[@"customExt"] ?: @"";
        [self.rewardMgr showAutoLoadRewardedVideoAD:placementID sceneID:scene showCustomExt:ext];
    } else {
        [self.rewardMgr showAutoLoadRewardedVideoAD:placementID sceneID:scenario ?: @""];
    }
    resolve(nil);
}

RCT_EXPORT_METHOD(showAutoLoadRewardedVideoWithConfig:(NSString *)placementID
                         showConfig:(NSDictionary *)showConfig
                            resolve:(RCTPromiseResolveBlock)resolve
                             reject:(RCTPromiseRejectBlock)reject)
{
    ATLog(@"showAutoLoadRewardedVideoWithConfig placementID=%@ showConfig=%@", placementID, showConfig);
    NSString *scene = showConfig[@"Scenario"] ?: showConfig[@"scenarioId"] ?: @"";
    NSString *ext = showConfig[@"showCustomExt"] ?: showConfig[@"customExt"] ?: @"";
    [self.rewardMgr showAutoLoadRewardedVideoAD:placementID sceneID:scene showCustomExt:ext];
    resolve(nil);
}

#pragma mark - 插屏
// 注意：播放失败回调用 interstitialDidFailToPlayVideo（在 ATInterstitialDelegate 内）。

RCT_EXPORT_METHOD(loadInterstitial:(NSString *)placementID settings:(NSDictionary *)settings)
{
    ATLog(@"loadInterstitial placementID=%@ settings=%@", placementID, settings);
    [self.interstitialMgr loadInterstitialAd:placementID extraDic:settings ?: @{}];
}

RCT_EXPORT_METHOD(showInterstitial:(NSString *)placementID
                scenario:(NSString *)scenario
                 resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
    ATLog(@"showInterstitial placementID=%@ scenario=%@", placementID, scenario);
    if (scenario.length > 0) {
        [self.interstitialMgr showInterstitialAd:placementID sceneID:scenario];
    } else {
        [self.interstitialMgr showInterstitialAd:placementID];
    }
    resolve(nil);
}

RCT_EXPORT_METHOD(showInterstitialWithConfig:(NSString *)placementID
                        showConfig:(NSDictionary *)showConfig
                           resolve:(RCTPromiseResolveBlock)resolve
                            reject:(RCTPromiseRejectBlock)reject)
{
    ATLog(@"showInterstitialWithConfig placementID=%@ showConfig=%@", placementID, showConfig);
    NSString *scene = showConfig[@"Scenario"] ?: showConfig[@"scenarioId"] ?: @"";
    NSString *ext = showConfig[@"showCustomExt"] ?: showConfig[@"customExt"] ?: @"";
    [self.interstitialMgr showInterstitialAdWithShowConfig:placementID sceneID:scene showCustomExt:ext];
    resolve(nil);
}

RCT_EXPORT_METHOD(interstitialReady:(NSString *)placementID
                  resolve:(RCTPromiseResolveBlock)resolve
                   reject:(RCTPromiseRejectBlock)reject)
{
    ATLog(@"interstitialReady placementID=%@", placementID);
    resolve(@([self.interstitialMgr hasInterstitialAdReady:placementID]));
}

RCT_EXPORT_METHOD(checkInterstitialLoadStatus:(NSString *)placementID
                            resolve:(RCTPromiseResolveBlock)resolve
                             reject:(RCTPromiseRejectBlock)reject)
{
    ATLog(@"checkInterstitialLoadStatus placementID=%@", placementID);
    NSDictionary *status = [self.interstitialMgr checkInterstitialLoadStatus:placementID];
    resolve(status ?: @{@"isLoading": @(NO), @"isReady": @(NO)});
}

RCT_EXPORT_METHOD(getInterstitialValidAds:(NSString *)placementID
                        resolve:(RCTPromiseResolveBlock)resolve
                         reject:(RCTPromiseRejectBlock)reject)
{
    ATLog(@"getInterstitialValidAds placementID=%@", placementID);
    NSString *json = [self.interstitialMgr getInterstitialValidAds:placementID];
    id parsed = nil;
    if (json.length > 0) {
        parsed = [NSJSONSerialization JSONObjectWithData:[json dataUsingEncoding:NSUTF8StringEncoding]
                                                 options:0 error:nil];
    }
    resolve(parsed ?: @[]);
}

RCT_EXPORT_METHOD(entryInterstitialScenario:(NSString *)placementID scenario:(NSString *)scenario tkExtra:(NSDictionary *)tkExtra)
{
    ATLog(@"entryInterstitialScenario placementID=%@ scenario=%@ tkExtra=%@", placementID, scenario, tkExtra);
    [self.interstitialMgr entryScenarioWithPlacementID:placementID sceneID:scenario];
}

RCT_EXPORT_METHOD(setInterstitialLocalExtra:(NSString *)placementID map:(NSDictionary *)map)
{
    ATLog(@"setInterstitialLocalExtra placementID=%@ map=%@ (待 Manager 补充接口)", placementID, map);
}

RCT_EXPORT_METHOD(setInterstitialTKExtra:(NSString *)placementID map:(NSDictionary *)map)
{
    ATLog(@"setInterstitialTKExtra placementID=%@ map=%@ (待 Manager 补充接口)", placementID, map);
}

RCT_EXPORT_METHOD(addAutoLoadInterstitial:(NSString *)placementID settings:(NSDictionary *)settings)
{
    ATLog(@"addAutoLoadInterstitial placementID=%@ settings=%@", placementID, settings);
    [self.interstitialMgr autoLoadInterstitialAD:placementID];
    if ([settings isKindOfClass:[NSDictionary class]] && settings.count > 0) {
        [self.interstitialMgr autoLoadInterstitialADSetLocalExtra:placementID extraDic:settings];
    }
}

RCT_EXPORT_METHOD(removeAutoLoadInterstitial:(NSString *)placementID)
{
    ATLog(@"removeAutoLoadInterstitial placementID=%@", placementID);
    [self.interstitialMgr cancelAutoLoadInterstitialAD:placementID];
}

RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD(autoLoadInterstitialReady:(NSString *)placementID)
{
    ATLog(@"autoLoadInterstitialReady placementID=%@", placementID);
    return @([self.interstitialMgr hasInterstitialAdReady:placementID]);
}

RCT_EXPORT_METHOD(showAutoLoadInterstitial:(NSString *)placementID
                        scenario:(NSString *)scenario
                         resolve:(RCTPromiseResolveBlock)resolve
                          reject:(RCTPromiseRejectBlock)reject)
{
    ATLog(@"showAutoLoadInterstitial placementID=%@ scenario=%@", placementID, scenario);
    NSDictionary *configDict = [ATCommonTool dictionaryWithJsonString:scenario];
    if ([configDict isKindOfClass:[NSDictionary class]] && configDict.count > 0) {
        NSString *scene = configDict[@"scenarioId"] ?: configDict[@"Scenario"] ?: @"";
        NSString *ext = configDict[@"showCustomExt"] ?: configDict[@"customExt"] ?: @"";
        [self.interstitialMgr showAutoLoadInterstitialADWithPlacementID:placementID sceneID:scene showCustomExt:ext];
    } else {
        [self.interstitialMgr showAutoLoadInterstitialADWithPlacementID:placementID sceneID:scenario ?: @""];
    }
    resolve(nil);
}

RCT_EXPORT_METHOD(showAutoLoadInterstitialWithConfig:(NSString *)placementID
                         showConfig:(NSDictionary *)showConfig
                            resolve:(RCTPromiseResolveBlock)resolve
                             reject:(RCTPromiseRejectBlock)reject)
{
    ATLog(@"showAutoLoadInterstitialWithConfig placementID=%@ showConfig=%@", placementID, showConfig);
    NSString *scene = showConfig[@"Scenario"] ?: showConfig[@"scenarioId"] ?: @"";
    NSString *ext = showConfig[@"showCustomExt"] ?: showConfig[@"customExt"] ?: @"";
    [self.interstitialMgr showAutoLoadInterstitialADWithPlacementID:placementID sceneID:scene showCustomExt:ext];
    resolve(nil);
}

#pragma mark - 开屏

RCT_EXPORT_METHOD(loadSplash:(NSString *)placementID settings:(NSDictionary *)settings)
{
    ATLog(@"loadSplash placementID=%@ settings=%@", placementID, settings);
    [self.splashMgr loadSplashAd:placementID extraDic:settings ?: @{}];
}

RCT_EXPORT_METHOD(showSplash:(NSString *)placementID
          scenario:(NSString *)scenario
           resolve:(RCTPromiseResolveBlock)resolve
            reject:(RCTPromiseRejectBlock)reject)
{
    ATLog(@"showSplash placementID=%@ scenario=%@", placementID, scenario);
    if (scenario.length > 0) {
        [self.splashMgr showSplashAd:placementID sceneID:scenario];
    } else {
        [self.splashMgr showSplashAd:placementID];
    }
    resolve(nil);
}

RCT_EXPORT_METHOD(showSplashWithConfig:(NSString *)placementID
                  showConfig:(NSDictionary *)showConfig
                     resolve:(RCTPromiseResolveBlock)resolve
                      reject:(RCTPromiseRejectBlock)reject)
{
    ATLog(@"showSplashWithConfig placementID=%@ showConfig=%@", placementID, showConfig);
    NSString *scene = showConfig[@"Scenario"] ?: showConfig[@"scenarioId"] ?: showConfig[@"scenario"] ?: showConfig[@"sceneID"] ?: @"";
    NSString *ext = showConfig[@"showCustomExt"] ?: showConfig[@"customExt"] ?: @"";
    [self.splashMgr showSplashAdWithShowConfig:placementID sceneID:scene showCustomExt:ext];
    resolve(nil);
}

RCT_EXPORT_METHOD(splashReady:(NSString *)placementID
            resolve:(RCTPromiseResolveBlock)resolve
             reject:(RCTPromiseRejectBlock)reject)
{
    ATLog(@"splashReady placementID=%@", placementID);
    resolve(@([self.splashMgr splashAdReady:placementID]));
}

RCT_EXPORT_METHOD(checkSplashLoadStatus:(NSString *)placementID
                      resolve:(RCTPromiseResolveBlock)resolve
                       reject:(RCTPromiseRejectBlock)reject)
{
    ATLog(@"checkSplashLoadStatus placementID=%@", placementID);
    NSDictionary *status = [self.splashMgr checkSplashAdLoadStatus:placementID];
    resolve(status ?: @{@"isLoading": @(NO), @"isReady": @(NO)});
}

RCT_EXPORT_METHOD(getSplashValidAds:(NSString *)placementID
                  resolve:(RCTPromiseResolveBlock)resolve
                   reject:(RCTPromiseRejectBlock)reject)
{
    ATLog(@"getSplashValidAds placementID=%@", placementID);
    NSString *json = [self.splashMgr getSplashAdValidAds:placementID];
    id parsed = nil;
    if (json.length > 0) {
        parsed = [NSJSONSerialization JSONObjectWithData:[json dataUsingEncoding:NSUTF8StringEncoding]
                                                 options:0 error:nil];
    }
    resolve(parsed ?: @[]);
}

RCT_EXPORT_METHOD(entrySplashScenario:(NSString *)placementID scenario:(NSString *)scenario tkExtra:(NSDictionary *)tkExtra)
{
    ATLog(@"entrySplashScenario placementID=%@ scenario=%@ tkExtra=%@", placementID, scenario, tkExtra);
    [self.splashMgr entryScenarioWithPlacementID:placementID sceneID:scenario];
}

RCT_EXPORT_METHOD(setSplashLocalExtra:(NSString *)placementID map:(NSDictionary *)map)
{
    ATLog(@"setSplashLocalExtra placementID=%@ map=%@ (待 Manager 补充接口)", placementID, map);
}

RCT_EXPORT_METHOD(setSplashTKExtra:(NSString *)placementID map:(NSDictionary *)map)
{
    ATLog(@"setSplashTKExtra placementID=%@ map=%@ (待 Manager 补充接口)", placementID, map);
}

#pragma mark - Banner
// 命令式部分；嵌入式视图（show in rect / position / hide / afreshShow）走 Fabric ComponentView，
// 不在 Bridge 暴露——但本入口仍提供 load/destroy/entryScenario 等命令式接口。

RCT_EXPORT_METHOD(loadBannerAd:(NSString *)placementID settings:(NSDictionary *)settings)
{
    ATLog(@"loadBannerAd placementID=%@ settings=%@", placementID, settings);
    [self.bannerMgr loadBannerWith:placementID extraDic:settings ?: @{}];
}

RCT_EXPORT_METHOD(checkBannerLoadStatus:(NSString *)placementID
                      resolve:(RCTPromiseResolveBlock)resolve
                       reject:(RCTPromiseRejectBlock)reject)
{
    ATLog(@"checkBannerLoadStatus placementID=%@", placementID);
    NSDictionary *status = [self.bannerMgr checkBannerLoadStatus:placementID];
    resolve(status ?: @{@"isLoading": @(NO), @"isReady": @(NO)});
}

RCT_EXPORT_METHOD(getBannerValidAds:(NSString *)placementID
                  resolve:(RCTPromiseResolveBlock)resolve
                   reject:(RCTPromiseRejectBlock)reject)
{
    
    NSString *json = [self.bannerMgr getBannerValidAds:placementID];
    ATLog(@"getBannerValidAds placementID=%@ json=%@", placementID, json);
    id parsed = nil;
    if (json.length > 0) {
        parsed = [NSJSONSerialization JSONObjectWithData:[json dataUsingEncoding:NSUTF8StringEncoding]
                                                 options:0 error:nil];
    }
    resolve(parsed ?: @[]);
}

RCT_EXPORT_METHOD(entryBannerScenario:(NSString *)placementID scenario:(NSString *)scenario tkExtra:(NSDictionary *)tkExtra)
{
    ATLog(@"entryBannerScenario placementID=%@ scenario=%@ tkExtra=%@", placementID, scenario, tkExtra);
    [self.bannerMgr entryScenarioWithPlacementID:placementID sceneID:scenario];
}

RCT_EXPORT_METHOD(setBannerShowConfig:(NSString *)placementID map:(NSDictionary *)map)
{
    ATLog(@"setBannerShowConfig placementID=%@ map=%@", placementID, map);
    [ATBannerTool setShowConfigMap:map forPlacementID:placementID];
}

RCT_EXPORT_METHOD(setBannerLocalExtra:(NSString *)placementID map:(NSDictionary *)map)
{
    ATLog(@"setBannerLocalExtra placementID=%@ map=%@", placementID, map);
    [ATBannerTool setLocalExtraMap:map forPlacementID:placementID];
}

RCT_EXPORT_METHOD(setBannerTKExtra:(NSString *)placementID map:(NSDictionary *)map)
{
    ATLog(@"setBannerTKExtra placementID=%@ map=%@ (待 Manager 补充接口)", placementID, map);
}

RCT_EXPORT_METHOD(destroyBanner:(NSString *)placementID)
{
    ATLog(@"destroyBanner placementID=%@", placementID);
    [self.bannerMgr destroyBannerAd:placementID];
}

#pragma mark - Native 信息流
// 命令式部分；自渲染 / Express 嵌入式视图走 Fabric ComponentView。

RCT_EXPORT_METHOD(loadNativeAd:(NSString *)placementID settings:(NSDictionary *)settings)
{
    ATLog(@"loadNativeAd placementID=%@ settings=%@", placementID, settings);
    NSDictionary *extra = ([settings isKindOfClass:[NSDictionary class]]) ? settings : @{};

    // 参考 Flutter ATFAdManger+NativeAd.m：默认 YES（自渲染 ATNativeManager）；
    // 显式传 isNativeShowType=NO 才走模板（ATPlatformNativeManager）。
    BOOL isNativeShowType = YES;
    id v = extra[@"isNativeShowType"];
    if (v != nil && [v respondsToSelector:@selector(boolValue)] && [v boolValue] == NO) {
        isNativeShowType = NO;
    }
    
    if (isNativeShowType) {
        ATLog(@"loadNativeAd → ATNativeManager (selfRender) placementID=%@", placementID);
        _nativeExpressMap[placementID] = @(NO);   // 自渲染
        [self.nativeMgr loadNativeWith:placementID extraDic:extra];
    } else {
        ATLog(@"loadNativeAd → ATPlatformNativeManager (express) placementID=%@", placementID);
        _nativeExpressMap[placementID] = @(YES);  // 模板（Express）
        [self.platformNativeMgr loadNativeWith:placementID extraDic:extra];
    }
}

RCT_EXPORT_METHOD(getNativeAd:(NSString *)placementID
            resolve:(RCTPromiseResolveBlock)resolve
             reject:(RCTPromiseRejectBlock)reject)
{
    ATLog(@"getNativeAd placementID=%@", placementID);
    BOOL hasAd = [self.nativeMgr hasNativeAdReady:placementID];
    if (hasAd) {
        // 生成跨桥对象引用 adId（= 安卓 NativeAd 对象引用的等价替代）。
        // 格式：placementID + "#" + 自增序号，简单可读、无冲突。
        _nativeAdIdCounter++;
        NSString *adId = [NSString stringWithFormat:@"%@#%lu", placementID, (unsigned long)_nativeAdIdCounter];
        BOOL isExpress = [_nativeExpressMap[placementID] boolValue];
        ATLog(@"getNativeAd hasAd=YES adId=%@ isExpress=%d", adId, isExpress);
        resolve(@{@"hasAd": @(YES), @"adId": adId, @"isExpress": @(isExpress)});
    } else {
        resolve([NSNull null]);
    }
}

RCT_EXPORT_METHOD(prepareNativeAd:(NSString *)placementID prepareInfo:(NSDictionary *)prepareInfo)
{
    ATLog(@"prepareNativeAd placementID=%@ prepareInfo=%@ (待 Manager 补充接口)", placementID, prepareInfo);
}

RCT_EXPORT_METHOD(checkNativeLoadStatus:(NSString *)placementID
                      resolve:(RCTPromiseResolveBlock)resolve
                       reject:(RCTPromiseRejectBlock)reject)
{
    ATLog(@"checkNativeLoadStatus placementID=%@", placementID); 
  NSDictionary *status = [self.nativeMgr checkNativeLoadStatus:placementID];
  resolve(status ?: @{@"isLoading": @(NO), @"isReady": @(NO)});
}

RCT_EXPORT_METHOD(getNativeValidAds:(NSString *)placementID
                  resolve:(RCTPromiseResolveBlock)resolve
                   reject:(RCTPromiseRejectBlock)reject)
{
    ATLog(@"getNativeValidAds placementID=%@ (Manager 未提供 validAds 查询，返回空)", placementID);
    resolve(@[]);
}

RCT_EXPORT_METHOD(getNativeAdMaterial:(NSString *)placementID
                      adId:(NSString *)adId
                   resolve:(RCTPromiseResolveBlock)resolve
                    reject:(RCTPromiseRejectBlock)reject)
{
    // 查 _nativeExpressMap 返回 isExpress，不调 getNativeAdOfferWithPlacementID（会消费 offer pool）。
    // ATNativeUIComponent.attachIfNeeded 是真正消费 offer 并渲染的唯一入口。
    BOOL isExpress = [_nativeExpressMap[placementID] boolValue];
    ATLog(@"getNativeAdMaterial placementID=%@ adId=%@ isExpress=%d", placementID, adId, isExpress);
    resolve(@{@"isExpress": @(isExpress)});
}

RCT_EXPORT_METHOD(entryNativeScenario:(NSString *)placementID scenario:(NSString *)scenario tkExtra:(NSDictionary *)tkExtra)
{
    ATLog(@"entryNativeScenario placementID=%@ scenario=%@ tkExtra=%@", placementID, scenario, tkExtra);
    [self.nativeMgr entryScenarioWithPlacementID:placementID sceneID:scenario];
}

RCT_EXPORT_METHOD(setNativeShowConfig:(NSString *)placementID map:(NSDictionary *)map)
{
    ATLog(@"setNativeShowConfig placementID=%@ map=%@", placementID, map);
    [ATNativeTool setShowConfigMap:map forPlacementID:placementID];
}

RCT_EXPORT_METHOD(setNativeLocalExtra:(NSString *)placementID map:(NSDictionary *)map)
{
    ATLog(@"setNativeLocalExtra placementID=%@ map=%@", placementID, map);
    [ATNativeTool setLocalExtraMap:map forPlacementID:placementID];
}

RCT_EXPORT_METHOD(setNativeTKExtra:(NSString *)placementID map:(NSDictionary *)map)
{
    ATLog(@"setNativeTKExtra placementID=%@ map=%@ (待 Manager 补充接口)", placementID, map);
}

RCT_EXPORT_METHOD(destroyNativeAd:(NSString *)placementID adId:(NSString *)adId)
{
    ATLog(@"destroyNativeAd placementID=%@ adId=%@", placementID, adId);
    [self.nativeMgr destroyNativeAd:placementID adId:adId ?: @""];
}

RCT_EXPORT_METHOD(nativeAdOnResume:(NSString *)placementID adId:(NSString *)adId)
{
    ATLog(@"nativeAdOnResume placementID=%@ adId=%@ (stub)", placementID, adId);
}

RCT_EXPORT_METHOD(nativeAdOnPause:(NSString *)placementID adId:(NSString *)adId)
{
    ATLog(@"nativeAdOnPause placementID=%@ adId=%@ (stub)", placementID, adId);
}

#pragma mark - NativeEventEmitter 必需

RCT_EXPORT_METHOD(addListener:(NSString *)eventName)
{
    ATLog(@"addListener eventName=%@", eventName);
}

RCT_EXPORT_METHOD(removeListeners:(double)count)
{
    ATLog(@"removeListeners count=%f", count);
}

#ifdef RCT_NEW_ARCH_ENABLED
- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeATBridgeSpecJSI>(params);
}
#endif

@end
