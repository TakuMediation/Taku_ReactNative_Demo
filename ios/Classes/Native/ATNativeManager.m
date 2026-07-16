//
//  ATNativeManager.m
//  topon_flutter_plugin
//
//  Created by GUO PENG on 2021/6/29.
//

#import "ATNativeManager.h"
#import <AnyThinkSDK/AnyThinkSDK.h>
#import "ATCommonTool.h"
#import "ATConfiguration.h"

#import "ATSendSignalManager.h"
#import "ATDisposeDataTool.h"
#import "ATNativeTool.h"

#import "ATNativeSelfRenderView.h"
#import "ATNativeUIComponent.h"
#define NativeCallName  @"NativeCall"


#define NativeAdFailToLoadAD  @"nativeAdFailToLoadAD"
#define NativeAdDidFinishLoading  @"nativeAdDidFinishLoading"
#define NativeAddidClick  @"nativeAdDidClick"
#define NativeAdDidDeepLink  @"nativeAdDidDeepLink"
#define NativeAddidEndPlayingVideo  @"nativeAdDidEndPlayingVideo"
#define NativeAdEnterFullScreenVideo  @"nativeAdEnterFullScreenVideo"
#define NativeAdExitFullScreenVideoInAd  @"nativeAdExitFullScreenVideoInAd"
#define NativeAddidShowNativeAd  @"nativeAdDidShowNativeAd"
#define NativeAddidStartPlayingVideo  @"nativeAdDidStartPlayingVideo"
#define NativeAddidTapCloseButton  @"nativeAdDidTapCloseButton"
#define NativeAdDidCloseDetailInAdView  @"nativeAdDidCloseDetailInAdView"

#define NativeAddidLoadSuccessDraw  @"nativeAdDidLoadSuccessDraw"

#define NativeAdDidMultipleLoaded          @"nativeAdDidMultipleLoaded"

#define NativeAdDidAdSourceBiddingAttempt  @"nativeAdDidAdSourceBiddingAttempt"
#define NativeAdDidAdSourceBiddingFilled   @"nativeAdDidAdSourceBiddingFilled"
#define NativeAdDidAdSourceBiddingFail     @"nativeAdDidAdSourceBiddingFail"
#define NativeAdDidAdSourceAttempt         @"nativeAdDidAdSourceAttempt"
#define NativeAdDidAdSourceLoadFilled      @"nativeAdDidAdSourceLoadFilled"
#define NativeAdDidAdSourceLoadFail        @"nativeAdDidAdSourceLoadFail"

static NSString *customView = @"customView";

@interface ATNativeManager()<ATNativeADDelegate, ATAdMultipleLoadingDelegate>
@property(nonatomic,strong) UIButton * container;
@property(nonatomic,copy)NSString * placementID;
@property (nonatomic,strong) NSMutableDictionary *nativeViewDic;
/// Fabric ATNativeAdView → UIComponent（weak），关闭回调时定位并销毁。
@property (nonatomic, strong) NSMapTable<ATNativeADView *, ATNativeUIComponent *> *fabricUIComponents;

@end

@implementation ATNativeManager

+ (instancetype)sharedManager {
    static ATNativeManager *_inst = nil;
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        _inst = [[ATNativeManager alloc] init];
        _inst.fabricUIComponents = [NSMapTable mapTableWithKeyOptions:NSPointerFunctionsWeakMemory
                                                           valueOptions:NSPointerFunctionsWeakMemory];
    });
    return _inst;
}

- (void)registerFabricUIComponent:(ATNativeUIComponent *)component
                  forNativeADView:(ATNativeADView *)adView {
    if (component == nil || adView == nil) {
        return;
    }
    [self.fabricUIComponents setObject:component forKey:adView];
}

- (void)unregisterFabricUIComponentForNativeADView:(ATNativeADView *)adView {
    if (adView == nil) {
        return;
    }
    [self.fabricUIComponents removeObjectForKey:adView];
}

- (void)destroyFabricNativeADView:(ATNativeADView *)adView {
    if (adView == nil) {
        return;
    }
    ATNativeUIComponent *component = [self.fabricUIComponents objectForKey:adView];
    if (component != nil) {
        ATLog(@"Native:ATNativeManager::destroyFabricNativeADView adView=%@ component=%@", adView, component);
        [component destroyFromUserClose];
        [self.fabricUIComponents removeObjectForKey:adView];
    }
}

- (void)destroyNativeAdViewIfNeeded:(ATNativeADView *)adView {
  ATLog(@"Native:ATNativeManager::destroyNativeAdViewIfNeeded:adView:%@", adView);
  if (adView == nil) {
    return;
  }
  SEL destroySelector = NSSelectorFromString(@"destroyNative");
  if ([adView respondsToSelector:destroySelector]) {
    IMP imp = [adView methodForSelector:destroySelector];
    void (*func)(id, SEL) = (void *)imp;
    func(adView, destroySelector);
  }
}

