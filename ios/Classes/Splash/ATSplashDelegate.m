//
//  ATSplashDelegate.m
//  anythink_sdk
//
//  Created by GUO PENG on 2023/9/7.
//

#import "ATSplashDelegate.h"
#import "ATCommonTool.h"
#import "ATSendSignalManager.h"
#import "ATConfiguration.h"
#import "ATDisposeDataTool.h"

#define SplashAdCallName  @"SplashCall"

//#define SplashAdDidFinishLoading  @"splashDidFinishLoading"

#define SplashAdDidFinishLoadingTimeout  @"splashDidFinishLoading"
#define SplashAdDidTimeout  @"splashDidTimeout"
#define SplashAdDidFailToLoad  @"splashDidFailToLoad"
#define SplashAdDidShowSucceed  @"splashDidShowSuccess"
#define SplashAdDidShowFail  @"splashDidShowFailed"
#define SplashAdDidClick  @"splashDidClick"
#define SplashAdDidClose  @"splashDidClose"
#define SplashAdWillClose  @"splashWillClose"

//#define SplashAdZoomOutViewDidClick  @"SplashAdZoomOutViewDidClick"

//#define SplashAdZoomOutViewDidClose  @"splashAdZoomOutViewDidClose"

#define SplashAdDetailsDidClose  @"splashAdDetailsDidClose"
#define SplashAdDidDeepLink  @"splashDidDeepLink"
//#define SplashAdDidCountdown  @"splashAdDidCountdown"

#define SplashDidMultipleLoaded          @"splashDidMultipleLoaded"

#define SplashDidAdSourceBiddingAttempt  @"splashDidAdSourceBiddingAttempt"
#define SplashDidAdSourceBiddingFilled   @"splashDidAdSourceBiddingFilled"
#define SplashDidAdSourceBiddingFail     @"splashDidAdSourceBiddingFail"
#define SplashDidAdSourceAttempt         @"splashDidAdSourceAttempt"
#define SplashDidAdSourceLoadFilled      @"splashDidAdSourceLoadFilled"
#define SplashDidAdSourceLoadFail        @"splashDidAdSourceLoadFail"

@implementation ATSplashDelegate

#pragma mark - 广告源打印
- (void)didStartLoadingADSourceWithPlacementID:(NSString *)placementID extra:(NSDictionary*)extra {
    ATLog(@"ADSource--AD--Start--ATSplashDelegate::didStartLoadingADSourceWithPlacementID:%@---extra:%@", placementID,extra);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:SplashDidAdSourceAttempt placementID:placementID extraDic:extra];
    [SendEventManager sendMethod: SplashAdCallName arguments:dic result:nil];
}

- (void)didFinishLoadingADSourceWithPlacementID:(NSString *)placementID extra:(NSDictionary*)extra {
    ATLog(@"ADSource--AD--Finish--ATSplashDelegate::didFinishLoadingADSourceWithPlacementID:%@---extra:%@", placementID,extra);
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:SplashDidAdSourceLoadFilled placementID:placementID extraDic:extra];
    [SendEventManager sendMethod: SplashAdCallName arguments:dic result:nil];
}

- (void)didFailToLoadADSourceWithPlacementID:(NSString*)placementID extra:(NSDictionary*)extra error:(NSError*)error {
    ATLog(@"ADSource--AD--Fail--ATSplashDelegate::didFailToLoadADSourceWithPlacementID:%@---error:%@", placementID,error);
    NSMutableDictionary *dic = [ATDisposeDataTool revampFailCallDic:SplashDidAdSourceLoadFail placementID:placementID extraDic:extra error:error];
    [SendEventManager sendMethod: SplashAdCallName arguments:dic result:nil];
}

#pragma mark - 广告源 级别回调 - 竞价
- (void)didStartBiddingADSourceWithPlacementID:(NSString *)placementID extra:(NSDictionary*)extra {
    ATLog(@"ADSource--bid--Start--ATSplashDelegate::didStartBiddingADSourceWithPlacementID:%@---extra:%@", placementID,extra);
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:SplashDidAdSourceBiddingAttempt placementID:placementID extraDic:extra];
    [SendEventManager sendMethod: SplashAdCallName arguments:dic result:nil];
}

- (void)didFinishBiddingADSourceWithPlacementID:(NSString *)placementID extra:(NSDictionary*)extra {
    ATLog(@"ADSource--bid--Finish--ATSplashDelegate::didFinishBiddingADSourceWithPlacementID:%@--extra:%@", placementID,extra);
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:SplashDidAdSourceBiddingFilled placementID:placementID extraDic:extra];
    [SendEventManager sendMethod: SplashAdCallName arguments:dic result:nil];
}

