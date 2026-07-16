#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * Native（模板/Express）UIComponent 中间层（参照 AppLovinMAXAdViewUIComponent 设计）。
 *
 * **不负责加载**——加载由 JS 端 `ATNative.makeAdRequest()` 触发（走 Bridge.loadNativeAd
 * → ATNativeManager.loadNativeWith），SDK 内部已经缓存好 offer。
 *
 * UIComponent 只负责"消费已加载的广告"：
 *   1. attachIfNeeded 调 [ATAdManager getNativeAdOfferWithPlacementID:] 拿缓存 offer
 *   2. 构造 ATNativeADView 渲染（仅模板 Express；自渲染待 viewTag binding）
 *   3. addSubview 到 hostView
 *
 * 为什么不在这里 loadAd？
 *   - 如果 ComponentView 也调 loadAd，会跟 JS 端 ATNative 的加载流程冲突（两套 delegate、
 *     两次 SDK 加载），导致广告渲染重复 / 失序。
 *   - JS 端流程 `await native.getNativeAd()` 已经保证 mount ComponentView 时缓存里有 offer。
 *
 * 时机兜底：
 *   - updateProps 第一次调 attachIfNeeded 时，hostView 可能还未 layout（bounds=0），跳过；
 *   - layoutSubviews 再次触发 attachIfNeeded（attached 守卫保证幂等）。
 */
@interface ATNativeUIComponent : NSObject

- (instancetype)initWithHostView:(UIView *)hostView
                     placementID:(NSString *)placementID;

/// 幂等：尝试从 SDK 缓存拿广告 offer 并渲染到 hostView。
/// hostView.bounds 为零或 offer 未就绪时安全跳过。
- (void)attachIfNeeded;

/// 自渲染场景下，SDK 在 rendererWithConfiguration: 内部会把 selfRenderView.frame 重置回 0×0。
/// 由 ATRNNativeAdView.layoutSubviews 调用，每次 layout 兜底把 selfRenderView 跟子 view frame 摆好。
- (void)syncLayout;

- (void)destroy;

/// 用户点关闭/dislike 后销毁，并阻止 layoutSubviews 再次 attach。
- (void)destroyFromUserClose;

@end

NS_ASSUME_NONNULL_END