/// 加载原生广告
- (void)loadNativeWith:(NSString *)placementID extraDic:(NSDictionary *)extraDic{
  ATLog(@"Native:ATNativeManager::loadNativeWith:extraDic:placementID:%@--extraDic:%@", placementID, extraDic);
    
  if (kATStringIsEmpty(placementID)) {
    return;
  }
  
  ATNativeAttributeMode *parentMode = [ATDisposeDataTool disposeNativeData:extraDic keyStr:NativeSize];
  BOOL isAdaptiveHeight = NO;
  if ([extraDic[IsAdaptiveHeight] respondsToSelector:@selector(boolValue)]) {
    isAdaptiveHeight = [extraDic[IsAdaptiveHeight] boolValue];
  }
  NSDictionary *mediaViewFrameDic = [extraDic[MediaViewFrame] isKindOfClass:[NSDictionary class]] ? extraDic[MediaViewFrame] : nil;
  
  ATLog(@"Native:ATNativeManager::loadNativeWith:extraDic:size:%@--isAdaptiveHeight:%@", NSStringFromCGSize(CGSizeMake(parentMode.width, parentMode.height)), @(isAdaptiveHeight));
  
  NSMutableDictionary *extraDicMutable = [NSMutableDictionary dictionaryWithDictionary:extraDic];
  [ATCommonTool applyAtAdRequestAndRemove:extraDicMutable];
  [[ATAdManager sharedManager] setMultipleLoadingDelegate:self placementId:placementID];
  NSMutableDictionary *loadExtra = [@{
    kATExtraInfoNativeAdSizeKey:[NSValue valueWithCGSize:CGSizeMake(parentMode.width, parentMode.height)],
    kATNativeAdSizeToFitKey:@(isAdaptiveHeight)
  } mutableCopy];
  if (mediaViewFrameDic.count > 0) {
    ATNativeAttributeMode *mediaViewFrameMode = [ATDisposeDataTool disposeNativeData:extraDic keyStr:MediaViewFrame];
    loadExtra[kATExtraInfoMediaViewFrameKey] = [NSValue valueWithCGRect:CGRectMake(mediaViewFrameMode.x, mediaViewFrameMode.y, mediaViewFrameMode.width, mediaViewFrameMode.height)];
  }
  [[ATAdManager sharedManager] loadADWithPlacementID:placementID extra:loadExtra delegate:self];
  
}

/// 展示原生广告
- (void)showNative:(NSString *)placementID isAdaptiveHeight:(BOOL)isAdaptiveHeight extraDic:(NSDictionary *)extraDic {
  ATLog(@"Native:ATNativeManager::showNative:isAdaptiveHeight:extraDic:placementID:%@--isAdaptiveHeight:%@--extraDic:%@", placementID, @(isAdaptiveHeight), extraDic);
  
  if (kATStringIsEmpty(placementID)) {
    return;
  }
  
  ATNativeADConfiguration *config = [self getATNativeADConfiguration:extraDic isAdaptiveHeight:isAdaptiveHeight];
  
  ATNativeAdOffer *offer = [[ATAdManager sharedManager] getNativeAdOfferWithPlacementID:placementID];
  [self renderOffer:offer config:config placementID:placementID extraDic:extraDic];
}

- (ATNativeSelfRenderView *)getSelfRenderViewOffer:(ATNativeAdOffer *)offer{
  ATLog(@"Native:ATNativeManager::getSelfRenderViewOffer:offer:%@", offer);
  
  ATNativeSelfRenderView *selfRenderView = [[ATNativeSelfRenderView alloc]initWithOffer:offer];
  
  return selfRenderView;
}

#define kNavigationBarHeight ([[UIApplication sharedApplication] statusBarOrientation] == UIInterfaceOrientationPortrait || [[UIApplication sharedApplication] statusBarOrientation] == UIInterfaceOrientationPortraitUpsideDown ? ([[UIApplication sharedApplication]statusBarFrame].size.height + 44) : ([[UIApplication sharedApplication]statusBarFrame].size.height - 4))

