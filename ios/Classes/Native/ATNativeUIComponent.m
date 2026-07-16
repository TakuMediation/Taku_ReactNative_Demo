#import "ATNativeUIComponent.h"
#import <AnyThinkSDK/AnyThinkSDK.h>
#import "ATNativeTool.h"
#import "ATCommonTool.h"
#import "ATNativeSelfRenderView.h"
#import "ATNativeManager.h"
#import "ATConfiguration.h"

@interface ATNativeUIComponent ()
@property (nonatomic, weak) UIView *hostView;
@property (nonatomic, copy) NSString *placementID;
@property (nonatomic, strong, nullable) ATNativeADView *nativeADView;
@property (nonatomic, strong, nullable) ATNativeSelfRenderView *selfRenderView;  // 自渲染缓存，避免重复 alloc + 便于 destroy 时统一清理
@property (nonatomic, assign) BOOL attached;
@property (nonatomic, assign) BOOL closedByUser;
@end

@implementation ATNativeUIComponent

- (instancetype)initWithHostView:(UIView *)hostView placementID:(NSString *)placementID {
    if ((self = [super init])) {
        _hostView = hostView;
        _placementID = [placementID copy];
      hostView.clipsToBounds = YES;
    }
    return self;
}

- (ATShowConfig *)showConfigForAttach {
    NSString *scene = [ATNativeTool scenarioIdForPlacementID:self.placementID];
    NSString *ext = [ATNativeTool showCustomExtForPlacementID:self.placementID];
    return [[ATShowConfig alloc] initWithScene:(scene ?: @"") showCustomExt:(ext ?: @"")];
}

