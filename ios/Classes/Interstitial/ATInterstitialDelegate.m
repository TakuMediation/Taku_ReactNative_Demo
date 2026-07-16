//
//  ATInterstitialDelegate.m
//  anythink_sdk
//
//  Created by GUO PENG on 2021/7/13.
//

#import "ATInterstitialDelegate.h"
#import "ATCommonTool.h"
#import "ATSendSignalManager.h"
#import "ATConfiguration.h"
#import "ATDisposeDataTool.h"

#define InterstitialCallName  @"InterstitialCall"


#define InterstitialAdFailToLoadAD  @"interstitialAdFailToLoadAD"
#define InterstitialAdDidFinishLoading  @"interstitialAdDidFinishLoading"
#define InterstitialAdDidDeepLink  @"interstitialAdDidDeepLink"
#define InterstitialAdDidClick  @"interstitialAdDidClick"
#define InterstitialAdDidClose  @"interstitialAdDidClose"
#define InterstitialAdDidStartPlaying  @"interstitialAdDidStartPlaying"
#define InterstitialAdDidEndPlaying  @"interstitialAdDidEndPlaying"
#define InterstitialDidFailToPlayVideo  @"interstitialDidFailToPlayVideo"
#define InterstitialDidShowSucceed  @"interstitialDidShowSucceed"
#define InterstitialFailedToShow  @"interstitialFailedToShow"

#define InterstitialAdDidMultipleLoaded          @"interstitialAdDidMultipleLoaded"

#define InterstitialAdDidAdSourceBiddingAttempt  @"interstitialAdDidAdSourceBiddingAttempt"
#define InterstitialAdDidAdSourceBiddingFilled   @"interstitialAdDidAdSourceBiddingFilled"
#define InterstitialAdDidAdSourceBiddingFail     @"interstitialAdDidAdSourceBiddingFail"
#define InterstitialAdDidAdSourceAttempt         @"interstitialAdDidAdSourceAttempt"
#define InterstitialAdDidAdSourceLoadFilled      @"interstitialAdDidAdSourceLoadFilled"
#define InterstitialAdDidAdSourceLoadFail        @"interstitialAdDidAdSourceLoadFail"

@implementation ATInterstitialDelegate

#pragma mark - 广告源打印
- (void)didStartLoadingADSourceWithPlacementID:(NSString *)placementID extra:(NSDictionary*)extra {
    ATLog(@"ADSource--AD--Start--ATInterstitialDelegate::didStartLoadingADSourceWithPlacementID:%@---extra:%@", placementID,extra);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:InterstitialAdDidAdSourceAttempt placementID:placementID extraDic:extra];
    [SendEventManager sendMethod: InterstitialCallName arguments:dic result:nil];
}

- (void)didFinishLoadingADSourceWithPlacementID:(NSString *)placementID extra:(NSDictionary*)extra {
    ATLog(@"ADSource--AD--Finish--ATInterstitialDelegate::didFinishLoadingADSourceWithPlacementID:%@---extra:%@", placementID,extra);
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:InterstitialAdDidAdSourceLoadFilled placementID:placementID extraDic:extra];
    [SendEventManager sendMethod: InterstitialCallName arguments:dic result:nil];
}

- (void)didFailToLoadADSourceWithPlacementID:(NSString*)placementID extra:(NSDictionary*)extra error:(NSError*)error {
    ATLog(@"ADSource--AD--Fail--ATInterstitialDelegate::didFailToLoadADSourceWithPlacementID:%@---error:%@", placementID,error);
    NSMutableDictionary *dic = [ATDisposeDataTool revampFailCallDic:InterstitialAdDidAdSourceLoadFail placementID:placementID extraDic:extra error:error];
    [SendEventManager sendMethod: InterstitialCallName arguments:dic result:nil];
}

