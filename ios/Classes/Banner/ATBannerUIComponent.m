#import "ATBannerUIComponent.h"
#import <AnyThinkSDK/AnyThinkSDK.h>
#import "ATBannerDelegate.h"
#import "ATBannerTool.h"
#import "ATCommonTool.h"
#import "ATConfiguration.h"

static const NSInteger kAttachMaxRetry = 40;

@interface ATBannerUIComponent ()
@property (nonatomic, weak) UIView *hostView;
@property (nonatomic, copy) NSString *placementID;
@property (nonatomic, assign) CGFloat adWidth;
@property (nonatomic, assign) CGFloat adHeight;
@property (nonatomic, strong) ATBannerDelegate *bannerDelegate;
@property (nonatomic, strong, nullable) ATBannerView *bannerView;
@property (nonatomic, assign) BOOL loaded;
@property (nonatomic, assign) BOOL attached;
@property (nonatomic, assign) BOOL pendingSizeReload;
@property (nonatomic, assign) NSInteger attachGeneration;
@end

@implementation ATBannerUIComponent

- (instancetype)initWithHostView:(UIView *)hostView placementID:(NSString *)placementID {
    if ((self = [super init])) {
        _hostView = hostView;
        _placementID = [placementID copy];
        _adWidth = 320.f;
        _adHeight = 50.f;
        _bannerDelegate = [[ATBannerDelegate alloc] init];

        __weak typeof(self) weakSelf = self;
        _bannerDelegate.onBannerLoaded = ^(NSString *pid) {
            __strong typeof(weakSelf) strongSelf = weakSelf;
            if (strongSelf == nil) return;
            [strongSelf handleBannerLoaded:pid];
        };
        _bannerDelegate.onBannerLoadFail = ^(NSString *pid, NSError *err) {
            ATLog(@"ATBannerUIComponent delegate.onBannerLoadFail placementID=%@ error=%@", pid, err);
        };
    }
    return self;
}

- (void)updateAdSizeWidth:(CGFloat)width height:(CGFloat)height {
    if (width > 0) {
        _adWidth = width;
    }
    if (height > 0) {
        _adHeight = height;
    }
}

- (NSDictionary *)loadExtraForAdSize {
    NSValue *adSize = [NSValue valueWithCGSize:CGSizeMake(_adWidth, _adHeight)];
    return @{ kATAdLoadingExtraBannerAdSizeKey: adSize };
}

- (CGSize)targetBannerSize {
    return CGSizeMake(_adWidth, _adHeight);
}

/// 仅当缓存或 SDK 明确记录了与目标不一致的尺寸时才 reload
- (BOOL)needsReloadForSizeMismatch {
    CGSize want = [self targetBannerSize];
    CGSize loaded = [ATBannerTool loadedSizeForPlacementID:self.placementID];
    if (loaded.width > 0 && ![ATBannerTool isBannerSize:loaded closeTo:want]) {
        return YES;
    }
    CGSize sdkSize = [ATBannerTool sdkBannerSizeForPlacementID:self.placementID];
    if (sdkSize.width > 0 && ![ATBannerTool isBannerSize:sdkSize closeTo:want]) {
        return YES;
    }
    return NO;
}

- (void)handleBannerLoaded:(NSString *)pid {
    if (pid.length == 0 || ![pid isEqualToString:self.placementID]) {
        return;
    }
    ATLog(@"ATBannerUIComponent delegate.onBannerLoaded placementID=%@ pendingSizeReload=%d",
          pid, self.pendingSizeReload);

    if (self.pendingSizeReload) {
        if ([self needsReloadForSizeMismatch]) {
            CGSize sdkSize = [ATBannerTool sdkBannerSizeForPlacementID:pid];
            ATLog(@"ATBannerUIComponent onBannerLoaded deferred sdkSize=%@ loaded=%@ want=%@",
                  NSStringFromCGSize(sdkSize),
                  NSStringFromCGSize([ATBannerTool loadedSizeForPlacementID:pid]),
                  NSStringFromCGSize([self targetBannerSize]));
            return;
        }
        self.pendingSizeReload = NO;
    } else if ([self needsReloadForSizeMismatch]) {
        CGSize sdkSize = [ATBannerTool sdkBannerSizeForPlacementID:pid];
        ATLog(@"ATBannerUIComponent onBannerLoaded size mismatch sdkSize=%@ loaded=%@ want=%@ — reload",
              NSStringFromCGSize(sdkSize),
              NSStringFromCGSize([ATBannerTool loadedSizeForPlacementID:pid]),
              NSStringFromCGSize([self targetBannerSize]));
        [self reloadForSizeMismatch];
        return;
    }

    [ATBannerTool setLoadedSize:[self targetBannerSize] forPlacementID:pid];
    [self attachBannerView];
}

