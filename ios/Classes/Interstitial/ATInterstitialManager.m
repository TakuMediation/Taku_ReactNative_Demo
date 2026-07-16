//
//  ATInterstitialManager.m
//  topon_flutter_plugin
//
//  Created by GUO PENG on 2021/6/28.
//

#import "ATInterstitialManager.h"
#import <AnyThinkSDK/AnyThinkSDK.h>
#import "ATCommonTool.h"
#import "ATInterstitialDelegate.h"
#import "ATConfiguration.h"

#define UseRewardedVideoAsInterstitialKey @"UseRewardedVideoAsInterstitialKey"
#define ATInterstitialExtraAdSizeKey @"size"

@interface ATInterstitialManager()

@property(nonatomic, strong) ATInterstitialDelegate *interstitialDelegate;
 
@end

@implementation ATInterstitialManager

#pragma mark - public
/// 加载插屏广告
- (void)loadInterstitialAd:(NSString *)placementID extraDic:(NSDictionary *)extraDic {
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
    
    NSMutableDictionary *dic = [NSMutableDictionary dictionaryWithDictionary:extraDic];

    // 激励视频当做插屏使用（调用Sigmob的激励视频API）
    if ([extraDic.allKeys containsObject:UseRewardedVideoAsInterstitialKey]) {
        [dic removeObjectForKey:UseRewardedVideoAsInterstitialKey];
        dic[kATInterstitialExtraUsesRewardedVideo] = extraDic[UseRewardedVideoAsInterstitialKey];
    }
    
    // 可通过以下代码设置穿山甲平台的插屏图片广告的尺寸
    if ([extraDic.allKeys containsObject:ATInterstitialExtraAdSizeKey]) {
        
        [dic removeObjectForKey:ATInterstitialExtraAdSizeKey];

        NSDictionary *tempDic = extraDic[ATInterstitialExtraAdSizeKey];

        NSNumber *widthNumeber = tempDic[@"width"];
        NSNumber *heightNumeber = tempDic[@"height"];

        CGSize tempSize = CGSizeMake([widthNumeber doubleValue], [heightNumeber doubleValue]);

        dic[kATInterstitialExtraAdSizeKey] = [NSValue valueWithCGSize:tempSize];
    }
    [ATCommonTool applyAtAdRequestAndRemove:dic];
    [[ATAdManager sharedManager] setMultipleLoadingDelegate:self.interstitialDelegate placementId:placementID];
    [[ATAdManager sharedManager] loadADWithPlacementID:placementID extra:dic delegate:self.interstitialDelegate];
}

/// 插屏广告是否准备好
- (BOOL)hasInterstitialAdReady:(NSString *)placementID {
    
    if (kATStringIsEmpty(placementID)) {
        return NO;
    }
    
    BOOL isReady = [[ATAdManager sharedManager] interstitialReadyForPlacementID:placementID];
    return  isReady;
}

/// 获取当前广告位下所有可用广告的信息
- (NSString *)getInterstitialValidAds:(NSString *)placementID {
    
    if (kATStringIsEmpty(placementID)) {
        return @"";
    }
    
    NSArray *array = [[ATAdManager sharedManager] getInterstitialValidAdsForPlacementID:placementID];

    NSString *str = [ATCommonTool toReadableJSONString:array];

    return str;
}

/// 获取广告位的状态
- (NSDictionary *)checkInterstitialLoadStatus:(NSString *)placementID {
    
    if (kATStringIsEmpty(placementID)) {
        return [NSDictionary dictionary];
    }
    
    ATCheckLoadModel *checkLoadModel = [[ATAdManager sharedManager] checkInterstitialLoadStatusForPlacementID:placementID];

    NSDictionary *dic = [ATCommonTool objectToJSONString:checkLoadModel];
    return  dic;
}
/// 展示插屏广告
- (void)showInterstitialAd:(NSString *)placementID {
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
    
    [[ATAdManager sharedManager] showInterstitialWithPlacementID:placementID inViewController:[ATCommonTool currentViewController] delegate:self.interstitialDelegate];
}

/// 展示场景插屏广告
- (void)showInterstitialAd:(NSString *)placementID sceneID:(NSString *)sceneID{
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
    
    sceneID = [ATCommonTool checkStrParamsEmptyAndReturn:sceneID];
    
    [[ATAdManager sharedManager] showInterstitialWithPlacementID:placementID scene:sceneID inViewController:[ATCommonTool currentViewController] delegate:self.interstitialDelegate];
}

///  展示场景插屏广告通过config
- (void)showInterstitialAdWithShowConfig:(NSString *)placementID sceneID:(NSString *)sceneID showCustomExt:(NSString *)showCustomExt {
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
    
    placementID = [ATCommonTool checkStrParamsEmptyAndReturn:placementID];
    sceneID = [ATCommonTool checkStrParamsEmptyAndReturn:sceneID];
    showCustomExt = [ATCommonTool checkStrParamsEmptyAndReturn:showCustomExt];
     
    ATShowConfig * showConfig = [[ATShowConfig alloc] initWithScene:sceneID showCustomExt:showCustomExt];
    
    [[ATAdManager sharedManager] showInterstitialWithPlacementID:placementID showConfig:showConfig inViewController:[ATCommonTool getRootViewController] delegate:self.interstitialDelegate nativeMixViewBlock:^(ATNativeMixInterstitialView * _Nonnull selfRenderingMixInterstitialView) {
        
    }]; 
}