#pragma mark - 广告源 级别回调 - 竞价
- (void)didStartBiddingADSourceWithPlacementID:(NSString *)placementID extra:(NSDictionary*)extra {
    ATLog(@"ADSource--bid--Start--ATInterstitialDelegate::didStartBiddingADSourceWithPlacementID:%@---extra:%@", placementID,extra);
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:InterstitialAdDidAdSourceBiddingAttempt placementID:placementID extraDic:extra];
    [SendEventManager sendMethod: InterstitialCallName arguments:dic result:nil];
}

- (void)didFinishBiddingADSourceWithPlacementID:(NSString *)placementID extra:(NSDictionary*)extra {
    ATLog(@"ADSource--bid--Finish--ATInterstitialDelegate::didFinishBiddingADSourceWithPlacementID:%@--extra:%@", placementID,extra);
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:InterstitialAdDidAdSourceBiddingFilled placementID:placementID extraDic:extra];
    [SendEventManager sendMethod: InterstitialCallName arguments:dic result:nil];
}

- (void)didFailBiddingADSourceWithPlacementID:(NSString*)placementID extra:(NSDictionary*)extra error:(NSError*)error {
    ATLog(@"ADSource--bid--Fail--ATInterstitialDelegate::didFailBiddingADSourceWithPlacementID:%@--extra:%@--error:%@", placementID, extra, error);
    NSMutableDictionary *dic = [ATDisposeDataTool revampFailCallDic:InterstitialAdDidAdSourceBiddingFail placementID:placementID extraDic:extra error:error];
    [SendEventManager sendMethod: InterstitialCallName arguments:dic result:nil];
}

#pragma mark - ATAdMultipleLoadingDelegate
- (void)didFinishMultipleLoadingADWithPlacementID:(NSString *)placementID
                                   requestingInfo:(ATAdRequestingInfo *)requestingInfo {
    ATLog(@"Interstitial ads multiple loaded:ATInterstitialDelegate::didFinishMultipleLoadingADWithPlacementID:%@--requestingInfo:%@", placementID, requestingInfo);

    NSMutableDictionary *requestingInfoDic = [NSMutableDictionary dictionary];
    if (requestingInfo.biddingAdInfoArrray != nil && requestingInfo.biddingAdInfoArrray.count != 0) {
        [requestingInfoDic setValue:requestingInfo.biddingAdInfoArrray forKey:MultipleLoadBiddingAttemptKey];
    }
    if (requestingInfo.loadingAdInfoArrray != nil && requestingInfo.loadingAdInfoArrray.count != 0) {
        [requestingInfoDic setValue:requestingInfo.loadingAdInfoArrray forKey:MultipleLoadLoadingKey];
    }
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:InterstitialAdDidMultipleLoaded placementID:placementID extraDic:requestingInfoDic];
    [SendEventManager sendMethod: InterstitialCallName arguments:dic result:nil];
}

#pragma mark - ATInterstitialDelegate

// 插屏广告加载失败
- (void)didFailToLoadADWithPlacementID:(NSString *)placementID error:(NSError *)error {
    ATLog(@"Interstitial:ATInterstitialDelegate::didFailToLoadADWithPlacementID:placementID:%@--error:%@", placementID, error);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampFailCallDic:InterstitialAdFailToLoadAD placementID:placementID extraDic:nil error:error];

    [SendEventManager sendMethod: InterstitialCallName arguments:dic result:nil];
}


// 插屏广告加载成功
- (void)didFinishLoadingADWithPlacementID:(NSString *)placementID {
    ATLog(@"Interstitial:ATInterstitialDelegate::didFinishLoadingADWithPlacementID:%@", placementID);
    NSMutableDictionary *dic =  [ATDisposeDataTool revampSucceedCallDic:InterstitialAdDidFinishLoading placementID:placementID extraDic:nil];

    [SendEventManager sendMethod: InterstitialCallName arguments:dic result:nil];
}

// 插屏广告点击跳转是否为Deeplink形式，目前只针对TopOn Adx的广告返回
- (void)interstitialDeepLinkOrJumpForPlacementID:(NSString *)placementID extra:(NSDictionary *)extra result:(BOOL)success {
    ATLog(@"Interstitial:ATInterstitialDelegate::interstitialDeepLinkOrJumpForPlacementID:placementID:%@--extra:%@--result:%@", placementID, extra, @(success));
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:InterstitialAdDidDeepLink placementID:placementID extraDic:extra];
    dic[DeepLink] = @(success);

    [SendEventManager sendMethod: InterstitialCallName arguments:dic result:nil];
}

