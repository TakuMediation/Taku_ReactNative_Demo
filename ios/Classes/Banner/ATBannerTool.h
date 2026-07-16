//
//  ATBannerTool.h
//  anythink_sdk
//
//  Created by GUO PENG on 2021/7/12.
//

#import <Foundation/Foundation.h>

@class ATBannerView;

NS_ASSUME_NONNULL_BEGIN

@interface ATBannerTool : NSObject

#pragma mark - 工具类

/// 解析flutter端参数,获取bannerView的rect
+ (CGRect)getSizeFromExtraDic:(NSDictionary *)extraDic;

/// 获取BannerView
+ (ATBannerView *)getBannerViewAdRect:(CGRect)rect placementID:(NSString *)placementID sceneID:(NSString * _Nullable)sceneID showCustomExt:(NSString * _Nullable)showCustomExt;


/// 横幅广告是否准备好
+ (BOOL)bannerAdReady:(NSString *)placementID;

/// 获取当前广告位下所有可用广告的信息 v5.7.53及以上版本支持
+ (NSString *)getBannerValidAds:(NSString *)placementID;

/// 获取广告位的状态
+ (NSDictionary *)checkBannerLoadStatus:(NSString *)placementID;

/// 缓存 JS setShowConfig（scenarioId / showCustomExt），供 Fabric attach 时使用
+ (void)setShowConfigMap:(NSDictionary *)map forPlacementID:(NSString *)placementID;

/// 缓存 JS setLocalExtra，供 retrieve Banner 时使用
+ (void)setLocalExtraMap:(NSDictionary *)map forPlacementID:(NSString *)placementID;

+ (NSDictionary *)localExtraForPlacementID:(NSString *)placementID;

+ (NSString *)showCustomExtForPlacementID:(NSString *)placementID;

+ (NSString *)scenarioIdForPlacementID:(NSString *)placementID;

/// 记录命令式 load 使用的尺寸，供 Fabric attach 时校验是否与 show 尺寸一致
+ (void)setLoadedSize:(CGSize)size forPlacementID:(NSString *)placementID;
+ (CGSize)loadedSizeForPlacementID:(NSString *)placementID;

+ (CGSize)sdkBannerSizeForPlacementID:(NSString *)placementID;

+ (BOOL)isBannerSize:(CGSize)a closeTo:(CGSize)b;

+ (void)clearPlacementState:(NSString *)placementID;

@end

NS_ASSUME_NONNULL_END
