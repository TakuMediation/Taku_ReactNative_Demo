#import "ATNativeAdViewManager.h"
#import "ATRNNativeAdView.h"
#import "ATConfiguration.h"
#import <React/RCTUIManager.h>

@implementation ATNativeAdViewManager

RCT_EXPORT_MODULE(ATNativeAdView)

RCT_EXPORT_VIEW_PROPERTY(placementID, NSString)
RCT_EXPORT_VIEW_PROPERTY(adId, NSString)
RCT_EXPORT_VIEW_PROPERTY(isAdaptiveHeight, BOOL)
RCT_EXPORT_VIEW_PROPERTY(adWidth, NSInteger)
RCT_EXPORT_VIEW_PROPERTY(adHeight, NSInteger)

+ (BOOL)requiresMainQueueSetup {
    return YES;
}

- (UIView *)view {
    return [[ATRNNativeAdView alloc] init];
}

RCT_EXPORT_METHOD(updateAssetView:(nonnull NSNumber *)viewTag
                  assetViewTag:(NSInteger)assetViewTag
                  assetViewName:(NSString *)assetViewName) {
    (void)assetViewTag;
    (void)assetViewName;
    RCTUIManager *uiManager = [self.bridge moduleForClass:[RCTUIManager class]];
    [uiManager addUIBlock:^(__unused RCTUIManager *uiManager,
                            NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        UIView *view = viewRegistry[viewTag];
        if (![view isKindOfClass:[ATRNNativeAdView class]]) {
            ATLog(@"ATNativeAdViewManager updateAssetView: view not found tag=%@", viewTag);
            return;
        }
        // iOS 自渲染 viewTag 绑定待后续对齐 Android；旧架构命令接口先预留。
    }];
}

RCT_EXPORT_METHOD(renderNativeAd:(nonnull NSNumber *)viewTag) {
    RCTUIManager *uiManager = [self.bridge moduleForClass:[RCTUIManager class]];
    [uiManager addUIBlock:^(__unused RCTUIManager *uiManager,
                            NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        UIView *view = viewRegistry[viewTag];
        if (![view isKindOfClass:[ATRNNativeAdView class]]) {
            ATLog(@"ATNativeAdViewManager renderNativeAd: view not found tag=%@", viewTag);
            return;
        }
        [(ATRNNativeAdView *)view renderNativeAd];
    }];
}

@end
