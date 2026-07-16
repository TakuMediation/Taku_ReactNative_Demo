//
//  ATNativeManager.h
//  topon_flutter_plugin
//
//  Created by GUO PENG on 2021/6/29.
//

#import <Foundation/Foundation.h>
#import <AnyThinkSDK/AnyThinkSDK.h>

@class ATNativeUIComponent;

NS_ASSUME_NONNULL_BEGIN

@interface ATNativeManager : NSObject

/// 全 app 共用同一个 ATNativeManager 实例——它同时是 SDK 加载 delegate（loadADWithPlacementID:delegate:）
/// 和展示 delegate（ATNativeADConfiguration.delegate）的接收方。UIComponent 必须用这个 sharedManager
/// 拿 config，否则 config.delegate=nil → 展示/点击/视频等回调全丢。
+ (instancetype)sharedManager;

/// 构造带 delegate=self 的 ATNativeADConfiguration（供 UIComponent 在 attachIfNeeded 里调用）。
- (ATNativeADConfiguration *)getATNativeADConfiguration:(NSDictionary *)extraDic
                                       isAdaptiveHeight:(BOOL)isAdaptiveHeight;

/// 加载原生广告
- (void)loadNativeWith:(NSString *)placementID extraDic:(NSDictionary *)extraDic;

/// 展示原生广告
- (void)showNative:(NSString *)placementID isAdaptiveHeight:(BOOL)isAdaptiveHeight extraDic:(NSDictionary *) extraDic;

/// 展示场景原生广告
- (void)showNative:(NSString *)placementID sceneID:(NSString *)sceneID isAdaptiveHeight:(BOOL)isAdaptiveHeight extraDic:(NSDictionary *) extraDic;

/// 展示场景原生广告 带showCustomExt
- (void)showNative:(NSString *)placementID sceneID:(NSString *)sceneID showCustomExt:(NSString *)showCustomExt isAdaptiveHeight:(BOOL)isAdaptiveHeight extraDic:(NSDictionary *)extraDic;

/// 获取广告位的状态
- (NSDictionary *)checkNativeLoadStatus:(NSString *)placementID;

/// 移除原生广告
- (void)removeNative:(NSString *)placementID;

/// 销毁原生广告（adId 空→整位销毁）
- (void)destroyNativeAd:(NSString *)placementID adId:(NSString *)adId;

/// 销毁 ATNativeADView（SDK destroyNative 反射调用）
- (void)destroyNativeAdViewIfNeeded:(ATNativeADView *)adView;

/// Fabric 路径：UIComponent attach 时注册，关闭回调时按 adView 定位销毁。
- (void)registerFabricUIComponent:(ATNativeUIComponent *)component
                  forNativeADView:(ATNativeADView *)adView;
- (void)unregisterFabricUIComponentForNativeADView:(ATNativeADView *)adView;
- (void)destroyFabricNativeADView:(ATNativeADView *)adView;

/// 统计场景到达率
- (void)entryScenarioWithPlacementID:(NSString *)placementID sceneID:(NSString *)sceneID;

/// 广告是否准备好
- (BOOL)hasNativeAdReady:(NSString *)placementID;


@end

NS_ASSUME_NONNULL_END