- (void)attachIfNeeded {
    if (self.closedByUser || self.attached || self.hostView == nil || self.placementID.length == 0) {
        return;
    }
    // hostView 还没 layout 时不要 attach（SDK 渲染拿 size=0 会出问题）。
    // 等 layoutSubviews 触发时再 attach（attached 守卫保证幂等）。
    if (self.hostView.bounds.size.width <= 0 || self.hostView.bounds.size.height <= 0) {
        ATLog(@"ATNativeUIComponent.attachIfNeeded hostView bounds 还未 layout (size=%@)，等下次触发",
              NSStringFromCGSize(self.hostView.bounds.size));
        return;
    }
    ATShowConfig *cfg = [self showConfigForAttach];
    ATNativeAdOffer *offer = [[ATAdManager sharedManager] getNativeAdOfferWithPlacementID:self.placementID
                                                                              showConfig:cfg];
    if (offer == nil) {
        ATLog(@"ATNativeUIComponent.attachIfNeeded offer=nil (JS 端 ATNative 是否已 makeAdRequest+getNativeAd?) placementID=%@", self.placementID);
        // 兜底：offer 可能因 JS 回调与 Fabric layout 时序差而尚未就绪。
        // 安排 500ms 后重试一次；attached 守卫保证幂等，不会二次 attach。
        __weak typeof(self) weakSelf = self;
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.5 * NSEC_PER_SEC)),
                       dispatch_get_main_queue(), ^{
            [weakSelf attachIfNeeded];
        });
        return;
    }

    BOOL isExpressAd = offer.nativeAd.isExpressAd;
    // 关键：用 ATNativeManager 的 instance method 拿 config——它内部 config.delegate=self（ATNativeManager 单例），
    // SDK 的 didShowNativeAdInAdView/didClickNativeAdInAdView 等回调才有接收方，否则全丢。
    // 直接调 [ATNativeTool getATNativeADConfiguration:] 拿到的 config.delegate=nil。
    ATNativeADConfiguration *config = [[ATNativeManager sharedManager]
                                       getATNativeADConfiguration:@{} isAdaptiveHeight:isExpressAd];
    // getATNativeADConfiguration 内部用 extraDic 解析宽高，传空 dict 会得到 ADFrame=0×0。
    // SDK 看到 ADFrame=0×0 时不会触发 didShowNativeAdInAdView:（认为广告不可见）。
    // 这里用 hostView 的实际 bounds 覆盖，确保 SDK 知道正确的渲染尺寸。
    config.ADFrame = self.hostView.bounds;
    ATNativeADView *adView = [[ATNativeADView alloc] initWithConfiguration:config
                                                              currentOffer:offer
                                                               placementID:self.placementID];
    // 必须在 rendererWithConfiguration: 之前先设好 frame：
    // SDK 在 renderer 内部会用 adView.bounds 计算子视图布局；若此时 bounds=0×0，
    // 子视图全挤到 (0,0)，模板内容不可见（或被裁剪成 0 面积）。
    adView.frame = self.hostView.bounds;

    if (isExpressAd) {
        // 模板（Express）：SDK 自己出整张视图
        [offer rendererWithConfiguration:config selfRenderView:nil nativeADView:adView];
    } else {
        // 自渲染（方案 B：SDK 模板）：用 SDK 自带 ATNativeSelfRenderView，子 view 默认布局由这里硬编码。
        // 把 selfRenderView 持有为 ivar：单次 attach 由 self.attached 守卫保证只 alloc 一次；
        // 跨 destroy 后再 attach（ivar 已清回 nil）时才会重新 alloc。
        if (self.selfRenderView == nil) {
            self.selfRenderView = [[ATNativeSelfRenderView alloc] initWithOffer:offer];
        }
        ATNativeSelfRenderView *selfRenderView = self.selfRenderView;
        // 必须给 selfRenderView 自己也设 frame。UIView 默认 frame=0×0；
        // CALayer 渲染不受 bounds 限制（子 view 仍能画出来），但 hit-test 会被 pointInside 截断在 0×0，
        // 导致子 view 上 SDK 注册的 tap recognizer 永远收不到 touch——广告"能看见但点不动"。
        selfRenderView.frame = self.hostView.bounds;
        [self applyDefaultSelfRenderLayout:selfRenderView size:self.hostView.bounds.size];

        UIView *mediaView = [adView getMediaView];
        NSMutableArray *clickable = [@[selfRenderView.iconImageView,
                                       selfRenderView.titleLabel,
                                       selfRenderView.textLabel,
                                       selfRenderView.ctaLabel,
                                       selfRenderView.mainImageView] mutableCopy];
        if (mediaView) {
            [clickable addObject:mediaView];
            selfRenderView.mediaView = mediaView;
            mediaView.frame = selfRenderView.mainImageView.frame;
            [selfRenderView addSubview:mediaView];
        }
        [adView registerClickableViewArray:clickable];

        ATNativePrepareInfo *info = [ATNativePrepareInfo loadPrepareInfo:^(ATNativePrepareInfo * _Nonnull pi) {
            pi.iconImageView = selfRenderView.iconImageView;
            pi.titleLabel = selfRenderView.titleLabel;
            pi.textLabel = selfRenderView.textLabel;
            pi.ctaLabel = selfRenderView.ctaLabel;
            pi.mainImageView = selfRenderView.mainImageView;
            pi.dislikeButton = selfRenderView.dislikeButton;
            pi.mediaView = selfRenderView.mediaView;
        }];
        [adView prepareWithNativePrepareInfo:info];
        [offer rendererWithConfiguration:config selfRenderView:selfRenderView nativeADView:adView];
        // SDK 在 rendererWithConfiguration: 内部会把 selfRenderView.frame 重置回 0×0,
        // 必须 render 之后再补一次（且子 view layout 也要重 apply）——否则 hit-test 在 0×0 被截断，
        // 子 view 上的 tap 永远不 fire。layoutSubviews 也会兜底（见 -syncLayout）。
        [self syncLayout];
        adView.logoImageView.hidden = selfRenderView.isHiddenLogo;
    }

    [self.hostView addSubview:adView];
    self.nativeADView = adView;
    self.attached = YES;
    [[ATNativeManager sharedManager] registerFabricUIComponent:self forNativeADView:adView];
    ATLog(@"ATNativeUIComponent.attachIfNeeded attached placementID=%@ isExpress=%d hostView.bounds=%@",
          self.placementID, isExpressAd, NSStringFromCGRect(self.hostView.bounds));
}

