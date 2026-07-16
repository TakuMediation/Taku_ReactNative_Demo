//
//  ATRewardVideoDelegate.m
//  anythink_sdk
//
//  Created by GUO PENG on 2021/7/13.
//

#import "ATRewardVideoDelegate.h"
#import "ATCommonTool.h"
#import "ATSendSignalManager.h"
#import "ATConfiguration.h"
#import "ATDisposeDataTool.h"


#define RewardedVideoCallName  @"RewardedVideoCall"


#define RewardedVideoDidFinishLoading  @"rewardedVideoDidFinishLoading"
#define RewardedVideoDidFailToLoad  @"rewardedVideoDidFailToLoad"
#define RewardedVideoDidStartPlaying  @"rewardedVideoDidStartPlaying"
#define RewardedVideoDidEndPlaying  @"rewardedVideoDidEndPlaying"
#define RewardedVideoDidRewardSuccess  @"rewardedVideoDidRewardSuccess"
#define RewardedVideoDidClick  @"rewardedVideoDidClick"
#define RewardedVideoDidClose  @"rewardedVideoDidClose"
#define RewardedVideoDidDeepLink  @"rewardedVideoDidDeepLink"
#define RewardedVideoDidAgainStartPlaying  @"rewardedVideoDidAgainStartPlaying"
#define RewardedVideoDidAgainEndPlaying  @"rewardedVideoDidAgainEndPlaying"
#define RewardedVideoDidAgainFailToPlay  @"rewardedVideoDidAgainFailToPlay"
#define RewardedVideoDidAgainClick  @"rewardedVideoDidAgainClick"
#define RewardedVideoDidAgainRewardSuccess  @"rewardedVideoDidAgainRewardSuccess"

#define RewardedVideoDidMultipleLoaded          @"rewardedVideoDidMultipleLoaded"

#define RewardedVideoDidAdSourceBiddingAttempt  @"rewardedVideoDidAdSourceBiddingAttempt"
#define RewardedVideoDidAdSourceBiddingFilled   @"rewardedVideoDidAdSourceBiddingFilled"
#define RewardedVideoDidAdSourceBiddingFail     @"rewardedVideoDidAdSourceBiddingFail"
#define RewardedVideoDidAdSourceAttempt         @"rewardedVideoDidAdSourceAttempt"
#define RewardedVideoDidAdSourceLoadFilled      @"rewardedVideoDidAdSourceLoadFilled"
#define RewardedVideoDidAdSourceLoadFail        @"rewardedVideoDidAdSourceLoadFail"

@implementation ATRewardVideoDelegate

#pragma mark - 广告源打印
- (void)didStartLoadingADSourceWithPlacementID:(NSString *)placementID extra:(NSDictionary*)extra {
    ATLog(@"ADSource--AD--Start--ATRewardVideoDelegate::didStartLoadingADSourceWithPlacementID:%@---extra:%@", placementID,extra);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:RewardedVideoDidAdSourceAttempt placementID:placementID extraDic:extra];
    [SendEventManager sendMethod: RewardedVideoCallName arguments:dic result:nil];
}

- (void)didFinishLoadingADSourceWithPlacementID:(NSString *)placementID extra:(NSDictionary*)extra {
    ATLog(@"ADSource--AD--Finish--ATRewardVideoDelegate::didFinishLoadingADSourceWithPlacementID:%@---extra:%@", placementID,extra);
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:RewardedVideoDidAdSourceLoadFilled placementID:placementID extraDic:extra];
    [SendEventManager sendMethod: RewardedVideoCallName arguments:dic result:nil];
}

- (void)didFailToLoadADSourceWithPlacementID:(NSString*)placementID extra:(NSDictionary*)extra error:(NSError*)error {
    ATLog(@"ADSource--AD--Fail--ATRewardVideoDelegate::didFailToLoadADSourceWithPlacementID:%@---error:%@", placementID,error);
    NSMutableDictionary *dic = [ATDisposeDataTool revampFailCallDic:RewardedVideoDidAdSourceLoadFail placementID:placementID extraDic:extra error:error];
    [SendEventManager sendMethod: RewardedVideoCallName arguments:dic result:nil];
}

#pragma mark - 广告源 级别回调 - 竞价
- (void)didStartBiddingADSourceWithPlacementID:(NSString *)placementID extra:(NSDictionary*)extra {
    ATLog(@"ADSource--bid--Start--ATRewardVideoDelegate::didStartBiddingADSourceWithPlacementID:%@---extra:%@", placementID,extra);
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:RewardedVideoDidAdSourceBiddingAttempt placementID:placementID extraDic:extra];
    [SendEventManager sendMethod: RewardedVideoCallName arguments:dic result:nil];
}

- (void)didFinishBiddingADSourceWithPlacementID:(NSString *)placementID extra:(NSDictionary*)extra {
    ATLog(@"ADSource--bid--Finish--ATRewardVideoDelegate::didFinishBiddingADSourceWithPlacementID:%@--extra:%@", placementID,extra);
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:RewardedVideoDidAdSourceBiddingFilled placementID:placementID extraDic:extra];
    [SendEventManager sendMethod: RewardedVideoCallName arguments:dic result:nil];
}