/// 展示场景原生广告
- (void)showNative:(NSString *)placementID sceneID:(NSString *)sceneID isAdaptiveHeight:(BOOL)isAdaptiveHeight extraDic:(NSDictionary *) extraDic{
  ATLog(@"Native:ATNativeManager::showNative:sceneID:isAdaptiveHeight:extraDic:placementID:%@--sceneID:%@--isAdaptiveHeight:%@--extraDic:%@", placementID, sceneID, @(isAdaptiveHeight), extraDic);
  
  if (kATStringIsEmpty(placementID)) {
    return;
  }
  
  ATNativeADConfiguration *config = [self getATNativeADConfiguration:extraDic isAdaptiveHeight:isAdaptiveHeight];
  
  sceneID = [ATCommonTool checkStrParamsEmptyAndReturn:sceneID];
  
  ATNativeAdOffer *offer = [[ATAdManager sharedManager] getNativeAdOfferWithPlacementID:placementID scene:sceneID];
  [self renderOffer:offer config:config placementID:placementID extraDic:extraDic];
}

/// 展示原生广告带showCustomExt
- (void)showNative:(NSString *)placementID sceneID:(NSString *)sceneID showCustomExt:(NSString *)showCustomExt isAdaptiveHeight:(BOOL)isAdaptiveHeight extraDic:(NSDictionary *) extraDic {
  ATLog(@"Native:ATNativeManager::showNative:sceneID:showCustomExt:isAdaptiveHeight:extraDic:placementID:%@--sceneID:%@--showCustomExt:%@--isAdaptiveHeight:%@--extraDic:%@", placementID, sceneID, showCustomExt, @(isAdaptiveHeight), extraDic);
  
  if (kATStringIsEmpty(placementID)) {
    return;
  }
  
  ATNativeADConfiguration *config = [self getATNativeADConfiguration:extraDic isAdaptiveHeight:isAdaptiveHeight];
  
  sceneID = [ATCommonTool checkStrParamsEmptyAndReturn:sceneID];
  if (![showCustomExt isKindOfClass:[NSString class]] || [showCustomExt length] == 0) {
    ATLog(@"Native:ATNativeManager::showNative:sceneID:showCustomExt:isAdaptiveHeight:extraDic:invalid showCustomExt:%@", showCustomExt);
    showCustomExt = @"";
  }
  
  ATShowConfig *showConfig = [[ATShowConfig alloc] initWithScene:sceneID showCustomExt:showCustomExt];
  
  ATNativeAdOffer *offer = [[ATAdManager sharedManager] getNativeAdOfferWithPlacementID:placementID showConfig:showConfig];
  [self renderOffer:offer config:config placementID:placementID extraDic:extraDic];
}

- (ATNativeADView *)getNativeADView:(ATNativeADConfiguration *)config offer:(ATNativeAdOffer *)offer selfRenderView:(ATNativeSelfRenderView *)selfRenderView withPlacementId:(NSString*)placementID extraDic:(NSDictionary *) extraDic {
  ATLog(@"Native:ATNativeManager::getNativeADView:offer:selfRenderView:withPlacementId:extraDic:placementID:%@--offer:%@--selfRenderView:%@--extraDic:%@", placementID, offer, selfRenderView, extraDic);
  
  ATNativeADView *nativeADView = [[ATNativeADView alloc]initWithConfiguration:config currentOffer:offer placementID:placementID];
  if (selfRenderView == nil) {
    return nativeADView;
  }
  
  UIView *mediaView = [nativeADView getMediaView];
  
  NSMutableArray *array = [@[selfRenderView.iconImageView,selfRenderView.titleLabel,selfRenderView.textLabel,selfRenderView.ctaLabel,selfRenderView.mainImageView] mutableCopy];
  
  if (mediaView) {
    [array addObject:mediaView];
    selfRenderView.mediaView = mediaView;
    [selfRenderView addSubview:mediaView];
  }
  
  NSArray<ATNativeAttributeMode *> *modes = [ATDisposeDataTool disposeCustomViewNativeData:extraDic keyStr:customView];
  [modes enumerateObjectsUsingBlock:^(ATNativeAttributeMode * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
    ATNativeAttributeModeType type = (ATNativeAttributeModeType)obj.type.integerValue;
    switch (type) {
      case ATNativeAttributeModeTypeImage: {
        UIImageView *imageView = [[UIImageView alloc] initWithImage:[UIImage imageNamed:obj.imagePath]];
        if (obj.backgroundColorStr && ![obj.backgroundColorStr isEqualToString:@""]) {
          imageView.backgroundColor = [ATCommonTool colorWithHexString:obj.backgroundColorStr];
        }
        imageView.frame = CGRectMake(obj.x, obj.y, obj.width, obj.height);
        if (obj.cornerRadius > 0) {
          imageView.layer.cornerRadius = obj.cornerRadius;
          imageView.layer.masksToBounds = YES;
        }
        [selfRenderView addSubview:imageView];
        [selfRenderView.customViews addObject:imageView];
      }
        break;
      case ATNativeAttributeModeTypeLabel: {
        UILabel *label = [[UILabel alloc] initWithFrame:CGRectMake(obj.x, obj.y, obj.width, obj.height)];
        if (obj.backgroundColorStr && ![obj.backgroundColorStr isEqualToString:@""]) {
          label.backgroundColor = [ATCommonTool colorWithHexString:obj.backgroundColorStr];
        }
        label.text = obj.title;
        label.textColor = [ATCommonTool colorWithHexString:obj.textColorStr];
        if (obj.cornerRadius > 0) {
          label.layer.cornerRadius = obj.cornerRadius;
          label.layer.masksToBounds = YES;
        }
        label.font = [UIFont systemFontOfSize:obj.textSize];
        if ([obj.textAlignmentStr isEqualToString:@"left"]) {
          label.textAlignment = NSTextAlignmentLeft;
        }
        if ([obj.textAlignmentStr isEqualToString:@"center"]) {
          label.textAlignment = NSTextAlignmentCenter;
        }
        if ([obj.textAlignmentStr isEqualToString:@"right"]) {
          label.textAlignment = NSTextAlignmentRight;
        }
        [selfRenderView addSubview:label];
        [selfRenderView.customViews addObject:label];
      }
        break;
      case ATNativeAttributeModeTypeView: {
        UIView *view = [[UIView alloc] initWithFrame:CGRectMake(obj.x, obj.y, obj.width, obj.height)];
        if (obj.backgroundColorStr && ![obj.backgroundColorStr isEqualToString:@""]) {
          view.backgroundColor = [ATCommonTool colorWithHexString:obj.backgroundColorStr];
        }
        
        if (obj.cornerRadius > 0) {
          view.layer.cornerRadius = obj.cornerRadius;
          view.layer.masksToBounds = YES;
        }
        
        [selfRenderView addSubview:view];
        [selfRenderView.customViews addObject:view];
      }
        break;
        
      default:
        break;
    }
  }];
  
  [array addObjectsFromArray:selfRenderView.customViews];
//  [nativeADView registerClickableViewArray:array];
  
  return nativeADView;
}