/// 统计场景到达率
- (void)entryScenarioWithPlacementID:(NSString *)placementID sceneID:(NSString *)sceneID {
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
     
    ATLog(@"entryInterScenarioWithPlacementID: %@ --- sceneID:%@",placementID,sceneID);
    
    [[ATAdManager sharedManager] entryInterstitialScenarioWithPlacementID:placementID scene:sceneID];
}

/// 设置全自动加载
- (void)autoLoadInterstitialAD:(NSString *)placementID {
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
    
    [ATInterstitialAutoAdManager sharedInstance].delegate = self.interstitialDelegate;

    [[ATInterstitialAutoAdManager sharedInstance] addAutoLoadAdPlacementIDArray:[placementID componentsSeparatedByString:@","]];
}

/// 取消全自动加载插屏
- (void)cancelAutoLoadInterstitialAD:(NSString *)placementID {
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
    
    [[ATInterstitialAutoAdManager sharedInstance] removeAutoLoadAdPlacementIDArray:[placementID componentsSeparatedByString:@","]];
}

/// 展示全自动加载插屏（仅 scenario）
- (void)showAutoLoadInterstitialADWithPlacementID:(NSString *)placementID sceneID:(NSString *)sceneID {
    [self showAutoLoadInterstitialADWithPlacementID:placementID sceneID:sceneID showCustomExt:@""];
}

/// 展示全自动加载插屏（showConfig）
- (void)showAutoLoadInterstitialADWithPlacementID:(NSString *)placementID sceneID:(NSString *)sceneID showCustomExt:(NSString *)showCustomExt {

    if (kATStringIsEmpty(placementID)) {
        return;
    }

    placementID = [ATCommonTool checkStrParamsEmptyAndReturn:placementID];
    sceneID = [ATCommonTool checkStrParamsEmptyAndReturn:sceneID];

    if (sceneID.length == 0) {
        [[ATInterstitialAutoAdManager sharedInstance] showAutoLoadInterstitialWithPlacementID:placementID inViewController:[ATCommonTool currentViewController] delegate:self.interstitialDelegate];
    } else {
        showCustomExt = [ATCommonTool checkStrParamsEmptyAndReturn:showCustomExt];
        ATShowConfig *config = [[ATShowConfig alloc] initWithScene:sceneID showCustomExt:showCustomExt];
        [[ATInterstitialAutoAdManager sharedInstance] showAutoLoadInterstitialWithPlacementID:placementID showConfig:config inViewController:[ATCommonTool currentViewController] delegate:self.interstitialDelegate];
    }
}

/// 设置自动加载插屏广告回传参数，没传入extra内容可以用于清空
- (void)autoLoadInterstitialADSetLocalExtra:(NSString *)placementID extraDic:(NSDictionary *)extraDic {

    if (kATStringIsEmpty(placementID)) {
        return;
    }
    
    NSMutableDictionary *dic = [NSMutableDictionary dictionaryWithDictionary:extraDic];
  
    // 激励视频当做插屏使用（调用Sigmob的激励视频API）
    if ([extraDic.allKeys containsObject:UseRewardedVideoAsInterstitialKey]) {
        [dic removeObjectForKey:UseRewardedVideoAsInterstitialKey];
        dic[kATInterstitialExtraUsesRewardedVideo] = extraDic[UseRewardedVideoAsInterstitialKey];
    }
    
    // 可通过以下代码设置穿山甲平台的插屏图片广告的尺寸
    if ([extraDic.allKeys containsObject:ATInterstitialExtraAdSizeKey]) {
        
        [dic removeObjectForKey:ATInterstitialExtraAdSizeKey];

        NSDictionary *tempDic = extraDic[ATInterstitialExtraAdSizeKey];

        NSNumber *widthNumeber = tempDic[@"width"];
        NSNumber *heightNumeber = tempDic[@"height"];

        CGSize tempSize = CGSizeMake([widthNumeber doubleValue], [heightNumeber doubleValue]);

        dic[kATInterstitialExtraAdSizeKey] = [NSValue valueWithCGSize:tempSize];
    }
    
    [[ATInterstitialAutoAdManager sharedInstance] setLocalExtra:dic placementID:placementID];
}

#pragma mark - lazy
- (ATInterstitialDelegate *)interstitialDelegate {

    if (_interstitialDelegate) return _interstitialDelegate;

    ATInterstitialDelegate *interstitialDelegate = [ATInterstitialDelegate new];

    return _interstitialDelegate = interstitialDelegate;
}

@end
