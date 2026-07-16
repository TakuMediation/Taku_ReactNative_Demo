//
//  ATRewardVideoManager.m
//  topon_flutter_plugin
//
//  Created by GUO PENG on 2021/6/26.
//

#import "ATRewardVideoManager.h"
//#import <AnyThinkSDK/AnyThinkSDK.h>
#import <AnyThinkSDK/AnyThinkSDK.h>
#import "ATCommonTool.h"
#import "ATRewardVideoDelegate.h"
#import "ATConfiguration.h"

#define kATAdLoadingExtraUserData  @"kATAdLoadingExtraMediaExtraKey"
#define kATAdLoadingExtraUserDataKeywordKey  @"kATAdLoadingExtraUserDataKeywordKey"
#define kATAdLoadingExtraUserID  @"kATAdLoadingExtraUserIDKey"

@interface ATRewardVideoManager()

@property(nonatomic, strong) ATRewardVideoDelegate *rewardedVideoDelegate;



@end


@implementation ATRewardVideoManager

#pragma mark - public
/// 加载激励视频
- (void)loadRewardedVideo:(NSString *)placementID extraDic:(NSDictionary *)extraDic {
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
    
    NSMutableDictionary *dic = [NSMutableDictionary dictionaryWithDictionary:extraDic];

    if ([extraDic.allKeys containsObject:kATAdLoadingExtraUserDataKeywordKey]) {
        [dic removeObjectForKey:kATAdLoadingExtraUserDataKeywordKey];
        dic[kATAdLoadingExtraMediaExtraKey] = extraDic[kATAdLoadingExtraUserDataKeywordKey];
    }
    if ([extraDic.allKeys containsObject:kATAdLoadingExtraUserID]) {
        [dic removeObjectForKey:kATAdLoadingExtraUserID];
        dic[kATAdLoadingExtraUserIDKey] = extraDic[kATAdLoadingExtraUserID];
    }
    [ATCommonTool applyAtAdRequestAndRemove:dic];
    [[ATAdManager sharedManager] setMultipleLoadingDelegate:self.rewardedVideoDelegate placementId:placementID];

    [[ATAdManager sharedManager] loadADWithPlacementID:placementID extra:dic delegate:self.rewardedVideoDelegate];
}


/// 展示激励视频广告
- (void)showRewardedVideo:(NSString *)placementID {
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
    
    [[ATAdManager sharedManager] showRewardedVideoWithPlacementID:placementID inViewController:[ATCommonTool currentViewController] delegate:self.rewardedVideoDelegate];
}

///  展示场景激励视频广告
- (void)showRewardedVideo:(NSString *)placementID sceneID:(NSString *)sceneID {
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
     
    sceneID = [ATCommonTool checkStrParamsEmptyAndReturn:sceneID];
    
    [[ATAdManager sharedManager] showRewardedVideoWithPlacementID:placementID scene:sceneID inViewController:[ATCommonTool currentViewController] delegate:self.rewardedVideoDelegate];
}

///  展示激励视频广告通过config
- (void)showRewardedVideoWithShowConfig:(NSString *)placementID sceneID:(NSString *)sceneID showCustomExt:(NSString *)showCustomExt {
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
     
    sceneID = [ATCommonTool checkStrParamsEmptyAndReturn:sceneID];
    showCustomExt = [ATCommonTool checkStrParamsEmptyAndReturn:showCustomExt];
     
    ATShowConfig *showConfig = [[ATShowConfig alloc] initWithScene:sceneID showCustomExt:showCustomExt];
    
    [[ATAdManager sharedManager] showRewardedVideoWithPlacementID:placementID config:showConfig inViewController:[ATCommonTool currentViewController] delegate:self.rewardedVideoDelegate];
}


/// 是否有广告缓存
- (BOOL)rewardedVideoReady:(NSString *)placementID{
    
    if (kATStringIsEmpty(placementID)) {
        return NO;
    }
    
    BOOL isReady = [[ATAdManager sharedManager] rewardedVideoReadyForPlacementID:placementID];
    return  isReady;
}

/// 检查广告状态
- (NSDictionary *)checkRewardedVideoLoadStatus:(NSString *)placementID {
    
    if (kATStringIsEmpty(placementID)) {
        return [NSDictionary dictionary];
    }
    
    ATCheckLoadModel *checkLoadModel = [[ATAdManager sharedManager] checkRewardedVideoLoadStatusForPlacementID:placementID];

    NSDictionary *dic = [ATCommonTool objectToJSONString:checkLoadModel];
    return  dic;
}