- (void)renderOffer:(ATNativeAdOffer *)offer config:(ATNativeADConfiguration *)config placementID:(NSString *)placementID extraDic:(NSDictionary *)extraDic {
  ATLog(@"Native:ATNativeManager::renderOffer:config:placementID:extraDic:placementID:%@--offer:%@--config:%@--extraDic:%@", placementID, offer, config, extraDic);
  if (offer == nil) {
    ATLog(@"Native:ATNativeManager::renderOffer:config:placementID:extraDic:offer is nil placementID:%@", placementID);
    return;
  }
  ATLog(@"Native:ATNativeManager::renderOffer:config:placementID:extraDic:isExpressAd:%@--networkFirmID:%ld--size:%fx%f", @(offer.nativeAd.isExpressAd), (long)offer.networkFirmID, offer.nativeAd.nativeExpressAdViewWidth, offer.nativeAd.nativeExpressAdViewHeight);
  
  BOOL isExpressAd = offer.nativeAd.isExpressAd;
  ATNativeSelfRenderView *selfRenderView = nil;
  if (!isExpressAd) {
    selfRenderView = [self getSelfRenderViewOffer:offer];
    [selfRenderView setUIWidget:extraDic];
  }
  
  ATNativeADView *adView = [self getNativeADView:config offer:offer selfRenderView:selfRenderView withPlacementId:placementID extraDic:extraDic];
  if (adView == nil) {
    return;
  }
  
  if (!isExpressAd) {
    [self prepareWithNativePrepareInfo:selfRenderView nativeADView:adView];
  }
  
  [offer rendererWithConfiguration:config selfRenderView:(isExpressAd ? nil : selfRenderView) nativeADView:adView];
  if (!isExpressAd) {
    adView.logoImageView.hidden = selfRenderView.isHiddenLogo;
  }
  
  [self removeNativeAdView:placementID];
  self.nativeViewDic[placementID] = adView;
  [self addNativeView:extraDic placementID:placementID];
}


- (void)prepareWithNativePrepareInfo:(ATNativeSelfRenderView *)selfRenderView nativeADView:(ATNativeADView *)nativeADView{
  ATLog(@"Native:ATNativeManager::prepareWithNativePrepareInfo:nativeADView:selfRenderView:%@--nativeADView:%@", selfRenderView, nativeADView);
  
  ATNativePrepareInfo *info = [ATNativePrepareInfo loadPrepareInfo:^(ATNativePrepareInfo * _Nonnull prepareInfo) {
    prepareInfo.textLabel = selfRenderView.textLabel;
    prepareInfo.advertiserLabel = selfRenderView.advertiserLabel;
    prepareInfo.titleLabel = selfRenderView.titleLabel;
    prepareInfo.ratingLabel = selfRenderView.ratingLabel;
    prepareInfo.iconImageView = selfRenderView.iconImageView;
    prepareInfo.mainImageView = selfRenderView.mainImageView;
    prepareInfo.dislikeButton = selfRenderView.dislikeButton;
    prepareInfo.ctaLabel = selfRenderView.ctaLabel;
    prepareInfo.mediaView = selfRenderView.mediaView;
  }];
  
  [nativeADView prepareWithNativePrepareInfo:info];
}