- (void)didFailBiddingADSourceWithPlacementID:(NSString*)placementID extra:(NSDictionary*)extra error:(NSError*)error {
    ATLog(@"ADSource--bid--Fail--ATSplashDelegate::didFailBiddingADSourceWithPlacementID:%@--extra:%@--error:%@", placementID, extra, error);
    NSMutableDictionary *dic = [ATDisposeDataTool revampFailCallDic:SplashDidAdSourceBiddingFail placementID:placementID extraDic:extra error:error];
    [SendEventManager sendMethod: SplashAdCallName arguments:dic result:nil];
}

#pragma mark - ATAdMultipleLoadingDelegate
- (void)didFinishMultipleLoadingADWithPlacementID:(NSString *)placementID
                                   requestingInfo:(ATAdRequestingInfo *)requestingInfo {
    ATLog(@"Splash ads multiple loaded:ATSplashDelegate::didFinishMultipleLoadingADWithPlacementID:%@--requestingInfo:%@", placementID, requestingInfo);

    NSMutableDictionary *requestingInfoDic = [NSMutableDictionary dictionary];
    if (requestingInfo.biddingAdInfoArrray != nil && requestingInfo.biddingAdInfoArrray.count != 0) {
        [requestingInfoDic setValue:requestingInfo.biddingAdInfoArrray forKey:MultipleLoadBiddingAttemptKey];
    }
    if (requestingInfo.loadingAdInfoArrray != nil && requestingInfo.loadingAdInfoArrray.count != 0) {
        [requestingInfoDic setValue:requestingInfo.loadingAdInfoArrray forKey:MultipleLoadLoadingKey];
    }
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:SplashDidMultipleLoaded placementID:placementID extraDic:requestingInfoDic];
    [SendEventManager sendMethod: SplashAdCallName arguments:dic result:nil];
}

#pragma mark - ATSplashDelegate
// 开屏广告加载成功
- (void)didFinishLoadingADWithPlacementID:(NSString *)placementID {
    ATLog(@"Splash:ATSplashDelegate::didFinishLoadingADWithPlacementID:%@", placementID);
    
//    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:SplashAdDidFinishLoading placementID:placementID extraDic:nil];
//
//    [SendEventManager sendMethod: SplashAdCallName arguments:dic result:nil];
//    ATLog(@"SplashAd ad loaded successfully");
}

// 广告加载失败
- (void)didFailToLoadADWithPlacementID:(NSString*)placementID
                                 error:(NSError*)error {
    ATLog(@"Splash:ATSplashDelegate::didFailToLoadADWithPlacementID:placementID:%@--error:%@", placementID, error);
    NSMutableDictionary *dic = [ATDisposeDataTool revampFailCallDic:SplashAdDidFailToLoad placementID:placementID extraDic:nil error:error];
    
    [SendEventManager sendMethod: SplashAdCallName arguments:dic result:nil];
}


// 加载成功
- (void)didFinishLoadingSplashADWithPlacementID:(NSString *)placementID
                                      isTimeout:(BOOL)isTimeout {
    ATLog(@"Splash:ATSplashDelegate::didFinishLoadingSplashADWithPlacementID:placementID:%@--isTimeout:%@", placementID, @(isTimeout));
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:SplashAdDidFinishLoadingTimeout placementID:placementID extraDic:nil];
    [dic setValue:@(isTimeout) forKey:@"isTimeout"];
    [SendEventManager sendMethod: SplashAdCallName arguments:dic result:nil];
}


// 加载超时
- (void)didTimeoutLoadingSplashADWithPlacementID:(NSString *)placementID {
    ATLog(@"Splash:ATSplashDelegate::didTimeoutLoadingSplashADWithPlacementID:%@", placementID);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampTimeoutCallDic:SplashAdDidTimeout placementID:placementID extraDic:nil];
    [SendEventManager sendMethod: SplashAdCallName arguments:dic result:nil];
}

// 广告展示成功
- (void)splashDidShowForPlacementID:(NSString *)placementID
                              extra:(NSDictionary *)extra {
    ATLog(@"Splash:ATSplashDelegate::splashDidShowForPlacementID:placementID:%@--extra:%@", placementID, extra);
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:SplashAdDidShowSucceed placementID:placementID extraDic:extra];
    [SendEventManager sendMethod: SplashAdCallName arguments:dic result:nil];
    [SendEventManager sendMethod:CommonADCallName arguments:[ATDisposeDataTool revampSucceedCallDic:AdShowRevenueCallbackKey placementID:placementID extraDic:extra] result:nil];
}

