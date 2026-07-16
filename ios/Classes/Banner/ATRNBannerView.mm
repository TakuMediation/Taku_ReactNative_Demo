#import "ATRNBannerView.h"
#import "ATBannerUIComponent.h"
#import "ATConfiguration.h"

#ifdef RCT_NEW_ARCH_ENABLED

#import <React/RCTViewComponentView.h>
#import <react/renderer/components/RNATBridgeSpec/ComponentDescriptors.h>
#import <react/renderer/components/RNATBridgeSpec/EventEmitters.h>
#import <react/renderer/components/RNATBridgeSpec/Props.h>

using namespace facebook::react;

#endif

@implementation ATRNBannerView {
    ATBannerUIComponent *_uiComponent;
    NSString *_placementID;
    CGFloat _adWidth;
    CGFloat _adHeight;
}

#ifdef RCT_NEW_ARCH_ENABLED

+ (ComponentDescriptorProvider)componentDescriptorProvider {
    return concreteComponentDescriptorProvider<ATBannerViewComponentDescriptor>();
}

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        static const auto defaultProps = std::make_shared<const ATBannerViewProps>();
        _props = defaultProps;
        ATLog(@"ATBannerView initWithFrame frame=%@", NSStringFromCGRect(frame));
    }
    return self;
}

- (void)updateProps:(Props::Shared const &)props oldProps:(Props::Shared const &)oldProps {
    const auto &newP = *std::static_pointer_cast<const ATBannerViewProps>(props);

    NSString *newPlacementID = newP.placementID.empty()
        ? nil
        : [NSString stringWithUTF8String:newP.placementID.c_str()];

    BOOL placementChanged = (_placementID == nil)
        ? (newPlacementID != nil)
        : ![_placementID isEqualToString:newPlacementID ?: @""];
    _placementID = newPlacementID;

    ATLog(@"ATBannerView updateProps placementID=%@ isAdaptiveHeight=%d adWidth=%d adHeight=%d placementChanged=%d",
          _placementID, newP.isAdaptiveHeight, (int)newP.adWidth, (int)newP.adHeight, placementChanged);

    if (placementChanged && _placementID.length > 0) {
        [_uiComponent destroy];
        _uiComponent = [[ATBannerUIComponent alloc] initWithHostView:self placementID:_placementID];
    }
    if (_uiComponent != nil && _placementID.length > 0) {
        [_uiComponent updateAdSizeWidth:(CGFloat)newP.adWidth height:(CGFloat)newP.adHeight];
        if (placementChanged) {
            [_uiComponent loadAd];
        } else {
            [_uiComponent attachBannerViewIfReady];
        }
    }

    [super updateProps:props oldProps:oldProps];
}

#else

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        ATLog(@"ATBannerView initWithFrame frame=%@", NSStringFromCGRect(frame));
        _adWidth = 320.f;
        _adHeight = 50.f;
    }
    return self;
}

- (void)setPlacementID:(NSString *)placementID {
    BOOL placementChanged = (_placementID == nil)
        ? (placementID != nil)
        : ![_placementID isEqualToString:placementID ?: @""];
    _placementID = [placementID copy];

    ATLog(@"ATBannerView setPlacementID=%@ placementChanged=%d", _placementID, placementChanged);

    if (placementChanged && _placementID.length > 0) {
        [_uiComponent destroy];
        _uiComponent = [[ATBannerUIComponent alloc] initWithHostView:self placementID:_placementID];
        [_uiComponent updateAdSizeWidth:_adWidth height:_adHeight];
        [_uiComponent loadAd];
    }
}

- (void)setIsAdaptiveHeight:(BOOL)isAdaptiveHeight {
    (void)isAdaptiveHeight;
}

- (void)setAdWidth:(NSInteger)adWidth {
    if (adWidth > 0) {
        _adWidth = (CGFloat)adWidth;
    }
    [_uiComponent updateAdSizeWidth:_adWidth height:0];
}

- (void)setAdHeight:(NSInteger)adHeight {
    if (adHeight > 0) {
        _adHeight = (CGFloat)adHeight;
    }
    [_uiComponent updateAdSizeWidth:0 height:_adHeight];
    [_uiComponent attachBannerViewIfReady];
}

- (void)dealloc {
    [_uiComponent destroy];
}

#endif

- (void)layoutSubviews {
    [super layoutSubviews];
    for (UIView *subview in self.subviews) {
        if (!CGRectEqualToRect(subview.frame, self.bounds)) {
            subview.frame = self.bounds;
        }
    }
    [_uiComponent attachBannerViewIfReady];
}

- (void)didMoveToWindow {
    [super didMoveToWindow];
    if (self.window != nil) {
        [_uiComponent attachBannerViewIfReady];
    }
}

#ifdef RCT_NEW_ARCH_ENABLED

- (void)prepareForRecycle {
    ATLog(@"ATBannerView prepareForRecycle placementID=%@", _placementID);
    [_uiComponent destroy];
    _uiComponent = nil;
    [super prepareForRecycle];
    _placementID = nil;
}

extern "C" __attribute__((used)) Class<RCTComponentViewProtocol> ATBannerViewCls(void) {
    return ATRNBannerView.class;
}

#endif

@end
