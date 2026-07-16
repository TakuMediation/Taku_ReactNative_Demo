//
//  ATBannerTool.m
//  anythink_sdk
//
//  Created by GUO PENG on 2021/7/12.
//

#import "ATBannerTool.h"
#import "ATCommonTool.h"
#import "ATConfiguration.h"
#import <AnyThinkSDK/AnyThinkSDK.h>

#define ATBannerAdLoadingExtraBannerAdSizeStruct  @"size"

static NSMutableDictionary *sBannerShowConfigByPlacement;
static NSMutableDictionary *sBannerLocalExtraByPlacement;
static NSMutableDictionary *sBannerLoadedSizeByPlacement;

@implementation ATBannerTool
/// 解析 RN 端 size 参数；无 size 时默认 320×50（标准 Banner），避免与 Fabric 展示尺寸不一致。
+ (CGRect)getSizeFromExtraDic:(NSDictionary *)extraDic{
    
    NSNumber *widthNumeber = [NSNumber numberWithDouble:320.0];
    NSNumber *heightNumeber = [NSNumber numberWithDouble:50.0];
    
    NSNumber *x = [NSNumber numberWithDouble:0.0f];
    
    NSNumber *y = [NSNumber numberWithDouble:0.0f];

    if ([extraDic.allKeys containsObject:ATBannerAdLoadingExtraBannerAdSizeStruct] &&extraDic[ATBannerAdLoadingExtraBannerAdSizeStruct][@"width"] != nil ) {
        
        x = extraDic[ATBannerAdLoadingExtraBannerAdSizeStruct][@"x"];
        y = extraDic[ATBannerAdLoadingExtraBannerAdSizeStruct][@"y"];
        widthNumeber = extraDic[ATBannerAdLoadingExtraBannerAdSizeStruct][@"width"];
        heightNumeber = extraDic[ATBannerAdLoadingExtraBannerAdSizeStruct][@"height"];
    }
    
    return  CGRectMake([x doubleValue], [y doubleValue], [widthNumeber doubleValue], [heightNumeber doubleValue]);
}


/// 获取BannerView
+ (ATBannerView *)getBannerViewAdRect:(CGRect)rect placementID:(NSString *)placementID sceneID:(NSString * _Nullable)sceneID showCustomExt:(NSString * _Nullable)showCustomExt {
    
    if (placementID == nil || placementID.length == 0) {
        ATLog(@"ATBannerTool.getBannerViewAdRect skip — empty placementID");
        return nil;
    }
    
    NSString *scene = (sceneID.length > 0) ? sceneID : @"";
    NSString *customExt = (showCustomExt.length > 0) ? showCustomExt : @"";
    
    ATShowConfig *showConfig = [[ATShowConfig alloc] initWithScene:scene showCustomExt:customExt];
    ATBannerView *bannerView = [[ATAdManager sharedManager] retrieveBannerViewForPlacementID:placementID config:showConfig];
  
    bannerView.frame = rect; 
    
    return bannerView;
    
}


/// 横幅广告是否准备好
+ (BOOL)bannerAdReady:(NSString *)placementID{
    
    BOOL isReady =[[ATAdManager sharedManager] bannerAdReadyForPlacementID:placementID];
    return  isReady;
}

/// 获取当前广告位下所有可用广告的信息
+ (NSString *)getBannerValidAds:(NSString *)placementID{
    
    NSArray *array = [[ATAdManager sharedManager] getBannerValidAdsForPlacementID:placementID];
      NSString *str = [ATCommonTool toReadableJSONString:array];
      return str;
}


/// 获取广告位的状态
+ (NSDictionary *)checkBannerLoadStatus:(NSString *)placementID{
    
    ATCheckLoadModel *model = [[ATAdManager sharedManager] checkBannerLoadStatusForPlacementID:placementID];
    
    NSDictionary *dic = [ATCommonTool objectToJSONString:model];
    return  dic;
}

+ (void)setShowConfigMap:(NSDictionary *)map forPlacementID:(NSString *)placementID {
    if (kATStringIsEmpty(placementID) || ![map isKindOfClass:[NSDictionary class]]) {
        return;
    }
    if (sBannerShowConfigByPlacement == nil) {
        sBannerShowConfigByPlacement = [NSMutableDictionary dictionary];
    }
    sBannerShowConfigByPlacement[placementID] = map;
}

+ (void)setLocalExtraMap:(NSDictionary *)map forPlacementID:(NSString *)placementID {
    if (kATStringIsEmpty(placementID) || ![map isKindOfClass:[NSDictionary class]]) {
        return;
    }
    if (sBannerLocalExtraByPlacement == nil) {
        sBannerLocalExtraByPlacement = [NSMutableDictionary dictionary];
    }
    sBannerLocalExtraByPlacement[placementID] = map;
}

+ (NSDictionary *)localExtraForPlacementID:(NSString *)placementID {
    if (kATStringIsEmpty(placementID) || sBannerLocalExtraByPlacement == nil) {
        return @{};
    }
    NSDictionary *map = sBannerLocalExtraByPlacement[placementID];
    return [map isKindOfClass:[NSDictionary class]] ? map : @{};
}