///  获取当前广告位下所有可用广告的信息，v5.7.53及以上版本支持
- (NSString *)getRewardedVideoValidAds:(NSString *)placementID {
    
    if (kATStringIsEmpty(placementID)) {
        return @"";
    }
    
    NSArray *array = [[ATAdManager sharedManager] getRewardedVideoValidAdsForPlacementID:placementID];
    
    NSString *str = [ATCommonTool toReadableJSONString:array];
    
    return str;
}

/// 统计场景到达率
- (void)entryScenarioWithPlacementID:(NSString *)placementID sceneID:(NSString *)sceneID {
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
    
    ATLog(@"entryRewardedVideoScenarioWithPlacementID: %@ --- sceneID:%@",placementID,sceneID);

    [[ATAdManager sharedManager] entryRewardedVideoScenarioWithPlacementID:placementID scene:sceneID];
}
 
/// 设置全自动加载激励视频广告
- (void)autoLoadRewardedVideo:(NSString *)placementID {
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
    
    [ATRewardedVideoAutoAdManager sharedInstance].delegate = self.rewardedVideoDelegate;
    [[ATRewardedVideoAutoAdManager sharedInstance] addAutoLoadAdPlacementIDArray:[placementID componentsSeparatedByString:@","]];
}

/// 取消全自动加载激励视频广告
- (void)cancelAutoLoadRewardedVideo:(NSString *)placementID {
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
    
    [[ATRewardedVideoAutoAdManager sharedInstance] removeAutoLoadAdPlacementIDArray:[placementID componentsSeparatedByString:@","]];
}

/// 展示全自动加载激励视频广告（仅 scenario）
- (void)showAutoLoadRewardedVideoAD:(NSString *)placementID sceneID:(NSString *)sceneID {
    [self showAutoLoadRewardedVideoAD:placementID sceneID:sceneID showCustomExt:@""];
}

/// 展示全自动加载激励视频广告（showConfig）
- (void)showAutoLoadRewardedVideoAD:(NSString *)placementID sceneID:(NSString *)sceneID showCustomExt:(NSString *)showCustomExt {

    if (kATStringIsEmpty(placementID)) {
        return;
    }
    sceneID = [ATCommonTool checkStrParamsEmptyAndReturn:sceneID];
  if (sceneID.length == 0) {
    [[ATRewardedVideoAutoAdManager sharedInstance] showAutoLoadRewardedVideoWithPlacementID:placementID inViewController:[ATCommonTool currentViewController] delegate:self.rewardedVideoDelegate];
  } else {
    showCustomExt = [ATCommonTool checkStrParamsEmptyAndReturn:showCustomExt];
    ATShowConfig *config = [[ATShowConfig alloc] initWithScene:sceneID showCustomExt:showCustomExt];
    [[ATRewardedVideoAutoAdManager sharedInstance] showAutoLoadRewardedVideoWithPlacementID:placementID showConfig:config inViewController:[ATCommonTool currentViewController] delegate:self.rewardedVideoDelegate];
  }

}

/// 设置自动加载激励视频广告回传参数，没传入extra内容可以用于清空
- (void)autoLoadRewardedVideoSetLocalExtra:(NSString *)placementID extraDic:(NSDictionary *)extraDic {
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
     
    NSMutableDictionary *dic = [NSMutableDictionary dictionaryWithDictionary:extraDic];

    if ([extraDic.allKeys containsObject:kATAdLoadingExtraUserDataKeywordKey]) {
        [dic removeObjectForKey:kATAdLoadingExtraUserDataKeywordKey];
        dic[kATAdLoadingExtraMediaExtraKey] = extraDic[kATAdLoadingExtraUserDataKeywordKey];
    }
    if ([extraDic.allKeys containsObject:kATAdLoadingExtraUserID]) {
        [dic removeObjectForKey:kATAdLoadingExtraUserID];
        dic[kATAdLoadingExtraUserIDKey] = extraDic[kATAdLoadingExtraUserID];
    }
     
    [[ATRewardedVideoAutoAdManager sharedInstance] setLocalExtra:dic placementID:placementID];
}
 
#pragma mark - lazy
- (ATRewardVideoDelegate *)rewardedVideoDelegate {

    if (_rewardedVideoDelegate) return _rewardedVideoDelegate;

    ATRewardVideoDelegate *rewardedVideoDelegate = [ATRewardVideoDelegate new];

    return _rewardedVideoDelegate = rewardedVideoDelegate;
}

@end



