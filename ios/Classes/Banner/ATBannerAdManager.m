//
//  ATBannerAdManager.m
//  topon_flutter_plugin
//
//  Created by GUO PENG on 2021/6/29.
//

#import "ATBannerAdManager.h"
#import <AnyThinkSDK/AnyThinkSDK.h>
#import "ATCommonTool.h"
#import "ATConfiguration.h"
#import "ATBannerDelegate.h"
#import "ATBannerTool.h"

// 针对Admob平台，支持Admob banner广告自适应
//#import <GoogleMobileAds/GoogleMobileAds.h>

static NSString *kATBannerAdLoadingExtraInlineAdaptiveWidthKey = @"adaptive_width";
static NSString *kATBannerAdLoadingExtraInlineAdaptiveOrientationKey = @"adaptive_orientation";

@interface ATBannerAdManager()

@property (nonatomic,strong) NSMutableDictionary *rectDic;

@property (nonatomic,strong) NSMutableDictionary *bannerViewDic;


@property(nonatomic, strong) ATBannerDelegate *bannerDelegate;



@end

@implementation ATBannerAdManager

#pragma mark - public

/// 检查广告位的加载状态（实现模式照搬 ATRewardVideoManager.checkRewardedVideoLoadStatus:）
- (NSDictionary *)checkBannerLoadStatus:(NSString *)placementID {
    ATLog(@"Banner:ATBannerAdManager::checkBannerLoadStatus:placementID:%@", placementID);
    if (kATStringIsEmpty(placementID)) {
        return [NSDictionary dictionary];
    }
    ATCheckLoadModel *checkLoadModel = [[ATAdManager sharedManager] checkBannerLoadStatusForPlacementID:placementID];
    NSDictionary *dic = [ATCommonTool objectToJSONString:checkLoadModel];
    return dic;
}

/// 获取当前广告位下所有可用广告的信息（v5.7.53+ 支持）
- (NSString *)getBannerValidAds:(NSString *)placementID {
    ATLog(@"Banner:ATBannerAdManager::getBannerValidAds:placementID:%@", placementID);
    if (kATStringIsEmpty(placementID)) {
        return @"";
    }
    NSArray *array = [[ATAdManager sharedManager] getBannerValidAdsForPlacementID:placementID];
    NSString *str = [ATCommonTool toReadableJSONString:array];
    return str;
}

