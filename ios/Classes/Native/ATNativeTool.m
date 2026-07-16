//
//  ATNativeTool.m
//  anythink_sdk
//
//  Created by GUO PENG on 2021/7/12.
//

#import "ATNativeTool.h"
#import "ATConfiguration.h"
#import "ATCommonTool.h"
#import "ATNativeAttributeMode.h"

static NSMutableDictionary *sNativeShowConfigByPlacement;
static NSMutableDictionary *sNativeLocalExtraByPlacement;

@implementation ATNativeTool

+ (ATNativeADConfiguration *)getATNativeADConfiguration:(NSDictionary *)extraDic{
    return [self getATNativeADConfiguration:extraDic isAdaptiveHeight:NO];
}

+ (ATNativeAttributeMode *)nativeModeFromExtraDic:(NSDictionary *)extraDic key:(NSString *)key {
    ATNativeAttributeMode *nativeAttributeMode = [[ATNativeAttributeMode alloc] init];
    if (![extraDic isKindOfClass:[NSDictionary class]]) {
        return nativeAttributeMode;
    }
    id value = extraDic[key];
    if (![value isKindOfClass:[NSDictionary class]]) {
        return nativeAttributeMode;
    }
    [nativeAttributeMode setValuesForKeysWithDictionary:(NSDictionary *)value];
    return nativeAttributeMode;
}

+ (ATNativeADConfiguration *)getATNativeADConfiguration:(NSDictionary *)extraDic isAdaptiveHeight:(BOOL)isAdaptiveHeight{
    
    ATNativeAttributeMode *parentMode = [self nativeModeFromExtraDic:extraDic key:Parent];
    ATNativeAttributeMode *mainImageMode = [self nativeModeFromExtraDic:extraDic key:MainImage];
    ATNativeAttributeMode *adLogoMode = [self nativeModeFromExtraDic:extraDic key:AdLogo];
    
    UIViewController *tempController = [ATCommonTool getRootViewController];
    
    ATNativeADConfiguration *config = [[ATNativeADConfiguration alloc] init];
    config.ADFrame = CGRectMake(parentMode.x + 100, parentMode.y, parentMode.width, parentMode.height);
    ATLog(@"原生广告--Config-WidgetShow-frame:%@",NSStringFromCGRect(config.ADFrame));
    config.mediaViewFrame = CGRectMake(mainImageMode.x, mainImageMode.y, mainImageMode.width, mainImageMode.height);
    config.rootViewController = tempController;
    config.logoViewFrame = CGRectMake(adLogoMode.x, adLogoMode.y, adLogoMode.width, adLogoMode.height);
    config.sizeToFit = isAdaptiveHeight;
    return  config;
}


/// 原生广告是否准备好
+ (BOOL)nativeAdReady:(NSString *)placementID{

    BOOL isReady = [[ATAdManager sharedManager] nativeAdReadyForPlacementID:placementID];
    return  isReady;
}

/// 获取当前广告位下所有可用广告的信息，v5.7.53及以上版本支持
+ (NSString *)getNativeValidAds:(NSString *)placementID{

    NSArray *array = [[ATAdManager sharedManager] getNativeValidAdsForPlacementID:placementID];
      NSString *str = [ATCommonTool toReadableJSONString:array];
      return str;
}


/// 获取原生广告位的状态
+ (NSDictionary *)checkNativeLoadStatus:(NSString *)placementID{
    
    ATCheckLoadModel *model = [[ATAdManager sharedManager] checkNativeLoadStatusForPlacementID:placementID];
    NSDictionary *dic = [ATCommonTool objectToJSONString:model];
    return  dic;
}

+ (void)setShowConfigMap:(NSDictionary *)map forPlacementID:(NSString *)placementID {
    if (kATStringIsEmpty(placementID) || ![map isKindOfClass:[NSDictionary class]]) {
        return;
    }
    if (sNativeShowConfigByPlacement == nil) {
        sNativeShowConfigByPlacement = [NSMutableDictionary dictionary];
    }
    sNativeShowConfigByPlacement[placementID] = map;
}

+ (void)setLocalExtraMap:(NSDictionary *)map forPlacementID:(NSString *)placementID {
    if (kATStringIsEmpty(placementID) || ![map isKindOfClass:[NSDictionary class]]) {
        return;
    }
    if (sNativeLocalExtraByPlacement == nil) {
        sNativeLocalExtraByPlacement = [NSMutableDictionary dictionary];
    }
    sNativeLocalExtraByPlacement[placementID] = map;
}

+ (NSDictionary *)localExtraForPlacementID:(NSString *)placementID {
    if (kATStringIsEmpty(placementID) || sNativeLocalExtraByPlacement == nil) {
        return @{};
    }
    NSDictionary *map = sNativeLocalExtraByPlacement[placementID];
    return [map isKindOfClass:[NSDictionary class]] ? map : @{};
}

+ (NSString *)showCustomExtForPlacementID:(NSString *)placementID {
    if (kATStringIsEmpty(placementID) || sNativeShowConfigByPlacement == nil) {
        return @"";
    }
    NSDictionary *map = sNativeShowConfigByPlacement[placementID];
    id ext = map[@"showCustomExt"];
    if ([ext isKindOfClass:[NSString class]] && [(NSString *)ext length] > 0) {
        return (NSString *)ext;
    }
    return @"";
}

+ (NSString *)scenarioIdForPlacementID:(NSString *)placementID {
    if (kATStringIsEmpty(placementID) || sNativeShowConfigByPlacement == nil) {
        return @"";
    }
    NSDictionary *map = sNativeShowConfigByPlacement[placementID];
    id sid = map[@"scenarioId"];
    if ([sid isKindOfClass:[NSString class]] && [(NSString *)sid length] > 0) {
        return (NSString *)sid;
    }
    return @"";
}

+ (void)clearPlacementState:(NSString *)placementID {
    if (kATStringIsEmpty(placementID)) {
        return;
    }
    if (sNativeShowConfigByPlacement != nil) {
        [sNativeShowConfigByPlacement removeObjectForKey:placementID];
    }
    if (sNativeLocalExtraByPlacement != nil) {
        [sNativeLocalExtraByPlacement removeObjectForKey:placementID];
    }
}

@end
