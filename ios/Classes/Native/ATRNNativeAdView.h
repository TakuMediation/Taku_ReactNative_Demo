#import <UIKit/UIKit.h>

#ifdef RCT_NEW_ARCH_ENABLED
#import <React/RCTViewComponentView.h>
#import <react/renderer/components/RNATBridgeSpec/RCTComponentViewHelpers.h>

@interface ATRNNativeAdView : RCTViewComponentView <RCTATNativeAdViewViewProtocol>
#else
@interface ATRNNativeAdView : UIView
#endif

- (void)renderNativeAd;

@end
