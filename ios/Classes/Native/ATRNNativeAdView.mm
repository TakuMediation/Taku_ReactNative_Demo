#import "ATRNNativeAdView.h"
#import "ATNativeUIComponent.h"
#import "ATConfiguration.h"

#ifdef RCT_NEW_ARCH_ENABLED

#import <React/RCTViewComponentView.h>
#import <react/renderer/components/RNATBridgeSpec/ComponentDescriptors.h>
#import <react/renderer/components/RNATBridgeSpec/EventEmitters.h>
#import <react/renderer/components/RNATBridgeSpec/Props.h>

using namespace facebook::react;

#endif

@implementation ATRNNativeAdView {
    ATNativeUIComponent *_uiComponent;
    NSString *_placementID;
    NSString *_adId;
}

#ifdef RCT_NEW_ARCH_ENABLED

+ (ComponentDescriptorProvider)componentDescriptorProvider {
    return concreteComponentDescriptorProvider<ATNativeAdViewComponentDescriptor>();
}

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        static const auto defaultProps = std::make_shared<const ATNativeAdViewProps>();
        _props = defaultProps;
        ATLog(@"ATNativeADView initWithFrame frame=%@", NSStringFromCGRect(frame));
    }
    return self;
}

- (void)updateProps:(Props::Shared const &)props oldProps:(Props::Shared const &)oldProps {
    const auto &newP = *std::static_pointer_cast<const ATNativeAdViewProps>(props);

    NSString *newPlacementID = newP.placementID.empty()
        ? nil
        : [NSString stringWithUTF8String:newP.placementID.c_str()];
    NSString *newAdId = newP.adId.empty()
        ? nil
        : [NSString stringWithUTF8String:newP.adId.c_str()];

    BOOL placementChanged = (_placementID == nil)
        ? (newPlacementID != nil)
        : ![_placementID isEqualToString:newPlacementID ?: @""];
    _placementID = newPlacementID;
    _adId = newAdId;

    ATLog(@"ATNativeADView updateProps placementID=%@ adId=%@ isAdaptiveHeight=%d adWidth=%d adHeight=%d placementChanged=%d",
          _placementID, _adId, newP.isAdaptiveHeight, (int)newP.adWidth, (int)newP.adHeight, placementChanged);

    if (placementChanged && _placementID.length > 0) {
        [_uiComponent destroy];
        _uiComponent = [[ATNativeUIComponent alloc] initWithHostView:self placementID:_placementID];
        [_uiComponent attachIfNeeded];
    }

    [super updateProps:props oldProps:oldProps];
}

- (void)handleCommand:(const NSString *)commandName args:(const NSArray *)args {
    ATLog(@"ATNativeADView handleCommand command=%@ args=%@", commandName, args);
    if ([commandName isEqualToString:@"renderNativeAd"]) {
        [_uiComponent attachIfNeeded];
    }
}

- (void)prepareForRecycle {
    ATLog(@"ATNativeADView prepareForRecycle placementID=%@", _placementID);
    [_uiComponent destroy];
    _uiComponent = nil;
    [super prepareForRecycle];
    _placementID = nil;
    _adId = nil;
}

- (facebook::react::SharedTouchEventEmitter)touchEventEmitterAtPoint:(CGPoint)point {
    return nullptr;
}

extern "C" __attribute__((used)) Class<RCTComponentViewProtocol> ATNativeADViewCls(void) {
    return ATRNNativeAdView.class;
}

#else

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        ATLog(@"ATNativeADView initWithFrame frame=%@", NSStringFromCGRect(frame));
    }
    return self;
}

- (void)setPlacementID:(NSString *)placementID {
    BOOL placementChanged = (_placementID == nil)
        ? (placementID != nil)
        : ![_placementID isEqualToString:placementID ?: @""];
    _placementID = [placementID copy];

    ATLog(@"ATNativeADView setPlacementID=%@ placementChanged=%d", _placementID, placementChanged);

    if (placementChanged && _placementID.length > 0) {
        [_uiComponent destroy];
        _uiComponent = [[ATNativeUIComponent alloc] initWithHostView:self placementID:_placementID];
        [_uiComponent attachIfNeeded];
    }
}

- (void)setAdId:(NSString *)adId {
    _adId = [adId copy];
}

- (void)setIsAdaptiveHeight:(BOOL)isAdaptiveHeight {
    (void)isAdaptiveHeight;
}

- (void)setAdWidth:(NSInteger)adWidth {
    (void)adWidth;
}

- (void)setAdHeight:(NSInteger)adHeight {
    (void)adHeight;
}

- (void)dealloc {
    [_uiComponent destroy];
}

#endif

- (void)renderNativeAd {
    [_uiComponent attachIfNeeded];
}

- (void)layoutSubviews {
    [super layoutSubviews];
    [_uiComponent attachIfNeeded];
    [_uiComponent syncLayout];
}

@end
