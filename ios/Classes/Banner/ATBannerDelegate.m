//
//  ATBannerDelegate.m
//  anythink_sdk
//
//  Created by GUO PENG on 2021/7/12.
//

#import "ATBannerDelegate.h"
#import "ATDisposeDataTool.h"
#import "ATSendSignalManager.h"
#import "ATConfiguration.h"


#define BannerCallName  @"BannerCall"

#define BannerAdFailToLoadAD  @"bannerAdFailToLoadAD"
#define BannerAdDidFinishLoading  @"bannerAdDidFinishLoading"
#define BannerAdAutoRefreshSucceed  @"bannerAdAutoRefreshSucceed"
#define BannerAdDidClick  @"bannerAdDidClick"
#define BannerAdDidClose  @"bannerAdDidClose"
#define BannerAdDidDeepLink  @"bannerAdDidDeepLink"
#define BannerAdDidShowSucceed  @"bannerAdDidShowSucceed"
#define BannerAdTapCloseButton  @"bannerAdTapCloseButton"
#define BannerAdAutoRefreshFail  @"bannerAdAutoRefreshFail"

#define BannerAdDidMultipleLoaded          @"bannerAdDidMultipleLoaded"

#define BannerAdDidAdSourceBiddingAttempt  @"bannerAdDidAdSourceBiddingAttempt"
#define BannerAdDidAdSourceBiddingFilled   @"bannerAdDidAdSourceBiddingFilled"
#define BannerAdDidAdSourceBiddingFail     @"bannerAdDidAdSourceBiddingFail"
#define BannerAdDidAdSourceAttempt         @"bannerAdDidAdSourceAttempt"
#define BannerAdDidAdSourceLoadFilled      @"bannerAdDidAdSourceLoadFilled"
#define BannerAdDidAdSourceLoadFail        @"bannerAdDidAdSourceLoadFail"

@interface ATBannerDelegate()

@end


@implementation ATBannerDelegate

#pragma mark - 广告源打印
- (void)didStartLoadingADSourceWithPlacementID:(NSString *)placementID extra:(NSDictionary*)extra {
    ATLog(@"ADSource--AD--Start--ATBannerDelegate::didStartLoadingADSourceWithPlacementID:%@---extra:%@", placementID,extra);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:BannerAdDidAdSourceAttempt placementID:placementID extraDic:extra];
    [SendEventManager sendMethod: BannerCallName arguments:dic result:nil];
}

- (void)didFinishLoadingADSourceWithPlacementID:(NSString *)placementID extra:(NSDictionary*)extra {
    ATLog(@"ADSource--AD--Finish--ATBannerDelegate::didFinishLoadingADSourceWithPlacementID:%@---extra:%@", placementID,extra);
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:BannerAdDidAdSourceLoadFilled placementID:placementID extraDic:extra];
    [SendEventManager sendMethod: BannerCallName arguments:dic result:nil];
}

- (void)didFailToLoadADSourceWithPlacementID:(NSString*)placementID extra:(NSDictionary*)extra error:(NSError*)error {
    ATLog(@"ADSource--AD--Fail--ATBannerDelegate::didFailToLoadADSourceWithPlacementID:%@---error:%@", placementID,error);
    NSMutableDictionary *dic = [ATDisposeDataTool revampFailCallDic:BannerAdDidAdSourceLoadFail placementID:placementID extraDic:extra error:error];
    [SendEventManager sendMethod: BannerCallName arguments:dic result:nil];
}

#pragma mark - 广告源 级别回调 - 竞价
- (void)didStartBiddingADSourceWithPlacementID:(NSString *)placementID extra:(NSDictionary*)extra {
    ATLog(@"ADSource--bid--Start--ATBannerDelegate::didStartBiddingADSourceWithPlacementID:%@---extra:%@", placementID,extra);
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:BannerAdDidAdSourceBiddingAttempt placementID:placementID extraDic:extra];
    [SendEventManager sendMethod: BannerCallName arguments:dic result:nil];
}

- (void)didFinishBiddingADSourceWithPlacementID:(NSString *)placementID extra:(NSDictionary*)extra {
    ATLog(@"ADSource--bid--Finish--ATBannerDelegate::didFinishBiddingADSourceWithPlacementID:%@--extra:%@", placementID,extra);
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:BannerAdDidAdSourceBiddingFilled placementID:placementID extraDic:extra];
    [SendEventManager sendMethod: BannerCallName arguments:dic result:nil];
}

- (void)didFailBiddingADSourceWithPlacementID:(NSString*)placementID extra:(NSDictionary*)extra error:(NSError*)error {
    ATLog(@"ADSource--bid--Fail--ATBannerDelegate::didFailBiddingADSourceWithPlacementID:%@--extra:%@--error:%@", placementID, extra, error);
    NSMutableDictionary *dic = [ATDisposeDataTool revampFailCallDic:BannerAdDidAdSourceBiddingFail placementID:placementID extraDic:extra error:error];
    [SendEventManager sendMethod: BannerCallName arguments:dic result:nil];
}