/// 自渲染默认布局：参照常见 320x250 信息流模板——
///   顶部：icon(L) + title(R, 2 行)
///   中部：mainImage / mediaView（视频）
///   底部：CTA 按钮（蓝底白字居中）
- (void)applyDefaultSelfRenderLayout:(ATNativeSelfRenderView *)v size:(CGSize)size {
    CGFloat pad = 8;
    CGFloat iconSide = 36;
    CGFloat headerH = iconSide;
    CGFloat ctaH = 32;
    CGFloat mediaY = pad + headerH + pad;
    CGFloat mediaH = MAX(0, size.height - mediaY - pad - ctaH - pad);

    v.iconImageView.frame = CGRectMake(pad, pad, iconSide, iconSide);
    v.titleLabel.frame = CGRectMake(pad + iconSide + pad, pad,
                                    size.width - (pad * 3 + iconSide), iconSide / 2);
    v.titleLabel.font = [UIFont boldSystemFontOfSize:14];
    v.titleLabel.numberOfLines = 1;

    v.textLabel.frame = CGRectMake(pad + iconSide + pad, pad + iconSide / 2,
                                   size.width - (pad * 3 + iconSide), iconSide / 2);
    v.textLabel.font = [UIFont systemFontOfSize:12];
    v.textLabel.textColor = [UIColor darkGrayColor];

    v.mainImageView.frame = CGRectMake(pad, mediaY, size.width - pad * 2, mediaH);
    v.mainImageView.contentMode = UIViewContentModeScaleAspectFill;
    v.mainImageView.clipsToBounds = YES;

    v.ctaLabel.frame = CGRectMake(pad, size.height - pad - ctaH, size.width - pad * 2, ctaH);
    v.ctaLabel.textAlignment = NSTextAlignmentCenter;
    v.ctaLabel.font = [UIFont boldSystemFontOfSize:14];
    v.ctaLabel.textColor = [UIColor whiteColor];
    v.ctaLabel.backgroundColor = [UIColor colorWithRed:0.20 green:0.55 blue:0.95 alpha:1.0];
    v.ctaLabel.layer.cornerRadius = 4;
    v.ctaLabel.layer.masksToBounds = YES;

    v.dislikeButton.frame = CGRectMake(size.width - pad - 18, pad, 18, 18);
}

/// SDK 在多个时机会把 selfRenderView.frame 抹回 0×0（rendererWithConfiguration: 至少一次，
/// 后续异步刷新可能再来）。这个方法统一把 selfRenderView + 各子 view 重新摆好。
/// 调用方：attachIfNeeded 内 render 之后；ATRNNativeAdView.layoutSubviews 兜底。
- (void)syncLayout {
    if (self.selfRenderView == nil || self.hostView == nil) return;
    if (self.hostView.bounds.size.width <= 0 || self.hostView.bounds.size.height <= 0) return;
    self.selfRenderView.frame = self.hostView.bounds;
    [self applyDefaultSelfRenderLayout:self.selfRenderView size:self.hostView.bounds.size];
    if (self.selfRenderView.mediaView) {
        self.selfRenderView.mediaView.frame = self.selfRenderView.mainImageView.frame;
    }
}

- (void)destroy {
    ATLog(@"ATNativeUIComponent.destroy placementID=%@ attached=%d closedByUser=%d",
          self.placementID, self.attached, self.closedByUser);
    if (self.nativeADView) {
        [[ATNativeManager sharedManager] unregisterFabricUIComponentForNativeADView:self.nativeADView];
        [[ATNativeManager sharedManager] destroyNativeAdViewIfNeeded:self.nativeADView];
        [self.nativeADView removeFromSuperview];
        self.nativeADView = nil;
    }
    // selfRenderView 作为 adView 子树会随 adView 释放，这里显式清 ivar 让下次 attach 重新 alloc
    self.selfRenderView = nil;
    self.attached = NO;
}

- (void)destroyFromUserClose {
    self.closedByUser = YES;
    [self destroy];
}

@end