- (void)didFailBiddingADSourceWithPlacementID:(NSString*)placementID extra:(NSDictionary*)extra error:(NSError*)error {
    ATLog(@"ADSource--bid--Fail--ATRewardVideoDelegate::didFailBiddingADSourceWithPlacementID:%@--extra:%@--error:%@", placementID, extra, error);
    NSMutableDictionary *dic = [ATDisposeDataTool revampFailCallDic:RewardedVideoDidAdSourceBiddingFail placementID:placementID extraDic:extra error:error];
    [SendEventManager sendMethod: RewardedVideoCallName arguments:dic result:nil];
}

#pragma mark - ATAdMultipleLoadingDelegate
- (void)didFinishMultipleLoadingADWithPlacementID:(NSString *)placementID
                                   requestingInfo:(ATAdRequestingInfo *)requestingInfo {
    ATLog(@"RewardVideo ads multiple loaded:ATRewardVideoDelegate::didFinishMultipleLoadingADWithPlacementID:%@--requestingInfo:%@", placementID, requestingInfo);

    NSMutableDictionary *requestingInfoDic = [NSMutableDictionary dictionary];
    if (requestingInfo.biddingAdInfoArrray != nil && requestingInfo.biddingAdInfoArrray.count != 0) {
        [requestingInfoDic setValue:requestingInfo.biddingAdInfoArrray forKey:MultipleLoadBiddingAttemptKey];
    }
    if (requestingInfo.loadingAdInfoArrray != nil && requestingInfo.loadingAdInfoArrray.count != 0) {
        [requestingInfoDic setValue:requestingInfo.loadingAdInfoArrray forKey:MultipleLoadLoadingKey];
    }
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:RewardedVideoDidMultipleLoaded placementID:placementID extraDic:requestingInfoDic];
    [SendEventManager sendMethod: RewardedVideoCallName arguments:dic result:nil];
}

#pragma mark - ATRewardedVideoDelegate
// 激励视频广告加载成功
- (void)didFinishLoadingADWithPlacementID:(NSString *)placementID {
    ATLog(@"RewardVideo:ATRewardVideoDelegate::didFinishLoadingADWithPlacementID:%@", placementID);
    
   NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:RewardedVideoDidFinishLoading placementID:placementID extraDic:nil];

    [SendEventManager sendMethod: RewardedVideoCallName arguments:dic result:nil];
}

// 激励视频广告加载失败
- (void)didFailToLoadADWithPlacementID:(NSString *)placementID error:(NSError *)error {
    ATLog(@"RewardVideo:ATRewardVideoDelegate::didFailToLoadADWithPlacementID:placementID:%@--error:%@", placementID, error);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampFailCallDic:RewardedVideoDidFailToLoad placementID:placementID extraDic:nil error:error];

    [SendEventManager sendMethod: RewardedVideoCallName arguments:dic result:nil];
}

// 激励视频广告播放开始
- (void)rewardedVideoDidStartPlayingForPlacementID:(NSString *)placementID extra:(NSDictionary *)extra {
    ATLog(@"RewardVideo:ATRewardVideoDelegate::rewardedVideoDidStartPlayingForPlacementID:placementID:%@--extra:%@", placementID, extra);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:RewardedVideoDidStartPlaying placementID:placementID extraDic:extra];

    [SendEventManager sendMethod: RewardedVideoCallName arguments:dic result:nil];
    [SendEventManager sendMethod:CommonADCallName arguments:[ATDisposeDataTool revampSucceedCallDic:AdShowRevenueCallbackKey placementID:placementID extraDic:extra] result:nil];
}

// 激励视频广告播放结束
- (void)rewardedVideoDidEndPlayingForPlacementID:(NSString *)placementID extra:(NSDictionary *)extra {
    ATLog(@"RewardVideo:ATRewardVideoDelegate::rewardedVideoDidEndPlayingForPlacementID:placementID:%@--extra:%@", placementID, extra);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:RewardedVideoDidEndPlaying placementID:placementID extraDic:extra];

    [SendEventManager sendMethod: RewardedVideoCallName arguments:dic result:nil];
}

// 激励视频广告播放失败
- (void)rewardedVideoDidFailToPlayForPlacementID:(NSString *)placementID error:(NSError *)error extra:(NSDictionary *)extra {
    ATLog(@"RewardVideo:ATRewardVideoDelegate::rewardedVideoDidFailToPlayForPlacementID:placementID:%@--error:%@--extra:%@", placementID, error, extra);

    NSMutableDictionary *dic = [ATDisposeDataTool revampFailCallDic:RewardedVideoDidFailToLoad placementID:placementID extraDic:extra error:error];
    
    [SendEventManager sendMethod: RewardedVideoCallName arguments:dic result:nil];
}