+ (NSString *)showCustomExtForPlacementID:(NSString *)placementID {
    if (kATStringIsEmpty(placementID) || sBannerShowConfigByPlacement == nil) {
        return @"";
    }
    NSDictionary *map = sBannerShowConfigByPlacement[placementID];
    id ext = map[@"showCustomExt"];
    if ([ext isKindOfClass:[NSString class]] && [(NSString *)ext length] > 0) {
        return (NSString *)ext;
    }
    return @"";
}

+ (NSString *)scenarioIdForPlacementID:(NSString *)placementID {
    if (kATStringIsEmpty(placementID) || sBannerShowConfigByPlacement == nil) {
        return @"";
    }
    NSDictionary *map = sBannerShowConfigByPlacement[placementID];
    id sid = map[@"scenarioId"];
    if ([sid isKindOfClass:[NSString class]] && [(NSString *)sid length] > 0) {
        return (NSString *)sid;
    }
    return @"";
}

+ (void)setLoadedSize:(CGSize)size forPlacementID:(NSString *)placementID {
    if (kATStringIsEmpty(placementID) || size.width <= 0 || size.height <= 0) {
        return;
    }
    if (sBannerLoadedSizeByPlacement == nil) {
        sBannerLoadedSizeByPlacement = [NSMutableDictionary dictionary];
    }
    sBannerLoadedSizeByPlacement[placementID] = [NSValue valueWithCGSize:size];
}

+ (CGSize)loadedSizeForPlacementID:(NSString *)placementID {
    if (kATStringIsEmpty(placementID) || sBannerLoadedSizeByPlacement == nil) {
        return CGSizeZero;
    }
    NSValue *v = sBannerLoadedSizeByPlacement[placementID];
    return v != nil ? [v CGSizeValue] : CGSizeZero;
}

+ (CGSize)sizeFromBannerAdSizeValue:(id)sizeVal {
    if (sizeVal == nil || [sizeVal isKindOfClass:[NSNull class]]) {
        return CGSizeZero;
    }
    if ([sizeVal isKindOfClass:[NSValue class]]) {
        CGSize s = [(NSValue *)sizeVal CGSizeValue];
        if (s.width > 0 && s.height > 0) {
            return s;
        }
    }
    if (![sizeVal isKindOfClass:[NSString class]]) {
        return CGSizeZero;
    }
    NSString *sizeStr = (NSString *)sizeVal;
    NSRange open = [sizeStr rangeOfString:@"{"];
    NSRange close = [sizeStr rangeOfString:@"}"];
    if (open.location == NSNotFound || close.location == NSNotFound || close.location <= open.location) {
        return CGSizeZero;
    }
    NSString *inner = [sizeStr substringWithRange:NSMakeRange(open.location + 1, close.location - open.location - 1)];
    NSArray *parts = [inner componentsSeparatedByString:@","];
    if (parts.count != 2) {
        return CGSizeZero;
    }
    CGFloat w = [[parts[0] stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]] doubleValue];
    CGFloat h = [[parts[1] stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]] doubleValue];
    if (w <= 0 || h <= 0) {
        return CGSizeZero;
    }
    return CGSizeMake(w, h);
}

/// 从 checkLoadStatus 的 user_load_extra_data.banner_ad_size 解析 SDK 实际 load 尺寸
+ (CGSize)sdkBannerSizeForPlacementID:(NSString *)placementID {
    if (kATStringIsEmpty(placementID)) {
        return CGSizeZero;
    }
    ATCheckLoadModel *model = [[ATAdManager sharedManager] checkBannerLoadStatusForPlacementID:placementID];
    if (!model.isReady || model.adOfferInfo == nil) {
        return CGSizeZero;
    }
    id userExtra = model.adOfferInfo[UserExtraKey];
    if (![userExtra isKindOfClass:[NSDictionary class]]) {
        return CGSizeZero;
    }
    return [self sizeFromBannerAdSizeValue:((NSDictionary *)userExtra)[@"banner_ad_size"]];
}

+ (BOOL)isBannerSize:(CGSize)a closeTo:(CGSize)b {
    return a.width > 0 && a.height > 0
        && fabs(a.width - b.width) <= 1
        && fabs(a.height - b.height) <= 1;
}

+ (void)clearPlacementState:(NSString *)placementID {
    if (kATStringIsEmpty(placementID)) {
        return;
    }
    if (sBannerLoadedSizeByPlacement != nil) {
        [sBannerLoadedSizeByPlacement removeObjectForKey:placementID];
    }
    if (sBannerShowConfigByPlacement != nil) {
        [sBannerShowConfigByPlacement removeObjectForKey:placementID];
    }
    if (sBannerLocalExtraByPlacement != nil) {
        [sBannerLocalExtraByPlacement removeObjectForKey:placementID];
    }
}



@end