#pragma mark - ATAdMultipleLoadingDelegate
- (void)didFinishMultipleLoadingADWithPlacementID:(NSString *)placementID
                                   requestingInfo:(ATAdRequestingInfo *)requestingInfo {
    ATLog(@"Banner ads multiple loaded:ATBannerDelegate::didFinishMultipleLoadingADWithPlacementID:%@--requestingInfo:%@", placementID, requestingInfo);

    NSMutableDictionary *requestingInfoDic = [NSMutableDictionary dictionary];
    if (requestingInfo.biddingAdInfoArrray != nil && requestingInfo.biddingAdInfoArrray.count != 0) {
        [requestingInfoDic setValue:requestingInfo.biddingAdInfoArrray forKey:MultipleLoadBiddingAttemptKey];
    }
    if (requestingInfo.loadingAdInfoArrray != nil && requestingInfo.loadingAdInfoArrray.count != 0) {
        [requestingInfoDic setValue:requestingInfo.loadingAdInfoArrray forKey:MultipleLoadLoadingKey];
    }
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:BannerAdDidMultipleLoaded placementID:placementID extraDic:requestingInfoDic];
    [SendEventManager sendMethod: BannerCallName arguments:dic result:nil];
}
 
#pragma mark - ATBannerDelegate
// 横幅广告加载失败
- (void)didFailToLoadADWithPlacementID:(NSString *)placementID error:(NSError *)error {
    ATLog(@"Banner:ATBannerDelegate::didFailToLoadADWithPlacementID:placementID:%@--error:%@", placementID, error);

    NSMutableDictionary *dic = [ATDisposeDataTool revampFailCallDic:BannerAdFailToLoadAD placementID:placementID extraDic:nil error:error];

    [SendEventManager sendMethod: BannerCallName arguments:dic result:nil];

    if (self.onBannerLoadFail) { self.onBannerLoadFail(placementID, error); }
}
// 横幅广告加载成功
- (void)didFinishLoadingADWithPlacementID:(NSString *)placementID {
    ATLog(@"Banner:ATBannerDelegate::didFinishLoadingADWithPlacementID:%@", placementID);

    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:BannerAdDidFinishLoading placementID:placementID extraDic:nil];

    [SendEventManager sendMethod: BannerCallName arguments:dic result:^(id reslut) {}];
    if (self.onBannerLoaded) { self.onBannerLoaded(placementID); }
}
// 横幅广告自动刷新成功
- (void)bannerView:(ATBannerView *)bannerView didAutoRefreshWithPlacement:(NSString *)placementID extra:(NSDictionary *)extra {
    ATLog(@"Banner:ATBannerDelegate::bannerView:didAutoRefreshWithPlacement:placementID:%@--extra:%@--bannerView:%@", placementID, extra, bannerView);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:BannerAdAutoRefreshSucceed placementID:placementID extraDic:extra];

    [SendEventManager sendMethod: BannerCallName arguments:dic result:nil];
    
}
// 横幅广告点击
- (void)bannerView:(ATBannerView *)bannerView didClickWithPlacementID:(NSString *)placementID extra:(NSDictionary *)extra {
    ATLog(@"Banner:ATBannerDelegate::bannerView:didClickWithPlacementID:placementID:%@--extra:%@--bannerView:%@", placementID, extra, bannerView);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:BannerAdDidClick placementID:placementID extraDic:extra];
    
    [SendEventManager sendMethod: BannerCallName arguments:dic result:nil];
}

// 横幅广告点击跳转是否为Deeplink形式，目前只针对TopOn Adx的广告返回
- (void)bannerView:(ATBannerView *)bannerView didDeepLinkOrJumpForPlacementID:(NSString *)placementID extra:(NSDictionary *)extra result:(BOOL)success {
    ATLog(@"Banner:ATBannerDelegate::bannerView:didDeepLinkOrJumpForPlacementID:placementID:%@--extra:%@--result:%@--bannerView:%@", placementID, extra, @(success), bannerView);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:BannerAdDidDeepLink placementID:placementID extraDic:extra];
    dic[DeepLink] = @(success);

    [SendEventManager sendMethod: BannerCallName arguments:dic result:nil];
    
}
// 横幅广告展示成功
- (void)bannerView:(ATBannerView *)bannerView didShowAdWithPlacementID:(NSString *)placementID extra:(NSDictionary *)extra {
    ATLog(@"Banner:ATBannerDelegate::bannerView:didShowAdWithPlacementID:placementID:%@--extra:%@--bannerView:%@", placementID, extra, bannerView);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:BannerAdDidShowSucceed placementID:placementID extraDic:extra];
    [SendEventManager sendMethod: BannerCallName arguments:dic result:nil];
    [SendEventManager sendMethod:CommonADCallName arguments:[ATDisposeDataTool revampSucceedCallDic:AdShowRevenueCallbackKey placementID:placementID extraDic:extra] result:nil];
}

// 横幅广告中关闭按钮点击
- (void)bannerView:(ATBannerView *)bannerView didTapCloseButtonWithPlacementID:(NSString *)placementID extra:(NSDictionary *)extra {
    ATLog(@"Banner:ATBannerDelegate::bannerView:didTapCloseButtonWithPlacementID:placementID:%@--extra:%@--bannerView:%@", placementID, extra, bannerView);
    NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:BannerAdTapCloseButton placementID:placementID extraDic:extra];


    [SendEventManager sendMethod: BannerCallName arguments:dic result:nil];
    
}
// 横幅广告自动刷新失败
- (void)bannerView:(ATBannerView *)bannerView failedToAutoRefreshWithPlacementID:(NSString *)placementID error:(NSError *)error {
    ATLog(@"Banner:ATBannerDelegate::bannerView:failedToAutoRefreshWithPlacementID:placementID:%@--error:%@--bannerView:%@", placementID, error, bannerView);
    
    NSMutableDictionary *dic = [ATDisposeDataTool revampFailCallDic:BannerAdAutoRefreshFail placementID:placementID extraDic:nil error:error];

    [SendEventManager sendMethod: BannerCallName arguments:dic result:nil];
}


@end
