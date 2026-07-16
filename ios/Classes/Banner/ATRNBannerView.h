#import <UIKit/UIKit.h>

#ifdef RCT_NEW_ARCH_ENABLED
#import <React/RCTViewComponentView.h>
#import <react/renderer/components/RNATBridgeSpec/RCTComponentViewHelpers.h>

@interface ATRNBannerView : RCTViewComponentView <RCTATBannerViewViewProtocol>
#else
@interface ATRNBannerView : UIView
#endif

@end
