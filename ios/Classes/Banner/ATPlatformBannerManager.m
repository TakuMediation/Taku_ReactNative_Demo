//
//  ATPlatformBannerManager.m
//  anythink_sdk
//
//  Created by GUO PENG on 2021/7/12.
//

#import "ATPlatformBannerManager.h"
#import <AnyThinkSDK/AnyThinkSDK.h>
#import "ATCommonTool.h"
#import "ATConfiguration.h"
#import "ATBannerDelegate.h"
#import "ATBannerTool.h"
//5.6.6版本以上支持 admob 自适应banner （用到时再import该头文件）
//#import <GoogleMobileAds/GoogleMobileAds.h>

static NSString *kATBannerAdLoadingExtraInlineAdaptiveWidthKey = @"adaptive_width";
static NSString *kATBannerAdLoadingExtraInlineAdaptiveOrientationKey = @"adaptive_orientation";

@interface ATPlatformBannerManager()

@property(nonatomic, strong) ATBannerDelegate *bannerDelegate;

@end

@implementation ATPlatformBannerManager

#pragma mark - public

/// 同步取已加载好的 SDK Banner UIView 加进 hostView。
/// 渲染路径与 Flutter `ATFBannerPlatformView.m` 一致：
///   ATBannerView * bv = [[ATAdManager sharedManager] retrieveBannerViewForPlacementID:config:];
///   bv.delegate / backgroundColor / presentingViewController；返回给容器作 subview。
- (BOOL)attachBannerToHostView:(UIView *)hostView
                   placementID:(NSString *)placementID
                       sceneID:(NSString *)sceneID {
    if (hostView == nil || placementID.length == 0) {
        return NO;
    }
    ATShowConfig *cfg = [[ATShowConfig alloc] initWithScene:(sceneID ?: @"") showCustomExt:@""];
    ATBannerView *bv = [[ATAdManager sharedManager] retrieveBannerViewForPlacementID:placementID config:cfg];
    if (bv == nil) {
        ATLog(@"ATPlatformBannerManager.attachBannerToHostView retrieveBannerView=nil (not loaded yet), placementID=%@",
              placementID);
        return NO;
    }
    bv.delegate = self.bannerDelegate;
    bv.backgroundColor = [UIColor whiteColor];
    bv.presentingViewController = [ATCommonTool getRootViewController];
    bv.frame = hostView.bounds;
    bv.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    [hostView addSubview:bv];
    ATLog(@"ATPlatformBannerManager.attachBannerToHostView attached placementID=%@ bv=%@", placementID, bv);
    return YES;
}

/// 加载横幅广告
- (void)loadBannerWith:(NSString *)placementID extraDic:(NSDictionary *)extraDic{
    
    CGRect rect = [ATBannerTool getSizeFromExtraDic:extraDic];
    NSValue *adSize = [NSValue valueWithCGSize:CGSizeMake(rect.size.width,  rect.size.height)];
    [[ATAdManager sharedManager] setMultipleLoadingDelegate:self.bannerDelegate placementId:placementID];

    if (extraDic[kATBannerAdLoadingExtraInlineAdaptiveWidthKey] != nil && extraDic[kATBannerAdLoadingExtraInlineAdaptiveOrientationKey] != nil) {

        [[ATAdManager sharedManager] loadADWithPlacementID:placementID extra:@{

            kATAdLoadingExtraBannerAdSizeKey:adSize,
            
            kATAdLoadingExtraAdmobBannerSizeKey:adSize,
            kATAdLoadingExtraAdmobAdSizeFlagsKey:@(YES)

        } delegate:self.bannerDelegate];
        
    } else {
        [[ATAdManager sharedManager] loadADWithPlacementID:placementID extra:@{kATAdLoadingExtraBannerAdSizeKey:adSize} delegate:self.bannerDelegate];
    }
}



#pragma mark - lazy
- (ATBannerDelegate *)bannerDelegate {

    if (_bannerDelegate) return _bannerDelegate;

    ATBannerDelegate *bannerDelegate = [ATBannerDelegate new];

    return _bannerDelegate = bannerDelegate;
}
@end