/// 移除原生广告
- (void)removeNative:(NSString *)placementID {
  ATLog(@"Native:ATNativeManager::removeNative:placementID:%@", placementID);
  [self removeNativeAdView:placementID];
}

- (void)destroyNativeAd:(NSString *)placementID adId:(NSString *)adId {
  ATLog(@"Native:ATNativeManager::destroyNativeAd:placementID:%@ adId:%@", placementID, adId);
  if (kATStringIsEmpty(placementID)) {
    return;
  }
  [self removeNativeAdView:placementID];
  if (adId.length > 0) {
    return;
  }
  NSString *scene = [ATNativeTool scenarioIdForPlacementID:placementID];
  NSString *ext = [ATNativeTool showCustomExtForPlacementID:placementID];
  ATShowConfig *cfg = [[ATShowConfig alloc] initWithScene:(scene ?: @"") showCustomExt:(ext ?: @"")];
  while ([[ATAdManager sharedManager] nativeAdReadyForPlacementID:placementID]) {
    ATNativeAdOffer *offer = [[ATAdManager sharedManager] getNativeAdOfferWithPlacementID:placementID
                                                                              showConfig:cfg];
    if (offer == nil) {
      break;
    }
    ATNativeADConfiguration *config = [self getATNativeADConfiguration:@{} isAdaptiveHeight:YES];
    ATNativeADView *adView = [[ATNativeADView alloc] initWithConfiguration:config
                                                              currentOffer:offer
                                                               placementID:placementID];
    [self destroyNativeAdViewIfNeeded:adView];
  }
  [ATNativeTool clearPlacementState:placementID];
}  

/// 获取广告位的状态
- (NSDictionary *)checkNativeLoadStatus:(NSString *)placementID {
  ATLog(@"Native:ATNativeManager::checkNativeLoadStatus:placementID:%@", placementID);
  if (kATStringIsEmpty(placementID)) {
      return [NSDictionary dictionary];
  }
  
  ATCheckLoadModel *checkLoadModel = [[ATAdManager sharedManager] checkNativeLoadStatusForPlacementID:placementID];

  NSDictionary *dic = [ATCommonTool objectToJSONString:checkLoadModel];
  return  dic;
}

#pragma mark - ATNativeADDelegate
// 广告加载失败
- (void)didFailToLoadADWithPlacementID:(NSString *)placementID error:(NSError *)error {
  ATLog(@"Native:ATNativeManager::didFailToLoadADWithPlacementID:placementID:%@--error:%@", placementID, error);
  
  if (kATStringIsEmpty(placementID)) {
    return;
  }
  NSMutableDictionary *dic = [ATDisposeDataTool revampFailCallDic:NativeAdFailToLoadAD placementID:placementID extraDic:nil error:error];
  [SendEventManager sendMethod: NativeCallName  arguments:dic result:nil];
}

// 广告加载成功
- (void)didFinishLoadingADWithPlacementID:(NSString *)placementID {
  ATLog(@"Native:ATNativeManager::didFinishLoadingADWithPlacementID:%@", placementID);
  
  if (kATStringIsEmpty(placementID)) {
    return;
  }
  
  NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic: NativeAdDidFinishLoading placementID:placementID extraDic:nil];
  [SendEventManager sendMethod:NativeCallName arguments:dic result:nil];
}

// 广告点击
- (void)didClickNativeAdInAdView:(ATNativeADView *)adView placementID:(NSString *)placementID extra:(NSDictionary *)extra {
  ATLog(@"Native:ATNativeManager::didClickNativeAdInAdView:placementID:%@--extra:%@--adView:%@", placementID, extra, adView);
  
  if (kATStringIsEmpty(placementID)) {
    return;
  }
  
  NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAddidClick placementID:placementID extraDic:extra];
  
  [SendEventManager sendMethod: NativeCallName  arguments:dic result:nil];
  
}