// 激励视频广告奖励下发
- (void)rewardedVideoDidRewardSuccessForPlacemenID:(NSString *)placementID extra:(NSDictionary *)extra {
    ATLog(@"RewardVideo:ATRewardVideoDelegate::rewardedVideoDidRewardSuccessForPlacemenID:placementID:%@--extra:%@", placementID, extra);

    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:RewardedVideoDidRewardSuccess placementID:placementID extraDic:extra];
    
    [SendEventManager sendMethod: RewardedVideoCallName arguments:dic result:nil];
}



// 激励视频广告点击
- (void)rewardedVideoDidClickForPlacementID:(NSString *)placementID extra:(NSDictionary *)extra {
    ATLog(@"RewardVideo:ATRewardVideoDelegate::rewardedVideoDidClickForPlacementID:placementID:%@--extra:%@", placementID, extra);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:RewardedVideoDidClick placementID:placementID extraDic:extra];
    
    [SendEventManager sendMethod: RewardedVideoCallName arguments:dic result:nil];
}

// 激励视频广告关闭
- (void)rewardedVideoDidCloseForPlacementID:(NSString *)placementID rewarded:(BOOL)rewarded extra:(NSDictionary *)extra {
    ATLog(@"RewardVideo:ATRewardVideoDelegate::rewardedVideoDidCloseForPlacementID:placementID:%@--rewarded:%@--extra:%@", placementID, @(rewarded), extra);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:RewardedVideoDidClose placementID:placementID extraDic:extra];

    [SendEventManager sendMethod: RewardedVideoCallName arguments:dic result:nil];
}

// 激励视频广告点击跳转是否为Deeplink形式，目前只针对TopOn Adx的广告返回
- (void)rewardedVideoDidDeepLinkOrJumpForPlacementID:(NSString *)placementID extra:(NSDictionary *)extra result:(BOOL)success {
    ATLog(@"RewardVideo:ATRewardVideoDelegate::rewardedVideoDidDeepLinkOrJumpForPlacementID:placementID:%@--extra:%@--result:%@", placementID, extra, @(success));
    
    NSMutableDictionary *dic  = [ATDisposeDataTool revampSucceedCallDic:RewardedVideoDidDeepLink placementID:placementID extraDic:extra];
    dic[DeepLink] = @(success);
    [SendEventManager sendMethod: RewardedVideoCallName arguments:dic result:nil];
}

// 激励视频再看一次广告播放开始
- (void)rewardedVideoAgainDidStartPlayingForPlacementID:(NSString *)placementID extra:(NSDictionary *)extra {
    ATLog(@"RewardVideo:ATRewardVideoDelegate::rewardedVideoAgainDidStartPlayingForPlacementID:placementID:%@--extra:%@", placementID, extra);

    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:RewardedVideoDidAgainStartPlaying placementID:placementID extraDic:extra];
    
    [SendEventManager sendMethod: RewardedVideoCallName arguments:dic result:nil];
}

// 激励视频再看一次广告播放结束
- (void)rewardedVideoAgainDidEndPlayingForPlacementID:(NSString *)placementID extra:(NSDictionary *)extra {
    ATLog(@"RewardVideo:ATRewardVideoDelegate::rewardedVideoAgainDidEndPlayingForPlacementID:placementID:%@--extra:%@", placementID, extra);

    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:RewardedVideoDidAgainEndPlaying placementID:placementID extraDic:extra];
    
    [SendEventManager sendMethod: RewardedVideoCallName arguments:dic result:nil];
}

// 激励视频再看一次广告播放失败
- (void)rewardedVideoAgainDidFailToPlayForPlacementID:(NSString *)placementID error:(NSError *)error extra:(NSDictionary *)extra {
    ATLog(@"RewardVideo:ATRewardVideoDelegate::rewardedVideoAgainDidFailToPlayForPlacementID:placementID:%@--error:%@--extra:%@", placementID, error, extra);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:RewardedVideoDidAgainFailToPlay placementID:placementID extraDic:extra];
    
    [SendEventManager sendMethod: RewardedVideoCallName arguments:dic result:nil];
}

// 激励视频再看一次广告点击
- (void)rewardedVideoAgainDidClickForPlacementID:(NSString *)placementID extra:(NSDictionary *)extra {
    ATLog(@"RewardVideo:ATRewardVideoDelegate::rewardedVideoAgainDidClickForPlacementID:placementID:%@--extra:%@", placementID, extra);

    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:RewardedVideoDidAgainClick placementID:placementID extraDic:extra];
    
    [SendEventManager sendMethod: RewardedVideoCallName arguments:dic result:nil];
}

// 激励视频再看一次广告奖励下发
- (void)rewardedVideoAgainDidRewardSuccessForPlacemenID:(NSString *)placementID extra:(NSDictionary *)extra {
    ATLog(@"RewardVideo:ATRewardVideoDelegate::rewardedVideoAgainDidRewardSuccessForPlacemenID:placementID:%@--extra:%@", placementID, extra);

    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:RewardedVideoDidAgainRewardSuccess placementID:placementID extraDic:extra];
    
    [SendEventManager sendMethod: RewardedVideoCallName arguments:dic result:nil];
}


@end
