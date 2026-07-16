#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * Banner UIComponent 中间层（参照 AppLovinMAXAdViewUIComponent 设计）。
 *
 * 职责：
 *  - 持有 ATBannerDelegate 实例（不复用 ATPlatformBannerManager 的）；
 *  - 调 SDK 加载 banner（[ATAdManager loadADWithPlacementID:extra:delegate:]）；
 *  - delegate 的 onBannerLoaded callback 触发后，调 retrieveBannerViewForPlacementID:config: 拿 ATBannerView
 *    并 addSubview 到外部传入的 hostView（不走 ATBannerAdManager.showBanner）；
 *  - 命令式 load（ATBannerAdManager）仅负责预加载；Show 挂载 Fabric 时若已 ready，同样经 onBannerLoaded 触发 attach；
 *  - 事件驱动，无 polling。
 */
@interface ATBannerUIComponent : NSObject

- (instancetype)initWithHostView:(UIView *)hostView
                     placementID:(NSString *)placementID;

/// Fabric 传入的请求尺寸（pt），load / attach 时使用
- (void)updateAdSizeWidth:(CGFloat)width height:(CGFloat)height;

/// 触发 SDK 加载；加载完成后 delegate callback 内自动 attach 到 hostView
- (void)loadAd;

/// 由 ATRNBannerView.layoutSubviews 兜底触发——SDK 已经 loaded 但 superview 还没撑开时，
/// attachBannerViewIfReady 会推迟到 layout 撑开后再 addSubview。
- (void)attachBannerViewIfReady;

/// 销毁：从 hostView 移除 SDK view + 清 delegate
- (void)destroy;

@end

NS_ASSUME_NONNULL_END