- (void)reloadForSizeMismatch {
    self.pendingSizeReload = YES;
    self.loaded = NO;
    self.attached = NO;
    if (self.bannerView) {
        [self.bannerView removeFromSuperview];
        self.bannerView = nil;
    }
    [self registerDelegates];
    ATLog(@"ATBannerUIComponent reloadForSizeMismatch placementID=%@ size=%.0fx%.0f",
          self.placementID, _adWidth, _adHeight);
    [[ATAdManager sharedManager] loadADWithPlacementID:self.placementID
                                                 extra:[self loadExtraForAdSize]
                                              delegate:self.bannerDelegate];
}

- (void)registerDelegates {
    [[ATAdManager sharedManager] setMultipleLoadingDelegate:self.bannerDelegate placementId:self.placementID];
}

- (void)loadAd {
    if (self.placementID.length == 0) {
        ATLog(@"ATBannerUIComponent.loadAd skip — empty placementID");
        return;
    }
    [self registerDelegates];

    if ([ATBannerTool bannerAdReady:self.placementID]) {
        if ([self needsReloadForSizeMismatch]) {
            [self reloadForSizeMismatch];
            return;
        }
        // 命令式 load 已完成：Show 时只走 onBannerLoaded → attach
        ATLog(@"ATBannerUIComponent.loadAd ad ready — attach via onBannerLoaded placementID=%@ loaded=%@",
              self.placementID,
              NSStringFromCGSize([ATBannerTool loadedSizeForPlacementID:self.placementID]));
        __weak typeof(self) weakSelf = self;
        dispatch_async(dispatch_get_main_queue(), ^{
            if (weakSelf.bannerDelegate.onBannerLoaded) {
                weakSelf.bannerDelegate.onBannerLoaded(weakSelf.placementID);
            }
        });
        return;
    }

    ATLog(@"ATBannerUIComponent.loadAd placementID=%@ size=%.0fx%.0f", self.placementID, _adWidth, _adHeight);
    [ATBannerTool setLoadedSize:CGSizeMake(_adWidth, _adHeight) forPlacementID:self.placementID];
    [[ATAdManager sharedManager] loadADWithPlacementID:self.placementID
                                                 extra:[self loadExtraForAdSize]
                                              delegate:self.bannerDelegate];
}

- (void)attachBannerView {
    ATLog(@"ATBannerUIComponent.attachBannerView placementID=%@", self.placementID);
    self.loaded = YES;
    [self attachBannerViewIfReady:0];
}

- (void)attachBannerViewIfReady {
    if (self.attached) {
        [self layoutAttachedBannerView];
        return;
    }
    [self attachBannerViewIfReady:0];
}

- (CGRect)effectiveHostRect {
    CGRect host = self.hostView.bounds;
    if (host.size.width > 0 && host.size.height > 0) {
        return host;
    }
    return CGRectMake(0, 0, _adWidth, _adHeight);
}

- (BOOL)isHostLayoutReady {
    CGRect host = self.hostView.bounds;
    if (host.size.width > 0 && host.size.height > 0) {
        return YES;
    }
    UIView *parent = self.hostView.superview;
    return parent != nil && parent.bounds.size.height > 0 && parent.bounds.size.width > 0;
}

- (void)layoutAttachedBannerView {
    if (self.bannerView == nil || self.hostView == nil) {
        return;
    }
    CGRect bounds = self.hostView.bounds;
    if (bounds.size.width <= 0 || bounds.size.height <= 0) {
        return;
    }
    self.bannerView.frame = bounds;
    [self.bannerView setNeedsLayout];
    [self.bannerView layoutIfNeeded];
    for (UIView *sub in self.bannerView.subviews) {
        sub.frame = self.bannerView.bounds;
        [sub setNeedsLayout];
        [sub layoutIfNeeded];
    }
}

