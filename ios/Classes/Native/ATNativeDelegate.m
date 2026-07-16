//
//  ATNativeDelegate.m
//  anythink_sdk
//
//  Created by GUO PENG on 2021/7/12.
//

#import "ATNativeDelegate.h"
#import "ATSendSignalManager.h"
#import "ATDisposeDataTool.h"
#import "ATConfiguration.h"

#define NativeCallName  @"NativeCall"

#define NativeAdFailToLoadAD  @"nativeAdFailToLoadAD"
#define NativeAdDidFinishLoading  @"nativeAdDidFinishLoading"
#define NativeAddidClick  @"nativeAdDidClick"
#define NativeAdDidDeepLink  @"nativeAdDidDeepLink"
#define NativeAddidEndPlayingVideo  @"nativeAdDidEndPlayingVideo"
#define NativeAdEnterFullScreenVideo  @"nativeAdEnterFullScreenVideo"
#define NativeAdExitFullScreenVideoInAd  @"nativeAdExitFullScreenVideoInAd"
#define NativeAddidShowNativeAd  @"nativeAdDidShowNativeAd"
#define NativeAddidStartPlayingVideo  @"nativeAdDidStartPlayingVideo"
#define NativeAddidTapCloseButton  @"nativeAdDidTapCloseButton"
#define NativeAdDidCloseDetailInAdView  @"nativeAdDidCloseDetailInAdView"

#define NativeAddidLoadSuccessDraw  @"nativeAdDidLoadSuccessDraw"

#define NativeAdDidMultipleLoaded          @"nativeAdDidMultipleLoaded"

#define NativeAdDidAdSourceBiddingAttempt  @"nativeAdDidAdSourceBiddingAttempt"
#define NativeAdDidAdSourceBiddingFilled   @"nativeAdDidAdSourceBiddingFilled"
#define NativeAdDidAdSourceBiddingFail     @"nativeAdDidAdSourceBiddingFail"
#define NativeAdDidAdSourceAttempt         @"nativeAdDidAdSourceAttempt"
#define NativeAdDidAdSourceLoadFilled      @"nativeAdDidAdSourceLoadFilled"
#define NativeAdDidAdSourceLoadFail        @"nativeAdDidAdSourceLoadFail"

@implementation ATNativeDelegate

#pragma mark - 广告源打印
- (void)didStartLoadingADSourceWithPlacementID:(NSString *)placementID extra:(NSDictionary*)extra {
    ATLog(@"ADSource--AD--Start--ATNativeDelegate::didStartLoadingADSourceWithPlacementID:%@---extra:%@", placementID,extra);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAdDidAdSourceAttempt placementID:placementID extraDic:extra];
    [SendEventManager sendMethod: NativeCallName arguments:dic result:nil];
}

- (void)didFinishLoadingADSourceWithPlacementID:(NSString *)placementID extra:(NSDictionary*)extra {
    ATLog(@"ADSource--AD--Finish--ATNativeDelegate::didFinishLoadingADSourceWithPlacementID:%@---extra:%@", placementID,extra);
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAdDidAdSourceLoadFilled placementID:placementID extraDic:extra];
    [SendEventManager sendMethod: NativeCallName arguments:dic result:nil];
}

- (void)didFailToLoadADSourceWithPlacementID:(NSString*)placementID extra:(NSDictionary*)extra error:(NSError*)error {
    ATLog(@"ADSource--AD--Fail--ATNativeDelegate::didFailToLoadADSourceWithPlacementID:%@---error:%@", placementID,error);
    NSMutableDictionary *dic = [ATDisposeDataTool revampFailCallDic:NativeAdDidAdSourceLoadFail placementID:placementID extraDic:extra error:error];
    [SendEventManager sendMethod: NativeCallName arguments:dic result:nil];
}

#pragma mark - 广告源 级别回调 - 竞价
- (void)didStartBiddingADSourceWithPlacementID:(NSString *)placementID extra:(NSDictionary*)extra {
    ATLog(@"ADSource--bid--Start--ATNativeDelegate::didStartBiddingADSourceWithPlacementID:%@---extra:%@", placementID,extra);
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAdDidAdSourceBiddingAttempt placementID:placementID extraDic:extra];
    [SendEventManager sendMethod: NativeCallName arguments:dic result:nil];
}

