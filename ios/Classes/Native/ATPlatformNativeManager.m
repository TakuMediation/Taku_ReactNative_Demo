//
//  ATPlatformNativeManager.m
//  anythink_sdk
//
//  Created by GUO PENG on 2021/7/12.
//

#import "ATPlatformNativeManager.h"
#import <AnyThinkSDK/AnyThinkSDK.h>
#import "ATNativeDelegate.h"
#import "ATNativeTool.h"
#import "ATDisposeDataTool.h"
#import "ATCommonTool.h"
#import "ATConfiguration.h"


@interface ATPlatformNativeManager()

@property(nonatomic, strong) ATNativeDelegate *nativeDelegate;


@end


@implementation ATPlatformNativeManager

#pragma mark - public

/// 同步取已加载好的 SDK Native UIView 加进 hostView。
/// 渲染路径参考 Flutter `ATFNativePlatformView.m`，简化版只处理模板（Express）；
/// 自渲染（self-render）模式涉及 RN 元素 viewTag → setTitleView 绑定，留待与 Android NativePrepareBinder 同模型时补齐。
- (BOOL)attachNativeToHostView:(UIView *)hostView
                   placementID:(NSString *)placementID
                       sceneID:(NSString *)sceneID {
    if (hostView == nil || placementID.length == 0) {
        return NO;
    }
    ATShowConfig *cfg = [[ATShowConfig alloc] initWithScene:(sceneID ?: @"") showCustomExt:@""];
    ATNativeAdOffer *offer = [[ATAdManager sharedManager] getNativeAdOfferWithPlacementID:placementID showConfig:cfg];
    if (offer == nil) {
        ATLog(@"ATPlatformNativeManager.attachNativeToHostView offer=nil (not loaded yet), placementID=%@",
              placementID);
        return NO;
    }
    if (!offer.nativeAd.isExpressAd) {
        ATLog(@"ATPlatformNativeManager.attachNativeToHostView self-render mode 暂未实现（viewTag binding 待接），placementID=%@",
              placementID);
        return NO;
    }
    ATNativeADConfiguration *config = [ATNativeTool getATNativeADConfiguration:@{} isAdaptiveHeight:YES];
    config.delegate = self.nativeDelegate;
    ATNativeADView *adView = [[ATNativeADView alloc] initWithConfiguration:config currentOffer:offer placementID:placementID];
    [offer rendererWithConfiguration:config selfRenderView:nil nativeADView:adView];
    adView.frame = hostView.bounds;
    adView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    [hostView addSubview:adView];
    ATLog(@"ATPlatformNativeManager.attachNativeToHostView attached placementID=%@ adView=%@ isExpress=%d",
          placementID, adView, offer.nativeAd.isExpressAd);
    return YES;
}

/// 加载原生广告
- (void)loadNativeWith:(NSString *)placementID extraDic:(NSDictionary *)extraDic{
    [[ATAdManager sharedManager] setMultipleLoadingDelegate:self.nativeDelegate placementId:placementID];
  
    ATNativeAttributeMode *parentMode = [ATDisposeDataTool disposeNativeData:extraDic keyStr:NativeSize];
    BOOL isAdaptiveHeight = NO;
    if ([extraDic[IsAdaptiveHeight] respondsToSelector:@selector(boolValue)]) {
        isAdaptiveHeight = [extraDic[IsAdaptiveHeight] boolValue];
    }
    NSDictionary *mediaViewFrameDic = [extraDic[MediaViewFrame] isKindOfClass:[NSDictionary class]] ? extraDic[MediaViewFrame] : nil;
    ATLog(@"原生广告--加载的大小-2--%@",NSStringFromCGSize(CGSizeMake(parentMode.width, parentMode.height)));
    NSMutableDictionary *loadExtra = [@{
        kATExtraInfoNativeAdSizeKey:[NSValue valueWithCGSize:CGSizeMake(parentMode.width, parentMode.height)],
        kATNativeAdSizeToFitKey:@(isAdaptiveHeight),
    } mutableCopy];
    if (mediaViewFrameDic.count > 0) {
        ATNativeAttributeMode *mediaViewFrameMode = [ATDisposeDataTool disposeNativeData:extraDic keyStr:MediaViewFrame];
        loadExtra[kATExtraInfoMediaViewFrameKey] = [NSValue valueWithCGRect:CGRectMake(mediaViewFrameMode.x, mediaViewFrameMode.y, mediaViewFrameMode.width, mediaViewFrameMode.height)];
    }
    [[ATAdManager sharedManager] loadADWithPlacementID:placementID extra:loadExtra delegate:self.nativeDelegate];
}


#pragma mark - lazy
- (ATNativeDelegate *)nativeDelegate {

    if (_nativeDelegate) return _nativeDelegate;

    ATNativeDelegate *nativeDelegate = [ATNativeDelegate new];

    return _nativeDelegate = nativeDelegate;
}

@end