- (void)attachBannerViewIfReady:(NSInteger)retryCount {
    if (self.placementID.length == 0 || self.attached || self.hostView == nil) {
        return;
    }
    if (!self.loaded) {
        return;
    }

    if (![self isHostLayoutReady]) {
        if (retryCount >= kAttachMaxRetry) {
            ATLog(@"ATBannerUIComponent.attach layout 超时 host=%@ parent=%@ placementID=%@",
                  NSStringFromCGRect(self.hostView.bounds),
                  NSStringFromCGRect(self.hostView.superview.bounds),
                  self.placementID);
            return;
        }
        NSInteger gen = self.attachGeneration;
        __weak typeof(self) weakSelf = self;
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.05 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
            if (weakSelf.attachGeneration != gen) {
                return;
            }
            [weakSelf attachBannerViewIfReady:retryCount + 1];
        });
        return;
    }

    if (self.hostView.window == nil) {
        if (retryCount >= kAttachMaxRetry) {
            ATLog(@"ATBannerUIComponent.attach window=nil 超时 placementID=%@ host=%@",
                  self.placementID, NSStringFromCGRect(self.hostView.bounds));
            return;
        }
        NSInteger gen = self.attachGeneration;
        __weak typeof(self) weakSelf = self;
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.05 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
            if (weakSelf.attachGeneration != gen) {
                return;
            }
            [weakSelf attachBannerViewIfReady:retryCount + 1];
        });
        return;
    }

    NSString *scene = [ATBannerTool scenarioIdForPlacementID:self.placementID];
    NSString *showCustomExt = [ATBannerTool showCustomExtForPlacementID:self.placementID];
    CGRect rect = [self effectiveHostRect];

    // 与 ATPlatformBannerManager / Flutter ATFBannerPlatformView 一致：retrieve + addSubview
    ATBannerView *bv = [ATBannerTool getBannerViewAdRect:rect
                                             placementID:self.placementID
                                                 sceneID:scene.length > 0 ? scene : nil
                                           showCustomExt:showCustomExt.length > 0 ? showCustomExt : nil];
    if (bv == nil) {
        ATLog(@"ATBannerUIComponent.attach retrieveBannerView=nil retry=%ld placementID=%@ ready=%d",
              (long)retryCount, self.placementID, [ATBannerTool bannerAdReady:self.placementID]);
        if (retryCount >= kAttachMaxRetry) {
            return;
        }
        NSInteger gen = self.attachGeneration;
        __weak typeof(self) weakSelf = self;
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.05 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
            if (weakSelf.attachGeneration != gen) {
                return;
            }
            [weakSelf attachBannerViewIfReady:retryCount + 1];
        });
        return;
    }

    if (bv.superview != nil && bv.superview != self.hostView) {
        [bv removeFromSuperview];
    }
    for (UIView *sub in [self.hostView.subviews copy]) {
        [sub removeFromSuperview];
    }

    [self registerDelegates];
    bv.delegate = self.bannerDelegate;
    bv.backgroundColor = [UIColor whiteColor];
    bv.presentingViewController = [ATCommonTool getRootViewController];
    bv.frame = self.hostView.bounds.size.width > 0 ? self.hostView.bounds : rect;
    bv.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    [self.hostView addSubview:bv];
    [self layoutAttachedBannerView];
    self.bannerView = bv;
    self.attached = YES;
    ATLog(@"ATBannerUIComponent.attach OK placementID=%@ retry=%ld bv.frame=%@ host.bounds=%@ window=%d subviews=%lu",
          self.placementID, (long)retryCount, NSStringFromCGRect(bv.frame), NSStringFromCGRect(self.hostView.bounds),
          bv.window != nil, (unsigned long)bv.subviews.count);
}

- (void)destroy {
    ATLog(@"ATBannerUIComponent.destroy placementID=%@ attached=%d", self.placementID, self.attached);
    self.attachGeneration++;
    self.pendingSizeReload = NO;
    self.bannerDelegate.onBannerLoaded = nil;
    self.bannerDelegate.onBannerLoadFail = nil;
    if (self.bannerView) {
        [self.bannerView removeFromSuperview];
        self.bannerView = nil;
    }
    self.attached = NO;
    self.loaded = NO;
}

@end
