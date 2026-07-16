//
//  ATNativeTool.h
//  anythink_sdk
//
//  Created by GUO PENG on 2021/7/12.
//

#import <Foundation/Foundation.h>
#import <AnyThinkSDK/AnyThinkSDK.h>

NS_ASSUME_NONNULL_BEGIN

@interface ATNativeTool : NSObject

+ (ATNativeADConfiguration *)getATNativeADConfiguration:(NSDictionary *)extraDic;
+ (ATNativeADConfiguration *)getATNativeADConfiguration:(NSDictionary *)extraDic isAdaptiveHeight:(BOOL)isAdaptiveHeight;

/// 原生广告是否准备好
+ (BOOL)nativeAdReady:(NSString *)placementID;

/// 获取当前广告位下所有可用广告的信息，v5.7.53及以上版本支持
+ (NSString *)getNativeValidAds:(NSString *)placementID;

/// 获取原生广告位的状态
+ (NSDictionary *)checkNativeLoadStatus:(NSString *)placementID;

/// 缓存 JS setShowConfig（scenarioId / showCustomExt），供 Fabric attach 时使用
+ (void)setShowConfigMap:(NSDictionary *)map forPlacementID:(NSString *)placementID;

/// 缓存 JS setLocalExtra
+ (void)setLocalExtraMap:(NSDictionary *)map forPlacementID:(NSString *)placementID;

+ (NSDictionary *)localExtraForPlacementID:(NSString *)placementID;

+ (NSString *)showCustomExtForPlacementID:(NSString *)placementID;

+ (NSString *)scenarioIdForPlacementID:(NSString *)placementID;

+ (void)clearPlacementState:(NSString *)placementID;

@end

NS_ASSUME_NONNULL_END