- (void)didFinishBiddingADSourceWithPlacementID:(NSString *)placementID extra:(NSDictionary*)extra {
    ATLog(@"ADSource--bid--Finish--ATNativeDelegate::didFinishBiddingADSourceWithPlacementID:%@--extra:%@", placementID,extra);
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAdDidAdSourceBiddingFilled placementID:placementID extraDic:extra];
    [SendEventManager sendMethod: NativeCallName arguments:dic result:nil];
}

- (void)didFailBiddingADSourceWithPlacementID:(NSString*)placementID extra:(NSDictionary*)extra error:(NSError*)error {
    ATLog(@"ADSource--bid--Fail--ATNativeDelegate::didFailBiddingADSourceWithPlacementID:%@--extra:%@--error:%@", placementID, extra, error);
    NSMutableDictionary *dic = [ATDisposeDataTool revampFailCallDic:NativeAdDidAdSourceBiddingFail placementID:placementID extraDic:extra error:error];
    [SendEventManager sendMethod: NativeCallName arguments:dic result:nil];
}

#pragma mark - ATAdMultipleLoadingDelegate
- (void)didFinishMultipleLoadingADWithPlacementID:(NSString *)placementID
                                   requestingInfo:(ATAdRequestingInfo *)requestingInfo {
    ATLog(@"Native ads multiple loaded:ATNativeDelegate::didFinishMultipleLoadingADWithPlacementID:%@--requestingInfo:%@", placementID, requestingInfo);

    NSMutableDictionary *requestingInfoDic = [NSMutableDictionary dictionary];
    if (requestingInfo.biddingAdInfoArrray != nil && requestingInfo.biddingAdInfoArrray.count != 0) {
        [requestingInfoDic setValue:requestingInfo.biddingAdInfoArrray forKey:MultipleLoadBiddingAttemptKey];
    }
    if (requestingInfo.loadingAdInfoArrray != nil && requestingInfo.loadingAdInfoArrray.count != 0) {
        [requestingInfoDic setValue:requestingInfo.loadingAdInfoArrray forKey:MultipleLoadLoadingKey];
    }
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAdDidMultipleLoaded placementID:placementID extraDic:requestingInfoDic];
    [SendEventManager sendMethod: NativeCallName arguments:dic result:nil];
}

#pragma mark - ATNativeADDelegate
// 广告加载失败
- (void)didFailToLoadADWithPlacementID:(NSString *)placementID error:(NSError *)error {
    ATLog(@"Native:ATNativeDelegate::didFailToLoadADWithPlacementID:placementID:%@--error:%@", placementID, error);

    NSMutableDictionary *dic = [ATDisposeDataTool revampFailCallDic:NativeAdFailToLoadAD placementID:placementID extraDic:nil error:error];
    [SendEventManager sendMethod: NativeCallName  arguments:dic result:nil];

    if (self.onNativeLoadFail) { self.onNativeLoadFail(placementID, error); }
}
// 广告加载成功
- (void)didFinishLoadingADWithPlacementID:(NSString *)placementID {
    ATLog(@"Native:ATNativeDelegate::didFinishLoadingADWithPlacementID:%@", placementID);


    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic: NativeAdDidFinishLoading placementID:placementID extraDic:nil];

    [SendEventManager sendMethod:NativeCallName arguments:dic result:nil];
    if (self.onNativeLoaded) { self.onNativeLoaded(placementID); }
}
// 广告点击
- (void)didClickNativeAdInAdView:(ATNativeADView *)adView placementID:(NSString *)placementID extra:(NSDictionary *)extra {
    ATLog(@"Native:ATNativeDelegate::didClickNativeAdInAdView:placementID:%@--extra:%@--adView:%@", placementID, extra, adView);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAddidClick placementID:placementID extraDic:extra];
    
    [SendEventManager sendMethod: NativeCallName  arguments:dic result:nil];
    
}
// 广告点击跳转是否为Deeplink形式，目前只针对TopOn Adx的广告返回
- (void)didDeepLinkOrJumpInAdView:(ATNativeADView *)adView placementID:(NSString *)placementID extra:(NSDictionary *)extra result:(BOOL)success {
    ATLog(@"Native:ATNativeDelegate::didDeepLinkOrJumpInAdView:placementID:%@--extra:%@--result:%@--adView:%@", placementID, extra, @(success), adView);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAdDidDeepLink placementID:placementID extraDic:extra];
    dic[DeepLink] = @(success);
    [SendEventManager sendMethod: NativeCallName  arguments:dic result:nil];

}
// 广告视频结束播放，部分广告平台有此回调
- (void)didEndPlayingVideoInAdView:(ATNativeADView *)adView placementID:(NSString *)placementID extra:(NSDictionary *)extra {
    ATLog(@"Native:ATNativeDelegate::didEndPlayingVideoInAdView:placementID:%@--extra:%@--adView:%@", placementID, extra, adView);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAddidEndPlayingVideo placementID:placementID extraDic:extra];
    
    [SendEventManager sendMethod: NativeCallName  arguments:dic result:nil];

}
// 广告进入全屏播放
- (void)didEnterFullScreenVideoInAdView:(ATNativeADView *)adView placementID:(NSString *)placementID extra:(NSDictionary *)extra {
    ATLog(@"Native:ATNativeDelegate::didEnterFullScreenVideoInAdView:placementID:%@--extra:%@--adView:%@", placementID, extra, adView);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAdEnterFullScreenVideo placementID:placementID extraDic:extra];
    
    [SendEventManager sendMethod: NativeCallName  arguments:dic result:nil];

}
// 离开全屏播放
- (void)didExitFullScreenVideoInAdView:(ATNativeADView *)adView placementID:(NSString *)placementID extra:(NSDictionary *)extra {
    ATLog(@"Native:ATNativeDelegate::didExitFullScreenVideoInAdView:placementID:%@--extra:%@--adView:%@", placementID, extra, adView);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAdExitFullScreenVideoInAd placementID:placementID extraDic:extra];
    
    [SendEventManager sendMethod: NativeCallName arguments:dic result:nil];

}

