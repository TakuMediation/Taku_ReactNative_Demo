#import "ATBannerViewManager.h"
#import "ATRNBannerView.h"

@implementation ATBannerViewManager

RCT_EXPORT_MODULE(ATBannerView)

RCT_EXPORT_VIEW_PROPERTY(placementID, NSString)
RCT_EXPORT_VIEW_PROPERTY(isAdaptiveHeight, BOOL)
RCT_EXPORT_VIEW_PROPERTY(adWidth, NSInteger)
RCT_EXPORT_VIEW_PROPERTY(adHeight, NSInteger)

+ (BOOL)requiresMainQueueSetup {
    return YES;
}

- (UIView *)view {
    return [[ATRNBannerView alloc] init];
}

@end