/// 加载横幅广告
- (void)loadBannerWith:(NSString *)placementID extraDic:(NSDictionary *)extraDic {
    ATLog(@"Banner:ATBannerAdManager::loadBannerWith:placementID:%@--extraDic:%@", placementID, extraDic);
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
    
    CGRect rect = [ATBannerTool getSizeFromExtraDic:extraDic];
    
    self.rectDic[placementID] = NSStringFromCGRect(rect);
    [ATBannerTool setLoadedSize:CGSizeMake(rect.size.width, rect.size.height) forPlacementID:placementID];
    
    NSMutableDictionary *extraDicMutable = [NSMutableDictionary dictionaryWithDictionary:extraDic];
    [ATCommonTool applyAtAdRequestAndRemove:extraDicMutable];

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
  
/// 用位置和宽高属性来展示横幅广告
- (void)showBannerInRectangle:(NSString *)placementID extraDic:(NSDictionary *)extraDic {
    ATLog(@"Banner:ATBannerAdManager::showBannerInRectangle:extraDic:placementID:%@--extraDic:%@", placementID, extraDic);
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
    
    CGRect rect = [ATBannerTool getSizeFromExtraDic:extraDic];
    
    id showCustomExt = extraDic[@"showCustomExt"];
    NSString *showCustomExtStr = @"";
    if ([showCustomExt isKindOfClass:[NSString class]] && !kATStringIsEmpty(showCustomExt)) {
        showCustomExtStr = (NSString *)showCustomExt;
    }
    
    ATBannerView *bannerView = [ATBannerTool getBannerViewAdRect:rect placementID:placementID sceneID:nil showCustomExt:showCustomExtStr];
    
    [self setBannerViewVlaue:bannerView placementID:placementID];
    
    [self showBanner:bannerView];
    
}

/// 用位置和宽高属性来展示横幅场景广告
- (void)showBannerInRectangle:(NSString *)placementID sceneID:(NSString *)sceneID extraDic:(NSDictionary *)extraDic {
    ATLog(@"Banner:ATBannerAdManager::showBannerInRectangle:sceneID:extraDic:placementID:%@--sceneID:%@--extraDic:%@", placementID, sceneID, extraDic);
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
    
    CGRect rect = [ATBannerTool getSizeFromExtraDic:extraDic];
    
    id showCustomExt = extraDic[@"showCustomExt"];
    NSString *showCustomExtStr = @"";
    if ([showCustomExt isKindOfClass:[NSString class]] && !kATStringIsEmpty(showCustomExt)) {
        showCustomExtStr = (NSString *)showCustomExt;
    }
    
    ATBannerView *bannerView = [ATBannerTool getBannerViewAdRect:rect placementID:placementID sceneID:sceneID showCustomExt:showCustomExtStr];
    
    [self setBannerViewVlaue:bannerView placementID:placementID];

    [self showBanner:bannerView];
}

/// 用预定义的位置来展示横幅广告
- (void)showAdInPosition:(NSString *)placementID position:(NSString *)positionStr {
    ATLog(@"Banner:ATBannerAdManager::showAdInPosition:position:placementID:%@--position:%@", placementID, positionStr);
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
    
    CGRect loadRect =  CGRectFromString(self.rectDic[placementID]);
     
    CGRect rect = [self getInPositionFromloadRect:loadRect positionStr:positionStr];
    
    ATLog(@"Banner:ATBannerAdManager::showAdInPosition:position:rect:%@", NSStringFromCGRect(rect));
    
    ATBannerView *bannerView = [ATBannerTool getBannerViewAdRect:rect placementID:placementID sceneID:nil showCustomExt:nil];
    [self setBannerViewVlaue:bannerView placementID:placementID];
    [self showBanner:bannerView];
}
 
/// 用预定义的位置来展示横幅场景广告
- (void)showAdInPosition:(NSString *)placementID sceneID:(NSString *)sceneID position:(NSString *)positionStr showCustomExt:(NSString *)showCustomExt {
    ATLog(@"Banner:ATBannerAdManager::showAdInPosition:sceneID:position:showCustomExt:placementID:%@--sceneID:%@--position:%@--showCustomExt:%@", placementID, sceneID, positionStr, showCustomExt);
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
    
    CGRect loadRect =  CGRectFromString(self.rectDic[placementID]);
    CGRect rect = [self getInPositionFromloadRect:loadRect positionStr:positionStr];
    ATLog(@"Banner:ATBannerAdManager::showAdInPosition:sceneID:position:showCustomExt:rect:%@", NSStringFromCGRect(rect));
    ATBannerView *bannerView = [ATBannerTool getBannerViewAdRect:rect placementID:placementID sceneID:sceneID showCustomExt:showCustomExt];
    [self setBannerViewVlaue:bannerView placementID:placementID];
    
    [self showBanner:bannerView];
}


/// 移除横幅广告
- (void)removeBannerAd:(NSString *)placementID {
    ATLog(@"Banner:ATBannerAdManager::removeBannerAd:placementID:%@", placementID);
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
    
    if ([self.bannerViewDic.allKeys containsObject:placementID]) {
        
        UIView *tempView = self.bannerViewDic[placementID];
        [tempView removeFromSuperview];
        tempView = nil;
        [self.bannerViewDic removeObjectForKey:placementID];
    }
}

- (void)destroyBannerView:(ATBannerView *)bannerView {
    if (bannerView == nil) {
        return;
    }
    ATLog(@"Banner:ATBannerAdManager::destroyBannerView:%@", bannerView);
    [bannerView destroyBanner];
    [bannerView removeFromSuperview];
}

/// 彻底销毁横幅广告（命令式 load / Fabric 展示共用）
- (void)destroyBannerAd:(NSString *)placementID {
    ATLog(@"Banner:ATBannerAdManager::destroyBannerAd:placementID:%@", placementID);
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
    
    ATBannerView *held = self.bannerViewDic[placementID];
    if (held != nil) {
        [self destroyBannerView:held];
        [self.bannerViewDic removeObjectForKey:placementID];
    }
    
    if ([[ATAdManager sharedManager] bannerAdReadyForPlacementID:placementID]) {
        ATShowConfig *cfg = [[ATShowConfig alloc] initWithScene:@"" showCustomExt:@""];
        ATBannerView *bv = [[ATAdManager sharedManager] retrieveBannerViewForPlacementID:placementID
                                                                                    config:cfg];
        if (bv != nil && bv != held) {
            [self destroyBannerView:bv];
        }
    }
    
    [self.rectDic removeObjectForKey:placementID];
    [ATBannerTool clearPlacementState:placementID];
}

/// 隐藏横幅广告
- (void)hideBannerAd:(NSString *)placementID {
    ATLog(@"Banner:ATBannerAdManager::hideBannerAd:placementID:%@", placementID);
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
    
    if ([self.bannerViewDic.allKeys containsObject:placementID]) {
        UIView *tempView = self.bannerViewDic[placementID];
        tempView.hidden = YES;
    }
}

/// 重新展示横幅广告
- (void)afreshShowBannerAd:(NSString *)placementID {
    ATLog(@"Banner:ATBannerAdManager::afreshShowBannerAd:placementID:%@", placementID);
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
    
    if ([self.bannerViewDic.allKeys containsObject:placementID]) {
        UIView *tempView = self.bannerViewDic[placementID];
        tempView.hidden = NO;
    }
}
 
#pragma mark - private

- (void)showBanner:(ATBannerView *)bannerView {
    ATLog(@"Banner:ATBannerAdManager::showBanner:bannerView:%@", bannerView);
    
    UIView *containerView = [ATCommonTool getRootViewController].view;

    [containerView addSubview:bannerView];

}

- (void)setBannerViewVlaue:(ATBannerView *)bannerView placementID:(NSString *)placementID {
    ATLog(@"Banner:ATBannerAdManager::setBannerViewVlaue:placementID:placementID:%@--bannerView:%@", placementID, bannerView);
    
    if (kATStringIsEmpty(placementID)) {
        return;
    }
    
    bannerView.delegate = self.bannerDelegate;
    bannerView.backgroundColor = [UIColor whiteColor];
    bannerView.presentingViewController = [ATCommonTool getRootViewController];
    
    if (bannerView != nil) {
        self.bannerViewDic[placementID] = bannerView;
    }else {
        ATLog(@"Banner:ATBannerAdManager::setBannerViewVlaue:placementID:retrieveBannerView failed placementID:%@", placementID);
    }
}


- (CGRect)getInPositionFromloadRect:(CGRect)loadRect positionStr:(NSString *)positionStr {
    ATLog(@"Banner:ATBannerAdManager::getInPositionFromloadRect:positionStr:loadRect:%@--positionStr:%@", NSStringFromCGRect(loadRect), positionStr);
    
    CGRect rect = CGRectMake(0, NavBarHeight, loadRect.size.width,loadRect.size.height);
        
    if ([positionStr isEqualToString:@"kATBannerAdShowingPositionTop"]) {
        rect.origin.x = 0;
        rect.origin.y = NavBarHeight + 20;
    }
    
    else if ([positionStr isEqualToString:@"kATBannerAdShowingPositionBottom"]) {
        
        rect.origin.x = 0;
        rect.origin.y = SCREEN_HEIGHT - loadRect.size.height - TabbarSafeBottomMargin;
    }
    return  rect;
}

- (void)entryScenarioWithPlacementID:(NSString *)placementID sceneID:(NSString *)sceneID {
    ATLog(@"Banner:ATBannerAdManager::entryScenarioWithPlacementID:sceneID:placementID:%@--sceneID:%@", placementID, sceneID);
    if (kATStringIsEmpty(placementID)) {
        return;
    }
    
    [[ATAdManager sharedManager] entryBannerScenarioWithPlacementID:placementID scene:sceneID];
}

#pragma mark - lazy
- (NSMutableDictionary *)rectDic {

    if (_rectDic) return _rectDic;

    NSMutableDictionary *rectDic = [NSMutableDictionary new];

    return _rectDic = rectDic;
}

- (NSMutableDictionary *)bannerViewDic {

    if (_bannerViewDic) return _bannerViewDic;

    NSMutableDictionary *bannerViewDic = [NSMutableDictionary new];

    return _bannerViewDic = bannerViewDic;
}

- (ATBannerDelegate *)bannerDelegate {

    if (_bannerDelegate) return _bannerDelegate;

    ATBannerDelegate *bannerDelegate = [ATBannerDelegate new];

    return _bannerDelegate = bannerDelegate;
}

 
@end