// 广告展示成功
- (void)didShowNativeAdInAdView:(ATNativeADView *)adView placementID:(NSString *)placementID extra:(NSDictionary *)extra {
    ATLog(@"Native:ATNativeDelegate::didShowNativeAdInAdView:placementID:%@--extra:%@--adView:%@", placementID, extra, adView);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAddidShowNativeAd placementID:placementID extraDic:extra];
    
    [SendEventManager sendMethod: NativeCallName arguments:dic result:nil];

}
// 广告视频开始播放，部分广告平台有此回调
- (void)didStartPlayingVideoInAdView:(ATNativeADView *)adView placementID:(NSString *)placementID extra:(NSDictionary *)extra {
    ATLog(@"Native:ATNativeDelegate::didStartPlayingVideoInAdView:placementID:%@--extra:%@--adView:%@", placementID, extra, adView);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAddidStartPlayingVideo placementID:placementID extraDic:extra];
    
    [SendEventManager sendMethod: NativeCallName arguments:dic result:nil];

}
// 广告关闭按钮被点击，部分广告平台有此回调
- (void)didTapCloseButtonInAdView:(ATNativeADView *)adView placementID:(NSString *)placementID extra:(NSDictionary *)extra {
    ATLog(@"Native:ATNativeDelegate::didTapCloseButtonInAdView:placementID:%@--extra:%@--adView:%@", placementID, extra, adView);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAddidTapCloseButton placementID:placementID extraDic:extra];
    
    [SendEventManager sendMethod: NativeCallName  arguments:dic result:nil];

}

- (void)didLoadSuccessDrawWith:(NSArray *)views placementID:(NSString *)placementID extra:(NSDictionary *)extra {
    ATLog(@"Native:ATNativeDelegate::didLoadSuccessDrawWith:placementID:%@--extra:%@--views:%@", placementID, extra, views);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAddidLoadSuccessDraw placementID:placementID extraDic:extra];
    [SendEventManager sendMethod: NativeCallName  arguments:dic result:nil];
}

- (void)didCloseDetailInAdView:(ATNativeADView *)adView placementID:(NSString *)placementID extra:(NSDictionary *)extra {
    ATLog(@"Native:ATNativeDelegate::didCloseDetailInAdView:placementID:%@--extra:%@--adView:%@", placementID, extra, adView);
    
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAdDidCloseDetailInAdView placementID:placementID extraDic:extra];
    
    [SendEventManager sendMethod: NativeCallName  arguments:dic result:nil];
    
}
@end