// 广告点击跳转是否为Deeplink形式，目前只针对TopOn Adx的广告返回
- (void)didDeepLinkOrJumpInAdView:(ATNativeADView *)adView placementID:(NSString *)placementID extra:(NSDictionary *)extra result:(BOOL)success {
  ATLog(@"Native:ATNativeManager::didDeepLinkOrJumpInAdView:placementID:%@--extra:%@--result:%@--adView:%@", placementID, extra, @(success), adView);
  
  if (kATStringIsEmpty(placementID)) {
    return;
  }
  
  NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAdDidDeepLink placementID:placementID extraDic:extra];
  
  [SendEventManager sendMethod: NativeCallName  arguments:dic result:nil];
}

// 广告视频结束播放，部分广告平台有此回调
- (void)didEndPlayingVideoInAdView:(ATNativeADView *)adView placementID:(NSString *)placementID extra:(NSDictionary *)extra {
  ATLog(@"Native:ATNativeManager::didEndPlayingVideoInAdView:placementID:%@--extra:%@--adView:%@", placementID, extra, adView);
  
  if (kATStringIsEmpty(placementID)) {
    return;
  }
  
  NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAddidEndPlayingVideo placementID:placementID extraDic:extra];
  
  [SendEventManager sendMethod: NativeCallName  arguments:dic result:nil];
}

// 广告进入全屏播放
- (void)didEnterFullScreenVideoInAdView:(ATNativeADView *)adView placementID:(NSString *)placementID extra:(NSDictionary *)extra {
  ATLog(@"Native:ATNativeManager::didEnterFullScreenVideoInAdView:placementID:%@--extra:%@--adView:%@", placementID, extra, adView);
  
  if (kATStringIsEmpty(placementID)) {
    return;
  }
  
  NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAdEnterFullScreenVideo placementID:placementID extraDic:extra];
  
  [SendEventManager sendMethod: NativeCallName  arguments:dic result:nil];
}

// 离开全屏播放
- (void)didExitFullScreenVideoInAdView:(ATNativeADView *)adView placementID:(NSString *)placementID extra:(NSDictionary *)extra {
  ATLog(@"Native:ATNativeManager::didExitFullScreenVideoInAdView:placementID:%@--extra:%@--adView:%@", placementID, extra, adView);
  
  if (kATStringIsEmpty(placementID)) {
    return;
  }
  
  NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAdExitFullScreenVideoInAd placementID:placementID extraDic:extra];
  
  [SendEventManager sendMethod: NativeCallName arguments:dic result:nil];
}

// 广告展示成功
- (void)didShowNativeAdInAdView:(ATNativeADView *)adView placementID:(NSString *)placementID extra:(NSDictionary *)extra {
  ATLog(@"Native:ATNativeManager::didShowNativeAdInAdView:placementID:%@--extra:%@--adView:%@", placementID, extra, adView);
  
  if (kATStringIsEmpty(placementID)) {
    return;
  }
  
  NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAddidShowNativeAd placementID:placementID extraDic:extra];
  
  [SendEventManager sendMethod: NativeCallName arguments:dic result:nil];
  [SendEventManager sendMethod:CommonADCallName arguments:[ATDisposeDataTool revampSucceedCallDic:AdShowRevenueCallbackKey placementID:placementID extraDic:extra] result:nil];
}

// 广告视频开始播放，部分广告平台有此回调
- (void)didStartPlayingVideoInAdView:(ATNativeADView *)adView placementID:(NSString *)placementID extra:(NSDictionary *)extra {
  ATLog(@"Native:ATNativeManager::didStartPlayingVideoInAdView:placementID:%@--extra:%@--adView:%@", placementID, extra, adView);
  
  if (kATStringIsEmpty(placementID)) {
    return;
  }
  
  NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAddidStartPlayingVideo placementID:placementID extraDic:extra];
  
  [SendEventManager sendMethod: NativeCallName arguments:dic result:nil];
}

// 广告关闭按钮被点击，部分广告平台有此回调
- (void)didTapCloseButtonInAdView:(ATNativeADView *)adView placementID:(NSString *)placementID extra:(NSDictionary *)extra {
  ATLog(@"Native:ATNativeManager::didTapCloseButtonInAdView:placementID:%@--extra:%@--adView:%@", placementID, extra, adView);
  
  if (kATStringIsEmpty(placementID)) {
    return;
  }
  
  NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAddidTapCloseButton placementID:placementID extraDic:extra];
  
  [SendEventManager sendMethod: NativeCallName  arguments:dic result:nil];
  [self destroyFabricNativeADView:adView];
  [self removeNativeAdView:placementID];
}

- (void)didLoadSuccessDrawWith:(NSArray *)views placementID:(NSString *)placementID extra:(NSDictionary *)extra {
  ATLog(@"Native:ATNativeManager::didLoadSuccessDrawWith:placementID:%@--extra:%@--views:%@", placementID, extra, views);
  
  if (kATStringIsEmpty(placementID)) {
    return;
  }
  
  NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAddidLoadSuccessDraw placementID:placementID extraDic:extra];
  [SendEventManager sendMethod: NativeCallName  arguments:dic result:nil];
}