// 广告点击
- (void)splashDidClickForPlacementID:(NSString *)placementID
                               extra:(NSDictionary *)extra {
    ATLog(@"Splash:ATSplashDelegate::splashDidClickForPlacementID:placementID:%@--extra:%@", placementID, extra);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:SplashAdDidClick placementID:placementID extraDic:extra];
    [SendEventManager sendMethod: SplashAdCallName arguments:dic result:nil];
}

/// Splash ad will closed
- (void)splashWillCloseForPlacementID:(NSString *)placementID
                                extra:(NSDictionary *)extra {
    ATLog(@"Splash:ATSplashDelegate::splashWillCloseForPlacementID:placementID:%@--extra:%@", placementID, extra);
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:SplashAdWillClose placementID:placementID extraDic:extra];
    [SendEventManager sendMethod: SplashAdCallName arguments:dic result:nil];
}

// 广告关闭
- (void)splashDidCloseForPlacementID:(NSString *)placementID
                               extra:(NSDictionary *)extra {
    ATLog(@"Splash:ATSplashDelegate::splashDidCloseForPlacementID:placementID:%@--extra:%@", placementID, extra);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:SplashAdDidClose placementID:placementID extraDic:extra];
    [SendEventManager sendMethod:SplashAdCallName arguments:dic result:nil];
}

// 展示失败
- (void)splashDidShowFailedForPlacementID:(NSString *)placementID
                                    error:(NSError *)error
                                    extra:(NSDictionary *)extra {
    ATLog(@"Splash:ATSplashDelegate::splashDidShowFailedForPlacementID:placementID:%@--error:%@--extra:%@", placementID, error, extra);

    if (placementID == nil || placementID.length == 0) {
        placementID = @"";
    }

    NSMutableDictionary *dic = [ATDisposeDataTool revampFailCallDic:SplashAdDidShowFail placementID:placementID extraDic:extra error:error];
    [SendEventManager sendMethod:SplashAdCallName arguments:dic result:nil];
}


// DeepLinK
- (void)splashDeepLinkOrJumpForPlacementID:(NSString *)placementID
                                     extra:(NSDictionary *)extra
                                    result:(BOOL)success {
    ATLog(@"Splash:ATSplashDelegate::splashDeepLinkOrJumpForPlacementID:placementID:%@--extra:%@--result:%@", placementID, extra, @(success));
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:SplashAdDidDeepLink placementID:placementID extraDic:extra];
    [SendEventManager sendMethod:SplashAdCallName arguments:dic result:nil];
}

// 详情页关闭
- (void)splashDetailDidClosedForPlacementID:(NSString *)placementID
                                      extra:(NSDictionary *)extra {
    
//    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:SplashAdDetailsDidClose placementID:placementID extraDic:extra];
//    [SendEventManager sendMethod:SplashAdCallName arguments:dic result:nil];
//    ATLog(@"SplashAd ad Detail Close");
}

// 开屏点睛点击
- (void)splashZoomOutViewDidClickForPlacementID:(NSString *)placementID
                                          extra:(NSDictionary *)extra {

//    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:SplashAdZoomOutViewDidClick placementID:placementID extraDic:extra];
//    [SendEventManager sendMethod: SplashAdCallName arguments:dic result:nil];
//    ATLog(@"SplashAd ad  ZoomOutView Click");
}

// 开屏点睛关闭
- (void)splashZoomOutViewDidCloseForPlacementID:(NSString *)placementID
                                          extra:(NSDictionary *)extra {
//    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:SplashAdZoomOutViewDidClose placementID:placementID extraDic:extra];
//    [SendEventManager sendMethod: SplashAdCallName arguments:dic result:nil];
//    ATLog(@"SplashAd ad  ZoomOutView Close");
}

// 开屏倒计时
- (void)splashCountdownTime:(NSInteger)countdown
             forPlacementID:(NSString *)placementID
                      extra:(NSDictionary *)extra {
    
//    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:SplashAdDidCountdown placementID:placementID extraDic:extra];
//    [dic setValue:@(countdown) forKey:@"countdown"];
//    [SendEventManager sendMethod: SplashAdCallName arguments:dic result:nil];
//    ATLog(@"SplashAd ad splashCountdownTime");
//
}

@end
