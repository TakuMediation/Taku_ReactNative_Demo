//
//  ATSplashAdManager.m
//  anythink_sdk
//
//  Created by GUO PENG on 2023/9/7.
//

#import "ATSplashAdManager.h"
#import "ATSplashDelegate.h"
#import <AnyThinkSDK/AnyThinkSDK.h>
#import "ATCommonTool.h"
#import "ATConfiguration.h"

@interface ATSplashAdManager()

@property(nonatomic, strong) ATSplashDelegate *splashAdDelegate;

@end


@implementation ATSplashAdManager

/// 加载开屏
- (void)loadSplashAd:(NSString *)placementID extraDic:(NSDictionary *)extraDic {
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
    
    NSMutableDictionary *dic = [NSMutableDictionary dictionaryWithDictionary:extraDic];
    NSString *keyString = @"tolerateTimeout";
    if ([extraDic.allKeys containsObject:keyString]) {
        [dic removeObjectForKey:keyString];
        dic[kATSplashExtraTolerateTimeoutKey] = @([extraDic[keyString] floatValue] * 0.001);
    }
    [ATCommonTool applyAtAdRequestAndRemove:dic];
    [[ATAdManager sharedManager] setMultipleLoadingDelegate:self.splashAdDelegate placementId:placementID];

    [[ATAdManager sharedManager] loadADWithPlacementID:placementID extra:dic delegate:self.splashAdDelegate];
}

/// 是否有广告缓存
- (BOOL)splashAdReady:(NSString *)placementID {
    
    if (kATStringIsEmpty(placementID)) {
        return NO;
    }
    
    BOOL isReady = [[ATAdManager sharedManager] splashReadyForPlacementID:placementID];
    return  isReady;
}

/// 检查广告状态
- (NSDictionary *)checkSplashAdLoadStatus:(NSString *)placementID {
    
    if (kATStringIsEmpty(placementID)) {
        return [NSDictionary dictionary];
    }
    
    ATCheckLoadModel *checkLoadModel = [[ATAdManager sharedManager] checkSplashLoadStatusForPlacementID:placementID];
    NSDictionary *dic = [ATCommonTool objectToJSONString:checkLoadModel];
    return  dic;
}

/// 获取当前广告位下所有可用广告的信息，v5.7.53及以上版本支持
- (NSString *)getSplashAdValidAds:(NSString *)placementID {

    if (kATStringIsEmpty(placementID)) {
        return @"";
    }
    
    NSArray *array = [[ATAdManager sharedManager] getSplashValidAdsForPlacementID:placementID];
    NSString *str = [ATCommonTool toReadableJSONString:array];
    return str;
}

/// 展示开屏广告
- (void)showSplashAd:(NSString *)placementID {
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
     
    UIWindow *window = [[[UIApplication sharedApplication] delegate] window];
    
    ATShowConfig * showConfig = [ATShowConfig new];
    [[ATAdManager sharedManager] showSplashWithPlacementID:placementID config:showConfig window:window inViewController:[ATCommonTool getRootViewController] extra:nil delegate:self.splashAdDelegate];
    
}

///  展示场景开屏广告
- (void)showSplashAd:(NSString *)placementID sceneID:(NSString *)sceneID {
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
     
    sceneID = [ATCommonTool checkStrParamsEmptyAndReturn:sceneID];
    
    UIWindow *window = [[[UIApplication sharedApplication] delegate] window];
    
    ATShowConfig * showConfig = [[ATShowConfig alloc] initWithScene:sceneID showCustomExt:nil];
    
    [[ATAdManager sharedManager] showSplashWithPlacementID:placementID config:showConfig window:window inViewController:[ATCommonTool getRootViewController] extra:nil delegate:self.splashAdDelegate];
}

///  展示开屏广告通过config
- (void)showSplashAdWithShowConfig:(NSString *)placementID sceneID:(NSString *)sceneID showCustomExt:(NSString *)showCustomExt {
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
     
    sceneID = [ATCommonTool checkStrParamsEmptyAndReturn:sceneID];
    showCustomExt = [ATCommonTool checkStrParamsEmptyAndReturn:showCustomExt];
    
    UIWindow *window = [[[UIApplication sharedApplication] delegate] window];
    
    ATShowConfig * showConfig = [[ATShowConfig alloc] initWithScene:sceneID showCustomExt:showCustomExt];
    
    [[ATAdManager sharedManager] showSplashWithPlacementID:placementID config:showConfig window:window inViewController:[ATCommonTool getRootViewController] extra:nil delegate:self.splashAdDelegate];
}

/// 统计场景到达率
- (void)entryScenarioWithPlacementID:(NSString *)placementID sceneID:(NSString *)sceneID {
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
    
    ATLog(@"entrySplashScenarioWithPlacementID: %@ --- sceneID:%@",placementID,sceneID);

    [[ATAdManager sharedManager] entrySplashScenarioWithPlacementID:placementID scene:sceneID];
}

#pragma mark - lazy
- (ATSplashDelegate *)splashAdDelegate {
    if (_splashAdDelegate) return _splashAdDelegate;
    ATSplashDelegate *splashDelegate = [ATSplashDelegate new];
    return _splashAdDelegate = splashDelegate;
}

@end