- (void)didCloseDetailInAdView:(ATNativeADView *)adView placementID:(NSString *)placementID extra:(NSDictionary *)extra {
  ATLog(@"Native:ATNativeManager::didCloseDetailInAdView:placementID:%@--extra:%@--adView:%@", placementID, extra, adView);
  
  if (kATStringIsEmpty(placementID)) {
    return;
  }
  
  NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAdDidCloseDetailInAdView placementID:placementID extraDic:extra];
  
  [SendEventManager sendMethod: NativeCallName  arguments:dic result:nil];
  
}

#pragma mark - private
- (void)removeNativeAdView:(NSString *)placementID{
  ATLog(@"Native:ATNativeManager::removeNativeAdView:placementID:%@", placementID);
  
  if (kATStringIsEmpty(placementID)) {
    return;
  }
  
  if ([self.nativeViewDic.allKeys containsObject:placementID]) {
    ATNativeADView *adView = self.nativeViewDic[placementID];
    [self destroyNativeAdViewIfNeeded:adView];
    [adView removeFromSuperview];
    [self.nativeViewDic removeObjectForKey:placementID];
    adView = nil;
    [self.container removeFromSuperview];
    self.container = nil;
  }
}

- (ATNativeADConfiguration *)getATNativeADConfiguration:(NSDictionary *)extraDic isAdaptiveHeight:(BOOL)isAdaptiveHeight {
  ATLog(@"Native:ATNativeManager::getATNativeADConfiguration:isAdaptiveHeight:extraDic:%@--isAdaptiveHeight:%@", extraDic, @(isAdaptiveHeight));
  ATNativeADConfiguration *config = [ATNativeTool getATNativeADConfiguration:extraDic isAdaptiveHeight:isAdaptiveHeight];
  config.delegate = self; 
  config.context = @{
    kATNativeAdConfigurationContextAdOptionsViewFrameKey:[NSValue valueWithCGRect:CGRectMake(CGRectGetWidth([UIScreen mainScreen].bounds) - 43.0f, .0f, 43.0f, 18.0f)],
    kATNativeAdConfigurationContextAdLogoViewFrameKey:[NSValue valueWithCGRect:CGRectMake(.0f, .0f, 54.0f, 18.0f)],
    kATNativeAdConfigurationContextNetworkLogoViewFrameKey:[NSValue valueWithCGRect:CGRectMake(CGRectGetWidth(config.ADFrame) - 54.0f, CGRectGetHeight(config.ADFrame) - 18.0f, 54.0f, 18.0f)]
  };
  ATLog(@"Native:ATNativeManager::getATNativeADConfiguration:isAdaptiveHeight:ADFrame:%@", NSStringFromCGRect(config.ADFrame));
  return  config;
}

- (void)addNativeView:(NSDictionary *)extraDic placementID:(NSString *)placementID {
  ATLog(@"Native:ATNativeManager::addNativeView:placementID:placementID:%@--extraDic:%@", placementID, extraDic);
  
  if (kATStringIsEmpty(placementID)) {
    return;
  }
  
  UIView *tempView = [ATCommonTool getRootViewController].view;
  
  if ([self.nativeViewDic.allKeys containsObject:placementID]) {
    
    ATNativeADView *adView = self.nativeViewDic[placementID];
    [tempView addSubview:adView];
    
    //[tempView addSubview:adView];
  }else {
    ATLog(@"Native:ATNativeManager::addNativeView:placementID:retrive ad view failed placementID:%@", placementID);
  }
}

/// 统计场景到达率
- (void)entryScenarioWithPlacementID:(NSString *)placementID sceneID:(NSString *)sceneID {
  ATLog(@"Native:ATNativeManager::entryScenarioWithPlacementID:sceneID:placementID:%@--sceneID:%@", placementID, sceneID);
  
  if (kATStringIsEmpty(placementID)) {
    return;
  }
  
  [[ATAdManager sharedManager] entryNativeScenarioWithPlacementID:placementID scene:sceneID];
}

#pragma mark - 广告源打印
- (void)didStartLoadingADSourceWithPlacementID:(NSString *)placementID extra:(NSDictionary*)extra {
  ATLog(@"ADSource--AD--Start--ATNativeManager::didStartLoadingADSourceWithPlacementID:%@---extra:%@", placementID,extra);
  
  NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAdDidAdSourceAttempt placementID:placementID extraDic:extra];
  [SendEventManager sendMethod: NativeCallName arguments:dic result:nil];
}