// 插屏广告点击
- (void)interstitialDidClickForPlacementID:(NSString *)placementID extra:(NSDictionary *)extra {
    ATLog(@"Interstitial:ATInterstitialDelegate::interstitialDidClickForPlacementID:placementID:%@--extra:%@", placementID, extra);
        
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:InterstitialAdDidClick placementID:placementID extraDic:extra];
    
    [SendEventManager sendMethod: InterstitialCallName arguments:dic result:nil];
    
}

// 插屏广告关闭
- (void)interstitialDidCloseForPlacementID:(NSString *)placementID extra:(NSDictionary *)extra {
    ATLog(@"Interstitial:ATInterstitialDelegate::interstitialDidCloseForPlacementID:placementID:%@--extra:%@", placementID, extra);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:InterstitialAdDidClose placementID:placementID extraDic:extra];

    [SendEventManager sendMethod: InterstitialCallName arguments:dic result:nil];
    
}
// 插屏视频广告播放开始
- (void)interstitialDidStartPlayingVideoForPlacementID:(NSString *)placementID extra:(NSDictionary *)extra {
    ATLog(@"Interstitial:ATInterstitialDelegate::interstitialDidStartPlayingVideoForPlacementID:placementID:%@--extra:%@", placementID, extra);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:InterstitialAdDidStartPlaying placementID:placementID extraDic:extra];
    
    [SendEventManager sendMethod: InterstitialCallName arguments:dic result:nil];
}
// 插屏视频广告播放结束
- (void)interstitialDidEndPlayingVideoForPlacementID:(NSString *)placementID extra:(NSDictionary *)extra {
    ATLog(@"Interstitial:ATInterstitialDelegate::interstitialDidEndPlayingVideoForPlacementID:placementID:%@--extra:%@", placementID, extra);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:InterstitialAdDidEndPlaying placementID:placementID extraDic:extra];

    [SendEventManager sendMethod: InterstitialCallName arguments:dic result:nil];
}
// 插屏视频广告播放失败
- (void)interstitialDidFailToPlayVideoForPlacementID:(NSString *)placementID error:(NSError *)error extra:(NSDictionary *)extra {
    ATLog(@"Interstitial:ATInterstitialDelegate::interstitialDidFailToPlayVideoForPlacementID:placementID:%@--error:%@--extra:%@", placementID, error, extra);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampFailCallDic:InterstitialDidFailToPlayVideo placementID:placementID extraDic:extra error:error];
    
    [SendEventManager sendMethod: InterstitialCallName arguments:dic result:nil];
    
}
// 插屏广告展示成功
- (void)interstitialDidShowForPlacementID:(NSString *)placementID extra:(NSDictionary *)extra {
    ATLog(@"Interstitial:ATInterstitialDelegate::interstitialDidShowForPlacementID:placementID:%@--extra:%@", placementID, extra);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:InterstitialDidShowSucceed placementID:placementID extraDic:extra];

    [SendEventManager sendMethod: InterstitialCallName arguments:dic result:nil];
    [SendEventManager sendMethod:CommonADCallName arguments:[ATDisposeDataTool revampSucceedCallDic:AdShowRevenueCallbackKey placementID:placementID extraDic:extra] result:nil];
    
}
// 插屏广告展示失败
- (void)interstitialFailedToShowForPlacementID:(NSString *)placementID error:(NSError *)error extra:(NSDictionary *)extra {
    ATLog(@"Interstitial:ATInterstitialDelegate::interstitialFailedToShowForPlacementID:placementID:%@--error:%@--extra:%@", placementID, error, extra);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampFailCallDic:InterstitialFailedToShow placementID:placementID extraDic:extra error:error];
    
    [SendEventManager sendMethod: InterstitialCallName arguments:dic result:nil];
    
}
@end