- (void)didFinishLoadingADSourceWithPlacementID:(NSString *)placementID extra:(NSDictionary*)extra {
  ATLog(@"ADSource--AD--Finish--ATNativeManager::didFinishLoadingADSourceWithPlacementID:%@---extra:%@", placementID,extra);
  NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAdDidAdSourceLoadFilled placementID:placementID extraDic:extra];
  [SendEventManager sendMethod: NativeCallName arguments:dic result:nil];
}

- (void)didFailToLoadADSourceWithPlacementID:(NSString*)placementID extra:(NSDictionary*)extra error:(NSError*)error {
  ATLog(@"ADSource--AD--Fail--ATNativeManager::didFailToLoadADSourceWithPlacementID:%@---error:%@", placementID,error);
  NSMutableDictionary *dic = [ATDisposeDataTool revampFailCallDic:NativeAdDidAdSourceLoadFail placementID:placementID extraDic:extra error:error];
  [SendEventManager sendMethod: NativeCallName arguments:dic result:nil];
}

#pragma mark - 广告源 级别回调 - 竞价
- (void)didStartBiddingADSourceWithPlacementID:(NSString *)placementID extra:(NSDictionary*)extra {
  ATLog(@"ADSource--bid--Start--ATNativeManager::didStartBiddingADSourceWithPlacementID:%@---extra:%@", placementID,extra);
  NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAdDidAdSourceBiddingAttempt placementID:placementID extraDic:extra];
  [SendEventManager sendMethod: NativeCallName arguments:dic result:nil];
}

- (void)didFinishBiddingADSourceWithPlacementID:(NSString *)placementID extra:(NSDictionary*)extra {
  ATLog(@"ADSource--bid--Finish--ATNativeManager::didFinishBiddingADSourceWithPlacementID:%@--extra:%@", placementID,extra);
  NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAdDidAdSourceBiddingFilled placementID:placementID extraDic:extra];
  [SendEventManager sendMethod: NativeCallName arguments:dic result:nil];
}

- (void)didFailBiddingADSourceWithPlacementID:(NSString*)placementID extra:(NSDictionary*)extra error:(NSError*)error {
  ATLog(@"ADSource--bid--Fail--ATNativeManager::didFailBiddingADSourceWithPlacementID:%@--extra:%@--error:%@", placementID, extra, error);
  NSMutableDictionary *dic = [ATDisposeDataTool revampFailCallDic:NativeAdDidAdSourceBiddingFail placementID:placementID extraDic:extra error:error];
  [SendEventManager sendMethod: NativeCallName arguments:dic result:nil];
}

#pragma mark - ATAdMultipleLoadingDelegate
- (void)didFinishMultipleLoadingADWithPlacementID:(NSString *)placementID
                                   requestingInfo:(ATAdRequestingInfo *)requestingInfo {
  ATLog(@"Native ads multiple loaded:ATNativeManager::didFinishMultipleLoadingADWithPlacementID:%@--requestingInfo:%@", placementID, requestingInfo);
  
  NSMutableDictionary *requestingInfoDic = [NSMutableDictionary dictionary];
  if (requestingInfo.biddingAdInfoArrray != nil && requestingInfo.biddingAdInfoArrray.count != 0) {
    [requestingInfoDic setValue:requestingInfo.biddingAdInfoArrray forKey:MultipleLoadBiddingAttemptKey];
  }
  if (requestingInfo.loadingAdInfoArrray != nil && requestingInfo.loadingAdInfoArrray.count != 0) {
    [requestingInfoDic setValue:requestingInfo.loadingAdInfoArrray forKey:MultipleLoadLoadingKey];
  }
  
  NSMutableDictionary *dic = [ATDisposeDataTool revampSucceedCallDic:NativeAdDidMultipleLoaded placementID:placementID extraDic:requestingInfoDic];
  [SendEventManager sendMethod: NativeCallName arguments:dic result:nil];
}

#pragma mark - lazy
- (NSMutableDictionary *)nativeViewDic {
  
  if (_nativeViewDic) return _nativeViewDic;
  
  NSMutableDictionary *nativeViewDic = [NSMutableDictionary new];
  
  return _nativeViewDic = nativeViewDic;
}
 
/// 广告是否准备好
- (BOOL)hasNativeAdReady:(NSString *)placementID {
    ATLog(@"Native:ATNativeManager::hasNativeAdReady:placementID:%@", placementID);
    
    if (kATStringIsEmpty(placementID)) {
        return NO;
    }
    
    BOOL isReady = [[ATAdManager sharedManager] nativeAdReadyForPlacementID:placementID];
    ATLog(@"Native:ATNativeManager::hasNativeAdReady:placementID:%@--isReady:%@", placementID, @(isReady));
    return  isReady;
}

@end
